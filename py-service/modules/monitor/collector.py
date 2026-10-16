# -*- coding: utf-8 -*-
"""系统监控数据采集器（纯函数，返回普通 dict，绝不向调用方抛异常）。

覆盖：
  • system_snapshot()     —— CPU / 内存 / 提交内存(commit) / 交换 / 磁盘 / 网络 / GPU / 主机 / 告警
  • services_snapshot()   —— Java/Node/Python/Vite 进程与端口存活
  • middleware_snapshot() —— MySQL / Redis / Nacos
  • security_snapshot()   —— 网关 IP 黑名单 + 限流桶 + 日志事件
  • events_snapshot()     —— JVM 崩溃转储 / 看门狗 / 滚动重启日志
  • overview_snapshot()   —— 上述 system+services+middleware 的合并轮询

设计要点：
  • 子进程型探针（commit / gpu）用模块级线程安全 TTL 缓存（~4s），
    避免 5s 一次的前端轮询反复拉起 powershell。
  • 静态主机信息（cpu_model / os）永久缓存。
  • 文件读取一律 seek 到尾部并按需读取，绝不整文件读入（本机内存紧张）。
  • 所有探针各自 try/except，失败即返回空值/None。
"""
from __future__ import annotations

import glob as _glob
import json
import os
import platform
import re
import shutil
import socket
import subprocess
import threading
import time
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional

import psutil

from .redis_client import RespClient

# ── 路径与环境 ────────────────────────────────────────────────
_REPO_ROOT = Path(__file__).resolve().parents[3]          # web3/
_GATEWAY_LOG_DIR = _REPO_ROOT / "backend" / "gateway" / "target" / "logs"
_HS_ERR_GLOB = str(_REPO_ROOT / "backend" / "*" / "target" / "hs_err_pid*.log")
_TEMP_DIR = Path(os.environ.get("TEMP") or os.environ.get("TMP") or "/tmp")

# 复用框架的 .env 机制（失败时退回环境变量 + 与 holdings.py 一致的默认值）
try:  # noqa: BLE001
    from core.config import load_dotenv as _load_dotenv
    _load_dotenv()
except Exception:  # noqa: BLE001
    pass

MiB = 1024.0 * 1024.0
GiB = 1024.0 * 1024.0 * 1024.0

# ── 进程对象持久缓存：复用同一 psutil.Process 才能拿到真实 cpu_percent ──
_PROC_CACHE: Dict[int, psutil.Process] = {}
_NET_PREV: Optional[tuple] = None

# 只对候选进程读取 cmdline/memory_info（Windows 上全量读取极慢）
_CANDIDATE_NAMES = {
    "java.exe", "javaw.exe", "node.exe", "python.exe", "pythonw.exe",
    "mysqld.exe", "redis-server.exe",
}

# 首帧 cpu_percent 预热（否则第一次读数为 0.0）
try:  # noqa: BLE001
    psutil.cpu_percent(interval=None)
    psutil.cpu_percent(interval=None, percpu=True)
except Exception:  # noqa: BLE001
    pass


# ============================================================
# 通用工具
# ============================================================
class _TTLCache:
    """线程安全的 TTL 缓存（stale-while-revalidate）。

    有旧值时立即返回旧值，过期后在后台线程刷新（同一时刻只允许一个刷新），
    因此除首次预热外，请求永远不会被慢探针阻塞；无旧值时才同步执行 producer。
    producer 自身需保证不抛异常。
    """

    def __init__(self, ttl: float):
        self.ttl = ttl
        self._lock = threading.Lock()
        self._val: Any = None
        self._ts = 0.0
        self._refreshing = False

    def _refresh(self, producer) -> Any:
        try:
            result = producer()
        except Exception:  # noqa: BLE001
            result = None
        with self._lock:
            if result is not None:
                self._val = result
                self._ts = time.time()
            self._refreshing = False
        return result

    def get(self, producer) -> Any:
        now = time.time()
        with self._lock:
            fresh = self._val is not None and (now - self._ts) < self.ttl
            if fresh:
                return self._val
            have_old = self._val is not None
            if have_old:
                if not self._refreshing:
                    self._refreshing = True
                    threading.Thread(
                        target=self._refresh, args=(producer,), daemon=True,
                    ).start()
                return self._val
        # 首次预热：同步执行，保证后续请求都有缓存可用
        return self._refresh(producer)


# 进程快照共享缓存（见 _procs_cached）
_PROCS_CACHE = _TTLCache(1.0)


def _powershell() -> str:
    return shutil.which("powershell") or shutil.which("pwsh") or "powershell"


def _run_ps(script: str, timeout: float = 12.0) -> Optional[str]:
    """执行一段 PowerShell 脚本，返回 stdout 文本；失败返回 None。"""
    cmd = [
        _powershell(), "-NoProfile", "-NonInteractive",
        "-ExecutionPolicy", "Bypass", "-Command", script,
    ]
    flags = 0
    if os.name == "nt":
        flags = getattr(subprocess, "CREATE_NO_WINDOW", 0x08000000)
    try:
        proc = subprocess.run(
            cmd, capture_output=True, timeout=timeout, creationflags=flags,
        )
    except Exception:  # noqa: BLE001
        return None
    if proc.returncode != 0 and not proc.stdout:
        return None
    out = proc.stdout
    if isinstance(out, bytes):
        out = out.decode("utf-8", "replace")
    # 去掉可能的 BOM
    return out.lstrip("\ufeff").strip()


