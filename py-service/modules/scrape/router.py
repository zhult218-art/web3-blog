# -*- coding: utf-8 -*-
"""
网页采集路由（/api/scrape）。

端点：
  POST /api/scrape/fetch    抓取页面 → HTML / 纯文本 / Markdown + 元信息
  POST /api/scrape/query    选择器查询（css / xpath / 自适应），支持字段抽取
  POST /api/scrape/sitemap  解析 sitemap.xml / RSS / Atom
"""
from __future__ import annotations

import time
from typing import Any, Dict, Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from . import fetcher

# 免鉴权：与 video(VIP解析) 等在线工具一致，页面侧由前端 perm='tools' 网关；
# 抓取侧已做 SSRF 防护（仅允许公网 http/https）。
router = APIRouter(prefix="/api/scrape", tags=["scrape"])

_MAX_HTML = 400_000  # 回传给前端的 HTML 上限，避免超大页面拖垮浏览器


class FetchBody(BaseModel):
    url: str = Field(..., description="目标页面地址")
    extract: Optional[list[str]] = Field(default=None, description="需要的内容类型：html/text/markdown")
    timeout: int = Field(default=20, ge=3, le=120)
    impersonate: str = Field(default="chrome", description="curl_cffi 指纹伪装浏览器")
    headers: Optional[Dict[str, str]] = None


class QueryBody(BaseModel):
    selector: str = Field(..., description="css 或 xpath 表达式")
    type: str = Field(default="css", description="css | xpath")
    url: Optional[str] = Field(default=None, description="直接抓取该地址后查询")
    html: Optional[str] = Field(default=None, description="使用已有 HTML 查询")
    limit: int = Field(default=50, ge=1, le=500)
    fields: Optional[Dict[str, str]] = Field(default=None, description="字段名 → 子选择器，取属性用 @attr")
    adaptive: bool = Field(default=False, description="启用自适应定位（元素变动后仍可命中）")
    save: bool = Field(default=False, description="保存本次选择器定位结果供后续自适应使用")
    join_url: bool = Field(default=True, description="把相对链接补全为绝对地址")
    timeout: int = Field(default=20, ge=3, le=120)


class SitemapBody(BaseModel):
    url: str = Field(..., description="sitemap.xml / feed 地址")
    timeout: int = Field(default=20, ge=3, le=120)


def _fail(exc: Exception) -> HTTPException:
    return HTTPException(status_code=400, detail=str(exc))


@router.post("/fetch")
def fetch(body: FetchBody) -> Dict[str, Any]:
    """抓取页面并返回正文（HTML / 文本 / Markdown）与统计信息。"""
    extract = body.extract or ["html", "text", "markdown"]
    started = time.time()
    try:
        resp = fetcher.http_fetch(
            body.url, timeout=body.timeout, headers=body.headers, impersonate=body.impersonate
        )
    except fetcher.ScrapeError as exc:
        raise _fail(exc) from exc
    html = fetcher._decode(resp)  # noqa: SLF001  同模块内部函数
    final_url = str(getattr(resp, "url", "") or body.url)
    ua = str(getattr(resp, "headers", {}).get("content-type", "") if hasattr(resp, "headers") else "")
    data: Dict[str, Any] = {
        "ok": True,
        "status": int(getattr(resp, "status_code", 0) or 0),
        "url": final_url,
        "title": fetcher.page_title(html),
        "contentType": ua,
        "bytes": len(resp.content or b""),
        "chars": len(html),
        "elapsed": round(time.time() - started, 3),
        "truncated": len(html) > _MAX_HTML,
    }
    if "html" in extract:
        data["html"] = html[:_MAX_HTML]
    if "text" in extract:
        data["text"] = fetcher.html_to_text(html)
    if "markdown" in extract:
        data["markdown"] = fetcher.html_to_markdown(html, base=final_url)
    return data


@router.post("/query")
def query(body: QueryBody) -> Dict[str, Any]:
    """在页面（或已抓取 HTML）上执行选择器查询。"""
    html = body.html
    url = body.url or ""
    if not html:
        if not body.url:
            raise HTTPException(status_code=400, detail="必须提供 url 或 html")
        try:
            resp = fetcher.http_fetch(body.url, timeout=body.timeout)
        except fetcher.ScrapeError as exc:
            raise _fail(exc) from exc
        html = fetcher._decode(resp)  # noqa: SLF001
        url = str(getattr(resp, "url", "") or body.url)
    try:
        items = fetcher.run_query(
            html,
            url=url,
            selector=body.selector,
            qtype=body.type,
            limit=body.limit,
            fields=body.fields,
            adaptive=body.adaptive,
            save=body.save,
            join_url=body.join_url,
        )
    except fetcher.ScrapeError as exc:
        raise _fail(exc) from exc
    return {"ok": True, "count": len(items), "items": items, "url": url}


@router.post("/sitemap")
def sitemap(body: SitemapBody) -> Dict[str, Any]:
    """解析 sitemap.xml / RSS / Atom 订阅，返回条目列表。"""
    try:
        resp = fetcher.http_fetch(body.url, timeout=body.timeout)
        xml_text = fetcher._decode(resp)  # noqa: SLF001
        kind, entries = fetcher.parse_feed(xml_text)
    except fetcher.ScrapeError as exc:
        raise _fail(exc) from exc
    return {"ok": True, "kind": kind, "count": len(entries), "entries": entries}
