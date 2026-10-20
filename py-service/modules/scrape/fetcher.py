# -*- coding: utf-8 -*-
"""
网页采集核心（轻量档）。

设计取舍：
- HTTP 抓取用 curl_cffi（浏览器 TLS/JA3 指纹伪装），**刻意不使用 playwright /
  patchright 等浏览器内核**——scrapling 0.5 的 Fetcher 在导入期就硬依赖 patchright
  （engines/toolbelt/convertor.py 顶层 import），故这里直接用 curl_cffi 发请求，
  只复用 scrapling 的解析层 scrapling.parser.Selector（含自适应定位能力）。
- HTML → Markdown 用自实现转换器（仅依赖 lxml），避免额外引入 markdownify 等包。
"""
from __future__ import annotations

import ipaddress
import os
import re
import socket
from html import unescape as html_unescape
from typing import Any, Dict, List, Optional, Tuple
from urllib.parse import urljoin, urlparse

from curl_cffi import requests as curl_requests
from lxml import etree, html as lxml_html
from scrapling.parser import Selector

# 允许访问内网地址（仅本地调试时通过环境变量开启，默认关闭以防 SSRF）
_ALLOW_PRIVATE = os.getenv("SCRAPE_ALLOW_PRIVATE", "").strip().lower() in {"1", "true", "yes", "on"}

DEFAULT_UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
)

_DROP_TAGS = {"script", "style", "noscript", "template", "svg", "iframe", "canvas", "head"}
_HEADINGS = {"h1": 1, "h2": 2, "h3": 3, "h4": 4, "h5": 5, "h6": 6}


class ScrapeError(Exception):
    """采集过程中的可预期错误（URL 非法 / 上游不可达等）。"""


# ──────────────────────────────────────────────
# SSRF 防护：仅允许公网 http/https，禁止内网与保留地址
# ──────────────────────────────────────────────
def _assert_public_url(url: str) -> str:
    url = (url or "").strip()
    if not url:
        raise ScrapeError("URL 不能为空")
    if "://" not in url:
        url = "http://" + url
    parsed = urlparse(url)
    if parsed.scheme not in ("http", "https"):
        raise ScrapeError("仅支持 http / https 协议")
    host = parsed.hostname
    if not host:
        raise ScrapeError("URL 缺少主机名")
    if _ALLOW_PRIVATE:
        return url
    # 字面量 IP 直接判定
    try:
        ip = ipaddress.ip_address(host)
        if _is_blocked_ip(ip):
            raise ScrapeError("禁止访问内网 / 保留地址")
        return url
    except ValueError:
        pass
    # 域名：解析后逐个校验，避免 DNS Rebinding 指向内网
    try:
        infos = socket.getaddrinfo(host, None)
    except socket.gaierror as exc:
        raise ScrapeError(f"域名解析失败：{host}") from exc
    for info in infos:
        try:
            ip = ipaddress.ip_address(info[4][0])
        except ValueError:
            continue
        if _is_blocked_ip(ip):
            raise ScrapeError("禁止访问内网 / 保留地址")
    return url


def _is_blocked_ip(ip: ipaddress._BaseAddress) -> bool:
    return bool(
        ip.is_private
        or ip.is_loopback
        or ip.is_link_local
        or ip.is_reserved
        or ip.is_multicast
        or ip.is_unspecified
    )


# ──────────────────────────────────────────────
# 抓取
# ──────────────────────────────────────────────
def http_fetch(
    url: str,
    *,
    timeout: int = 20,
    headers: Optional[Dict[str, str]] = None,
    impersonate: str = "chrome",
    method: str = "GET",
    max_bytes: int = 5 * 1024 * 1024,
) -> Any:
    """发起 HTTP 请求（curl_cffi，带浏览器指纹伪装），返回 curl_cffi Response。"""
    safe_url = _assert_public_url(url)
    hdrs = {
        "User-Agent": DEFAULT_UA,
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
    }
    if headers:
        for k, v in headers.items():
            if k and v is not None:
                hdrs[str(k)] = str(v)
    try:
        resp = curl_requests.request(
            method.upper(),
            safe_url,
            headers=hdrs,
            timeout=max(3, min(int(timeout or 20), 120)),
            impersonate=impersonate or None,
            allow_redirects=True,
        )
    except Exception as exc:  # noqa: BLE001  curl_cffi 异常类型不稳定，统一包装
        raise ScrapeError(f"请求失败：{exc}") from exc
    if len(resp.content or b"") > max_bytes:
        raise ScrapeError(f"响应体过大（>{max_bytes // 1024}KB），已拒绝解析")
    return resp


