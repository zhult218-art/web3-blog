# -*- coding: utf-8 -*-
"""系统监控模块（系统资源 / 服务存活 / 中间件 / 网关安全 / 崩溃事件）。

纯只读采集：不依赖 redis-py（内置极简 RESP 客户端），子进程探针带 TTL 缓存。
路径前缀 /api/monitor。
"""
from __future__ import annotations

from .router import router
from . import collector

MODULE_META = {
    "title": "系统监控",
    "description": "CPU/内存/提交内存/GPU、服务存活、MySQL/Redis/Nacos、网关安全与崩溃事件",
    "icon": "📊",
    "version": "1.0.0",
}


def health() -> dict:
    return {"ok": True, "gpu": collector.gpu_available()}


__all__ = ["router", "MODULE_META", "health", "collector"]
