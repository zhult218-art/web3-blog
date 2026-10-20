# -*- coding: utf-8 -*-
"""IP 追踪（ipwho.is 公开接口，免 API Key）。

安全说明（SSRF）：请求 URL 只由「固定主机 https://ipwho.is/ + 已用 ipaddress
校验过的 IP」拼出，用户输入无法改变目标主机或路径，因此不存在服务端请求任意
地址的通道。非 IP 的输入一律在拼 URL 之前就被拒绝。

「查我自己」的取法：优先使用服务端观测到的调用方 IP；本地开发时该地址必然是
回环/内网地址（127.0.0.1），ipwho.is 查不到，此时退回查询本机出口公网 IP，
并在结果里用 source 字段如实标注来源（client / egress / manual）。
"""
from __future__ import annotations

import ipaddress
import json
import urllib.request

API = "https://ipwho.is/"
TIMEOUT = 12
UA = "Mozilla/5.0 (compatible; GhostTrack/1.0)"


def _is_public(ip: str) -> bool:
    """判断是否为可查询的公网地址（回环 / 内网 / 保留 / 链路本地均不算）。"""
    try:
        addr = ipaddress.ip_address(ip)
    except ValueError:
        return False
    return not (
        addr.is_private or addr.is_loopback or addr.is_link_local
        or addr.is_reserved or addr.is_multicast or addr.is_unspecified
    )


def _fetch(url: str) -> dict:
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
        return json.loads(resp.read().decode("utf-8"))


def _normalize(raw: dict, query: str, source: str) -> dict:
    """把 ipwho.is 原始响应整理成前端可直接渲染的字段。"""
    conn = raw.get("connection") or {}
    tz = raw.get("timezone") or {}
    flag = raw.get("flag") or {}
    lat, lon = raw.get("latitude"), raw.get("longitude")
    return {
        "ok": True,
        # manual=查指定 IP；client=查调用方自己的公网 IP；egress=查本机出口公网 IP
        "source": source,
        "query": query or raw.get("ip", ""),
        "ip": raw.get("ip") or query,
        "type": raw.get("type", ""),
        "continent": raw.get("continent", ""),
        "country": raw.get("country", ""),
        "countryCode": raw.get("country_code", ""),
        "flag": flag.get("emoji", ""),
        "region": raw.get("region", ""),
        "city": raw.get("city", ""),
        "postal": raw.get("postal", ""),
        "latitude": lat,
        "longitude": lon,
        "mapUrl": f"https://www.google.com/maps?q={lat},{lon}" if lat is not None and lon is not None else "",
        "capital": raw.get("capital", ""),
        "callingCode": raw.get("calling_code", ""),
        "isEU": bool(raw.get("is_eu")),
        "asn": conn.get("asn", ""),
        "org": conn.get("org", ""),
        "isp": conn.get("isp", ""),
        "domain": conn.get("domain", ""),
        "tzId": tz.get("id", ""),
        "tzAbbr": tz.get("abbr", ""),
        "tzUtc": tz.get("utc", ""),
        "tzIsDst": tz.get("is_dst"),
        "tzCurrent": tz.get("current_time", ""),
    }


def lookup(ip: str = "", client_ip: str = "") -> dict:
    """查询 IP 信息；ip 为空时查「你自己的公网 IP」。"""
    ip = (ip or "").strip()
    source = "manual"

    if ip:
        try:
            ipaddress.ip_address(ip)
        except ValueError as exc:
            raise ValueError("请输入合法的 IP 地址，例如 8.8.8.8 或 2001:4860:4860::8888") from exc
    elif _is_public(client_ip):
        ip, source = client_ip, "client"
    else:
        # 回环 / 内网（本地开发必然如此）→ 改为查询本机出口公网 IP
        ip, source = "", "egress"

    try:
        raw = _fetch(API + ip)
    except Exception as exc:  # noqa: BLE001
        raise RuntimeError(f"查询 ipwho.is 失败：{exc}") from exc

    if not raw.get("success", True):
        raise RuntimeError(raw.get("message") or "查询失败，请确认该 IP 是否为公网地址")

    return _normalize(raw, ip, source)
