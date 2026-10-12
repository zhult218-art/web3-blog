# -*- coding: utf-8 -*-
"""
OSINT 信息收集模块（移植自 SpiderFoot 核心架构）。

事件驱动扫描引擎：目标解析 → 模块调度 → 事件链传播 → 结果聚合。
内置模块：DNS 解析、WHOIS 查询、子域名枚举、端口扫描、IP 地理定位、Shodan 查询。
路径前缀 /api/osint。
"""
from __future__ import annotations

from .router import router
from .engine import list_modules

MODULE_META = {
    "title": "OSINT 信息收集",
    "description": "DNS / WHOIS / 子域名 / 端口扫描 / IP 地理 / Shodan",
    "icon": "🔍",
    "version": "1.0.0",
}


def health() -> dict:
    mods = list_modules()
    return {"ok": True, "modules": len(mods), "names": [m["name"] for m in mods]}


__all__ = ["router", "MODULE_META", "health"]
