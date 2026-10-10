# -*- coding: utf-8 -*-
"""时光回溯 · QQ 空间说说备份模块。路径前缀 /api/qzone。"""
from __future__ import annotations

import shutil
import threading

from . import config, store
from .router import router

MODULE_META = {
    "title": "QQ空间备份",
    "description": "扫码登录，后台分页抓取说说并导出 JSON/HTML/CSV",
    "icon": "🕰️",
    "version": "1.0.0",
}


def startup() -> None:
    config.DATA_DIR.mkdir(parents=True, exist_ok=True)
    # 上次残留任务目录清除（临时数据不跨进程保留）
    for name in __import__("os").listdir(config.DATA_DIR):
        p = config.DATA_DIR / name
        if p.is_dir():
            shutil.rmtree(p, ignore_errors=True)
    threading.Thread(target=store.reap_loop, daemon=True).start()


def health() -> dict:
    return {
        "ok": True,
        "running": store.running_count,
        "qr_sessions": len(store.qr_sessions),
    }


__all__ = ["router", "MODULE_META", "startup", "health"]
