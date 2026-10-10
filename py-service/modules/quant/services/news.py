# -*- coding: utf-8 -*-
from __future__ import annotations

import os
import sys
import json
import random
import time
import uuid
import logging
import re
import asyncio
import hashlib
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timedelta
from pathlib import Path
from typing import Optional, List, Dict, Any
from dataclasses import dataclass, field, asdict
from enum import Enum

import numpy as np
import pandas as pd
import requests
from fastapi import HTTPException, Query, Request, Body
from fastapi.responses import JSONResponse, StreamingResponse
from pydantic import BaseModel

logger = logging.getLogger("quant")

from ..datasources.astock import AStockDataService
from ..datasources.supabase import SupabaseStore


try:
    import akshare as ak
except Exception:  # noqa: BLE001
    ak = None


def rng_float(seed: str, lo: float, hi: float) -> float:
    s = int(hashlib.md5(seed.encode()).hexdigest()[:8], 16)
    rng = np.random.RandomState(s)
    return float(rng.uniform(lo, hi))


# ============================================================
# 新闻雷达服务
# ============================================================
class NewsRadarService:
    """新闻雷达服务 - 综合财经新闻。"""

    NEWS_SOURCES = [
        {"source": "财联社", "category": "宏观", "priority": "high"},
        {"source": "东方财富", "category": "市场", "priority": "high"},
        {"source": "新浪财经", "category": "综合", "priority": "medium"},
        {"source": "同花顺", "category": "资讯", "priority": "medium"},
        {"source": "华尔街见闻", "category": "国际", "priority": "high"},
    ]

    _news_cache: List[Dict] = []
    _news_ts: float = 0.0
    _NEWS_TTL = 120.0  # 2 分钟缓存

    @staticmethod
    def _sentiment_of(text: str) -> str:
        """简易情感分析：根据关键词判断正面/负面/中性。"""
        if not text:
            return "neutral"
        pos_kw = {"利好", "上涨", "增长", "突破", "创新高", "走强", "涨停", "大涨", "加速", "超预期",
                  "回暖", "回升", "提振", "利好", "批准", "支持", "提振", "发力", "爆发"}
        neg_kw = {"利空", "下跌", "下降", "下滑", "暴跌", "跌停", "大跌", "承压", "亏损", "警示",
                  "风险", "违约", "退市", "警示", "下调", "不及预期", "收缩", "疲软"}
        t = text[:200]
        pos = sum(1 for k in pos_kw if k in t)
        neg = sum(1 for k in neg_kw if k in t)
        if pos > neg:
            return "positive"
        if neg > pos:
            return "negative"
        return "neutral"

    @staticmethod
    def _impact_of(text: str) -> str:
        """根据关键词判断新闻影响力等级。"""
        if not text:
            return "medium"
        t = text[:200]
        high_kw = {"央行", "降准", "降息", "国务院", "证监会", "财政部", "重大", "重组", "涨停", "暴跌"}
        if any(k in t for k in high_kw):
            return "high"
        return "medium"

    @staticmethod
    def get_latest_news(limit: int = 20) -> List[Dict]:
        """获取最新财经新闻（akshare 东财全球财经直播，真实实时数据）。"""
        now = time.time()
        if NewsRadarService._news_cache and now - NewsRadarService._news_ts < NewsRadarService._NEWS_TTL:
            return NewsRadarService._news_cache[:limit]
        # 一级源：akshare 东财全球财经直播（stock_info_global_em）
        try:
            if ak is not None:
                df = AStockDataService._with_retry(lambda: ak.stock_info_global_em(), 2, 1.0)
                if df is not None and len(df):
                    out = []
                    for _, r in df.head(max(limit * 2, 40)).iterrows():
                        title = str(r.get("标题") or "").strip()
                        summary = str(r.get("摘要") or "").strip()
                        ts = str(r.get("发布时间") or "")[:16]
                        link = str(r.get("链接") or "").strip()
                        if not title and not summary:
                            continue
                        combined = title + " " + summary
                        out.append({
                            "id": f"em_{abs(hash(title + ts)) % (10**12)}",
                            "title": title or summary[:40],
                            "source": "东方财富",
                            "category": "快讯",
                            "sentiment": NewsRadarService._sentiment_of(combined),
                            "impact": NewsRadarService._impact_of(combined),
                            "timestamp": ts or datetime.now().strftime("%Y-%m-%d %H:%M"),
                            "summary": summary[:120] + ("..." if len(summary) > 120 else ""),
                            "url": link,
                        })
                        if len(out) >= limit:
                            break
                    if out:
                        NewsRadarService._news_cache = out
                        NewsRadarService._news_ts = now
                        threading.Thread(target=lambda: SupabaseStore.save_news(out), daemon=True).start()
                        return out[:limit]
        except Exception as e:
            logger.warning("akshare news (stock_info_global_em) failed: %s", e)
        # 二级源：直接请求东财 7x24 快讯 API（兜底）
        try:
            headers = {
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36",
                "Referer": "https://finance.eastmoney.com/",
            }
            url = "https://np-listapi.eastmoney.com/comm/web/getFastNewsList"
            params = {"client": "web", "biz": "web_724", "pageSize": str(limit * 2), "pageNo": "1", "device": "1"}
            r = requests.get(url, params=params, headers=headers, timeout=10)
            r.raise_for_status()
            payload = r.json()
            items = ((payload or {}).get("data") or {}).get("list") or []
            if isinstance(items, list) and items:
                out = []
                for x in items[:limit]:
                    if not isinstance(x, dict):
                        continue
                    title = str(x.get("title") or x.get("showTitle") or "").strip()
                    digest = str(x.get("digest") or x.get("content") or "").strip()
                    if not title and not digest:
                        continue
                    ts = str(x.get("showtime") or x.get("publishDate") or "")[:16]
                    combined = title + " " + digest
                    out.append({
                        "id": str(x.get("newsID") or x.get("id") or ""),
                        "title": title or digest[:40],
                        "source": "东方财富",
                        "category": "快讯",
                        "sentiment": NewsRadarService._sentiment_of(combined),
                        "impact": NewsRadarService._impact_of(combined),
                        "timestamp": ts or datetime.now().strftime("%Y-%m-%d %H:%M"),
                        "summary": digest[:120] + ("..." if len(digest) > 120 else ""),
                        "url": str(x.get("url") or ""),
                    })
                if out:
                    NewsRadarService._news_cache = out
                    NewsRadarService._news_ts = now
                    threading.Thread(target=lambda: SupabaseStore.save_news(out), daemon=True).start()
                    return out[:limit]
        except Exception as e:
            logger.warning("em fast news fallback failed: %s", e)
        # 末级兜底：静态新闻（仅当所有实时源都失败时）
        return NewsRadarService._fallback_news(limit)

    @staticmethod
    def _fallback_news(limit: int) -> List[Dict]:
        """末级兜底（仅当所有实时数据源都不可用时）。"""
        headlines = [
            "财经新闻数据源暂时不可用，请稍后刷新重试",
            "如持续无数据，请检查 quant-py-service 日志",
        ]
        return [{
            "id": f"fallback_{i}",
            "title": h,
            "source": "系统提示",
            "category": "提示",
            "sentiment": "neutral",
            "impact": "low",
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M"),
            "summary": h,
        } for i, h in enumerate(headlines[:limit])]

    @staticmethod
    def get_sector_heat() -> Dict[str, float]:
        """获取板块热度（兜底：东财板块涨跌幅，无实时源时返回空）。"""
        try:
            if ak is not None:
                df = AStockDataService._with_retry(lambda: ak.stock_board_industry_summary_ths(), 2, 0.8)
                if df is not None and len(df):
                    out = {}
                    for _, r in df.head(30).iterrows():
                        nm = str(r.iloc[0] or "").strip()
                        pct = AStockDataService._row_num(r.to_dict(), "涨跌幅")
                        if nm:
                            out[nm] = round(pct, 2)
                    if out:
                        return out
        except Exception as e:
            logger.warning("sector heat fallback failed: %s", e)
        return {}


# ============================================================
# 券商研报（东财研报中心元数据，不下载 PDF 全文）# ============================================================
