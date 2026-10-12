# -*- coding: utf-8 -*-
"""TCP 端口扫描模块（SpiderFoot sfp_portscan 对位实现）。

使用标准库 socket 实现 TCP connect 扫描（无需 root）。
默认扫描 Top-20 常用端口，可配置端口范围。
监听 IP_ADDRESS 事件，产生 TCP_PORT_OPEN / TCP_PORT_OPEN_BANNER 事件。
"""
from __future__ import annotations

import socket

from ..engine import (
    IP_ADDRESS, TCP_PORT_OPEN, TCP_PORT_OPEN_BANNER,
    OsintEvent, OsintModule, register_module,
)

# Top-20 常用端口
DEFAULT_PORTS = [
    21, 22, 23, 25, 53, 80, 110, 143, 443, 445,
    993, 995, 1433, 1521, 3306, 3389, 5432, 5900, 6379, 8080,
]

PORT_NAMES = {
    21: "FTP", 22: "SSH", 23: "Telnet", 25: "SMTP", 53: "DNS",
    80: "HTTP", 110: "POP3", 143: "IMAP", 443: "HTTPS", 445: "SMB",
    993: "IMAPS", 995: "POP3S", 1433: "MSSQL", 1521: "Oracle",
    3306: "MySQL", 3389: "RDP", 5432: "PostgreSQL", 5900: "VNC",
    6379: "Redis", 8080: "HTTP-Alt", 27017: "MongoDB", 9200: "Elasticsearch",
}


class PortScanModule(OsintModule):
    meta = {
        "name": "port_scan",
        "title": "TCP 端口扫描",
        "summary": "TCP connect 扫描常用端口，抓取服务 Banner",
        "category": "Reconnaissance",
    }

    opts = {
        "ports": DEFAULT_PORTS,
        "timeout": 1.5,
        "grab_banner": True,
        "max_ports": 20,
    }

    def watched_events(self) -> list[str]:
        return [IP_ADDRESS]

    def produced_events(self) -> list[str]:
        return [TCP_PORT_OPEN, TCP_PORT_OPEN_BANNER]

    def handle_event(self, event, target, emit) -> None:
        ip = event.data
        ports = self.opts.get("ports", DEFAULT_PORTS)[:self.opts.get("max_ports", 20)]
        timeout = self.opts.get("timeout", 1.5)

        for port in ports:
            try:
                with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                    s.settimeout(timeout)
                    result = s.connect_ex((ip, port))
                    if result == 0:
                        port_str = str(port)
                        service = PORT_NAMES.get(port, "")
                        label = f"{port}/{service}" if service else port_str
                        emit(OsintEvent(TCP_PORT_OPEN, label,
                                        self.meta["name"], event))

                        # 尝试抓取 Banner
                        if self.opts.get("grab_banner") and port in (21, 22, 25, 80, 110, 143, 443, 8080):
                            banner = self._grab_banner(ip, port, timeout)
                            if banner:
                                emit(OsintEvent(TCP_PORT_OPEN_BANNER,
                                                f"{label}: {banner[:100]}",
                                                self.meta["name"], event))
            except (socket.error, OSError):
                continue

    def _grab_banner(self, ip: str, port: int, timeout: float) -> str:
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                s.settimeout(timeout)
                s.connect_ex((ip, port))
                if port in (80, 443, 8080):
                    s.sendall(b"GET / HTTP/1.0\r\nHost: " + ip.encode() + b"\r\n\r\n")
                else:
                    # 等待服务主动发送 banner（FTP/SSH/SMTP 等）
                    pass
                data = s.recv(512)
                return data.decode("utf-8", errors="ignore").strip()
        except (socket.error, OSError):
            return ""


register_module(PortScanModule())
