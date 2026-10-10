# -*- coding: utf-8 -*-
"""
wifi 模块 Windows 能力：基于 netsh wlan。
解析对中英文双语系统做了容错：SSID/BSSID 字面量不变，信号取百分号数字，
信道/认证/加密等按键名关键字或取值正则识别。
"""
from __future__ import annotations

import re

from .process import run_command

_SSID_HEAD = re.compile(r"^\s*SSID\s*\d+\s*:\s*(.*)$", re.I)
_BSSID_HEAD = re.compile(r"^\s*BSSID\s*\d+\s*:\s*([0-9a-fA-F:]{17})\s*$")
_MAC = re.compile(r"([0-9a-fA-F]{2}[:-]){5}[0-9a-fA-F]{2}")


def _kv(line: str) -> tuple[str, str]:
    """按第一个冒号切 key/val（兼容全角：）。"""
    for sep in (":", "："):
        if sep in line:
            k, v = line.split(sep, 1)
            return k.strip().lower(), v.strip()
    return line.strip().lower(), ""


def _netsh(*args) -> str:
    rc, out = run_command(["netsh", "wlan", *args])
    return out if rc == 0 else ""


def scan_networks() -> list[dict]:
    """扫描附近网络（netsh wlan show networks mode=bssid）。"""
    out = _netsh("show", "networks", "mode=bssid")
    networks: list[dict] = []
    cur_net = None
    cur_ap = None

    for line in out.splitlines():
        m = _SSID_HEAD.match(line)
        if m:
            cur_net = {
                "ssid": m.group(1).strip(),
                "hidden": not m.group(1).strip(),
                "authentication": "",
                "encryption": "",
                "bssids": [],
            }
            networks.append(cur_net)
            cur_ap = None
            continue
        bm = _BSSID_HEAD.match(line)
        if bm and cur_net is not None:
            cur_ap = {
                "bssid": bm.group(1).lower(),
                "signal": None,
                "channel": None,
                "radio": "",
                "band": "",
            }
            cur_net["bssids"].append(cur_ap)
            continue

        key, val = _kv(line)
        if not val or ":" not in line and "：" not in line:
            continue

        if cur_ap is not None:
            if "%" in val or "signal" in key or "信号" in key:
                num = re.search(r"(\d+)\s*%", val)
                if num:
                    cur_ap["signal"] = int(num.group(1))
            if "channel" in key or "信道" in key or "频道" in key:
                num = re.search(r"(\d+)", val)
                if num:
                    cur_ap["channel"] = int(num.group(1))
            if "radio" in key or "无线电" in key:
                cur_ap["radio"] = val
            if "band" in key or "频段" in key:
                cur_ap["band"] = val

        if cur_net is not None:
            if "authentication" in key or "身份验证" in key:
                cur_net["authentication"] = val
            if "encryption" in key or "加密" in key:
                cur_net["encryption"] = val

    # 汇总每个网络的最佳信号与信道
    for net in networks:
        signals = [b["signal"] for b in net["bssids"] if b["signal"] is not None]
        channels = [b["channel"] for b in net["bssids"] if b["channel"] is not None]
        net["best_signal"] = max(signals) if signals else None
        net["channel"] = channels[0] if channels else None
        net["ap_count"] = len(net["bssids"])
    return networks


def interfaces() -> list[dict]:
    """无线网卡信息（netsh wlan show interfaces）。"""
    out = _netsh("show", "interfaces")
    blocks: list[dict] = []
    cur = None
    for raw in out.splitlines():
        line = raw.rstrip()
        if not line.strip():
            continue
        # 新块：顶层（缩进小）且 key 为 Name/名称
        leading = len(line) - len(line.lstrip())
        key, val = _kv(line)
        if leading <= 4 and key in ("name", "名称") and val:
            cur = {"raw": {}}
            blocks.append(cur)
        if cur is None:
            cur = {"raw": {}}
            blocks.append(cur)
        if val:
            cur["raw"][key] = val

    result = []
    for blk in blocks:
        raw = blk["raw"]
        if not raw:
            continue
        signal = None
        for k, v in raw.items():
            num = re.search(r"(\d+)\s*%", v)
            if ("signal" in k or "信号" in k) and num:
                signal = int(num.group(1))
        channel = None
        for k, v in raw.items():
            num = re.search(r"(\d+)", v)
            if ("channel" in k or "信道" in k or "频道" in k) and num:
                channel = int(num.group(1))
        state = ""
        for k, v in raw.items():
            if k in ("state", "状态"):
                state = v
        result.append({
            "name": _first(raw, ("name", "名称")),
            "description": _first(raw, ("description", "描述")),
            "guid": _first(raw, ("guid",)),
            "mac": _find_mac(raw),
            "state": state,
            "ssid": _first(raw, ("ssid",)),
            "bssid": _find_mac(raw, key_hint=("bssid",)),
            "channel": channel,
            "signal": signal,
            "radio": _first(raw, ("radio type", "radio", "无线电类型", "无线电")),
            "raw": raw,
        })
    return result


def _first(raw: dict, keys) -> str:
    for k in keys:
        if k in raw:
            return raw[k]
    return ""


def _find_mac(raw: dict, key_hint=("physical address", "物理地址")) -> str:
    for k, v in raw.items():
        if any(h in k for h in key_hint):
            m = _MAC.search(v)
            if m:
                return m.group(0)
    return ""


def list_profiles() -> list[str]:
    """已保存的 WiFi 配置文件（netsh wlan show profiles）。"""
    out = _netsh("show", "profiles")
    names = []
    for line in out.splitlines():
        key, val = _kv(line)
        if not val:
            continue
        if "profile" in key or "配置文件" in key:
            names.append(val)
    return names


def profile_detail(name: str) -> dict:
    """配置文件详情（含明文密钥，需管理员）。"""
    out = _netsh("show", "profiles", f"name={name}", "key=clear")
    raw: dict = {}
    for line in out.splitlines():
        key, val = _kv(line)
        if val:
            raw[key] = val
    password = ""
    for k, v in raw.items():
        if "key content" in k or "关键内容" in k or "密钥内容" in k:
            password = v
    return {"name": name, "password": password, "raw": raw}
