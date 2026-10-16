# -*- coding: utf-8 -*-
"""monitor 路由：统一前缀 /api/monitor（前端经 /monitor 代理重写到此前缀）。"""
from __future__ import annotations

from fastapi import APIRouter

from . import collector

router = APIRouter(prefix="/api/monitor", tags=["monitor"])


def _safe(fn, empty):
    """确保任何采集异常都不会抛出到端点外层。"""
    try:
        return fn()
    except Exception:  # noqa: BLE001
        return empty


@router.get("/overview")
async def overview() -> dict:
    """仪表盘单次合并轮询：system + services + middleware。"""
    return _safe(collector.overview_snapshot, {"ts": 0, "system": {}, "services": {}, "middleware": {}})


@router.get("/system")
async def system() -> dict:
    """主机资源：CPU / 内存 / 提交内存 / 交换 / 磁盘 / 网络 / GPU / 告警。"""
    return _safe(collector.system_snapshot, {})


@router.get("/services")
async def services() -> dict:
    """Java/Node/Python/Vite 服务存活与进程指标。"""
    return _safe(collector.services_snapshot, {"summary": {}, "services": []})


@router.get("/middleware")
async def middleware() -> dict:
    """中间件：MySQL / Redis / Nacos。"""
    return _safe(collector.middleware_snapshot, {"items": []})


@router.get("/security")
async def security() -> dict:
    """网关安全：IP 黑名单 / 限流桶 / 日志事件。"""
    return _safe(collector.security_snapshot,
                 {"summary": {}, "blocklist": [], "rate_limited": [], "events": []})


@router.get("/events")
async def events() -> dict:
    """事件：JVM 崩溃转储 / 看门狗 / 滚动重启日志。"""
    return _safe(collector.events_snapshot,
                 {"crashes": [], "watchdog": {"path": "", "exists": False, "tail": []},
                  "roll": {"path": "", "exists": False, "tail": []}})