def _run_ps_json(script: str, timeout: float = 12.0) -> Optional[dict]:
    text = _run_ps(script, timeout=timeout)
    if not text:
        return None
    try:
        return json.loads(text)
    except Exception:  # noqa: BLE001
        return None


def _env(name: str, default: str = "") -> str:
    return (os.environ.get(name) or default).strip()


def _tcp_up(port: int, host: str = "127.0.0.1", timeout: float = 0.3) -> bool:
    try:
        with socket.create_connection((host, int(port)), timeout=timeout):
            return True
    except OSError:
        return False


def _scan_processes() -> Dict[int, dict]:
    """一次性快照"关注"进程（服务 + 中间件）。

    关键性能约束：Windows 上取 cmdline/memory_info 对全量 ~300 进程相当昂贵
    （约 6s）。因此先用廉价的 name 过滤，仅对候选进程（java/node/python/
    mysqld/redis-server）读取完整信息，把耗时压到亚秒级。
    使用持久 Process 对象以获得真实 cpu_percent。
    """
    result: Dict[int, dict] = {}
    alive = set()
    for proc in psutil.process_iter(["pid", "name"]):
        try:
            name = proc.info.get("name") or ""
        except Exception:  # noqa: BLE001
            continue
        if name.lower() not in _CANDIDATE_NAMES:
            continue
        pid = proc.pid
        cp = _PROC_CACHE.get(pid)
        if cp is None:
            try:
                cp = psutil.Process(pid)
                cp.cpu_percent(None)          # 预热
            except Exception:  # noqa: BLE001
                continue
            _PROC_CACHE[pid] = cp
        alive.add(pid)
        try:
            info = cp.as_dict(attrs=["name", "cmdline", "create_time", "memory_info", "num_threads"])
            cpu = cp.cpu_percent(None)
        except Exception:  # noqa: BLE001
            _PROC_CACHE.pop(pid, None)
            continue
        cmdline = info.get("cmdline") or []
        cmd = " ".join(cmdline)
        meminfo = info.get("memory_info")
        result[pid] = {
            "pid": pid,
            "name": info.get("name") or name,
            "cmd": cmd,
            "cmd_norm": cmd.replace("\\", "/").lower(),
            "create": info.get("create_time") or 0.0,
            "mem_mb": (meminfo.rss / 1e6) if meminfo else 0.0,
            "threads": info.get("num_threads"),
            "cpu": float(cpu or 0.0),
        }
    # 清理已退出进程
    for pid in list(_PROC_CACHE):
        if pid not in alive:
            _PROC_CACHE.pop(pid, None)
    return result


def _procs_cached() -> Dict[int, dict]:
    """带 ~1s 共享缓存的进程快照。

    同一次 overview 中 services 与 middleware 会各调用一次，共享一份结果即可；
    1s TTL 短于前端 5s 轮询间隔，仍能保证 cpu_percent 是真实增量。
    """
    return _PROCS_CACHE.get(_scan_processes) or {}


def _best_proc(procs: Dict[int, dict], predicate) -> Optional[dict]:
    """返回满足条件、内存占用最大的进程快照。"""
    best = None
    for p in procs.values():
        try:
            if predicate(p) and (best is None or p["mem_mb"] > best["mem_mb"]):
                best = p
        except Exception:  # noqa: BLE001
            continue
    return best


