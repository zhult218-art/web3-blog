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
from ..models.entities import AnalysisTask, BacktestResult
from ..models.enums import TaskStatus
from ..services.ai import AIAnalysisService
from ..services.strategy import StrategyService


try:
    import akshare as ak
except Exception:  # noqa: BLE001
    ak = None


BACKTEST_STRATEGY_META = [
    {"id": "moving_avg", "name": "双均线策略", "desc": "MA5上穿MA20金叉买入，死叉卖出"},
    {"id": "macd_signal", "name": "MACD信号策略", "desc": "MACD金叉且量能配合买入，死叉卖出"},
    {"id": "bollinger", "name": "布林带策略", "desc": "触及布林下轨买入，触及上轨卖出"},
    {"id": "rsi_meanrev", "name": "RSI均值回归", "desc": "RSI超卖(<30)买入，超买(>70)卖出"},
    {"id": "bse_smallcap", "name": "小市值持仓", "desc": "等权持仓+定期轮换，配止损止盈"},
    {"id": "grid_okx", "name": "网格交易", "desc": "布林通道内网格低吸高抛赚取波动"},
    {"id": "kdj_golden", "name": "KDJ金叉", "desc": "J<20超卖后K上穿D金叉买入，死叉或止盈卖出"},
    {"id": "volume_breakout", "name": "放量突破", "desc": "成交量>2倍20日均量且收盘创20日新高买入，跌破10日线卖出"},
    {"id": "turtle", "name": "海龟策略", "desc": "收盘突破20日最高价买入，跌破10日最低价卖出，2倍ATR止损"},
    {"id": "momentum", "name": "动量轮动", "desc": "每调仓周期持有近20日涨幅最高的前3只"},
    {"id": "mean_reversion", "name": "均值回归", "desc": "收盘低于MA20超5%且RSI<35买入，回归MA20卖出"},
]


