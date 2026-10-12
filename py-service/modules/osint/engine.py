# -*- coding: utf-8 -*-
"""
OSINT 扫描引擎 —— 简化版 SpiderFoot 事件驱动架构。

核心概念：
  • OsintEvent   —— 一次发现（类型 + 数据 + 来源模块 + 父事件引用）
  • OsintModule  —— 插件基类，声明 watchedEvents / producedEvents / handleEvent
  • ScanEngine   —— 目标解析 → 模块加载 → 事件队列调度 → 结果聚合

事件类型常量参考 SpiderFoot：IP_ADDRESS / INTERNET_NAME / DOMAIN_NAME /
DOMAIN_WHOIS / TCP_PORT_OPEN / IP_INFO / etc.
"""
from __future__ import annotations

import ipaddress
import os
import re
import socket
import threading
import time
import uuid
from dataclasses import dataclass, field
from typing import Any, Callable

# ============================================================
# 事件类型常量
# ============================================================
ROOT = "ROOT"
IP_ADDRESS = "IP_ADDRESS"
IPV6_ADDRESS = "IPV6_ADDRESS"
INTERNET_NAME = "INTERNET_NAME"
DOMAIN_NAME = "DOMAIN_NAME"
DOMAIN_WHOIS = "DOMAIN_WHOIS"
DOMAIN_REGISTRAR = "DOMAIN_REGISTRAR"
NETBLOCK_OWNER = "NETBLOCK_OWNER"
TCP_PORT_OPEN = "TCP_PORT_OPEN"
TCP_PORT_OPEN_BANNER = "TCP_PORT_OPEN_BANNER"
IP_INFO = "IP_INFO"
SUBDOMAIN = "SUBDOMAIN"
EMAIL = "EMAIL"
SHODAN_RESULT = "SHODAN_RESULT"
RAW_DNS_RECORDS = "RAW_DNS_RECORDS"


# ============================================================
# 事件对象
# ============================================================
@dataclass
class OsintEvent:
    event_type: str
    data: str
    module: str
    source_event: OsintEvent | None = None
    generated: float = field(default_factory=time.time)
    confidence: int = 100
    visibility: int = 100
    risk: int = 0
    _id: str = field(default_factory=lambda: uuid.uuid4().hex)

    @property
    def source_data(self) -> str:
        return self.source_event.data if self.source_event else ""

    def to_dict(self) -> dict:
        return {
            "type": self.event_type,
            "data": self.data,
            "module": self.module,
            "source": self.source_data,
            "generated": int(self.generated),
            "confidence": self.confidence,
            "risk": self.risk,
        }


# ============================================================
# 目标解析
# ============================================================
@dataclass
class ScanTarget:
    value: str
    target_type: str = ""           # IP_ADDRESS / INTERNET_NAME / DOMAIN_NAME / EMAIL
    aliases: list[str] = field(default_factory=list)

    def matches(self, value: str) -> bool:
        if value == self.value:
            return True
        return value in self.aliases


def parse_target(value: str) -> ScanTarget:
    value = value.strip().lower()
    if not value:
        raise ValueError("目标不能为空")

    # Email
    if re.match(r"^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$", value):
        domain = value.split("@")[1]
        return ScanTarget(value, EMAIL, aliases=[domain])

    # IP
    try:
        ip = ipaddress.ip_address(value)
        t_type = IPV6_ADDRESS if ip.version == 6 else IP_ADDRESS
        return ScanTarget(value, t_type)
    except ValueError:
        pass

    # CIDR
    if "/" in value:
        try:
            net = ipaddress.ip_network(value, strict=False)
            return ScanTarget(value, NETBLOCK_OWNER)
        except ValueError:
            pass

    # Domain / hostname
    if re.match(r"^[a-z0-9]([a-z0-9\-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9\-]*[a-z0-9])?)+$", value):
        # Extract root domain
        parts = value.split(".")
        if len(parts) >= 2:
            domain = ".".join(parts[-2:])
            return ScanTarget(value, DOMAIN_NAME, aliases=[domain] if domain != value else [])
        return ScanTarget(value, INTERNET_NAME)

    raise ValueError(f"无法识别目标类型：{value}")


# ============================================================
# 模块基类
# ============================================================
class OsintModule:
    meta: dict = {}
    opts: dict = {}

    def setup(self, opts: dict | None = None) -> None:
        if opts:
            self.opts = {**self.opts, **opts}

    def watched_events(self) -> list[str]:
        return []

    def produced_events(self) -> list[str]:
        return []

    def handle_event(self, event: OsintEvent, target: ScanTarget,
                     emit: Callable[[OsintEvent], None]) -> None:
        pass


# ============================================================
# 模块注册表（自动发现 modules/ 下的子模块）
# ============================================================
_module_registry: dict[str, OsintModule] = {}


def register_module(mod: OsintModule) -> None:
    name = mod.meta.get("name", mod.__class__.__name__)
    _module_registry[name] = mod


def list_modules() -> list[dict]:
    return [
        {
            "name": name,
            "title": mod.meta.get("title", name),
            "summary": mod.meta.get("summary", ""),
            "category": mod.meta.get("category", ""),
            "watched": mod.watched_events(),
            "produced": mod.produced_events(),
            "enabled": True,
        }
        for name, mod in _module_registry.items()
    ]


def get_module(name: str) -> OsintModule | None:
    return _module_registry.get(name)