def _decode_bytes(data: bytes) -> str:
    """稳健解码：兼容 UTF-8 / UTF-16（部分脚本日志为 UTF-16LE）。"""
    if data.startswith(b"\xff\xfe"):
        return data[2:].decode("utf-16-le", "replace")
    if data.startswith(b"\xfe\xff"):
        return data[2:].decode("utf-16-be", "replace")
    if data.count(b"\x00") > max(4, len(data) // 4):
        return data.decode("utf-16-le", "replace")
    return data.decode("utf-8", "replace")


def _tail_lines(path: Path, max_bytes: int, max_lines: int) -> List[str]:
    """读取文件末尾最多 max_bytes 字节，返回最后 max_lines 行。"""
    try:
        if not path.is_file():
            return []
        size = path.stat().st_size
        with path.open("rb") as fh:
            if size > max_bytes:
                fh.seek(size - max_bytes)
                # 丢弃可能被截断的首个不完整行
                head = fh.readline()
            data = fh.read(max_bytes)
        text = _decode_bytes(data)
        lines = text.splitlines()
        if size > max_bytes and lines:
            lines = lines[1:] if len(lines) > 1 else lines
        return [ln.rstrip() for ln in lines[-max_lines:]]
    except Exception:  # noqa: BLE001
        return []


# ============================================================
# system —— 子进程探针（TTL 缓存）
# ============================================================
_COMMIT_CACHE = _TTLCache(4.0)
_GPU_CACHE = _TTLCache(4.0)
_HOST_STATIC: Optional[dict] = None

_PS_COMMIT = (
    "[Console]::OutputEncoding=[System.Text.Encoding]::UTF8;" 
    "$m=Get-CimInstance Win32_PerfFormattedData_PerfOS_Memory;"
    "$p=@(Get-CimInstance Win32_PageFileUsage);"
    "[pscustomobject]@{committed=[double]$m.CommittedBytes;limit=[double]$m.CommitLimit;"
    "pages=@($p | ForEach-Object {[pscustomobject]@{name=[string]$_.Name;"
    "allocated_mb=[int]$_.AllocatedBaseSize;peak_mb=[int]$_.PeakUsage;"
    "current_mb=[int]$_.CurrentUsage}})} | ConvertTo-Json -Compress -Depth 5"
)

_PS_GPU = (
    "[Console]::OutputEncoding=[System.Text.Encoding]::UTF8;"
    "$name='';$ram=0;"
    "try{$v=Get-CimInstance Win32_VideoController | Select-Object -First 1;"
    "$name=[string]$v.Name;$ram=[double]$v.AdapterRAM}catch{};"
    "$util=$null;try{$c=Get-Counter '\\GPU Engine(*engtype_3D)\\Utilization Percentage' "
    "-ErrorAction Stop;$util=($c.CounterSamples | Measure-Object -Property CookedValue -Sum).Sum}catch{};"
    "$shared=$null;try{$a=Get-Counter '\\GPU Adapter Memory(*)\\Shared Usage' "
    "-ErrorAction Stop;$shared=($a.CounterSamples | Measure-Object -Property CookedValue -Sum).Sum}catch{};"
    "[pscustomobject]@{name=$name;ram=$ram;util=$util;shared=$shared} | ConvertTo-Json -Compress"
)

_PS_CPU = (
    "$n=(Get-CimInstance Win32_Processor | Select-Object -First 1).Name;"
    "[pscustomobject]@{name=[string]$n} | ConvertTo-Json -Compress"
)


def _probe_commit() -> Optional[dict]:
    return _run_ps_json(_PS_COMMIT, timeout=10.0)


def _probe_gpu() -> Optional[dict]:
    return _run_ps_json(_PS_GPU, timeout=14.0)


def _probe_host() -> dict:
    """静态主机信息（永久缓存）。"""
    cpu_model = ""
    d = _run_ps_json(_PS_CPU, timeout=8.0)
    if d:
        cpu_model = str(d.get("name") or "")
    if not cpu_model:
        cpu_model = platform.processor() or ""
    return {
        "os": f"{platform.system()} {platform.release()}",
        "cpu_model": cpu_model,
    }


def _host() -> dict:
    global _HOST_STATIC
    if _HOST_STATIC is None:
        try:
            _HOST_STATIC = _probe_host()
        except Exception:  # noqa: BLE001
            _HOST_STATIC = {"os": platform.system(), "cpu_model": platform.processor() or ""}
    try:
        boot = int(psutil.boot_time())
    except Exception:  # noqa: BLE001
        boot = 0
    uptime_hours = round((time.time() - boot) / 3600.0, 2) if boot else 0.0
    return {
        "os": _HOST_STATIC.get("os", ""),
        "platform": platform.platform(),
        "boot_time": boot,
        "uptime_hours": uptime_hours,
        "cpu_model": _HOST_STATIC.get("cpu_model", ""),
    }


def _cpu() -> dict:
    try:
        percent = float(psutil.cpu_percent(interval=None))
    except Exception:  # noqa: BLE001
        percent = 0.0
    try:
        per_core = [float(x) for x in psutil.cpu_percent(interval=None, percpu=True)]
    except Exception:  # noqa: BLE001
        per_core = []
    try:
        count = int(psutil.cpu_count(logical=True) or 0)
    except Exception:  # noqa: BLE001
        count = 0
    try:
        freq = psutil.cpu_freq()
        freq_mhz = float(freq.current) if freq else 0.0
    except Exception:  # noqa: BLE001
        freq_mhz = 0.0
    load_avg: Optional[List[float]] = None
    if os.name != "nt":
        try:
            load_avg = [float(x) for x in psutil.getloadavg()]
        except Exception:  # noqa: BLE001
            load_avg = None
    return {
        "percent": round(percent, 1),
        "per_core": [round(x, 1) for x in per_core],
        "count": count,
        "freq_mhz": round(freq_mhz, 1),
        "load_avg": load_avg,
    }


def _memory() -> dict:
    try:
        vm = psutil.virtual_memory()
    except Exception:  # noqa: BLE001
        return {"total_mb": 0.0, "used_mb": 0.0, "available_mb": 0.0, "percent": 0.0}
    return {
        "total_mb": round(vm.total / MiB, 1),
        "used_mb": round(vm.used / MiB, 1),
        "available_mb": round(vm.available / MiB, 1),
        "percent": round(float(vm.percent), 1),
    }


def _commit() -> dict:
    data = _COMMIT_CACHE.get(_probe_commit)
    empty = {"limit_gb": 0.0, "committed_gb": 0.0, "headroom_gb": 0.0, "percent": 0.0, "pagefile": []}
    if not data:
        return empty
    try:
        limit = float(data.get("limit") or 0.0)
        committed = float(data.get("committed") or 0.0)
    except Exception:  # noqa: BLE001
        return empty
    pages_raw = data.get("pages")
    if isinstance(pages_raw, dict):
        pages_raw = [pages_raw]
    if not isinstance(pages_raw, list):
        pages_raw = []
    pages = []
    for p in pages_raw:
        try:
            pages.append({
                "name": str(p.get("name") or ""),
                "allocated_mb": int(p.get("allocated_mb") or 0),
                "peak_mb": int(p.get("peak_mb") or 0),
                "current_mb": int(p.get("current_mb") or 0),
            })
        except Exception:  # noqa: BLE001
            continue
    headroom = limit - committed
    percent = round(committed / limit * 100.0, 1) if limit else 0.0
    return {
        "limit_gb": round(limit / GiB, 2),
        "committed_gb": round(committed / GiB, 2),
        "headroom_gb": round(headroom / GiB, 2),
        "percent": percent,
        "pagefile": pages,
    }


def _swap() -> dict:
    try:
        sm = psutil.swap_memory()
    except Exception:  # noqa: BLE001
        return {"total_mb": 0.0, "used_mb": 0.0, "percent": 0.0}
    return {
        "total_mb": round(sm.total / MiB, 1),
        "used_mb": round(sm.used / MiB, 1),
        "percent": round(float(sm.percent), 1),
    }


def _disk() -> List[dict]:
    out: List[dict] = []
    try:
        parts = psutil.disk_partitions(all=False)
    except Exception:  # noqa: BLE001
        return out
    for p in parts:
        try:
            usage = psutil.disk_usage(p.mountpoint)
        except Exception:  # noqa: BLE001
            continue
        out.append({
            "mount": p.mountpoint,
            "total_gb": round(usage.total / GiB, 1),
            "used_gb": round(usage.used / GiB, 1),
            "free_gb": round(usage.free / GiB, 1),
            "percent": round(float(usage.percent), 1),
        })
    return out


def _net() -> dict:
    global _NET_PREV
    try:
        c = psutil.net_io_counters()
    except Exception:  # noqa: BLE001
        return {"bytes_sent": 0, "bytes_recv": 0, "sent_kbps": 0.0, "recv_kbps": 0.0}
    now = time.time()
    sent_kbps = recv_kbps = 0.0
    if _NET_PREV is not None:
        prev_ts, prev_sent, prev_recv = _NET_PREV
        dt = now - prev_ts
        if dt > 0:
            sent_kbps = max(0.0, (c.bytes_sent - prev_sent) * 8.0 / dt / 1000.0)
            recv_kbps = max(0.0, (c.bytes_recv - prev_recv) * 8.0 / dt / 1000.0)
    _NET_PREV = (now, c.bytes_sent, c.bytes_recv)
    return {
        "bytes_sent": int(c.bytes_sent),
        "bytes_recv": int(c.bytes_recv),
        "sent_kbps": round(sent_kbps, 1),
        "recv_kbps": round(recv_kbps, 1),
    }


def _gpu() -> dict:
    data = _GPU_CACHE.get(_probe_gpu)
    name = str((data or {}).get("name") or "") or "GPU"
    ram = 0.0
    if data:
        try:
            ram = float(data.get("ram") or 0.0)
        except Exception:  # noqa: BLE001
            ram = 0.0
    dedicated = int(ram / MiB) if ram > 0 else 1024
    shared = 0
    if data and data.get("shared") is not None:
        try:
            shared = int(float(data["shared"]) / 1e6)   # 字节 → MB
        except Exception:  # noqa: BLE001
            shared = 0
    util = None
    if data and data.get("util") is not None:
        try:
            util = round(min(100.0, float(data["util"])), 1)
        except Exception:  # noqa: BLE001
            util = None
    available = util is not None
    return {
        "name": name,
        "util_percent": util if available else None,
        "dedicated_mb": dedicated,
        "shared_used_mb": shared,
        "available": available,
    }


def gpu_available() -> bool:
    """供 health() 使用：GPU 计数器当前是否可用。"""
    try:
        return bool(_gpu().get("available"))
    except Exception:  # noqa: BLE001
        return False


def _alerts(mem: dict, commit: dict, disks: List[dict], gpu: dict) -> List[dict]:
    alerts: List[dict] = []
    try:
        headroom = commit.get("headroom_gb", 0.0)
        if headroom < 2.0:
            alerts.append({
                "level": "critical",
                "title": "提交内存余量不足",
                "detail": "仅剩 %.2f GB（%.2f/%.2f GB），JVM 可能因 Chunk::new malloc failed 崩溃"
                          % (headroom, commit.get("committed_gb", 0), commit.get("limit_gb", 0)),
            })
    except Exception:  # noqa: BLE001
        pass
    try:
        if mem.get("percent", 0) > 90:
            alerts.append({
                "level": "warn",
                "title": "物理内存占用过高",
                "detail": "已用 %.1f%%（%.0f/%.0f MB）"
                          % (mem.get("percent", 0), mem.get("used_mb", 0), mem.get("total_mb", 0)),
            })
    except Exception:  # noqa: BLE001
        pass
    for d in disks:
        try:
            if d.get("free_gb", 999) < 5:
                alerts.append({
                    "level": "warn",
                    "title": "磁盘空间不足",
                    "detail": "%s 剩余 %.1f GB" % (d.get("mount", "?"), d.get("free_gb", 0)),
                })
        except Exception:  # noqa: BLE001
            continue
    try:
        if not gpu.get("available"):
            alerts.append({
                "level": "warn",
                "title": "GPU 计数器不可用",
                "detail": "Get-Counter GPU Engine/Adapter Memory 探针失败",
            })
    except Exception:  # noqa: BLE001
        pass
    return alerts


def system_snapshot() -> dict:
    """GET /api/monitor/system 的数据体。"""
    mem = _memory()
    commit = _commit()
    disks = _disk()
    gpu = _gpu()
    return {
        "cpu": _cpu(),
        "memory": mem,
        "commit": commit,
        "swap": _swap(),
        "disk": disks,
        "net": _net(),
        "gpu": gpu,
        "host": _host(),
        "alerts": _alerts(mem, commit, disks, gpu),
    }


# ============================================================
# services —— 服务目录（端口已验证，勿改）
# ============================================================
# (name, port, kind, label, 匹配关键字 —— 针对 cmd_norm，已小写/正斜杠化)
# 关键字为空时用端口监听方判定（本目录均给定关键字）。
_SERVICE_CATALOG: List[dict] = [
    ("gateway", 8080, "java", "API网关", "gateway-1.0.0.jar"),
    ("user-service", 8081, "java", "用户服务", "user-service-1.0.0.jar"),
    ("blog-service", 8082, "java", "博客服务", "blog-service-1.0.0.jar"),
    ("forum-service", 8083, "java", "论坛服务", "forum-service-1.0.0.jar"),
    ("shop-service", 8084, "java", "商城服务", "shop-service-1.0.0.jar"),
    ("media-service", 8085, "java", "媒体服务", "media-service-1.0.0.jar"),
    ("quant-service", 8086, "java", "量化服务", "quant-service-1.0.0.jar"),
    ("tool-service", 8087, "java", "工具服务", "tool-service-1.0.0.jar"),
    ("resource-service", 8089, "java", "资源服务", "resource-service-1.0.0.jar"),
    ("software-service", 8090, "java", "软件服务", "software-service-1.0.0.jar"),
    ("ai-proxy-service", 8093, "java", "AI中转站", "ai-proxy-service-1.0.0.jar"),
    ("jarvis", 9001, "java", "星图助手", "jarvis-service-1.0.0.jar"),
    ("admin", 9002, "java", "管理后台", "admin-service-1.0.0.jar"),
    ("netease-music-api", 3000, "node", "网易云代理", "netease-music-api/server.js"),
    ("chat-gateway", 3010, "node", "聊天网关", "chat-gateway/server.js"),
    ("py-service", 9006, "python", "Python服务", "uvicorn main:app"),
    ("frontend", 5173, "vite", "前端Vite", "vite/bin/vite.js"),
]


def _match_service_proc(procs: Dict[int, dict], name: str, keyword: str) -> Optional[dict]:
    if name == "py-service":
        cands = [p for p in procs.values()
                 if keyword in p["cmd_norm"] and "9006" in p["cmd_norm"]]
        if not cands:
            return None
        cands.sort(key=lambda p: (("py-service" in p["cmd_norm"]), p["mem_mb"]), reverse=True)
        return cands[0]
    if name == "frontend":
        cands = [p for p in procs.values() if keyword in p["cmd_norm"]]
        if not cands:
            return None
        cands.sort(key=lambda p: p["mem_mb"], reverse=True)
        return cands[0]
    best = None
    for p in procs.values():
        if keyword in p["cmd_norm"] and (best is None or p["mem_mb"] > best["mem_mb"]):
            best = p
    return best


def _svc_item(name: str, port: int, kind: str, label: str, keyword: str,
              procs: Dict[int, dict]) -> dict:
    up = _tcp_up(port)
    p = _match_service_proc(procs, name, keyword)
    pid = threads = uptime = None
    mem_mb = cpu = 0.0
    cmd = None
    if p is not None:
        pid = p["pid"]
        mem_mb = round(p["mem_mb"], 1)
        cpu = round(p["cpu"], 1)
        threads = int(p["threads"]) if p["threads"] is not None else None
        uptime = int(time.time() - p["create"]) if p["create"] else None
        cmd = (p["cmd"][:500] or None)
    return {
        "name": name,
        "label": label,
        "port": port,
        "kind": kind,
        "up": bool(up),
        "pid": pid,
        "mem_mb": mem_mb,
        "cpu_percent": cpu,
        "threads": threads,
        "uptime_s": uptime,
        "cmd": cmd,
    }


def services_snapshot() -> dict:
    """GET /api/monitor/services 的数据体。"""
    try:
        procs = _procs_cached()
    except Exception:  # noqa: BLE001
        procs = {}
    items: List[dict] = []
    for name, port, kind, label, keyword in _SERVICE_CATALOG:
        try:
            items.append(_svc_item(name, port, kind, label, keyword, procs))
        except Exception:  # noqa: BLE001
            items.append({
                "name": name, "label": label, "port": port, "kind": kind,
                "up": False, "pid": None, "mem_mb": 0.0, "cpu_percent": 0.0,
                "threads": None, "uptime_s": None, "cmd": None,
            })
    up_count = sum(1 for i in items if i["up"])
    return {
        "summary": {
            "total": len(items),
            "up": up_count,
            "down": len(items) - up_count,
            "mem_mb": round(sum(i["mem_mb"] for i in items), 1),
            "cpu_percent": round(sum(i["cpu_percent"] for i in items), 1),
        },
        "services": items,
    }


# ============================================================
# middleware —— MySQL / Redis / Nacos
# ============================================================
def _proc_metrics(procs: Dict[int, dict], predicate) -> dict:
    p = _best_proc(procs, predicate)
    if p is None:
        return {"pid": None, "mem_mb": 0.0, "cpu_percent": 0.0}
    return {"pid": p["pid"], "mem_mb": round(p["mem_mb"], 1), "cpu_percent": round(p["cpu"], 1)}


def _mysql_probe() -> dict:
    host = _env("MYSQL_HOST", "127.0.0.1")
    port = int(_env("MYSQL_PORT", "3306") or "3306")
    user = _env("MYSQL_USERNAME", "root")
    password = _env("MYSQL_PASSWORD", "root")
    try:
        import pymysql
    except Exception as exc:  # noqa: BLE001
        return {"up": False, "version": None, "detail": None, "error": "pymysql 不可用: %s" % exc}
    conn = None
    try:
        conn = pymysql.connect(
            host=host, port=port, user=user, password=password,
            charset="utf8mb4", connect_timeout=2, read_timeout=3, write_timeout=3,
            cursorclass=pymysql.cursors.DictCursor,
        )
        with conn.cursor() as cur:
            cur.execute("SELECT VERSION() AS v")
            row = cur.fetchone() or {}
            version = str(row.get("v") or "")
            cur.execute("SHOW STATUS")
            rows = cur.fetchall() or []
        st = {str(r.get("Variable_name")): str(r.get("Value")) for r in rows}
        detail = {
            "connections": int(st.get("Threads_connected", 0) or 0),
            "threads_running": int(st.get("Threads_running", 0) or 0),
            "uptime_s": int(st.get("Uptime", 0) or 0),
            "questions": int(st.get("Questions", 0) or 0),
            "slow_queries": int(st.get("Slow_queries", 0) or 0),
        }
        return {"up": True, "version": version or None, "detail": detail, "error": None}
    except Exception as exc:  # noqa: BLE001
        return {"up": False, "version": None, "detail": None, "error": str(exc)[:200]}
    finally:
        if conn is not None:
            try:
                conn.close()
            except Exception:  # noqa: BLE001
                pass


def _redis_probe() -> dict:
    host = _env("REDIS_HOST", "127.0.0.1")
    port = int(_env("REDIS_PORT", "6379") or "6379")
    password = _env("REDIS_PASSWORD", "")
    try:
        with RespClient(host, port, timeout=2.0, password=password) as c:
            info = c.command("INFO")
            dbsize = c.command("DBSIZE")
    except Exception as exc:  # noqa: BLE001
        return {"up": False, "version": None, "detail": None, "error": str(exc)[:200]}
    meta: Dict[str, str] = {}
    for line in (info or "").splitlines():
        if not line or line.startswith("#") or ":" not in line:
            continue
        k, _, v = line.partition(":")
        meta[k.strip()] = v.strip()
    try:
        used_bytes = float(meta.get("used_memory") or 0.0)
    except Exception:  # noqa: BLE001
        used_bytes = 0.0

    def _to_int(key: str) -> int:
        try:
            return int(meta.get(key, 0) or 0)
        except Exception:  # noqa: BLE001
            return 0

    detail = {
        "used_memory_human": meta.get("used_memory_human", ""),
        "used_memory_mb": round(used_bytes / MiB, 3),
        "connected_clients": _to_int("connected_clients"),
        "total_keys": int(dbsize or 0),
        "uptime_s": _to_int("uptime_in_seconds"),
    }
    return {"up": True, "version": meta.get("redis_version") or None, "detail": detail, "error": None}


def _nacos_probe() -> dict:
    server = _env("NACOS_SERVER", "127.0.0.1:8848")
    host, _, port_s = server.partition(":")
    host = host or "127.0.0.1"
    try:
        port = int(port_s or "8848")
    except Exception:  # noqa: BLE001
        port = 8848
    up = _tcp_up(port, host, timeout=0.4)
    registered: Optional[int] = None
    if up:
        try:
            import urllib.request
            url = ("http://%s:%d/nacos/v1/ns/service/list?pageNo=1&pageSize=1"
                   % (host, port))
            req = urllib.request.Request(url, headers={"User-Agent": "py-service-monitor"})
            with urllib.request.urlopen(req, timeout=2.0) as resp:
                payload = json.loads(resp.read().decode("utf-8", "replace"))
            if isinstance(payload, dict):
                registered = int(payload.get("count")) if payload.get("count") is not None else None
        except Exception:  # noqa: BLE001
            registered = None
    return {
        "up": up,
        "version": None,
        "detail": {"registered_services": registered},
        "error": None if up else "TCP 连接失败",
    }


def middleware_snapshot() -> dict:
    """GET /api/monitor/middleware 的数据体。"""
    try:
        procs = _procs_cached()
    except Exception:  # noqa: BLE001
        procs = {}

    mysql = _mysql_probe()
    mysql.update(_proc_metrics(procs, lambda p: "mysqld" in p["name"].lower()))
    mysql_item = {"name": "MySQL", "port": 3306, **mysql}

    redis = _redis_probe()
    redis.update(_proc_metrics(procs, lambda p: "redis-server" in p["name"].lower()))
    redis_item = {"name": "Redis", "port": 6379, **redis}

    nacos = _nacos_probe()
    nacos.update(_proc_metrics(procs, lambda p: "nacos" in p["cmd_norm"]))
    nacos_item = {"name": "Nacos", "port": 8848, **nacos}

    return {"items": [mysql_item, redis_item, nacos_item]}


# ============================================================
# security —— 网关黑名单 / 限流 / 日志事件
# ============================================================
_TS_RE = re.compile(r"^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:[+-]\d{2}:\d{2})?)")
_BLOCK_RE = re.compile(r"\[ip-blocklist\]\s+blocked\s+ip=(\S+)")
_RL_RE = re.compile(r"\[rate-limit\]\s+rejected\s+ip=(\S+)\s+bucket=(\S+)")


def _newest_gateway_log() -> Optional[Path]:
    try:
        files = _glob.glob(str(_GATEWAY_LOG_DIR / "gateway-*.out.log"))
    except Exception:  # noqa: BLE001
        return None
    newest = None
    newest_mtime = -1.0
    for f in files:
        try:
            m = os.path.getmtime(f)
        except OSError:
            continue
        if m > newest_mtime:
            newest_mtime = m
            newest = Path(f)
    return newest


def _scan_gateway_log() -> tuple:
    """返回 (events, 扫描行数)。尾部读取最近约 400KB。"""
    events: List[dict] = []
    scanned = 0
    path = _newest_gateway_log()
    if path is None:
        return events, scanned
    try:
        size = path.stat().st_size
        tail_bytes = 400 * 1024
        with path.open("rb") as fh:
            if size > tail_bytes:
                fh.seek(size - tail_bytes)
                fh.readline()
            data = fh.read(tail_bytes)
        lines = _decode_bytes(data).splitlines()
        scanned = len(lines)
        for line in reversed(lines):          # 最新在前
            if len(events) >= 200:
                break
            try:
                m = _BLOCK_RE.search(line)
                if m:
                    ts = _TS_RE.match(line)
                    events.append({
                        "ts": ts.group(1) if ts else "",
                        "type": "ip_block",
                        "ip": m.group(1),
                        "detail": "命中 IP 黑名单被网关拦截",
                        "source": path.name,
                    })
                    continue
                m = _RL_RE.search(line)
                if m:
                    ts = _TS_RE.match(line)
                    events.append({
                        "ts": ts.group(1) if ts else "",
                        "type": "rate_limit",
                        "ip": m.group(1),
                        "detail": "bucket=%s" % m.group(2),
                        "source": path.name,
                    })
            except Exception:  # noqa: BLE001
                continue
    except Exception:  # noqa: BLE001
        pass
    return events, scanned


def _redis_security() -> tuple:
    """返回 (blocklist, rate_limited)。失败返回空列表。"""
    host = _env("REDIS_HOST", "127.0.0.1")
    port = int(_env("REDIS_PORT", "6379") or "6379")
    password = _env("REDIS_PASSWORD", "")
    blocklist: List[str] = []
    rate_limited: List[dict] = []
    try:
        with RespClient(host, port, timeout=2.0, password=password) as c:
            members = c.command("SMEMBERS", "ip:blocklist")
            if isinstance(members, list):
                blocklist = [str(x) for x in members]
            keys: List[str] = []
            cursor = "0"
            for _ in range(52):
                res = c.command("SCAN", cursor, "MATCH", "rl:*", "COUNT", "200")
                if not isinstance(res, list) or len(res) != 2:
                    break
                cursor = str(res[0] or "0")
                batch = res[1]
                if isinstance(batch, list):
                    keys.extend(str(k) for k in batch)
                if cursor == "0":
                    break
            for key in keys:
                rest = key[3:] if key.startswith("rl:") else key
                ip, _, bucket = rest.partition(":")
                scope = "global" if bucket == "global" else "path"
                tokens = 0.0
                try:
                    flat = c.command("HGETALL", key)
                    if isinstance(flat, list):
                        hd = dict(zip(flat[0::2], flat[1::2]))
                        tokens = float(hd.get("tokens") or 0.0)
                except Exception:  # noqa: BLE001
                    tokens = 0.0
                rate_limited.append({
                    "ip": ip,
                    "bucket": bucket or "global",
                    "tokens": round(tokens, 2),
                    "scope": scope,
                })
    except Exception:  # noqa: BLE001
        return [], []
    return blocklist, rate_limited


def security_snapshot() -> dict:
    """GET /api/monitor/security 的数据体。"""
    try:
        blocklist, rate_limited = _redis_security()
    except Exception:  # noqa: BLE001
        blocklist, rate_limited = [], []
    try:
        events, scanned = _scan_gateway_log()
    except Exception:  # noqa: BLE001
        events, scanned = [], 0
    return {
        "summary": {
            "blocked_ips": len(blocklist),
            "rate_limited_ips": len(rate_limited),
            "events": len(events),
            "log_lines_scanned": scanned,
        },
        "blocklist": blocklist,
        "rate_limited": rate_limited,
        "events": events,
    }


# ============================================================
# events —— JVM 崩溃 / 看门狗 / 滚动重启
# ============================================================
_CRASH_MARKERS = ("Native memory allocation (malloc) failed", "Out of Memory Error")
_FATAL_RE = re.compile(r"#\s+(EXCEPTION_[A-Z_]+|A fatal error has been detected)")


def _crash_reason(path: Path) -> str:
    """从 hs_err 日志头部（最多 24KB）提取原因行；无 OOM 特征时回退到致命错误行。"""
    try:
        with path.open("rb") as fh:
            head = fh.read(24 * 1024)
        lines = _decode_bytes(head).splitlines()
        for line in lines:
            if any(mk in line for mk in _CRASH_MARKERS):
                return line.strip()[:300]
        for line in lines:
            if _FATAL_RE.search(line):
                return line.strip().lstrip("#").strip()[:300]
    except Exception:  # noqa: BLE001
        pass
    return "未知原因"


def _crashes() -> List[dict]:
    out: List[dict] = []
    try:
        files = _glob.glob(_HS_ERR_GLOB)
    except Exception:  # noqa: BLE001
        return out
    entries = []
    for f in files:
        try:
            entries.append((os.path.getmtime(f), f))
        except OSError:
            continue
    entries.sort(reverse=True)
    for mtime, f in entries[:20]:
        p = Path(f)
        try:
            ts = datetime.fromtimestamp(mtime).isoformat()
        except Exception:  # noqa: BLE001
            ts = ""
        out.append({
            "service": p.parent.parent.name,   # 如 blog-service
            "file": p.name,
            "path": str(p),
            "ts": ts,
            "reason": _crash_reason(p),
        })
    return out


def _temp_log(name: str, tail_bytes: int, max_lines: int) -> dict:
    path = _TEMP_DIR / name
    exists = False
    try:
        exists = path.is_file()
    except Exception:  # noqa: BLE001
        exists = False
    tail = _tail_lines(path, tail_bytes, max_lines) if exists else []
    return {"path": str(path), "exists": exists, "tail": tail}


def events_snapshot() -> dict:
    """GET /api/monitor/events 的数据体。"""
    try:
        crashes = _crashes()
    except Exception:  # noqa: BLE001
        crashes = []
    try:
        watchdog = _temp_log("web3-watchdog.log", 64 * 1024, 30)
    except Exception:  # noqa: BLE001
        watchdog = {"path": "", "exists": False, "tail": []}
    try:
        roll = _temp_log("web3-roll-restart.log", 64 * 1024, 20)
    except Exception:  # noqa: BLE001
        roll = {"path": "", "exists": False, "tail": []}
    return {"crashes": crashes, "watchdog": watchdog, "roll": roll}


# ============================================================
# overview —— 合并轮询
# ============================================================
def overview_snapshot() -> dict:
    """GET /api/monitor/overview 的数据体。"""
    def _safe(fn):
        try:
            return fn()
        except Exception:  # noqa: BLE001
            return {}
    return {
        "ts": int(time.time()),
        "system": _safe(system_snapshot),
        "services": _safe(services_snapshot),
        "middleware": _safe(middleware_snapshot),
    }


# ============================================================
# 后台预热：让首个前端请求也能命中缓存（异步，不阻塞导入）
# ============================================================
def _warmup() -> None:
    # 先预热主机静态信息（每次 system 轮询都要用），再预热慢子进程探针
    try:
        _host()
    except Exception:  # noqa: BLE001
        pass
    for cache, producer in (
        (_COMMIT_CACHE, _probe_commit),
        (_GPU_CACHE, _probe_gpu),
        (_PROCS_CACHE, _scan_processes),
    ):
        try:
            cache.get(producer)
        except Exception:  # noqa: BLE001
            continue


try:  # noqa: BLE001
    threading.Thread(target=_warmup, name="monitor-warmup", daemon=True).start()
except Exception:  # noqa: BLE001
    pass