class BacktestEngine:
    STRATEGIES = {"moving_avg", "macd_signal", "bollinger", "rsi_meanrev", "bse_smallcap", "grid_okx",
                  "kdj_golden", "volume_breakout", "turtle", "momentum", "mean_reversion"}

    def __init__(self, config: Dict):
        self.config = config
        self.initial_cash = config.get("initial_cash", 1000000)
        self.cash = self.initial_cash
        self.strategy_id = config.get("strategy_id", "bse_smallcap")
        self.custom_handle = None
        self.positions: Dict[str, Dict] = {}
        self.transactions: List[Dict] = []
        self.daily_values: List[float] = []
        self.dates: List[str] = []
        self.stop_loss = config.get("stop_loss", -0.10)
        self.max_position = config.get("max_position", 0.05)
        self.commission = config.get("commission", 0.0003)
        self.slippage = config.get("slippage", 0.001)
        self.rebalance_days = int(config.get("rebalance_days", 10))
        self._kdj_oversold: Dict[str, bool] = {}  # KDJ 策略：J<20 超卖标记（按标的）

    # ---------------- 技术指标序列（逐日）----------------
    @staticmethod
    def _ta_series(closes: List[float], highs: Optional[List[float]] = None,
                   lows: Optional[List[float]] = None) -> Dict[str, list]:
        n = len(closes)
        # 高低价缺失时以收盘价兜底（KDJ/ATR 退化为收盘估算）
        if highs is None or len(highs) != n:
            highs = closes
        if lows is None or len(lows) != n:
            lows = closes

        def ma(p: int) -> list:
            out = []
            s = 0.0
            for i in range(n):
                s += closes[i]
                if i >= p:
                    s -= closes[i - p]
                out.append(s / p if i + 1 >= p else None)
            return out

        ma5, ma20, ma60 = ma(5), ma(20), ma(60)

        def ema(p: int) -> list:
            k = 2 / (p + 1)
            out, e = [], closes[0] if n else 0.0
            for v in closes:
                e = v * k + e * (1 - k)
                out.append(e)
            return out

        e12, e26 = ema(12), ema(26)
        dif = [a - b for a, b in zip(e12, e26)]
        k9 = 2 / 10
        dea, e = [], dif[0] if dif else 0.0
        for v in dif:
            e = v * k9 + e * (1 - k9)
            dea.append(e)

        # RSI14（Wilder 平滑）
        rsi = [None] * n
        if n > 1:
            gains, losses = [], []
            for i in range(1, n):
                diff = closes[i] - closes[i - 1]
                gains.append(max(diff, 0.0))
                losses.append(max(-diff, 0.0))
            avg_g = sum(gains[:14]) / 14 if len(gains) >= 14 else None
            avg_l = sum(losses[:14]) / 14 if len(losses) >= 14 else None
            for i in range(13, n):
                if i == 13:
                    pass
                elif i >= 14:
                    avg_g = (avg_g * 13 + gains[i - 1]) / 14 if avg_g is not None else None
                    avg_l = (avg_l * 13 + losses[i - 1]) / 14 if avg_l is not None else None
                if avg_g is not None and avg_l is not None:
                    rs = avg_g / avg_l if avg_l > 0 else 999.0
                    rsi[i] = 100 - 100 / (1 + rs)

        # 甯冩灄甯?(20, 2)
        boll_up, boll_mid, boll_low = [], [], []
        for i in range(n):
            if i + 1 >= 20:
                win = closes[i - 19:i + 1]
                m = sum(win) / 20
                sd = (sum((x - m) ** 2 for x in win) / 20) ** 0.5
                boll_mid.append(m)
                boll_up.append(m + 2 * sd)
                boll_low.append(m - 2 * sd)
            else:
                boll_up.append(None)
                boll_mid.append(None)
                boll_low.append(None)

        vol20 = ma(20)
        ma10 = ma(10)

        # KDJ(9,3,3)：RSV 基于 9 日最高/最低，K/D 三日指数平滑，J=3K-2D
        kdj_k, kdj_d, kdj_j = [None] * n, [None] * n, [None] * n
        kv, dv = 50.0, 50.0
        for i in range(n):
            if i + 1 >= 9:
                hh = max(highs[i - 8:i + 1])
                ll = min(lows[i - 8:i + 1])
                rsv = (closes[i] - ll) / (hh - ll) * 100 if hh > ll else 50.0
                kv = (2 * kv + rsv) / 3
                dv = (2 * dv + kv) / 3
                kdj_k[i], kdj_d[i], kdj_j[i] = kv, dv, 3 * kv - 2 * dv

        # ATR(14)（Wilder 平滑的真实波幅）
        atr = [None] * n
        tr_sum = 0.0
        for i in range(n):
            pc = closes[i - 1] if i > 0 else closes[0]
            tr = max(highs[i] - lows[i], abs(highs[i] - pc), abs(lows[i] - pc))
            if i < 14:
                tr_sum += tr
                if i == 13:
                    atr[i] = tr_sum / 14
            elif atr[i - 1] is not None:
                atr[i] = (atr[i - 1] * 13 + tr) / 14

        # 动量 ROC(12)：较 12 日前收盘的涨跌幅（%）
        roc = [None] * n
        for i in range(12, n):
            if closes[i - 12]:
                roc[i] = (closes[i] / closes[i - 12] - 1) * 100

        return {"ma5": ma5, "ma10": ma10, "ma20": ma20, "ma60": ma60, "dif": dif, "dea": dea,
                "rsi": rsi, "boll_up": boll_up, "boll_mid": boll_mid, "boll_low": boll_low,
                "vol20": vol20, "kdj_k": kdj_k, "kdj_d": kdj_d, "kdj_j": kdj_j,
                "atr": atr, "roc": roc}

    # ---------------- 涓诲叆鍙?----------------
    def run(self, strategy_id: str, symbols: List[str],
            price_data: Dict[str, List[Dict]], custom_code: str = "") -> Dict:
        """运行回测。price_data: {symbol: [{date,open,close,high,low,volume}]}。"""
        self.strategy_id = strategy_id or self.strategy_id
        # 优先加载用户自定义策略（策略管理里维护的代码）
        self.custom_handle = self._compile_custom_handle(custom_code) if (custom_code and custom_code.strip()) else None
        if self.custom_handle is None and strategy_id not in self.STRATEGIES:
            # 无自定义代码且非内置策略 -> 退化为小市值持仓型
            self.strategy_id = "bse_smallcap"
        norm: Dict[str, Dict] = {}
        for sym, rows in (price_data or {}).items():
            clean = [{"date": str(r["date"])[:10], "close": float(r["close"]),
                      "open": float(r.get("open") or r["close"]), "high": float(r.get("high") or r["close"]),
                      "low": float(r.get("low") or r["close"]), "volume": float(r.get("volume") or 0)}
                     for r in rows if r.get("close")]
            clean.sort(key=lambda r: r["date"])
            if clean:
                closes = [r["close"] for r in clean]
                highs = [r["high"] for r in clean]
                lows = [r["low"] for r in clean]
                norm[sym] = {"rows": clean, "ta": BacktestEngine._ta_series(closes, highs, lows)}

        dates = sorted(set(r["date"] for d in norm.values() for r in d["rows"]))
        default_days = min(int(self.config.get("days", 120) or 120), len(dates) or 120)
        if not dates:
            return self._calculate_metrics([1.0], [1.0], [], [])
        dates = dates[-default_days:]
        date_set = set(dates)

        # 基准：真实上证指数（不缺失时），缺失时用等权股票指数兜底
        bench_raw = self._benchmark_daily()
        bench_dates = {d["date"]: d["close"] for d in bench_raw}
        bench0 = 1.0
        if bench_dates:
            for bd in reversed(bench_raw):
                if bd["date"] <= dates[0]:
                    bench0 = bd["close"]
                    break
            else:
                bench0 = next(iter(bench_dates.values()))

        rebalance_dates = dates[::self.rebalance_days]  # 定期调仓日（小市值/动量共用）

        benchmark = [1.0]
        for i, date in enumerate(dates):
            # 1) 策略信号（事件驱动）
            if str(self.strategy_id) == "moving_avg":
                self._signal_ma_cross(date, norm, date_set)
            elif str(self.strategy_id) == "macd_signal":
                self._signal_macd(date, norm, date_set)
            elif str(self.strategy_id) == "bollinger":
                self._signal_bollinger(date, norm, date_set)
            elif str(self.strategy_id) == "rsi_meanrev":
                self._signal_rsi(date, norm, date_set)
            elif str(self.strategy_id) == "bse_smallcap":
                if date in rebalance_dates:
                    self._portfolio_rebalance(date, symbols, norm, date_set)
            elif str(self.strategy_id) == "grid_okx":
                self._signal_grid(date, norm, date_set)
            elif str(self.strategy_id) == "kdj_golden":
                self._signal_kdj(date, norm, date_set)
            elif str(self.strategy_id) == "volume_breakout":
                self._signal_volume_breakout(date, norm, date_set)
            elif str(self.strategy_id) == "turtle":
                self._signal_turtle(date, norm, date_set)
            elif str(self.strategy_id) == "momentum":
                if date in rebalance_dates:
                    self._signal_momentum(date, norm, date_set)
            elif str(self.strategy_id) == "mean_reversion":
                self._signal_mean_reversion(date, norm, date_set)
            elif self.custom_handle is not None:
                self._signal_custom(date, norm, date_set)
            else:  # 退化为持仓型
                if date in rebalance_dates:
                    self._portfolio_rebalance(date, symbols, norm, date_set)

            # 2) 止损检查
            self._check_stop_loss(date, norm, date_set)

            # 3) 组合市值
            value = self.cash + sum(p["shares"] * (norm.get(c, {}).get("rows") or [])[0].get("close", 0)
                                    for c, p in self.positions.items() if c in norm)
            self.daily_values.append(value)
            self.dates.append(date)

            # 4) 基准曲线（按日期对齐现值）
            b_cur = bench_dates.get(date)
            if bench0 and b_cur:
                benchmark.append(b_cur / bench0)
            else:
                benchmark.append(benchmark[-1])

        equity = [round(v / self.initial_cash, 4) for v in self.daily_values]
        return self._calculate_metrics(equity, benchmark, self.dates, self.transactions)

    # ---------------- 基准 ----------------
    def _benchmark_daily(self) -> List[Dict]:
        """上证指数真实日K（东方财富直连，akshare 兜底）。"""
        try:
            headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                       "Referer": "https://quote.eastmoney.com/"}
            beg = (datetime.now() - timedelta(days=400)).strftime("%Y%m%d")
            end = datetime.now().strftime("%Y%m%d")
            url = ("https://push2his.eastmoney.com/api/qt/stock/kline/get"
                   f"?secid=1.000001&fields1=f1,f2,f3,f4,f5,f6,f7,f8"
                   f"&fields2=f51,f52,f53,f54,f55,f56,f57,f58&klt=101&fqt=1&beg={beg}&end={end}")
            r = requests.get(url, headers=headers, timeout=10)
            r.raise_for_status()
            data = (r.json() or {}).get("data") or {}
            klines = data.get("klines") or []
            out = []
            for line in klines[-300:]:
                parts = str(line).split(",")
                if len(parts) >= 3:
                    try:
                        out.append({"date": parts[0][:10], "close": float(parts[2])})
                    except (TypeError, ValueError):
                        continue
            if out:
                return out
        except Exception as e:  # noqa: BLE001
            logger.warning("benchmark EM fetch failed: %s", e)
        try:
            if ak is not None:
                df = ak.stock_zh_index_daily(symbol="sh000001")
                if df is not None and len(df):
                    return [{"date": str(r["date"])[:10], "close": float(r["close"])}
                            for _, r in df.tail(300).iterrows()]
        except Exception as e:  # noqa: BLE001
            logger.warning("benchmark akshare fetch failed: %s", e)
        return []

    # ---------------- 各策略信号 ----------------
    def _ta_at(self, data: Dict, idx: int) -> Dict:
        return {k: (v[idx] if v and 0 <= idx < len(v) else None) for k, v in data["ta"].items()}

    def _row_at(self, data: Dict, date: str):
        """返回 (index, row)；找到最接近该日期的 row。"""
        rows = data["rows"]
        for i in range(len(rows) - 1, -1, -1):
            if rows[i]["date"] <= date:
                return i, rows[i]
        return -1, None

    def _signal_ma_cross(self, date: str, norm: Dict, date_set):
        for sym, data in norm.items():
            i, row = self._row_at(data, date)
            if i < 1 or not date_set:
                continue
            prev = self._ta_at(data, i - 1)
            cur = self._ta_at(data, i)
            price = row["close"]
            if None in (cur["ma5"], cur["ma20"], prev["ma5"], prev["ma20"]):
                continue
            if prev["ma5"] <= prev["ma20"] and cur["ma5"] > cur["ma20"]:
                self._buy(sym, self._target_shares(price), price, date, "MA5上穿MA20金叉")
            elif prev["ma5"] >= prev["ma20"] and cur["ma5"] < cur["ma20"]:
                if sym in self.positions:
                    self._sell(sym, self.positions[sym]["shares"], price, date, "MA5下穿MA20死叉")

    def _signal_macd(self, date: str, norm: Dict, date_set):
        for sym, data in norm.items():
            i, row = self._row_at(data, date)
            if i < 1:
                continue
            prev = self._ta_at(data, i - 1)
            cur = self._ta_at(data, i)
            price = row["close"]
            if None in (cur["dif"], cur["dea"], prev["dif"], prev["dea"]):
                continue
            vol_ok = True
            if cur["vol20"] and row.get("volume"):
                vol_ok = row["volume"] >= cur["vol20"]
            if prev["dif"] <= prev["dea"] and cur["dif"] > cur["dea"] and vol_ok:
                self._buy(sym, self._target_shares(price), price, date, "MACD金叉+量能")
            elif prev["dif"] >= prev["dea"] and cur["dif"] < cur["dea"]:
                if sym in self.positions:
                    self._sell(sym, self.positions[sym]["shares"], price, date, "MACD死叉")

    def _signal_bollinger(self, date: str, norm: Dict, date_set):
        for sym, data in norm.items():
            i, row = self._row_at(data, date)
            if i < 0:
                continue
            cur = self._ta_at(data, i)
            price = row["close"]
            if cur["boll_low"] is None or cur["boll_up"] is None:
                continue
            if price <= cur["boll_low"]:
                self._buy(sym, self._target_shares(price), price, date, "触及布林下轨")
            elif price >= cur["boll_up"] and sym in self.positions:
                self._sell(sym, self.positions[sym]["shares"], price, date, "触及布林上轨")

    def _signal_rsi(self, date: str, norm: Dict, date_set):
        for sym, data in norm.items():
            i, row = self._row_at(data, date)
            if i < 0:
                continue
            r = self._ta_at(data, i)["rsi"]
            price = row["close"]
            if r is None:
                continue
            if r < 30:
                self._buy(sym, self._target_shares(price), price, date, f"RSI超卖({r:.0f})")
            elif r > 70 and sym in self.positions:
                self._sell(sym, self.positions[sym]["shares"], price, date, f"RSI超买({r:.0f})")

    def _signal_grid(self, date: str, norm: Dict, date_set):
        for sym, data in norm.items():
            i, row = self._row_at(data, date)
            if i < 20:
                continue
            price = row["close"]
            cur = self._ta_at(data, i)
            lo, hi = cur["boll_low"], cur["boll_up"]
            if lo is None:
                continue
            step = max((hi - lo) * 0.1, price * 0.01)
            if sym not in self.positions and price <= lo + step:
                self._buy(sym, self._target_shares(price), price, date, "网格买入(接近下轨)")
            elif sym in self.positions:
                cost = self.positions[sym]["cost"]
                if price >= cost * (1 + 0.02) or price >= hi:
                    self._sell(sym, self.positions[sym]["shares"], price, date, "网格卖出(获利)")

    def _signal_kdj(self, date: str, norm: Dict, date_set):
        """KDJ金叉：J<20 超卖后 K 上穿 D 买入；死叉或止盈(+15%/J>90)卖出。"""
        for sym, data in norm.items():
            i, row = self._row_at(data, date)
            if i < 1:
                continue
            prev = self._ta_at(data, i - 1)
            cur = self._ta_at(data, i)
            price = row["close"]
            if None in (cur["kdj_k"], cur["kdj_d"], cur["kdj_j"], prev["kdj_k"], prev["kdj_d"]):
                continue
            # J 值进入超卖区后打开买入窗口
            if cur["kdj_j"] < 20 or (prev["kdj_j"] is not None and prev["kdj_j"] < 20):
                self._kdj_oversold[sym] = True
            golden = prev["kdj_k"] <= prev["kdj_d"] and cur["kdj_k"] > cur["kdj_d"]
            dead = prev["kdj_k"] >= prev["kdj_d"] and cur["kdj_k"] < cur["kdj_d"]
            if sym in self.positions:
                pos = self.positions[sym]
                pnl = (price - pos["cost"]) / pos["cost"] if pos["cost"] else 0.0
                if dead:
                    self._sell(sym, pos["shares"], price, date, "KDJ死叉")
                elif pnl >= 0.15 or cur["kdj_j"] > 90:
                    self._sell(sym, pos["shares"], price, date, "KDJ止盈")
            elif golden and self._kdj_oversold.get(sym):
                self._buy(sym, self._target_shares(price), price, date, "KDJ超卖后金叉")
                self._kdj_oversold[sym] = False

    def _signal_volume_breakout(self, date: str, norm: Dict, date_set):
        """放量突破：成交量>2倍20日均量且收盘创20日新高买入；跌破10日线卖出。"""
        for sym, data in norm.items():
            i, row = self._row_at(data, date)
            if i < 20:
                continue
            price = row["close"]
            cur = self._ta_at(data, i)
            vol20 = cur["vol20"]
            if not vol20 or vol20 <= 0 or not row.get("volume") or price <= 0:
                continue
            breakout = row["volume"] >= 2 * vol20 and price >= max(r["close"] for r in data["rows"][i - 20:i])
            if sym not in self.positions:
                if breakout:
                    self._buy(sym, self._target_shares(price), price, date, "放量突破20日新高")
            elif cur["ma10"] is not None and price < cur["ma10"]:
                self._sell(sym, self.positions[sym]["shares"], price, date, "跌破10日线")

    def _signal_turtle(self, date: str, norm: Dict, date_set):
        """海龟策略：收盘突破20日最高价买入；跌破10日最低价卖出；入场价-2*ATR 止损。"""
        for sym, data in norm.items():
            i, row = self._row_at(data, date)
            if i < 20:
                continue
            price = row["close"]
            if price <= 0:
                continue
            hh20 = max(r["high"] for r in data["rows"][i - 20:i])
            if sym not in self.positions:
                if price >= hh20:
                    self._buy(sym, self._target_shares(price), price, date, "海龟突破20日高点")
                continue
            pos = self.positions[sym]
            ll10 = min(r["low"] for r in data["rows"][i - 10:i])
            atr = self._ta_at(data, i)["atr"]
            if price < ll10:
                self._sell(sym, pos["shares"], price, date, "海龟跌破10日低点")
            elif atr is not None and price <= pos["cost"] - 2 * atr:
                self._sell(sym, pos["shares"], price, date, "ATR通道止损")

    def _signal_momentum(self, date: str, norm: Dict, date_set):
        """动量轮动：清出组合后等权买入近20日涨幅最高的前3只（每 rebalance_days 调仓）。"""
        scored = []
        for sym, data in norm.items():
            i, row = self._row_at(data, date)
            if i < 20:
                continue
            base = data["rows"][i - 20]["close"]
            price = row["close"]
            if base > 0 and price > 0:
                scored.append((sym, price / base - 1))
        scored.sort(key=lambda x: x[1], reverse=True)
        top = [s for s, _ in scored[:3]]
        # 先卖出不在目标组合的持仓，释放资金
        for sym in list(self.positions.keys()):
            if sym not in top:
                i, row = self._row_at(norm[sym], date)
                if i >= 0 and row:
                    self._sell(sym, self.positions[sym]["shares"], row["close"], date, "动量轮出")
        for sym in top:
            i, row = self._row_at(norm[sym], date)
            price = row["close"]
            if sym not in self.positions and price > 0:
                self._buy(sym, self._target_shares(price, self.initial_cash / 3), price, date, "动量轮入(20日涨幅TOP3)")

    def _signal_mean_reversion(self, date: str, norm: Dict, date_set):
        """均值回归：收盘低于MA20超5%且RSI<35买入；回归MA20卖出。"""
        for sym, data in norm.items():
            i, row = self._row_at(data, date)
            if i < 0:
                continue
            price = row["close"]
            cur = self._ta_at(data, i)
            ma20 = cur["ma20"]
            if ma20 is None or price <= 0:
                continue
            if sym not in self.positions:
                rsi = cur["rsi"]
                if rsi is not None and rsi < 35 and price <= ma20 * 0.95:
                    self._buy(sym, self._target_shares(price), price, date, "均值回归(MA20下方5%+RSI超卖)")
            elif price >= ma20:
                self._sell(sym, self.positions[sym]["shares"], price, date, "回归MA20")

    def _compile_custom_handle(self, code: str):
        """编译用户自定义策略源代码（可为多行字符串），由调用方提供 handle(data)->str。
        data 字段: code/date/close/open/high/low/volume/ma5/ma10/ma20/ma60/rsi/dif/dea/boll_low/boll_up/mid
        /kdj_k/kdj_d/kdj_j/atr/roc。
        # 应返回 "BUY"/"SELL"/"HOLD" 用于交易决策；禁止 import；可选沙箱一致性。"""
        from ..services.screen import CustomScreenSandbox

        import builtins as _bi
        import logging as _lg
        try:
            nspace: Dict = {"data": None, "__builtins__": {k: getattr(_bi, k) for k in CustomScreenSandbox._SAFE_BUILTINS}}
            exec(compile(code, "<user_backtest_strategy>", "exec"), nspace)  # noqa: S102
        except Exception as e:  # noqa: BLE001
            _lg.getLogger(__name__).warning("custom backtest code compile failed: %s", e)
            return None
        if callable(nspace.get("handle")):
            return nspace["handle"]
        return None

    def _signal_custom(self, date: str, norm: Dict, date_set):
        """事件驱动：每个交易日调用用户 handle(data)，BUY 买入 / SELL 清仓 / HOLD 持有。        handle 抛异常或返回非法值时按 HOLD 处理（不影响回测主循环）。"""
        if self.custom_handle is None:
            return
        for sym, data in norm.items():
            i, row = self._row_at(data, date)
            if i < 0:
                continue
            cur = self._ta_at(data, i)
            bar = {
                "code": sym, "date": date, "close": row["close"],
                "open": row.get("open", row["close"]), "high": row.get("high", row["close"]),
                "low": row.get("low", row["close"]), "volume": row.get("volume", 0),
                "ma5": cur["ma5"], "ma10": cur["ma10"], "ma20": cur["ma20"], "ma60": cur["ma60"],
                "rsi": cur["rsi"], "dif": cur["dif"], "dea": cur["dea"],
                "boll_low": cur["boll_low"], "boll_up": cur["boll_up"], "boll_mid": cur["boll_mid"],
                "kdj_k": cur["kdj_k"], "kdj_d": cur["kdj_d"], "kdj_j": cur["kdj_j"],
                "atr": cur["atr"], "roc": cur["roc"],
            }
            price = row["close"]
            if price <= 0:
                continue
            try:
                sig = str(self.custom_handle(bar)).upper()
            except Exception as e:  # noqa: BLE001
                logger.debug("custom strategy handle error on %s/%s: %s", sym, date, e)
                sig = "HOLD"
            if sig == "BUY" and sym not in self.positions:
                self._buy(sym, self._target_shares(price), price, date, "买入信号")
            elif sig == "SELL" and sym in self.positions:
                self._sell(sym, self.positions[sym]["shares"], price, date, "卖出信号")

    def _portfolio_rebalance(self, date: str, symbols: List[str], norm: Dict, date_set):
        """小市值持仓型：等权持有全部 symbols（按最大仓位分批），定期轮换 + 止损。"""
        target_value = self.initial_cash * max(self.max_position, 0.2)
        for sym in symbols:
            if sym not in norm:
                continue
            i, row = self._row_at(norm[sym], date)
            if i < 0:
                continue
            price = row["close"]
            if price <= 0:
                continue
            if sym not in self.positions:
                self._buy(sym, self._target_shares(price, target_value), price, date, "持仓建仓")
            else:
                cost = self.positions[sym]["cost"]
                pnl = (price - cost) / cost
                if pnl <= self.stop_loss:
                    self._sell(sym, self.positions[sym]["shares"], price, date, "止损")
                elif pnl >= 0.30:
                    self._sell(sym, self.positions[sym]["shares"], price, date, "止盈+30%")

    def _target_shares(self, price: float, target_value: float = 0) -> int:
        if price <= 0:
            return 0
        tv = target_value or (self.initial_cash * self.max_position)
        shares = int(tv / price / 100) * 100
        if shares < 100 and price * 100 <= self.cash:
            shares = 100  # 100 股为A股最小交易单位，模拟真实A股成交量
        return shares

    def _check_stop_loss(self, date: str, norm: Dict, date_set):
        for sym, pos in list(self.positions.items()):
            i, row = self._row_at(norm.get(sym, {}), date)
            if i < 0:
                continue
            price = row["close"]
            if price <= 0:
                continue
            cost = pos["cost"]
            pnl = (price - cost) / cost
            if pnl <= self.stop_loss:
                self._sell(sym, pos["shares"], price, date, "止损")

    # ---------------- 成交 ----------------
    def _buy(self, code, shares, price, date, reason="买入"):
        cost = shares * price * (1 + self.commission + self.slippage)
        if cost <= 0 or cost > self.cash or shares <= 0:
            return
        self.cash -= cost
        self.positions[code] = {"shares": shares, "cost": price, "buy_date": date}
        self.transactions.append({
            "date": date, "code": code, "action": "BUY", "shares": shares,
            "price": round(price, 3), "amount": round(cost, 2), "reason": reason,
            "pnl": None,
        })

    def _sell(self, code, shares, price, date, reason="卖出"):
        if code not in self.positions or shares <= 0:
            return
        pos = self.positions[code]
        sell_shares = min(shares, pos["shares"])
        revenue = sell_shares * price * (1 - self.commission - self.slippage)
        pnl_pct = (price - pos["cost"]) / pos["cost"] * 100 if pos["cost"] else 0
        pnl_amt = (price - pos["cost"]) * sell_shares - sell_shares * price * (self.commission + self.slippage)
        self.cash += revenue
        self.positions[code]["shares"] -= sell_shares
        if self.positions[code]["shares"] <= 0:
            del self.positions[code]
        self.transactions.append({
            "date": date, "code": code, "action": "SELL", "shares": sell_shares,
            "price": round(price, 3), "amount": round(revenue, 2), "reason": reason,
            "pnl_pct": round(pnl_pct, 2), "pnl": round(pnl_amt, 2),
        })

    def _get_price(self, code, date, price_data):
        for d in price_data.get(code, []):
            if d["date"] == date:
                return d["close"]
        return 0

    def _calculate_metrics(self, equity, benchmark, dates, transactions) -> Dict:
        if len(equity) < 2:
            return {
                "initial_cash": self.initial_cash, "final_value": self.initial_cash,
                "total_return": 0.0, "annual_return": 0.0, "benchmark_return": 0.0,
                "max_drawdown": 0.0, "sharpe": 0.0, "sharpe_ratio": 0.0,
                "sortino": 0.0, "calmar": 0.0, "win_rate": 0.0,
                "profit_loss_ratio": 0.0, "trade_count": 0, "monthly_returns": {},
                "equity_curve": [1.0], "equity": [1.0],
                "benchmark_curve": [1.0], "benchmark": [1.0],
                "dates": [str(d) for d in (dates or [])],
                "strategy_id": self.strategy_id, "transactions": [],
            }
        total_return = (equity[-1] - 1) * 100
        days = len(equity)
        annual_return = ((equity[-1] / equity[0]) ** (252 / max(days, 1)) - 1) * 100 if equity[0] else 0
        peak = np.maximum.accumulate(equity)
        drawdown = (peak - equity) / peak
        max_dd = float(np.max(drawdown) * 100) if len(drawdown) > 0 else 0
        equity_arr = np.array(equity, dtype=float)
        returns = np.diff(equity_arr) / equity_arr[:-1] if len(equity) > 1 else np.array([0.0])
        # 样本不足 20 时无法测算标准差，Sharpe 无意义，置 0
        sharpe = float((np.mean(returns) / (np.std(returns) + 1e-8)) * np.sqrt(252) * 100) \
            if len(returns) >= 20 else 0.0
        # 索提诺比率：下行波动率（无下行样本时置 0）
        sortino = 0.0
        if len(returns) >= 20:
            dstd = float(np.sqrt(np.mean(np.minimum(returns, 0.0) ** 2)))
            if dstd > 1e-12:
                sortino = float(np.mean(returns) / dstd * np.sqrt(252) * 100)
        # 卡玛比率：年化收益 / 最大回撤
        calmar = (annual_return / max_dd) if max_dd > 0 else 0.0

        sells = [t for t in transactions if t.get("action") == "SELL"]
        wins = [t for t in sells if (t.get("pnl") or 0) >= 0]
        losses = [t for t in sells if (t.get("pnl") or 0) < 0]
        win_rate = (len(wins) / len(sells) * 100) if sells else 0.0
        # 盈亏比：平均单笔盈利% / 平均单笔亏损%（无亏损样本时置 0）
        avg_win = sum(float(t.get("pnl_pct") or 0) for t in wins) / len(wins) if wins else 0.0
        avg_loss = abs(sum(float(t.get("pnl_pct") or 0) for t in losses) / len(losses)) if losses else 0.0
        profit_loss_ratio = (avg_win / avg_loss) if avg_loss > 0 else 0.0

        # 将基准线对齐到同等长度
        bench = benchmark[:len(equity)]
        if len(bench) < len(equity):
            bench = bench + [bench[-1]] * (len(equity) - len(bench))
        bench = [round(float(b), 4) if b is not None else 1.0 for b in bench]
        benchmark_return = (bench[-1] - 1) * 100

        # 月度收益表 {YYYY-MM: pct}（按自然月复利）
        month_mult: Dict[str, float] = {}
        for idx in range(1, len(equity)):
            prev_e = equity[idx - 1]
            r = (equity[idx] / prev_e - 1) if prev_e else 0.0
            m = str(dates[idx])[:7] if idx < len(dates) else ""
            if m:
                month_mult[m] = month_mult.get(m, 1.0) * (1 + r)
        monthly_returns = {m: round((v - 1) * 100, 2) for m, v in sorted(month_mult.items())}

        return {
            "initial_cash": self.initial_cash,
            "final_value": round(self.initial_cash * equity[-1], 2),
            "total_return": round(total_return, 2),
            "annual_return": round(annual_return, 2),
            "benchmark_return": round(benchmark_return, 2),
            "max_drawdown": round(max_dd, 2),
            "sharpe": round(sharpe, 2),
            "sharpe_ratio": round(sharpe, 2),
            "sortino": round(sortino, 2),
            "calmar": round(calmar, 2),
            "win_rate": round(win_rate, 2),
            "profit_loss_ratio": round(profit_loss_ratio, 2),
            "trade_count": len(transactions),
            "monthly_returns": monthly_returns,
            "equity_curve": [round(float(v), 4) for v in equity],
            "equity": [round(float(v), 4) for v in equity],
            "benchmark_curve": bench,
            "benchmark": bench,
            "dates": [str(d) for d in dates],
            "strategy_id": self.strategy_id,
            "transactions": transactions[-200:],
        }