def _decode(resp: Any) -> str:
    """稳健解码：优先 let curl_cffi 的 .text（内部做过 charset 嗅探），失败再回退。"""
    try:
        text = resp.text
        if isinstance(text, str):
            return text
    except Exception:  # noqa: BLE001
        pass
    raw = resp.content or b""
    for enc in ("utf-8", "gb18030", "latin-1"):
        try:
            return raw.decode(enc, errors="replace")
        except Exception:  # noqa: BLE001
            continue
    return ""


def page_title(html: str) -> str:
    """从 HTML 中提取 <title> 文本。"""
    try:
        root = lxml_html.fromstring(html)
        node = root.find(".//title")
        if node is not None and node.text:
            return node.text.strip()
    except Exception:  # noqa: BLE001
        pass
    m = re.search(r"<title[^>]*>(.*?)</title>", html or "", re.I | re.S)
    return re.sub(r"\s+", " ", m.group(1)).strip() if m else ""


def html_to_text(html: str) -> str:
    """抽取纯文本（保留块级换行）。"""
    try:
        sel = Selector(html)
        text = sel.get_all_text(strip=True)
        return re.sub(r"\n{3,}", "\n\n", text or "").strip()
    except Exception:  # noqa: BLE001
        return re.sub(r"<[^>]+>", " ", html or "").strip()


# ──────────────────────────────────────────────
# HTML → Markdown（自实现，仅 lxml）
# ──────────────────────────────────────────────
def _inline_text(node: Any, base: str) -> str:
    """把一个元素（含其子节点与 tail）转换成行内 Markdown。"""
    parts: List[str] = [node.text or ""]
    for child in node:
        parts.append(_inline_node(child, base))
    return "".join(parts)


def _inline_node(node: Any, base: str) -> str:
    tail = node.tail or ""
    if not isinstance(node.tag, str):  # 注释 / 处理指令
        return tail
    tag = node.tag
    if tag in _DROP_TAGS:
        return tail
    if tag == "br":
        return "  \n" + tail
    if tag == "img":
        src = node.get("src") or node.get("data-src") or ""
        if src:
            src = urljoin(base, src) if base else src
        alt = node.get("alt") or node.get("title") or ""
        return (f"![{alt}]({src})" if src else "") + tail
    inner = _inline_text(node, base)
    if tag == "a":
        href = node.get("href") or ""
        if href:
            href = urljoin(base, href) if base else href
            out = f"[{inner.strip()}]({href})" if inner.strip() else href
        else:
            out = inner
    elif tag in ("strong", "b"):
        out = f"**{inner.strip()}**" if inner.strip() else ""
    elif tag in ("em", "i"):
        out = f"*{inner.strip()}*" if inner.strip() else ""
    elif tag in ("del", "s", "strike"):
        out = f"~~{inner.strip()}~~" if inner.strip() else ""
    elif tag == "code":
        out = f"`{inner.strip()}`" if inner.strip() else ""
    else:
        out = inner
    return out + tail


def _walk_list(node: Any, out: List[str], base: str, depth: int) -> None:
    ordered = (node.tag or "").lower() == "ol"
    idx = 1
    for li in node:
        if not isinstance(li.tag, str) or li.tag.lower() != "li":
            continue
        # 行内内容（嵌套列表在 inline 阶段会被折叠，这里单独剔除后递归）
        nested = [c for c in li if isinstance(c.tag, str) and c.tag.lower() in ("ul", "ol")]
        for c in nested:
            li.remove(c)
        marker = f"{idx}. " if ordered else "- "
        text = _inline_text(li, base).strip()
        out.append("  " * depth + marker + text)
        for c in nested:
            _walk_list(c, out, base, depth + 1)
            li.append(c)
        idx += 1
    out.append("")