# ============================================================
# 扫描引擎
# ============================================================
class ScanJob:
    def __init__(self, target: ScanTarget, module_names: list[str]):
        self.jid = uuid.uuid4().hex[:12]
        self.target = target
        self.module_names = module_names
        self.status = "pending"
        self.events: list[OsintEvent] = []
        self.results: list[dict] = []
        self.logs: list[str] = []
        self.started = 0.0
        self.finished = 0.0
        self._stop = threading.Event()
        self._lock = threading.Lock()

    def _log(self, msg: str) -> None:
        ts = time.strftime("%H:%M:%S")
        self.logs.append(f"[{ts}] {msg}")
        if len(self.logs) > 2000:
            del self.logs[: len(self.logs) - 2000]

    def _emit(self, event: OsintEvent) -> None:
        with self._lock:
            self.events.append(event)
            self.results.append(event.to_dict())
        self._log(f"+ {event.event_type}: {event.data[:80]}")

    def request_stop(self) -> None:
        self._stop.set()

    def to_dict(self) -> dict:
        return {
            "jid": self.jid,
            "status": self.status,
            "target": self.target.value,
            "target_type": self.target.target_type,
            "modules": self.module_names,
            "events": len(self.events),
            "results": self.results,
            "logs": self.logs[-200:],
            "started": self.started,
            "finished": self.finished,
        }

    def summary(self) -> dict:
        """按事件类型聚合统计。"""
        stats: dict[str, int] = {}
        for e in self.events:
            stats[e.event_type] = stats.get(e.event_type, 0) + 1
        return {
            "jid": self.jid,
            "status": self.status,
            "target": self.target.value,
            "target_type": self.target.target_type,
            "total_events": len(self.events),
            "by_type": stats,
            "logs": self.logs[-100:],
        }


_jobs: dict[str, ScanJob] = {}
_jobs_lock = threading.Lock()


def create_scan(target_value: str, module_names: list[str] | None = None) -> ScanJob:
    target = parse_target(target_value)

    if module_names:
        # Validate
        for name in module_names:
            if name not in _module_registry:
                raise ValueError(f"模块不存在：{name}")
    else:
        module_names = list(_module_registry.keys())

    job = ScanJob(target, module_names)
    with _jobs_lock:
        _jobs[job.jid] = job

    def runner():
        job.status = "running"
        job.started = time.time()
        job._log(f"扫描目标：{target.value}（{target.target_type}）")
        job._log(f"已加载模块：{', '.join(module_names)}")

        # ROOT event — 每个模块根据 watchedEvents 决定是否消费
        root = OsintEvent(ROOT, target.value, "engine")
        job._emit(root)

        # 根据目标类型生成初始事件
        initial_events: list[OsintEvent] = []
        if target.target_type in (IP_ADDRESS, IPV6_ADDRESS):
            initial_events.append(OsintEvent(target.target_type, target.value, "engine", root))
        elif target.target_type in (DOMAIN_NAME, INTERNET_NAME):
            initial_events.append(OsintEvent(INTERNET_NAME, target.value, "engine", root))
            if target.target_type == DOMAIN_NAME:
                for alias in target.aliases:
                    initial_events.append(OsintEvent(DOMAIN_NAME, alias, "engine", root))
        elif target.target_type == EMAIL:
            initial_events.append(OsintEvent(EMAIL, target.value, "engine", root))
            for alias in target.aliases:
                initial_events.append(OsintEvent(DOMAIN_NAME, alias, "engine", root))
        elif target.target_type == NETBLOCK_OWNER:
            initial_events.append(OsintEvent(NETBLOCK_OWNER, target.value, "engine", root))

        for evt in initial_events:
            job._emit(evt)

        # 事件队列：BFS — 初始事件入队，每轮取出事件分发给所有模块
        # 模块产生的新事件追加到队列尾部
        queue: list[OsintEvent] = list(initial_events)
        seen_hashes: set[str] = set()

        max_rounds = 50  # 防止无限循环
        rounds = 0
        while queue and not job._stop.is_set() and rounds < max_rounds:
            rounds += 1
            current_batch = queue[:]
            queue.clear()

            for event in current_batch:
                for name in module_names:
                    if job._stop.is_set():
                        break
                    mod = _module_registry[name]
                    if event.event_type in mod.watched_events() or ROOT in mod.watched_events():
                        produced: list[OsintEvent] = []
                        try:
                            mod.handle_event(event, target, produced.append)
                        except Exception as exc:  # noqa: BLE001
                            job._log(f"模块 {name} 异常：{exc}")
                        for new_evt in produced:
                            h = f"{new_evt.event_type}:{new_evt.data}"
                            if h in seen_hashes:
                                continue
                            seen_hashes.add(h)
                            job._emit(new_evt)
                            queue.append(new_evt)

        job.finished = time.time()
        elapsed = job.finished - job.started
        job._log(f"扫描完成：{len(job.events)} 个事件，耗时 {elapsed:.1f}s")
        job.status = "stopped" if job._stop.is_set() else "done"

    threading.Thread(target=runner, daemon=True).start()
    return job


def get_job(jid: str) -> ScanJob | None:
    return _jobs.get(jid)


def list_jobs() -> list[ScanJob]:
    return list(_jobs.values())


def stop_job(jid: str) -> ScanJob | None:
    job = _jobs.get(jid)
    if job and job.status == "running":
        job.request_stop()
    return job


# ============================================================
# 自动加载 modules/ 下的所有模块
# ============================================================
def _autoload():
    import importlib
    import pkgutil
    from . import modules as _mods_pkg

    for _, mod_name, _ in pkgutil.iter_modules(_mods_pkg.__path__):
        try:
            importlib.import_module(f".modules.{mod_name}", package=__package__)
        except Exception:  # noqa: BLE001
            pass


_autoload()
