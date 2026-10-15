# -*- coding: utf-8 -*-
"""引导层：把用户的「操作类意图」解析为页面导航 + 高亮提示。

两级来源：
  1) 静态导航表（确定性、零延迟）—— 命中即返回 {route, tip}；
  2) LLM 在回复末尾输出的 `[[GUIDE]]{...}` JSON 块 —— 处理表外的自由表达。

静态表移植自前端 XingTuAssistant.vue 的 LOCAL_CMDS（语义一致）。
"""
from __future__ import annotations

import json
import re

# 关键词 → {route, tip}（按顺序首个命中生效）
NAV_TABLE: list[tuple[tuple[str, ...], dict]] = [
    (("首页", "主页"), {"route": "/", "tip": "这里是全站首页，最新动态与快捷入口都在这里。"}),
    (("社区", "论坛", "帖子"), {"route": "/community", "tip": "在社区可以发帖、评论、和同伴交流。"}),
    (("博客", "日志", "文章"), {"route": "/blog", "tip": "博客里是技术笔记与随笔，点开即可阅读。"}),
    (("商城", "购物", "商店"), {"route": "/shop", "tip": "商城可以挑选商品并下单。"}),
    (("购物车",), {"route": "/shop/cart", "tip": "这里是你的购物车，确认后即可结算。"}),
    (("媒体", "书影音", "视频"), {"route": "/media", "tip": "多媒体中心收录了书影音内容。"}),
    (("音乐", "听歌"), {"route": "/media", "tip": "音乐馆在多媒体中心，说“我想听周杰伦”我也可以直接放。"}),
    (("量化", "交易", "股票", "行情", "看板"), {"route": "/quant", "tip": "这里是金融量化平台，可以看行情与策略。"}),
    (("工具", "脚本"), {"route": "/tools", "tip": "在线工具箱，常用小工具都在这里。"}),
    (("API中心", "接口"), {"route": "/tools/api", "tip": "API 中心可以查看和调试接口。"}),
    (("软件", "下载"), {"route": "/software", "tip": "软件库里可以浏览与下载软件。"}),
    (("资源",), {"route": "/resources", "tip": "资源中心汇总了可用的资源。"}),
    (("上传",), {"route": "/upload", "tip": "在这里上传你的资源。"}),
    (("架构",), {"route": "/architecture", "tip": "这里展示全站微服务架构。"}),
    (("AI中转站", "中转站", "令牌"), {"route": "/ai-station", "tip": "AI 中转站可以管理令牌与额度。"}),
    (("AI引擎", "AI助手", "知识库"), {"route": "/ai", "tip": "AI 引擎与知识库在这里。"}),
    (("智能体", "工作流", "agent"), {"route": "/agent", "tip": "Agent 工作台可以编排自动化任务。"}),
    (("相册",), {"route": "/album", "tip": "相册集记录了图片与回忆。"}),
    (("友链", "友人帐"), {"route": "/link", "tip": "友人帐收录了友站链接。"}),
    (("留言",), {"route": "/comments", "tip": "留言板可以留下你的想法。"}),
    (("关于",), {"route": "/about", "tip": "关于页介绍了这个站点。"}),
    (("订单",), {"route": "/profile/orders", "tip": "这里可以查看你的订单。"}),
    (("个人中心", "我的"), {"route": "/profile", "tip": "个人中心可以管理资料与设置。"}),
    (("支付", "充值"), {"route": "/pay", "tip": "支付页可以完成付款或充值。"}),
]

# 导航意图判定：只在「像导航指令」时触发静态表，避免「介绍一下你自己」被误判为打开关于页
NAV_VERBS = ("打开", "去", "进入", "跳转", "前往", "回到", "返回", "看看", "看一下", "瞧瞧", "我想", "我要", "帮我", "带我去")
NAV_MAX_LEN = 8

_GUIDE_RE = re.compile(r"\[\[GUIDE\]\]\s*(\{.*?\})", re.DOTALL | re.IGNORECASE)


def resolve_guide(text: str) -> dict | None:
    """静态导航表匹配。命中返回 {route, tip, selector?}（selector 由前端按页面 DOM 决定）。"""
    t = (text or "").strip().lower()
    if not t:
        return None
    # 短句（≤8 字）或显式动作词才算导航意图
    if len(t) > NAV_MAX_LEN and not any(v in t for v in NAV_VERBS):
        return None
    for keys, payload in NAV_TABLE:
        for k in keys:
            if k.lower() in t:
                return dict(payload)
    return None


def extract_llm_guide(full_text: str) -> tuple[str, dict | None]:
    """从 LLM 输出中剥离 `[[GUIDE]]{...}` 块，返回 (可朗读文本, guide 或 None)。"""
    if not full_text:
        return full_text, None
    guide = None
    m = _GUIDE_RE.search(full_text)
    if m:
        try:
            data = json.loads(m.group(1))
            if isinstance(data, dict) and data.get("route"):
                guide = {
                    "route": str(data.get("route")),
                    "tip": str(data.get("tip") or "").strip(),
                    "selector": (str(data.get("selector")).strip() if data.get("selector") else None),
                }
        except Exception:  # noqa: BLE001
            guide = None
    clean = _GUIDE_RE.sub("", full_text).strip()
    return clean, guide
