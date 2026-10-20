# -*- coding: utf-8 -*-
"""
网页采集模块（轻量档，移植自 D4Vinci/Scrapling 的解析能力）。

- 抓取：curl_cffi（浏览器 TLS 指纹伪装），不含任何浏览器内核依赖
- 解析：scrapling.parser.Selector（CSS / XPath / 自适应定位）
- 转换：内置 HTML → Markdown / 纯文本
- 附加：sitemap.xml / RSS / Atom 订阅解析

路径前缀 /api/scrape。
"""
from __future__ import annotations

from .router import router

MODULE_META = {
    "title": "网页采集",
    "description": "静态抓取 / CSS·XPath 选择器 / 自适应定位 / HTML 转 Markdown / sitemap·feed 解析",
    "icon": "🕸️",
    "version": "1.0.0",
}


def health() -> dict:
    try:
        import curl_cffi  # noqa: F401
        import scrapling  # noqa: F401

        return {"ok": True, "engine": "curl_cffi + scrapling.Selector", "browser": False}
    except Exception as exc:  # noqa: BLE001
        return {"ok": False, "error": str(exc)}


__all__ = ["router", "MODULE_META", "health"]
