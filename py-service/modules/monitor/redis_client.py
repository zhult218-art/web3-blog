# -*- coding: utf-8 -*-
"""极简 Redis RESP 客户端（纯 socket 实现）。

刻意不依赖 redis-py —— 本仓库 venv 未安装 redis-py，而系统监控只需要
INFO / DBSIZE / SMEMBERS / SCAN / HGETALL 等少量只读命令，用 RESP2 协议
自行编码即可，避免引入新依赖。
"""
from __future__ import annotations

import socket
from typing import Any, List, Optional


class RedisError(Exception):
    """RESP 协议或网络错误。"""


class RespClient:
    """同步 RESP 客户端，支持上下文管理（with RespClient(...) as c:）。"""

    def __init__(self, host: str, port: int, timeout: float = 2.0, password: str = ""):
        self.host = host
        self.port = int(port)
        self.timeout = timeout
        self.password = password or ""
        self._sock: Optional[socket.socket] = None
        self._buf = b""

    # ── 连接管理 ──────────────────────────────────────────────
    def connect(self) -> "RespClient":
        self._sock = socket.create_connection((self.host, self.port), self.timeout)
        self._sock.settimeout(self.timeout)
        self._buf = b""
        if self.password:
            self.command("AUTH", self.password)
        return self

    def close(self) -> None:
        if self._sock is not None:
            try:
                self._sock.close()
            except OSError:
                pass
            self._sock = None

    def __enter__(self) -> "RespClient":
        return self.connect()

    def __exit__(self, *exc) -> None:
        self.close()

    # ── 编码 / 读取 ──────────────────────────────────────────
    def _send(self, args: List[Any]) -> None:
        assert self._sock is not None, "未连接"
        out = b"*%d\r\n" % len(args)
        for a in args:
            if isinstance(a, str):
                a = a.encode("utf-8")
            elif not isinstance(a, (bytes, bytearray)):
                a = str(a).encode("utf-8")
            out += b"$%d\r\n%s\r\n" % (len(a), a)
        self._sock.sendall(out)

    def _read_line(self) -> bytes:
        while b"\r\n" not in self._buf:
            data = self._sock.recv(65536)  # type: ignore[union-attr]
            if not data:
                raise RedisError("连接被关闭")
            self._buf += data
        line, _, self._buf = self._buf.partition(b"\r\n")
        return line

    def _read_body(self, n: int) -> bytes:
        while len(self._buf) < n + 2:
            data = self._sock.recv(65536)  # type: ignore[union-attr]
            if not data:
                raise RedisError("连接被关闭")
            self._buf += data
        chunk = self._buf[:n]
        self._buf = self._buf[n + 2:]
        return chunk

    def _read_reply(self) -> Any:
        line = self._read_line()
        kind, rest = line[:1], line[1:]
        if kind == b"+":
            return rest.decode("utf-8", "replace")
        if kind == b"-":
            raise RedisError(rest.decode("utf-8", "replace"))
        if kind == b":":
            return int(rest)
        if kind == b"$":
            n = int(rest)
            return None if n == -1 else self._read_body(n).decode("utf-8", "replace")
        if kind == b"*":
            n = int(rest)
            return None if n == -1 else [self._read_reply() for _ in range(n)]
        raise RedisError("未知响应：%r" % line[:32])

    def command(self, *args: Any) -> Any:
        """执行一条命令并返回解析后的结果（str / int / list / None）。"""
        self._send(list(args))
        return self._read_reply()
