# -*- coding: utf-8 -*-
"""
WiFi 安全审计模块（混合方案）。

• Windows：netsh 实现真实的周边扫描 / 网卡信息 / 已存配置（含本机配置审计）；
• Linux：能力检测 + 内置 wifiphisher（流氓 AP 钓鱼）与 wifite（自动无线审计）
  的子进程封装，实时日志 WebSocket。需要 root 与监听/注入网卡。
路径前缀 /api/wifi。
"""
from __future__ import annotations

from .router import router
from .capabilities import summary

MODULE_META = {
    "title": "WiFi 安全审计",
    "description": "周边网络扫描 / 网卡与配置审计 / wifiphisher·wifite 引擎封装",
    "icon": "📶",
    "version": "1.0.0",
}


def health() -> dict:
    info = summary()
    return {"ok": True, "platform": info["platform"], "engines": info["engines"]}


__all__ = ["router", "MODULE_META", "health"]