# ============================================================
# LLM 深度投研（OpenAI 兼容客户端）
# ============================================================
# 预置主流通用 OpenAI 兼容网关，前台可在基础上编辑 base_url / api_key / model
async def run_backtest_task(task_id: str, strategy_id: str, symbols: List[str], config: Dict):
    """异步执行回测任务。"""
    task = AnalysisTask(
        task_id=task_id, symbol=",".join(symbols),
        status=TaskStatus.RUNNING, progress=0,
        current_stage="准备数据", created_at=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    )
    _task_store[task_id] = task

    try:
        # 阶段 1: 获取数据
        task.current_stage = "获取行情数据"
        task.progress = 10
        price_data = {}
        for sym in symbols:
            price_data[sym] = AStockDataService.get_kline(sym, days=config.get("days", 120))
            await asyncio.sleep(0.1)

        # 阶段 2: 运行回测
        task.current_stage = "运行回测引擎"
        task.progress = 40
        engine = BacktestEngine(config)
        metrics = engine.run(strategy_id, symbols, price_data, custom_code=config.get("code", ""))
        strat = StrategyService.get_strategy(strategy_id)
        if config.get("name"):
            strat_name = config["name"]
        elif strat:
            strat_name = strat["name"]
        else:
            strat_name = strategy_id
        _backtest_store[task_id] = BacktestResult(
            strategy_name=strat_name,
            initial_cash=metrics.get("initial_cash", config.get("initial_cash", 1000000)),
            final_value=metrics.get("final_value", 0),
            total_return=metrics.get("total_return", 0),
            annual_return=metrics.get("annual_return", 0),
            max_drawdown=metrics.get("max_drawdown", 0),
            sharpe_ratio=metrics.get("sharpe_ratio", 0),
            win_rate=metrics.get("win_rate", 0),
            trade_count=metrics.get("trade_count", 0),
            equity_curve=metrics.get("equity_curve", []),
            transactions=metrics.get("transactions", []),
        )

        # 阶段 3: 生成报告
        task.current_stage = "生成分析报告"
        task.progress = 70
        await asyncio.sleep(0.5)

        # 阶段 4: AI分析
        task.current_stage = "AI智能分析"
        task.progress = 85
        ai_result = await AIAnalysisService.analyze_stock(symbols[0] if symbols else "", task_id)

        task.progress = 100
        task.status = TaskStatus.COMPLETED
        task.current_stage = "完成"
        task.result = {
            "backtest": metrics,
            "ai_analysis": ai_result,
            "strategy": ({"id": strategy_id, "name": strat_name, "code": config.get("code", "")}
                         if config.get("code") else StrategyService.get_strategy(strategy_id)),
        }
        task.completed_at = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    except Exception as e:
        task.status = TaskStatus.FAILED
        task.error = str(e)
        task.current_stage = "失败"


# ============================================================
# FastAPI 应用
# ============================================================
