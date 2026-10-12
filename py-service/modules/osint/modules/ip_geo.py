# -*- coding: utf-8 -*-
"""IP 地理定位模块（SpiderFoot sfp_ipinfo / sfp_ipgeo 对位实现）。

使用免费 ip-api.com（无需 API Key，每分钟 45 次限制）。
监听 IP_ADDRESS 事件，产生 IP_INFO 事件。
"""
from __future__ import annotations

import json
import os
import urllib.request

from ..engine import (
    IP_ADDRESS, IP_INFO,
    OsintEvent, OsintModule, register_module,
)


class IpGeoModule(OsintModule):
    meta = {
        "name": "ip_geo",
        "title": "IP 地理定位",
        "summary": "查询 IP 地址的地理位置、ISP、ASN 信息（ip-api.com）",
        "category": "Geolocation",
    }

    opts = {
        "api_url": "http://ip-api.com/json/",
        "fields": "status,country,countryCode,region,regionName,city,zip,lat,lon,timezone,isp,as,asname,org,reverse,query",
    }

    def watched_events(self) -> list[str]:
        return [IP_ADDRESS]

    def produced_events(self) -> list[str]:
        return [IP_INFO]

    def handle_event(self, event, target, emit) -> None:
        ip = event.data
        url = f"{self.opts['api_url']}{ip}?fields={self.opts['fields']}"

        try:
            req = urllib.request.Request(url, headers={"User-Agent": "osint-scan"})
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.loads(resp.read().decode("utf-8"))
        except Exception:  # noqa: BLE001
            return

        if data.get("status") != "success":
            return

        # 格式化为可读信息
        info_parts = []
        for key in ("country", "regionName", "city", "zip", "timezone", "isp", "as", "org", "lat", "lon"):
            val = data.get(key)
            if val:
                info_parts.append(f"{key}: {val}")
        info_str = " | ".join(info_parts)
        emit(OsintEvent(IP_INFO, info_str, self.meta["name"], event))


register_module(IpGeoModule())
