# -*- coding: utf-8 -*-
"""
wifi 攻击引擎会话管理：以子进程启动 wifiphisher / wifite，后台读取实时日志，
支持停止。仅在 Linux 且引擎可用时允许启动；参数以列表传递，不走 shell。
"""
from __future__ import annotations

import secrets
import subprocess
import sys
import threading
import datetime

from . import config
from .capabilities import IS_LINUX, engines
from .process import decode_output


def _now() -> str:
    return datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")


class Session:
    def __init__(self, sid: str, engine: str, argv: list[str]):
        self.sid = sid
        self.engine = engine
        self.argv = argv
        self.status = "starting"
        self.returncode: int | None = None
        self.logs: list[str] = []
        self.started_at = _now()
        self.finished_at = ""
        self.process: subprocess.Popen | None = None
        self._thread: threading.Thread | None = None

    def to_dict(self) -> dict:
        return {
            "sid": self.sid, "engine": self.engine, "argv": self.argv,
            "status": self.status, "returncode": self.returncode,
            "started_at": self.started_at, "finished_at": self.finished_at,
        }


_sessions: dict[str, Session] = {}
_lock = threading.Lock()


def _engine_mode(engine: str) -> str:
    for info in engines():
        if info["name"] == engine:
            return info["mode"]
    return "unavailable"


def _build_argv(engine: str, mode: str, args: list[str]) -> list[str]:
    if engine == "wifiphisher":
        if mode == "system":
            return ["wifiphisher", *args]
        return ["python3", str(config.WIFIPHISHER_BIN), *args]
    if engine == "wifite":
        if mode == "system":
            return ["wifite", *args]
        return ["python2", str(config.WIFITE_SCRIPT), *args]
    raise ValueError(f"未知引擎：{engine}")


def start_session(engine: str, args: list[str]) -> Session:
    if not IS_LINUX:
        raise PermissionError("当前系统非 Linux，无法启动无线攻击引擎")
    mode = _engine_mode(engine)
    if mode == "unavailable":
        raise PermissionError(f"引擎 {engine} 不可用（见 /api/wifi/capabilities）")

    argv = _build_argv(engine, mode, args)
    sid = secrets.token_urlsafe(12)
    session = Session(sid, engine, argv)

    creationflags = getattr(subprocess, "CREATE_NO_WINDOW", 0) if sys.platform.startswith("win") else 0
    proc = subprocess.Popen(
        argv,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        bufsize=0,
        creationflags=creationflags,
    )
    session.process = proc
    session.status = "running"
    session.logs.append(f"$ {' '.join(argv)}")

    with _lock:
        _sessions[sid] = session

    thread = threading.Thread(target=_reader, args=(session,), daemon=True)
    session._thread = thread
    thread.start()
    return session


def _reader(session: Session) -> None:
    proc = session.process
    assert proc is not None and proc.stdout is not None
    try:
        for raw in iter(proc.stdout.readline, b""):
            text = decode_output(raw).rstrip("\r\n")
            if text:
                session.logs.append(text)
    finally:
        proc.wait()
        session.returncode = proc.returncode
        session.status = "done" if proc.returncode == 0 else "failed"
        session.finished_at = _now()
        session.logs.append(f"[进程退出 code={proc.returncode}]")


def stop_session(sid: str) -> Session:
    session = _sessions.get(sid)
    if session is None:
        raise KeyError("会话不存在")
    proc = session.process
    if proc and proc.poll() is None:
        proc.terminate()
        try:
            proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            proc.kill()
        session.logs.append("[已请求停止]")
    return session


def get_session(sid: str) -> Session | None:
    return _sessions.get(sid)


def list_sessions() -> list[Session]:
    return list(_sessions.values())