def _walk_table(node: Any, out: List[str], base: str) -> None:
    rows = node.xpath(".//tr")
    if not rows:
        return
    grid: List[List[str]] = []
    for tr in rows:
        cells = [c for c in tr if isinstance(c.tag, str) and c.tag.lower() in ("td", "th")]
        grid.append([_inline_text(c, base).replace("\n", " ").replace("|", "\\|").strip() for c in cells])
    if not grid:
        return
    width = max(len(r) for r in grid)
    for r in grid:
        r.extend([""] * (width - len(r)))
    out.append("| " + " | ".join(grid[0]) + " |")
    out.append("| " + " | ".join(["---"] * width) + " |")
    for r in grid[1:]:
        out.append("| " + " | ".join(r) + " |")
    out.append("")


def _walk(node: Any, out: List[str], base: str) -> None:
    if not isinstance(node.tag, str):  # 注释 / 处理指令
        return
    low = node.tag.lower()
    if low in _DROP_TAGS:
        return
    if low in _HEADINGS:
        text = _inline_text(node, base).strip()
        if text:
            out.append("#" * _HEADINGS[low] + " " + text)
            out.append("")
        return
    if low == "p":
        text = _inline_text(node, base).strip()
        if text:
            out.append(text)
            out.append("")
        return
    if low in ("ul", "ol"):
        _walk_list(node, out, base, 0)
        return
    if low == "table":
        _walk_table(node, out, base)
        return
    if low == "blockquote":
        text = _inline_text(node, base).strip()
        if text:
            out.append("> " + text.replace("\n", "\n> "))
            out.append("")
        return
    if low == "pre":
        code_el = node.find("code")
        code = (code_el if code_el is not None else node).itertext()
        code_text = "".join(code).rstrip()
        lang = ""
        cls = (code_el.get("class") if code_el is not None else "") or ""
        m = re.search(r"(?:language|lang)-([\w+-]+)", cls)
        if m:
            lang = m.group(1)
        if code_text:
            out.append(f"```{lang}")
            out.append(code_text)
            out.append("```")
            out.append("")
        return
    if low == "hr":
        out.append("---")
        out.append("")
        return
    # 其余容器：递归子节点；若自身直接文本则单列一段
    if node.text and node.text.strip() and low in ("div", "section", "article", "main", "td", "span", "li"):
        out.append(node.text.strip())
    for child in node:
        _walk(child, out, base)


def html_to_markdown(html: str, base: str = "") -> str:
    """把 HTML 转为可读 Markdown（标题 / 列表 / 代码块 / 表格 / 链接 / 图片）。"""
    if not (html or "").strip():
        return ""
    try:
        root = lxml_html.fromstring(html)
    except Exception:  # noqa: BLE001
        return html_to_text(html)
    out: List[str] = []
    _walk(root, out, base)
    text = "\n".join(out)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


# ──────────────────────────────────────────────
# 选择器查询（基于 scrapling Selector，支持自适应定位）
# ──────────────────────────────────────────────
def _identifier(selector: str, qtype: str) -> str:
    return f"scrape-{qtype}-{abs(hash(selector)) % (10 ** 10)}"


def run_query(
    html: str,
    *,
    url: str = "",
    selector: str,
    qtype: str = "css",
    limit: int = 50,
    fields: Optional[Dict[str, str]] = None,
    adaptive: bool = False,
    save: bool = False,
    join_url: bool = True,
) -> List[Dict[str, Any]]:
    """在 HTML 上执行 css / xpath 查询，返回结构化结果列表。"""
    selector = (selector or "").strip()
    if not selector:
        raise ScrapeError("选择器不能为空")
    sel = Selector(html, url=url, adaptive=True)
    ident = _identifier(selector, qtype)
    if qtype == "xpath":
        nodes = sel.xpath(selector, adaptive=adaptive, identifier=ident, auto_save=save)
    else:
        nodes = sel.css(selector, adaptive=adaptive, identifier=ident, auto_save=save)
    nodes = list(nodes)[: max(1, min(int(limit or 50), 500))]
    items: List[Dict[str, Any]] = []
    for el in nodes:
        attrs = dict(el.attrib or {})
        if join_url and url:
            for k in ("href", "src"):
                if attrs.get(k):
                    attrs[k] = urljoin(url, attrs[k])
        if fields:
            row: Dict[str, Any] = {"_text": (el.text or "").strip()}
            for name, sub in fields.items():
                row[name] = _extract_field(el, sub, url, join_url)
            items.append(row)
        else:
            items.append({
                "text": (el.text or "").strip(),
                "html": el.html_content,
                "attrs": attrs,
            })
    return items


