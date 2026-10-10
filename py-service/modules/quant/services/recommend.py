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


class RecommendStrategyService:
    """多策略推荐：基于全市场快照字段（价格/涨跌幅/成交额/换手率/PE/市值/振幅/高低开收）
    实现 8 种推荐视角，每个策略独立过滤 + 评分(0-100) + 中文理由，整体结果缓存 10 分钟。
    快照无历史K线，涉及趋势的条件用当日盘面特征（高开/阳线/收盘贴近最高价）近似；
    某策略数据不足或无符合条件标的时以 disabled + disabled_reason 标注，前端可直接渲染。"""

    _cache: Optional[tuple] = None
    _TTL = 600.0  # 10 分钟

    STRATEGY_META = [
        {"id": "trend_up", "name": "趋势多头", "desc": "高开高走阳线、收盘贴近最高价，涨幅居前"},
        {"id": "volume_surge", "name": "放量突破", "desc": "成交额较市场均值显著放大，涨幅2-9%"},
        {"id": "low_valuation", "name": "低估值蓝筹", "desc": "0<PE<25、市值居前、当日未破位"},
        {"id": "oversold_rebound", "name": "超跌反弹", "desc": "跌幅居前但换手活跃的超跌候选"},
        {"id": "turnover_hot", "name": "高换手活跃", "desc": "换手率5-25%且涨幅1-9%"},
        {"id": "small_cap_growth", "name": "小盘成长", "desc": "低价、振幅适中、涨幅为正的小市值标的"},
        {"id": "high_amount", "name": "资金聚焦", "desc": "成交额TOP30中当日涨幅为正者"},
        {"id": "limit_candidate", "name": "涨停候选", "desc": "主板涨幅7-9.5%+换手3-20%+盘中触板基因"},
    ]

    # ---- 快照池构建 ----
    @staticmethod
    def _build_universe() -> List[Dict]:
        """从全市场快照构建统一字段池（剔除北交所/ST/退市/新股）。"""
        df, _src = AStockDataService._load_all_snapshot()
        if df is None or not len(df):
            return []
        uni: List[Dict] = []
        for r in df.to_dict("records")[:6000]:
            code = str(r.get("代码") or "")
            name = str(r.get("名称") or "")
            if not code.startswith(("6", "0", "3")):
                continue
            if "ST" in name.upper() or "退" in name or (name and name[:1].upper() == "N"):
                continue
            price = AStockDataService._row_num(r, "最新价", "trade", "price")
            if price <= 0:
                continue
            uni.append({
                "code": code, "name": name, "price": price,
                "pct": AStockDataService._row_num(r, "涨跌幅", "changepercent"),
                "amount": AStockDataService._row_num(r, "成交额", "amount"),
                "volume": AStockDataService._row_num(r, "成交量", "volume"),
                "turnover_rate": AStockDataService._row_num(r, "换手率", "turnoverratio"),
                "pe": AStockDataService._row_num(r, "市盈率", "市盈率-动态", "pe"),
                "cap": AStockDataService._row_num(r, "总市值", "mktcap"),
                "amp": AStockDataService._row_num(r, "振幅", "amplitude"),
                "high": AStockDataService._row_num(r, "最高", "high"),
                "low": AStockDataService._row_num(r, "最低", "low"),
                "open": AStockDataService._row_num(r, "今开", "open"),
                "prev": AStockDataService._row_num(r, "昨收", "settlement"),
            })
        return uni

    @staticmethod
    def _median(vals: List[float]) -> float:
        """正数中位数（放量倍数的市场基准）。"""
        s = sorted(v for v in vals if v > 0)
        return s[len(s) // 2] if s else 0.0

    @staticmethod
    def _item(u: Dict, score: float, reasons: List[str]) -> Dict:
        """统一推荐条目结构。"""
        return {"code": u["code"], "name": u["name"], "price": round(u["price"], 2),
                "change_pct": round(u["pct"], 2), "score": round(max(0.0, min(100.0, score)), 1),
                "reasons": [x for x in reasons if x][:3]}

    # ---- 各推荐策略（返回最多 8 只）----
    @staticmethod
    def _strategy_trend_up(uni: List[Dict], ctx: Dict) -> List[Dict]:
        """趋势多头（快照近似）：高开高走阳线 + 收盘贴近最高价 + 涨幅居前。"""
        med = ctx["med_amount"]
        hits = []
        for u in uni:
            if not (2 <= u["pct"] <= 9.5) or u["open"] <= 0 or u["price"] <= u["open"]:
                continue
            if not (u["high"] > 0 and u["price"] >= u["high"] * 0.985):
                continue
            if med > 0 and u["amount"] < med * 2:
                continue
            score = 55 + min(20, u["pct"] * 3)
            reasons = [f"涨幅{u['pct']:.2f}% 高开高走", "收盘贴近全天最高价，日内趋势强劲"]
            if med > 0:
                score += min(10, u["amount"] / med)
                reasons.append(f"成交额{u['amount'] / 1e8:.2f}亿 显著放量")
            hits.append(RecommendStrategyService._item(u, score, reasons))
        hits.sort(key=lambda x: x["score"], reverse=True)
        return hits[:8]

    @staticmethod
    def _strategy_volume_surge(uni: List[Dict], ctx: Dict) -> List[Dict]:
        """放量突破：成交额 ≥2 倍市场中位数 + 涨幅2-9% + 换手活跃。"""
        med = ctx["med_amount"]
        hits = []
        for u in uni:
            if med <= 0 or not (2 <= u["pct"] <= 9):
                continue
            ratio = u["amount"] / med
            if ratio < 2:
                continue
            score = 50 + min(25, ratio * 4) + min(15, u["pct"] * 2) + min(10, u["turnover_rate"])
            reasons = [f"成交额达市场中位数{ratio:.1f}倍，显著放量",
                       f"涨幅{u['pct']:.2f}% 突破上行"]
            if u["turnover_rate"] > 0:
                reasons.append(f"换手率{u['turnover_rate']:.1f}%")
            hits.append(RecommendStrategyService._item(u, score, reasons))
        hits.sort(key=lambda x: x["score"], reverse=True)
        return hits[:8]

    @staticmethod
    def _strategy_low_valuation(uni: List[Dict], ctx: Dict) -> List[Dict]:
        """低估值蓝筹：0<PE<25 + 市值居前 + 当日未明显破位。"""
        cands = [u for u in uni if 0 < u["pe"] < 25 and u["pct"] > -2 and u["cap"] > 0]
        cands.sort(key=lambda x: x["cap"], reverse=True)
        hits = []
        for u in cands[:300]:
            score = 55 + min(25, 25 - u["pe"]) + min(15, u["cap"] / 2e10 * 5) + (5 if u["pct"] > 0 else 0)
            reasons = [f"PE {u['pe']:.1f} 估值偏低", f"总市值{u['cap'] / 1e8:.0f}亿 体量居前",
                       "当日企稳未破位" if u["pct"] >= 0 else f"小幅回调{u['pct']:.2f}%"]
            hits.append(RecommendStrategyService._item(u, score, reasons))
        hits.sort(key=lambda x: x["score"], reverse=True)
        return hits[:8]

    @staticmethod
    def _strategy_oversold_rebound(uni: List[Dict], ctx: Dict) -> List[Dict]:
        """超跌反弹：跌幅居前（全市场倒数50）但换手 ≥2% 承接活跃。"""
        losers = sorted((u for u in uni if u["pct"] < 0), key=lambda x: x["pct"])[:50]
        hits = []
        for u in losers:
            if u["turnover_rate"] < 2:
                continue
            score = 45 + min(30, -u["pct"] * 4) + min(25, u["turnover_rate"] * 3)
            reasons = [f"跌幅{u['pct']:.2f}% 超跌明显",
                       f"换手率{u['turnover_rate']:.1f}% 承接活跃",
                       "超跌反弹候选，注意风险"]
            hits.append(RecommendStrategyService._item(u, score, reasons))
        hits.sort(key=lambda x: x["score"], reverse=True)
        return hits[:8]

    @staticmethod
    def _strategy_turnover_hot(uni: List[Dict], ctx: Dict) -> List[Dict]:
        """高换手活跃：换手率 5-25% + 涨幅 1-9%。"""
        hits = []
        for u in uni:
            if not (5 <= u["turnover_rate"] <= 25 and 1 <= u["pct"] <= 9):
                continue
            score = 50 + min(25, (u["turnover_rate"] - 5) * 2) + min(15, u["pct"] * 2)
            reasons = [f"换手率{u['turnover_rate']:.1f}% 交投火热",
                       f"涨幅{u['pct']:.2f}% 量价齐升", "短线资金关注度高的活跃标的"]
            hits.append(RecommendStrategyService._item(u, score, reasons))
        hits.sort(key=lambda x: x["score"], reverse=True)
        return hits[:8]

    @staticmethod
    def _strategy_small_cap_growth(uni: List[Dict], ctx: Dict) -> List[Dict]:
        """小盘成长：价格<20元 + 振幅3-9%适中 + 涨幅为正 + 市值≤150亿。"""
        hits = []
        for u in uni:
            if u["price"] >= 20 or not (3 <= u["amp"] <= 9) or not (0 < u["pct"] < 7):
                continue
            if u["cap"] > 1.5e10:
                continue
            score = 45 + min(20, 20 - u["price"]) + min(15, u["amp"] * 2) + min(15, u["pct"] * 3)
            reasons = [f"低价{u['price']:.2f}元 弹性充足",
                       f"振幅{u['amp']:.2f}% 波动适中",
                       f"涨幅{u['pct']:.2f}% 企稳向上"]
            hits.append(RecommendStrategyService._item(u, score, reasons))
        hits.sort(key=lambda x: x["score"], reverse=True)
        return hits[:8]

    @staticmethod
    def _strategy_high_amount(uni: List[Dict], ctx: Dict) -> List[Dict]:
        """资金聚焦：成交额 TOP30 中当日涨幅为正者（排名越靠前分越高）。"""
        top30 = sorted((u for u in uni if u["amount"] > 0), key=lambda x: x["amount"], reverse=True)[:30]
        hits = []
        for rank, u in enumerate(top30):
            if u["pct"] <= 0:
                continue
            score = 60 + min(25, u["pct"] * 5) + max(0.0, 10 - rank * 0.4)
            reasons = [f"成交额{u['amount'] / 1e8:.1f}亿 全市场第{rank + 1}名，资金聚焦",
                       f"涨幅{u['pct']:.2f}% 放量上行"]
            hits.append(RecommendStrategyService._item(u, score, reasons))
        hits.sort(key=lambda x: x["score"], reverse=True)
        return hits[:8]

    @staticmethod
    def _strategy_limit_candidate(uni: List[Dict], ctx: Dict) -> List[Dict]:
        """涨停候选：主板(60开头)涨幅7-9.5% + 换手3-20% + 盘中最高涨幅≥8.5%（距涨停近，封板基因）。"""
        hits = []
        for u in uni:
            if not u["code"].startswith("60"):
                continue
            if not (7 <= u["pct"] <= 9.5 and 3 <= u["turnover_rate"] <= 20):
                continue
            high_pct = (u["high"] - u["prev"]) / u["prev"] * 100 if u["prev"] > 0 else 0.0
            if high_pct < 8.5:
                continue
            score = 60 + min(25, (high_pct - 8.5) * 12) + min(15, u["turnover_rate"])
            reasons = [f"涨幅{u['pct']:.2f}% 逼近涨停",
                       f"盘中最高冲高{high_pct:.2f}% 具备封板基因",
                       f"换手率{u['turnover_rate']:.1f}% 筹码交换充分"]
            hits.append(RecommendStrategyService._item(u, score, reasons))
        hits.sort(key=lambda x: x["score"], reverse=True)
        return hits[:8]

    # ---- 主入口 ----
    @staticmethod
    def recommend_all(force: bool = False) -> List[Dict]:
        """返回全部推荐策略结果（10分钟缓存，force 强制刷新）。"""
        now = time.time()
        if not force and RecommendStrategyService._cache is not None \
                and now - RecommendStrategyService._cache[0] < RecommendStrategyService._TTL:
            return RecommendStrategyService._cache[1]
        uni = RecommendStrategyService._build_universe()
        ctx = {"med_amount": RecommendStrategyService._median([u["amount"] for u in uni])}
        out: List[Dict] = []
        for meta in RecommendStrategyService.STRATEGY_META:
            entry = {"id": meta["id"], "name": meta["name"], "desc": meta["desc"],
                     "items": [], "disabled": False, "disabled_reason": ""}
            if not uni:
                entry["disabled"] = True
                entry["disabled_reason"] = "全市场快照数据不可用，无法计算"
            else:
                try:
                    fn = getattr(RecommendStrategyService, "_strategy_" + meta["id"])
                    entry["items"] = fn(uni, ctx)
                    if not entry["items"]:
                        entry["disabled"] = True
                        entry["disabled_reason"] = "当前市场无符合该策略条件的标的"
                except Exception as e:  # noqa: BLE001
                    logger.warning("recommend strategy %s failed: %s", meta["id"], e)
                    entry["disabled"] = True
                    entry["disabled_reason"] = f"计算失败: {e}"
            out.append(entry)
        RecommendStrategyService._cache = (now, out)
        return out


# ============================================================
# 自定义Python策略选股沙箱（受限执行）
# ============================================================
