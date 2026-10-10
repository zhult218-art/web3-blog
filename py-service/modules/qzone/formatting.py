# -*- coding: utf-8 -*-
"""qzone 用户隔离 / 媒体代理 / 表情解析 / 结构压平（纯函数）。"""
from __future__ import annotations

import html

from urllib.parse import quote as url_quote

from . import config
from .protocol import fmt_ts


def is_qq_media(url: str) -> bool:
    """仅放行 QQ 域名下的图片/视频/表情，防止被滥用为开放代理。"""
    if not url:
        return False
    u = url.lower()
    if not (u.startswith("http://") or u.startswith("https://")):
        return False
    rest = u.split("://", 1)[1]
    host = rest.split("/", 1)[0].split("?", 1)[0]
    host = host.split(":", 1)[0]
    return any(host == s[1:] or host.endswith(s) for s in config.QQ_MEDIA_HOST_SUFFIXES)


def blog_uid(x_blog_uid: str | None) -> str:
    """从 X-Blog-Uid 头提取博客用户 ID，用于隔离扫码会话与任务。"""
    return (x_blog_uid or "").strip()


def proxy_url(url: str, kind: str = "img") -> str:
    """把 QQ 媒体 URL 改写为后端 /qzapi 代理 URL（绕过防盗链）。"""
    if not url or not is_qq_media(url):
        return url or ""
    return f"/qzapi/proxy?url={url_quote(url, safe='')}&type={kind}"


def parse_emojis(text: str) -> str:
    """content 中的 [em]eN[/em] 替换为代理同源 <img>（先转义防 XSS）。"""
    if not text:
        return ""
    safe = html.escape(text)

    def repl(m):
        eid = m.group(1)
        src = proxy_url(config.EMOJI_URL_TPL.format(eid), "img")
        return (f'<img class="qz-emo" src="{html.escape(src)}" '
                f'alt="[e{eid}]" loading="lazy" />')

    return config.EMOJI_RE.sub(repl, safe)


def parse_emojis_export(text: str) -> str:
    """导出 HTML 用表情解析：使用原始 URL，并把换行转 <br>。"""
    if not text:
        return ""
    safe = html.escape(text).replace("\n", "<br>")

    def repl(m):
        eid = m.group(1)
        src = config.EMOJI_URL_TPL.format(eid)
        return (f'<img class="qz-emo" src="{html.escape(src)}" '
                f'alt="[e{eid}]" loading="lazy" />')

    return config.EMOJI_RE.sub(repl, safe)


def normalize_moment(item: dict) -> dict:
    """把腾讯原始结构压平为预览/导出用的干净结构。"""
    pics, videos = [], []
    for p in item.get("pic") or []:
        url = p.get("url1") or p.get("smallurl") or ""
        if url:
            pics.append({"url": proxy_url(url, "img"),
                         "small": proxy_url(p.get("smallurl") or url, "img"),
                         "origin": url,
                         "w": p.get("width"), "h": p.get("height")})
    for v in item.get("video") or []:
        url = v.get("url1") or v.get("url") or ""
        if url:
            videos.append({"url": proxy_url(url, "video"), "origin": url})

    comments = []
    for c in item.get("commentlist") or []:
        replies = []
        for r3 in c.get("list_3") or []:
            raw_rc = r3.get("content", "")
            replies.append({"name": r3.get("name", ""),
                            "content": raw_rc,
                            "content_html": parse_emojis(raw_rc),
                            "time": r3.get("createTime2", "")})
        raw_cc = c.get("content", "")
        comments.append({"name": c.get("name", ""), "uin": c.get("uin", ""),
                         "time": c.get("createTime2", ""),
                         "content": raw_cc,
                         "content_html": parse_emojis(raw_cc),
                         "replies": replies})

    raw_content = item.get("content") or ""
    return {
        "tid": item.get("tid", ""),
        "time": fmt_ts(item.get("created_time")),
        "content": raw_content,
        "content_html": parse_emojis(raw_content),
        "name": item.get("name", ""),
        "secret": item.get("secret", 0),
        "cmtnum": item.get("cmtnum", 0),
        "fwdnum": item.get("fwdnum", 0),
        "source": item.get("source_name", ""),
        "lbs": (item.get("lbs") or {}).get("name", ""),
        "pics": pics,
        "videos": videos,
        "comments": comments,
    }
