# -*- coding: utf-8 -*-
"""wifi 模块：跨平台子进程工具（输出按控制台代码页解码，Windows 隐藏控制台窗口）。"""
from __future__ import annotations

import locale
import subprocess
import sys


def decode_output(data: bytes) -> str:
    encodings = []
    if sys.platform.startswith("win"):
        encodings = ["gbk", "cp936", locale.getpreferredencoding(False), "utf-8"]
    else:
        encodings = ["utf-8", locale.getpreferredencoding(False), "gbk"]
    for enc in encodings:
        if not enc:
            continue
        try:
            return data.decode(enc)
        except (UnicodeDecodeError, LookupError):
            continue
    return data.decode("utf-8", errors="ignore")


def run_command(argv, timeout: float = 25.0) -> tuple[int, str]:
    """运行命令，返回 (returncode, 解码后的合并输出)。"""
    creationflags = 0
    startupinfo = None
    if sys.platform.startswith("win"):
        creationflags = getattr(subprocess, "CREATE_NO_WINDOW", 0)
    try:
        proc = subprocess.run(
            argv,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            timeout=timeout,
            creationflags=creationflags,
        )
        return proc.returncode, decode_output(proc.stdout or b"")
    except subprocess.TimeoutExpired as exc:
        partial = decode_output(exc.stdout or b"") if isinstance(exc.stdout, bytes) else ""
        return 124, partial
    except FileNotFoundError as exc:
        return 127, str(exc)


def is_elevated() -> bool:
    """Windows：是否管理员；Linux/macOS：是否 root。"""
    if sys.platform.startswith("win"):
        try:
            import ctypes

            return bool(ctypes.windll.shell32.IsUserAnAdmin())
        except Exception:  # noqa: BLE001
            return False
    try:
        import os

        return os.geteuid() == 0
    except AttributeError:
        return False
