# -*- coding: utf-8 -*-
"""DNS 解析模块（移植自 SpiderFoot sfp_dnsresolve）。

使用标准库 socket.getaddrinfo 实现 A/AAAA 解析，无外部依赖。
监听 INTERNET_NAME / IP_ADDRESS 事件，产生 IP_ADDRESS / INTERNET_NAME 事件。
"""
from __future__ import annotations

import socket

from ..engine import (
    DOMAIN_NAME, INTERNET_NAME, IP_ADDRESS, IPV6_ADDRESS,
    OsintEvent, OsintModule, register_module,
)


class DnsResolveModule(OsintModule):
    meta = {
        "name": "dns_resolve",
        "title": "DNS 解析",
        "summary": "将域名解析为 IP 地址，反向解析 IP 为域名",
        "category": "DNS",
    }

    opts = {
        "resolve_ipv6": True,
        "reverse_resolve": True,
    }

    def watched_events(self) -> list[str]:
        return [INTERNET_NAME, IP_ADDRESS, IPV6_ADDRESS, DOMAIN_NAME]

    def produced_events(self) -> list[str]:
        return [IP_ADDRESS, IPV6_ADDRESS, INTERNET_NAME, DOMAIN_NAME]

    def handle_event(self, event, target, emit) -> None:
        data = event.data

        if event.event_type in (INTERNET_NAME, DOMAIN_NAME):
            # 正向解析：域名 → IP
            try:
                results = socket.getaddrinfo(data, None, proto=socket.IPPROTO_TCP)
            except socket.gaierror:
                return

            seen = set()
            for fam, _, _, _, sa in results:
                ip = sa[0]
                if ip in seen or ip == data:
                    continue
                seen.add(ip)

                if ":" in ip:
                    evt = OsintEvent(IPV6_ADDRESS, ip, self.meta["name"], event)
                else:
                    evt = OsintEvent(IP_ADDRESS, ip, self.meta["name"], event)
                emit(evt)

            # 反向解析验证
            if self.opts.get("reverse_resolve"):
                for ip in seen:
                    try:
                        names = socket.gethostbyaddr(ip)
                        for name in names:
                            if name and name != data and not name.startswith(ip):
                                emit(OsintEvent(INTERNET_NAME, name,
                                                self.meta["name"], event))
                    except (socket.herror, socket.gaierror):
                        pass

        elif event.event_type in (IP_ADDRESS, IPV6_ADDRESS):
            # 反向解析：IP → 域名
            if not self.opts.get("reverse_resolve"):
                return
            try:
                hostname, _, _ = socket.gethostbyaddr(data)
                if hostname and hostname != data:
                    emit(OsintEvent(INTERNET_NAME, hostname,
                                    self.meta["name"], event))
            except (socket.herror, socket.gaierror):
                pass


register_module(DnsResolveModule())
