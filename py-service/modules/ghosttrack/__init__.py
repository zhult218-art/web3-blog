# -*- coding: utf-8 -*-
"""
GhostTrack 网络信息追踪模块（移植自 GhostTrack）。

三类单次查询（无状态，不跑事件链）：
  • IP 追踪     —— ipwho.is 公开接口；可查任意 IP，也可查「你的公网 IP」
  • 手机号解析  —— phonenumbers 本地库（Google libphonenumber 移植，纯离线解析，不出网）
  • 用户名跨站  —— 白名单站点并发探测账号存在性

路径前缀 /api/ghosttrack。免鉴权：与 scrape / video 等在线工具一致，页面侧由前端
perm='tools' 网关；后端只做参数校验与 SSRF 防护（目标主机全部来自固定模板或已校验 IP）。
"""
from __future__ import annotations

from . import phone_lookup
from .router import router

MODULE_META = {
    "title": "GhostTrack 网络追踪",
    "description": "IP 追踪 / 手机号归属解析 / 用户名跨站检测",
    "icon": "👻",
    "version": "1.0.0",
}


def health() -> dict:
    return {"ok": True, "phonenumbers": phone_lookup.AVAILABLE}


__all__ = ["router", "MODULE_META", "health"]
