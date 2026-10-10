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
from ..services.backtest import BacktestEngine
from ..services.news import NewsRadarService
from ..services.portfolio import PortfolioService


# ============================================================
# 监控大屏快照服务（真实 A 股行情驱动；market 参数预留全球扩展）
# ============================================================
class BigScreenService:
    """量化监控大屏快照：持仓盈亏/净值/月度收益/日志全部来自真实行情。
    - 持仓：portfolio.json 真实持仓；为空时用默认观察组合（成本价 = 20 个交易日前真实收盘价）
    - 净值：组合等权买入持有曲线（真实日线）vs 上证指数（东财直连）
    - 日志：异动/止盈止损/涨停/快讯/心跳 事件驱动，服务端去重后带自增 seq 增量下发
    - 后续接入全球股票：扩展 _holdings/_benchmark 的 market 分支即可，快照结构不变
    """

    MARKET = "a"                      # 当前仅 A 股
    NAV_DAYS = 120                    # 净值曲线交易日数
    REFRESH_TTL = 10.0                # 服务端快照缓存秒数（前端 1.5s 轮询，实际 ≥10s 才重建）
    DEMO_SYMBOLS = ["sh600519", "sz300750", "sh601899", "sz000333", "sh600036"]
    DEMO_EQUITY = 1_000_000.0         # 默认观察组合初始资金（演示口径，真实持仓时不参与计算）
    STOP_PCT, TAKE_PCT = 0.94, 1.16   # 监控阈值：止损/止盈线（相对成本价）
    LOG_BUF_MAX = 200

    _cache = [0.0, None]              # [ts, snapshot]
    _lock = threading.Lock()
    _log_seq = 0
    _log_buf = []                     # 已下发事件环形缓冲 [{seq,time,tag,cls,msg}]
    _event_keys = set()               # 事件去重（容量受控）
    _refresh_count = 0                # 心跳计数器

    # ---- 事件日志 ----
    @classmethod
    def _emit(cls, tag: str, cls_name: str, msg: str, key: str):
        """按 key 去重后追加事件日志（key 一般含交易日/新闻 id，跨日自动重发）。"""
        if key in cls._event_keys:
            return
        cls._event_keys.add(key)
        if len(cls._event_keys) > 800:
            cls._event_keys.clear()   # 简单防膨胀：清空重来（跨日后事件会重新触发一次，无害）
        cls._log_seq += 1
        cls._log_buf.append({
            "seq": cls._log_seq,
            "time": datetime.now().strftime("%H:%M:%S"),
            "tag": tag, "cls": cls_name, "msg": msg,
        })
        if len(cls._log_buf) > cls.LOG_BUF_MAX:
            cls._log_buf = cls._log_buf[-cls.LOG_BUF_MAX:]

    # ---- 持仓 ----
    @classmethod
    def _holdings(cls) -> List[Dict]:
        """真实持仓优先（portfolio.json）；为空时回退默认观察组合（真实行情驱动）。"""
        pf = PortfolioService.get_portfolio()
        hs = pf.get("holdings") or []
        if hs:
            out = []
            for h in hs:
                sym = AStockDataService._to_symbol(h.get("symbol", ""))
                if not sym:
                    continue
                out.append({
                    "symbol": sym,
                    "name": AStockDataService.STOCK_POOL.get(sym, h.get("name") or sym),
                    "cost_price": float(h.get("cost_price", 0) or 0),
                    "shares": int(h.get("shares", 0) or 0),
                    "demo": False,
                })
            if out:
                return out
        # 默认观察组合：成本价取 20 个交易日前真实收盘价，按 100 股取整
        out = []
        for sym in cls.DEMO_SYMBOLS:
            try:
                kl = cls._kline_cached(sym, 40)
                if len(kl) < 21:
                    continue
                cost = round(float(kl[-21]["close"]), 2)
                shares = max(int(cls.DEMO_EQUITY / len(cls.DEMO_SYMBOLS) / cost / 100) * 100, 100)
                out.append({"symbol": sym, "name": AStockDataService.STOCK_POOL.get(sym, sym),
                            "cost_price": cost, "shares": shares, "demo": True})
            except Exception as e:  # noqa: BLE001
                logger.warning("bigscreen demo holding %s failed: %s", sym, e)
        return out

    # ---- 净值序列 ----
    _kline_cache: Dict[str, tuple] = {}   # symbol -> (ts, rows)：日线历史 30 分钟缓存（历史不变，实时价由 quote 提供）
    _bench_cache = [0.0, None]            # 上证指数日线 30 分钟缓存

    @classmethod
    def _kline_cached(cls, symbol: str, days: int) -> List[Dict]:
        memo = cls._kline_cache.get(symbol)
        if memo and time.time() - memo[0] < 1800:
            return memo[1]
        try:
            rows = AStockDataService.get_kline(symbol, "1d", days)
        except Exception:  # noqa: BLE001
            rows = []
        cls._kline_cache[symbol] = (time.time(), rows)
        return rows

    @classmethod
    def _benchmark_cached(cls) -> List[Dict]:
        if cls._bench_cache[1] is None or time.time() - cls._bench_cache[0] > 1800:
            try:
                cls._bench_cache[1] = BacktestEngine._benchmark_daily(None) or []  # 该方法不依赖实例状态
            except Exception:  # noqa: BLE001
                cls._bench_cache[1] = []
            cls._bench_cache[0] = time.time()
        return cls._bench_cache[1]

    @classmethod
    def _nav_series(cls, holdings: List[Dict], days: int):
        """组合等权买入持有净值 vs 上证指数净值（同一交易日历，停牌前向填充）。"""
        bench = cls._benchmark_cached()
        if not bench or not holdings:
            return [], []
        cal = [r["date"] for r in bench[-(days + 1):]]
        bench_close = {r["date"]: r["close"] for r in bench}
        aligned, bases = [], []
        for h in holdings:
            rows = cls._kline_cached(h["symbol"], days + 60)
            cm = {r["date"]: float(r["close"]) for r in rows if r.get("date") and r.get("close")}
            if not cm:
                continue
            series, last = [], None
            for d in cal:
                if d in cm:
                    last = cm[d]
                series.append(last)
            base = next((x for x in series if x is not None), None)
            if base:
                aligned.append(series)
                bases.append(base)
        if not aligned:
            return [], []
        b0 = bench_close.get(cal[0])
        if not b0:
            return [], []
        strat, bench_nav = [], []
        for i, d in enumerate(cal):
            acc = 0.0
            for s, b in zip(aligned, bases):
                v = s[i] if s[i] is not None else b   # 上市/数据未覆盖时段按基准价计（等价首日即持有）
                acc += v / b
            strat.append(round(acc / len(aligned), 4))
            bench_nav.append(round(bench_close[d] / b0, 4))
        return cal[-days:], list(zip(strat[-days:], bench_nav[-days:]))

    # ---- 月度收益 ----
    @staticmethod
    def _monthly(dates: List[str], nav: List[float], keep: int = 6) -> List[Dict]:
        out, cur_month, prev_base, last_val = [], None, None, None
        for d, v in zip(dates, nav):
            m = d[:7]
            if m != cur_month:
                if cur_month is not None and prev_base:
                    out.append({"month": cur_month, "profit": round((last_val / prev_base - 1) * 100, 2)})
                cur_month = m
                prev_base = last_val if last_val is not None else v
            last_val = v
        if cur_month is not None and prev_base:
            out.append({"month": cur_month, "profit": round((last_val / prev_base - 1) * 100, 2)})
        return out[-keep:]

    # ---- 主入口 ----
    @classmethod
    def snapshot(cls) -> Dict:
        with cls._lock:
            now_ts = time.time()
            if cls._cache[1] is not None and now_ts - cls._cache[0] < cls.REFRESH_TTL:
                return cls._cache[1]

            holdings = cls._holdings()
            quotes, pos_detail = {}, []
            total_value = 0.0
            for h in holdings:
                try:
                    q = AStockDataService.get_realtime_quote(h["symbol"], h["name"])
                except Exception:  # noqa: BLE001
                    continue
                quotes[h["symbol"]] = q
                mv = q.price * h["shares"]
                total_value += mv
            total_cost = sum(h["cost_price"] * h["shares"] for h in holdings if h["symbol"] in quotes)
            cash = 0.0
            if holdings and holdings[0]["demo"]:
                cash = max(cls.DEMO_EQUITY - total_cost, 0.0)     # 观察组合：初始资金未买入部分
            else:
                cash = float(PortfolioService.get_portfolio().get("cash") or 0)
            initial = (cls.DEMO_EQUITY if (holdings and holdings[0]["demo"]) else total_cost + cash) or 1.0
            total_equity = total_value + cash

            for h in holdings:
                q = quotes.get(h["symbol"])
                if not q or h["cost_price"] <= 0:
                    continue
                pnl = (q.price - h["cost_price"]) * h["shares"]
                pos_detail.append({
                    "code": h["symbol"], "name": h["name"], "direction": "做多",
                    "weight": round(mv_weight, 1) if (mv_weight := (q.price * h["shares"] / total_value * 100 if total_value else 0)) else 0.0,
                    "costPrice": round(h["cost_price"], 3), "lastPrice": round(q.price, 3),
                    "floatPnl": round(pnl, 0),
                    "floatPnlPct": round((q.price - h["cost_price"]) / h["cost_price"] * 100, 2),
                    "stopLoss": round(h["cost_price"] * cls.STOP_PCT, 3),
                    "takeProfit": round(h["cost_price"] * cls.TAKE_PCT, 3),
                })

            # 净值 / 月度 / 日度盈亏
            dates, navs = cls._nav_series(holdings, cls.NAV_DAYS)
            nav_payload = [{"date": d, "strategy": s, "benchmark": b} for d, (s, b) in zip(dates, navs)]
            monthly = cls._monthly(dates, [s for s, _ in navs]) if dates else []
            daily = [round((navs[i][0] / navs[i - 1][0] - 1) * initial) for i in range(1, len(navs))] if navs else []

            # 指标
            day_pnl = 0.0
            for h in holdings:
                q = quotes.get(h["symbol"])
                if q:
                    day_pnl += q.change * h["shares"]
            day_base = total_equity - day_pnl
            rets = [navs[i][0] / navs[i - 1][0] - 1 for i in range(1, len(navs))] if len(navs) > 1 else []
            sharpe = round(float(np.mean(rets) / np.std(rets) * np.sqrt(252)), 2) \
                if len(rets) > 2 and np.std(rets) > 0 else 0.0
            peak, max_dd = -1e18, 0.0
            for s, _ in navs:
                peak = max(peak, s)
                max_dd = max(max_dd, (peak - s) / peak * 100)
            win = sum(1 for p in pos_detail if p["floatPnl"] > 0)

            # 事件日志（异动/止盈止损/涨停/快讯/心跳）
            cls._refresh_count += 1
            day_key = datetime.now().strftime("%Y%m%d")
            for p in pos_detail:
                q = quotes[p["code"]]
                qp = q.change_percent
                if qp >= 3:
                    cls._emit("盘中异动", "take", f"{p['name']} 日内大涨 {qp:+.2f}% 现价 {q.price}", f"surge_{p['code']}_{day_key}")
                elif qp <= -3:
                    cls._emit("盘中异动", "warn", f"{p['name']} 日内大跌 {qp:+.2f}% 现价 {q.price}", f"plunge_{p['code']}_{day_key}")
                if q.price >= p["takeProfit"]:
                    cls._emit("止盈触发", "take", f"{p['name']} 触及止盈线 {p['takeProfit']} 现价 {q.price}", f"tk_{p['code']}_{day_key}")
                if q.price <= p["stopLoss"]:
                    cls._emit("止损触发", "stop", f"{p['name']} 跌破止损线 {p['stopLoss']} 现价 {q.price}", f"sl_{p['code']}_{day_key}")
            try:
                pools = AStockDataService.get_limit_pools()
                hold_codes = {p["code"] for p in pos_detail}
                for row in (pools.get("limit_up") or []):
                    c = row.get("code") or row.get("symbol") or ""
                    if AStockDataService._to_symbol(c) in hold_codes:
                        cls._emit("涨停", "open", f"{row.get('name', c)} 涨停 封单 {row.get('amount_str', '-')} 观察池命中", f"zt_{c}_{day_key}")
            except Exception as e:  # noqa: BLE001
                logger.debug("bigscreen limit pool skip: %s", e)
            try:
                for n in NewsRadarService.get_latest_news(8)[:6]:
                    cls._emit("快讯", "info", (n.get("title") or "")[:60], f"news_{n.get('id')}")
            except Exception as e:  # noqa: BLE001
                logger.debug("bigscreen news skip: %s", e)
            if cls._refresh_count % 12 == 1:   # 约每 2 分钟一条心跳
                up = sum(1 for q in quotes.values() if q.change_percent > 0)
                down = sum(1 for q in quotes.values() if q.change_percent < 0)
                cls._emit("心跳", "info", f"行情链路正常 持仓 {len(pos_detail)} 只 组内涨/跌 {up}/{down}", f"hb_{cls._refresh_count}")

            snap = {
                "market": "A股",
                "source": "a-share-live",
                "tradesLabel": "日度盈亏 · 近30日",
                "account": {
                    "totalEquity": round(total_equity, 2),
                    "dayPnl": round(day_pnl, 2),
                    "dayPnlPct": round(day_pnl / day_base * 100, 2) if day_base else 0.0,
                    "totalReturnPct": round((total_equity / initial - 1) * 100, 2) if initial else 0.0,
                    "winRate": round(win / len(pos_detail) * 100, 1) if pos_detail else 0.0,
                    "sharpe": sharpe,
                    "maxDrawdown": round(max_dd, 2),
                    "positionCount": len(pos_detail),
                    "availableCash": round(cash, 2),
                },
                "nav": nav_payload,
                "positions": pos_detail,
                "trades": daily[-30:],
                "monthly": monthly,
                "logs": list(cls._log_buf[-50:]),
                "stamp": datetime.now().strftime("%H:%M:%S"),
            }
            cls._cache = [now_ts, snap]
            return snap