def _extract_field(el: Any, sub: str, url: str, join_url: bool) -> str:
    """按子选择器从元素中取字段值；支持 `@attr` 语法直接取属性。"""
    sub = (sub or "").strip()
    if not sub:
        return ""
    if sub.startswith("@"):
        val = el.attrib.get(sub[1:], "")
        return urljoin(url, val) if (join_url and url and sub[1:] in ("href", "src") and val) else val
    try:
        found = el.css(sub)
        if found:
            return (found[0].text or "").strip()
    except Exception:  # noqa: BLE001
        pass
    return ""


# ──────────────────────────────────────────────
# sitemap / RSS / Atom 解析
# ──────────────────────────────────────────────
def parse_feed(xml_text: str) -> Tuple[str, List[Dict[str, str]]]:
    """解析 sitemap.xml / RSS / Atom，返回 (kind, entries)。"""
    parser = etree.XMLParser(recover=True, huge_tree=True, resolve_entities=False)
    raw = (xml_text or "").encode("utf-8", errors="replace")
    root = etree.fromstring(raw, parser=parser)
    if root is None:
        raise ScrapeError("XML 解析失败")
    tag = etree.QName(root).localname.lower()
    ns = {"sm": "http://www.sitemaps.org/schemas/sitemap/0.9"}
    entries: List[Dict[str, str]] = []
    if tag == "sitemapindex":
        kind = "sitemap"
        for loc in root.findall(".//sm:loc", ns) or root.findall(".//loc"):
            entries.append({"url": (loc.text or "").strip(), "title": "", "lastmod": "", "description": ""})
    elif tag == "urlset":
        kind = "sitemap"
        for url_el in root.findall(".//sm:url", ns) or root.findall(".//url"):
            loc = url_el.find("sm:loc", ns)
            lastmod = url_el.find("sm:lastmod", ns)
            entries.append({
                "url": (loc.text or "").strip() if loc is not None else "",
                "title": "", "lastmod": (lastmod.text or "").strip() if lastmod is not None else "",
                "description": "",
            })
    elif tag == "rss":
        kind = "feed"
        for item in root.findall(".//item"):
            entries.append({
                "url": (item.findtext("link") or "").strip(),
                "title": (item.findtext("title") or "").strip(),
                "lastmod": (item.findtext("pubDate") or "").strip(),
                "description": _strip_tag(item.findtext("description") or "")[:300],
            })
    elif tag == "feed":
        kind = "feed"
        atom_ns = {"a": "http://www.w3.org/2005/Atom"}
        for entry in root.findall("a:entry", atom_ns):
            link_el = entry.find("a:link", atom_ns)
            href = link_el.get("href") if link_el is not None else ""
            entries.append({
                "url": (href or "").strip(),
                "title": (entry.findtext("a:title", "", atom_ns) or "").strip(),
                "lastmod": (entry.findtext("a:updated", "", atom_ns) or "").strip(),
                "description": (entry.findtext("a:summary", "", atom_ns) or "").strip()[:300],
            })
    else:
        raise ScrapeError(f"未识别的订阅格式：<{tag}>")
    return kind, [e for e in entries if e.get("url")]


def _strip_tag(text: str) -> str:
    """剥离 HTML 标签并反转义实体，用于 RSS/Atom 摘要字段。"""
    return html_unescape(re.sub(r"<[^>]+>", "", text or "")).strip()
