# -*- coding: utf-8 -*-
"""用户名跨站存在性检测（并发探测固定白名单站点，移植自 GhostTrack）。

安全说明（SSRF）：请求目标全部来自下面这张固定表，URL 只由「固定模板 + 已净化
用户名」拼出；用户名在路由层已被限制为 [A-Za-z0-9._-]{1,40}，不含 / : @ ? #
等可改变主机或路径的字符，因此用户输入无法把请求导到任意地址。

判定口径（诚实优先）：
  • mode='absent404'  —— 站点对不存在的账号返回 404，故 200 才判为命中，可信；
  • mode='ambiguous'  —— 站点对不存在的账号同样返回 200（SPA 空壳或跳登录页），
    状态码无法作为依据，只回链接并标注「需人工确认」，避免给出假命中。
其余状态（3xx 跳登录、403 反爬、429 限流、超时）一律记为 unknown。
"""
from __future__ import annotations

import asyncio
import re

import httpx

NAME_RE = re.compile(r"^[A-Za-z0-9._-]{1,40}$")

CONCURRENCY = 6        # 同时在飞的请求数上限，避免把对端打限流
TIMEOUT = 8.0          # 单站超时
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")

# 站点表：{u} 为用户名占位。mode 见模块说明。
SITES: list[dict] = [
    # ── 可信：不存在时返回 404 ──
    {"key": "github", "name": "GitHub", "url": "https://github.com/{u}", "mode": "absent404"},
    {"key": "gitlab", "name": "GitLab", "url": "https://gitlab.com/{u}", "mode": "absent404"},
    {"key": "gitee", "name": "Gitee", "url": "https://gitee.com/{u}", "mode": "absent404"},
    {"key": "codeberg", "name": "Codeberg", "url": "https://codeberg.org/{u}", "mode": "absent404"},
    {"key": "bitbucket", "name": "Bitbucket", "url": "https://bitbucket.org/{u}/", "mode": "absent404"},
    {"key": "npm", "name": "npm", "url": "https://www.npmjs.com/~{u}", "mode": "absent404"},
    {"key": "pypi", "name": "PyPI", "url": "https://pypi.org/user/{u}/", "mode": "absent404"},
    {"key": "dockerhub", "name": "Docker Hub", "url": "https://hub.docker.com/u/{u}", "mode": "absent404"},
    {"key": "youtube", "name": "YouTube", "url": "https://www.youtube.com/@{u}", "mode": "absent404"},
    {"key": "twitch", "name": "Twitch", "url": "https://www.twitch.tv/{u}", "mode": "absent404"},
    {"key": "medium", "name": "Medium", "url": "https://medium.com/@{u}", "mode": "absent404"},
    {"key": "keybase", "name": "Keybase", "url": "https://keybase.io/{u}", "mode": "absent404"},
    {"key": "dribbble", "name": "Dribbble", "url": "https://dribbble.com/{u}", "mode": "absent404"},
    {"key": "soundcloud", "name": "SoundCloud", "url": "https://soundcloud.com/{u}", "mode": "absent404"},
    {"key": "tiktok", "name": "TikTok", "url": "https://www.tiktok.com/@{u}", "mode": "absent404"},
    {"key": "zhihu", "name": "知乎", "url": "https://www.zhihu.com/people/{u}", "mode": "absent404"},
    # ── 需人工确认：对不存在账号也返回 200 ──
    {"key": "x", "name": "X (Twitter)", "url": "https://x.com/{u}", "mode": "ambiguous"},
    {"key": "instagram", "name": "Instagram", "url": "https://www.instagram.com/{u}/", "mode": "ambiguous"},
    {"key": "telegram", "name": "Telegram", "url": "https://t.me/{u}", "mode": "ambiguous"},
    {"key": "steam", "name": "Steam", "url": "https://steamcommunity.com/id/{u}", "mode": "ambiguous"},
    {"key": "ycombinator", "name": "Hacker News", "url": "https://news.ycombinator.com/user?id={u}", "mode": "ambiguous"},
    {"key": "weibo", "name": "微博", "url": "https://weibo.com/{u}", "mode": "ambiguous"},
]


def list_sites() -> list[dict]:
    """白名单站点清单（前端用于展示「将检测哪些站点」）。"""
    return [
        {"key": s["key"], "name": s["name"], "reliable": s["mode"] == "absent404"}
        for s in SITES
    ]


async def _probe(client: httpx.AsyncClient, site: dict, name: str, sem: asyncio.Semaphore) -> dict:
    url = site["url"].format(u=name)
    base = {"key": site["key"], "name": site["name"], "url": url,
            "reliable": site["mode"] == "absent404"}
    async with sem:
        try:
            # 流式打开：拿到状态码即关闭，不下载正文（部分站点首页体积很大）
            async with client.stream("GET", url) as resp:
                code = resp.status_code
        except httpx.TimeoutException:
            return {**base, "status": 0, "found": "unknown", "reason": "请求超时"}
        except Exception as exc:  # noqa: BLE001
            return {**base, "status": 0, "found": "unknown", "reason": f"请求失败：{exc.__class__.__name__}"}

    if site["mode"] == "ambiguous":
        return {**base, "status": code, "found": "unknown",
                "reason": "该站点对不存在账号也返回页面，需人工打开确认"}
    if code == 200:
        return {**base, "status": code, "found": "yes", "reason": "HTTP 200"}
    if code == 404:
        return {**base, "status": code, "found": "no", "reason": "HTTP 404"}
    if code in (403, 429):
        return {**base, "status": code, "found": "unknown", "reason": f"HTTP {code}（反爬或限流）"}
    if 300 <= code < 400:
        return {**base, "status": code, "found": "unknown", "reason": f"HTTP {code}（跳转，可能跳往登录页）"}
    return {**base, "status": code, "found": "unknown", "reason": f"HTTP {code}"}


async def check(name: str) -> list[dict]:
    """并发探测所有白名单站点，返回按「命中 → 待确认 → 未注册」排序的结果。"""
    name = (name or "").strip()
    if not NAME_RE.match(name):
        raise ValueError("用户名仅支持字母 / 数字 / . _ -，最长 40 位")

    sem = asyncio.Semaphore(CONCURRENCY)
    headers = {"User-Agent": UA, "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
               "Accept": "text/html,application/xhtml+xml"}
    async with httpx.AsyncClient(headers=headers, timeout=TIMEOUT,
                                 follow_redirects=False) as client:
        results = await asyncio.gather(*(_probe(client, s, name, sem) for s in SITES))

    order = {"yes": 0, "unknown": 1, "no": 2}
    return sorted(results, key=lambda r: (order.get(r["found"], 3), r["key"]))
