# -*- coding: utf-8 -*-
"""wifi 模块能力检测：平台 / 权限 / 外部命令 / 网卡 / 攻击引擎可用性。"""
from __future__ import annotations

import shutil
import sys

from . import config
from .process import is_elevated, run_command

IS_WINDOWS = sys.platform.startswith("win")
IS_LINUX = sys.platform.startswith("linux")

# Linux 审计套件命令
LINUX_TOOLS = [
    "iw", "nmcli", "airmon-ng", "airodump-ng", "aireplay-ng",
    "aircrack-ng", "hostapd", "dnsmasq", "iwconfig", "python2",
]


def platform_name() -> str:
    if IS_WINDOWS:
        return "windows"
    if IS_LINUX:
        return "linux"
    if sys.platform == "darwin":
        return "macos"
    return sys.platform


def commands() -> dict:
    """探测外部命令可用性（PATH）。"""
    result = {}
    if IS_WINDOWS:
        result["netsh"] = bool(shutil.which("netsh"))
    else:
        for tool in LINUX_TOOLS:
            result[tool] = bool(shutil.which(tool))
    return result


def adapters() -> list[dict]:
    try:
        if IS_WINDOWS:
            from . import windows
            return windows.interfaces()
        if IS_LINUX:
            from . import linuxscan
            return linuxscan.interfaces()
    except Exception as exc:  # noqa: BLE001
        return [{"error": str(exc)}]
    return []


def _engine(name: str, system_bin: str, bundled_path, interpreter: str = "") -> dict:
    info = {"name": name, "available": False, "mode": "unavailable",
            "requires": "linux + root + 监听/注入网卡"}
    if not IS_LINUX:
        info["reason"] = "仅支持 Linux（当前系统无法执行真实无线攻击）"
        return info
    if shutil.which(system_bin):
        info.update(available=True, mode="system")
        return info
    if bundled_path.exists():
        ok_interp = (not interpreter) or bool(shutil.which(interpreter))
        if ok_interp:
            info.update(available=True, mode="bundled")
        else:
            info["reason"] = f"内置脚本存在但缺少解释器 {interpreter}"
        return info
    info["reason"] = "未安装且内置工具缺失"
    return info


def engines() -> list[dict]:
    return [
        _engine("wifiphisher", "wifiphisher", config.WIFIPHISHER_BIN, ""),
        _engine("wifite", "wifite", config.WIFITE_SCRIPT, "python2"),
    ]


def win_engines() -> list[dict]:
    """Windows 原生引擎：邪恶孪生热点 + WPA 离线破解。"""
    from .process import is_elevated
    from . import winrogue, wincrack

    elevated = is_elevated()
    ok_hosted, why = winrogue.hosted_supported()
    hs = winrogue.hotspot_query()
    hotspot_ok = hs is not None

    # 钓鱼热点：三条路径任一可用即可（服务本体均免提权）
    if elevated and ok_hosted:
        rogue_reason = "就绪（管理员 · 承载网络模式：自管 DHCP/DNS，零提示）"
    elif hotspot_ok:
        rogue_reason = "就绪（免管理员 · Windows 移动热点模式：系统 DHCP/NAT + DNS 劫持门户）"
    elif ok_hosted:
        rogue_reason = "就绪（承载网络模式：启动/停止时弹一次 UAC 管理员确认，服务本体免提权）"
    else:
        rogue_reason = f"不可用：驱动不支持承载网络（{why}），移动热点（WiFi Direct）也不可控"

    rogue = {
        "name": "win-rogue-ap",
        "title": "邪恶孪生·钓鱼热点",
        "available": ok_hosted or hotspot_ok,
        "requires": "移动热点（免管理员）或承载网络（启动时 UAC 确认一次）",
        "reason": rogue_reason,
        "modes": {
            "hosted": {"available": ok_hosted, "elevated": elevated, "why": why},
            "hotspot": {"available": hotspot_ok,
                        "state": (hs or {}).get("state", ""),
                        "ssid": (hs or {}).get("ssid", "")},
        },
    }

    hashcat_bin = wincrack.find_hashcat()
    crack = {
        "name": "win-wpa-crack",
        "title": "WPA/WPA2 离线破解",
        "available": True,
        "requires": "22000 握手哈希 + 字典",
        "reason": f"就绪（{'已检测到 hashcat：' + hashcat_bin if hashcat_bin else 'hashcat 未安装，使用内置 Python 引擎（较慢）'}）",
        "hashcat": hashcat_bin,
    }
    return [rogue, crack]


def summary() -> dict:
    win_engs = win_engines() if IS_WINDOWS else []
    return {
        "platform": platform_name(),
        "elevated": is_elevated(),
        "commands": commands(),
        "adapters": adapters(),
        "engines": engines(),
        "win_engines": win_engs,
        "features": {
            "scan": True,
            "interfaces": True,
            "profiles": IS_WINDOWS,
            "rogue_ap": IS_LINUX or bool(win_engs and win_engs[0]["available"]),
            "offline_crack": IS_WINDOWS or IS_LINUX,
            "monitor_injection": IS_LINUX,
        },
    }
