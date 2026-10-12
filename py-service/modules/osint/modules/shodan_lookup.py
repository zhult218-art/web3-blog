# -*- coding: utf-8 -*-
"""Shodan 查询模块（移植自 SpiderFoot sfp_shodan）。

需要 SHODAN_API_KEY 环境变量；未配置时模块标记不可用但不会报错。
监听 IP_ADDRESS 事件，产生 SHODAN_RESULT 事件。
"""
from __future__ import annotations

import json
import os
import urllib.request

from ..engine import (
    IP_ADDRESS, SHODAN_RESULT,
    OsintEvent, OsintModule, register_module,
)


class ShodanModule(OsintModule):
    meta = {
        "name": "shodan_lookup",
        "title": "Shodan 查询",
        "summary": "通过 Shodan API 查询 IP 的开放端口、服务、漏洞信息",
        "category": "Reconnaissance",
        "requires_key": "SHODAN_API_KEY",
    }

    opts = {}

    def watched_events(self) -> list[str]:
        return [IP_ADDRESS]

    def produced_events(self) -> list[str]:
        return [SHODAN_RESULT]

    def is_available(self) -> bool:
        return bool(os.environ.get("SHODAN_API_KEY"))

    def handle_event(self, event, target, emit) -> None:
        api_key = os.environ.get("SHODAN_API_KEY", "")
        if not api_key:
            return

        ip = event.data
        url = f"https://api.shodan.io/shodan/host/{ip}?key={api_key}"

        try:
            req = urllib.request.Request(url, headers={"User-Agent": "osint-scan"})
            with urllib.request.urlopen(req, timeout=15) as resp:
                data = json.loads(resp.read().decode("utf-8"))
        except Exception:  # noqa: BLE001
            return

        # 提取关键信息
        info_parts = []
        org = data.get("org", "")
        if org:
            info_parts.append(f"org: {org}")

        ports = data.get("ports", [])
        if ports:
            info_parts.append(f"ports: {','.join(map(str, ports))}")

        services = data.get("data", [])
        for svc in services[:5]:
            product = svc.get("product", "")
            port = svc.get("port", "")
            version = svc.get("version", "")
            banner = svc.get("data", "")[:80]
            if product:
                line = f"{port}/{product}"
                if version:
                    line += f" {version}"
                info_parts.append(line)
            if banner and product:
                info_parts.append(f"banner: {banner}")

        vulns = data.get("vulns", [])
        if vulns:
            info_parts.append(f"vulns: {','.join(vulns[:10])}")

        if info_parts:
            info_str = " | ".join(info_parts)
            emit(OsintEvent(SHODAN_RESULT, info_str[:1000],
                            self.meta["name"], event))


register_module(ShodanModule())
