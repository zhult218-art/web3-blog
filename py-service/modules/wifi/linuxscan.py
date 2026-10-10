# -*- coding: utf-8 -*-
"""
wifi 模块 Linux 能力：网卡与扫描。

扫描优先使用 NetworkManager 的 nmcli（非 root 可用），否则回退 `iw scan`（需 root）。
监听/注入模式通过 iw / airmon-ng 探测。
"""
from __future__ import annotations

import re

from .process import run_command as run


def has_nmcli() -> bool:
    rc, _ = run(["sh", "-c", "command -v nmcli"], timeout=5)
    return rc == 0


def interfaces() -> list[dict]:
    """无线网卡：优先 nmcli，否则解析 `iw dev`。"""
    if has_nmcli():
        rc, out = run(["nmcli", "-t", "-f", "DEVICE,TYPE,STATE,CONNECTION", "device", "status"])
        if rc == 0:
            result = []
            for line in out.splitlines():
                parts = _unesc(line, 4)
                if len(parts) >= 2 and parts[1] == "wifi":
                    result.append({"name": parts[0], "state": parts[2] if len(parts) > 2 else "",
                                   "ssid": parts[3] if len(parts) > 3 else ""})
            if result:
                return result

    rc, out = run(["iw", "dev"])
    if rc == 0:
        return [{"name": n, "state": "", "ssid": "", "monitor": _is_monitor(n)}
                for n in re.findall(r"Interface\s+(\S+)", out)]
    return []


def _is_monitor(iface: str) -> bool:
    rc, out = run(["iw", "dev", iface, "info"])
    return rc == 0 and "type monitor" in out


def scan_networks(iface: str | None = None) -> list[dict]:
    if has_nmcli():
        argv = ["nmcli", "-t", "-f", "BSSID,SSID,CHAN,SIGNAL,SECURITY", "device", "wifi", "list"]
        if iface:
            argv += ["ifname", iface]
        rc, out = run(argv, timeout=30)
        if rc == 0:
            return _parse_nmcli(out)
    target = iface or (interfaces()[0]["name"] if interfaces() else None)
    if target:
        return _iw_scan(target)
    return []


def _parse_nmcli(out: str) -> list[dict]:
    networks = []
    for line in out.splitlines():
        parts = _unesc(line, 5)
        if len(parts) < 4:
            continue
        bssid, ssid, chan, signal = parts[:4]
        security = parts[4] if len(parts) > 4 else ""
        try:
            sig = int(signal)
        except ValueError:
            sig = None
        networks.append({
            "ssid": ssid, "hidden": ssid == "", "authentication": security,
            "encryption": "", "best_signal": sig, "channel": _chan_num(chan),
            "ap_count": 1,
            "bssids": [{"bssid": bssid.lower(), "signal": sig,
                        "channel": _chan_num(chan), "radio": "", "band": ""}],
        })
    return networks


def _iw_scan(iface: str) -> list[dict]:
    rc, out = run(["iw", "dev", iface, "scan"], timeout=30)
    if rc != 0:
        return []
    networks = []
    cur = None
    for line in out.splitlines():
        s = line.strip()
        bm = re.match(r"BSS\s+([0-9a-fA-F:]{17})", s)
        if bm:
            cur = {"ssid": "", "hidden": True, "authentication": "", "encryption": "",
                   "best_signal": None, "channel": None, "ap_count": 1,
                   "bssids": [{"bssid": bm.group(1).lower(), "signal": None,
                               "channel": None, "radio": "", "band": ""}]}
            networks.append(cur)
            continue
        if cur is None:
            continue
        if s.startswith("SSID:"):
            cur["ssid"] = s.split(":", 1)[1].strip()
            cur["hidden"] = not cur["ssid"]
        elif s.startswith("signal:"):
            m = re.search(r"([-\d.]+)", s)
            if m:
                cur["bssids"][0]["signal"] = float(m.group(1))
                cur["best_signal"] = float(m.group(1))
        elif s.startswith("DS Parameter set:") or s.startswith("channel:"):
            m = re.search(r"(\d+)", s)
            if m:
                cur["channel"] = int(m.group(1))
                cur["bssids"][0]["channel"] = int(m.group(1))
        elif s.startswith("RSN:") or s.startswith("WPA:"):
            cur["authentication"] = cur["authentication"] or "WPA"
    return networks


def _chan_num(value: str):
    m = re.search(r"(\d+)", value or "")
    return int(m.group(1)) if m else None


def _unesc(line: str, count: int) -> list[str]:
    """nmcli -t 用 '\\' 转义 ':' 和 '\\\\'，先反转义再按 ':' 切。"""
    line = line.replace("\\:", "\x00").replace("\\\\", "\x01")
    parts = line.split(":", count - 1)
    return [p.replace("\x00", ":").replace("\x01", "\\") for p in parts]
