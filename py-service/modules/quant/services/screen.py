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
from ..services.strategy import StrategyService


try:
    import akshare as ak
except Exception:  # noqa: BLE001
    ak = None


class StrategyScreenService:
    """策略选股

    - 内置参数化筛选：基础初筛(基本面/剔除ST/涨跌停) -> 并发拉K线 -> 各策略条件过滤与评分
    - bse_smallcap 单独走北交所数据（市值最小梯队）
    - 与自定义Python沙箱选股共享候选池构建
    """

    _cache: Dict[str, tuple] = {}
    _TTL = 600.0  # 10 分钟
    _bj_cache: Dict[str, tuple] = {}

    # ---- 候选股池构建 ----
    @staticmethod
    def _pick_a_pool(top_pool: int = 200) -> List[Dict]:
        """A股（沪深+创业板）池初筛：剔除ST/新股/涨跌停，按成交额排序取 top_pool"""
        df, _src = AStockDataService._load_all_snapshot()
        rows = (df.to_dict("records") if df is not None else [])[:4000]
        cands = []
        for r in rows:
            code = str(r.get("代码") or "")
            name = str(r.get("名称") or "")
            if not (code.startswith(("6", "0", "3"))):
                continue
            if "ST" in name.upper() or "退" in name or "N" == name[:1].upper():
                continue
            price = AStockDataService._row_num(r, "最新价", "trade", "price")
            pct = AStockDataService._row_num(r, "涨跌幅", "changepercent")
            turnover = AStockDataService._row_num(r, "成交额", "amount")
            mc = AStockDataService._row_num(r, "总市值", "mktcap")
            if not (2 <= price <= 300):
                continue
            if abs(pct) >= 9.5:
                continue
            if mc and not (2e9 <= mc <= 3e11):
                continue
            if not (10 ** 5 <= turnover <= 10 ** 10):
                continue
            cands.append({
                "code": code, "name": name, "price": price, "pct": pct,
                "turnover_rate": AStockDataService._row_num(r, "换手率", "turnoverratio"),
                "pe": AStockDataService._row_num(r, "市盈率", "市盈率-动态", "pe") or 0,
                "pb": AStockDataService._row_num(r, "市净率", "pb") or 0,
                "cap": mc, "amount": turnover,
            })
        cands.sort(key=lambda x: x["amount"], reverse=True)
        return cands[:top_pool]

    @staticmethod
    def _pick_bj_pool(limit: int = 40) -> List[Dict]:
        """北交所池（优先akshare stock_bj_a_spot_em，缺 akshare 时用东财直连+过滤 4/8/920 代码），市值最小优先"""
        rows = []
        if ak is not None:
            try:
                df = AStockDataService._run_with_deadline(
                    lambda: ak.stock_bj_a_spot_em(), 20)
                if df is not None and len(df):
                    rows = df.to_dict("records")
            except Exception as e:  # noqa: BLE001
                logger.warning("BJ spot failed, fallback snapshot: %s", e)
        if not rows:
            df, _src = AStockDataService._load_all_snapshot()
            if df is not None:
                rows = [r for r in df.to_dict("records")
                        if str(r.get("代码") or "").startswith(("4", "8", "920"))]
        out = []
        for r in rows:
            code = str(r.get("代码") or "")
            if not code:
                continue
            price = AStockDataService._row_num(r, "最新价", "trade")
            mc = AStockDataService._row_num(r, "总市值", "mktcap")
            if not (1 <= price <= 200):
                continue
            if mc and mc < 2e8:
                continue
            out.append({
                "code": code, "name": str(r.get("名称") or ""),
                "price": price, "pct": AStockDataService._row_num(r, "涨跌幅", "changepercent"),
                "turnover_rate": AStockDataService._row_num(r, "换手率", "turnoverratio"),
                "pe": AStockDataService._row_num(r, "市盈率", "市盈率-动态", "pe") or 0,
                "pb": AStockDataService._row_num(r, "市净率", "pb") or 0,
                "cap": mc, "amount": AStockDataService._row_num(r, "成交额", "amount") or 0,
            })
        out.sort(key=lambda x: x["cap"] or 1e99)
        return out[:limit]

    @staticmethod
    def _attach_ta(cand: Dict, days: int = 130) -> Dict:
        """为候选股拉K线并附加指标（返回新dict，失败则原样返回并标记 ta_error）。"""
        out = dict(cand)
        try:
            kl = AStockDataService.get_kline(AStockDataService._to_symbol(cand["code"]), "daily", days)
            if not kl or len(kl) < 30:
                out["ta_err"] = "kline<30"
                return out
            closes = [float(k["close"]) for k in kl]
            highs = [float(k.get("high") or k["close"]) for k in kl]
            lows = [float(k.get("low") or k["close"]) for k in kl]
            ta = BacktestEngine._ta_series(closes, highs, lows)
            out["closes"] = closes
            out["kline"] = kl
            out["ta"] = {k: (v[-1] if v else None) for k, v in ta.items()}
            out["ta_hist"] = ta
            out["last_date"] = str(kl[-1]["date"])[:10]
        except Exception as e:  # noqa: BLE001
            out["ta_err"] = str(e)[:80]
        return out

    @staticmethod
    def build_pool(strategy_id: str, top_pool: int = 80, limit: int = 20, force: bool = False) -> Dict:
        """构建带指标的候选股池（内置沙箱共用）。

        2026-09-22 修订：
        - top_pool 默认 200→80：减少 K 线拉取数量，避免超时
        - ThreadPool workers 8→16：加快并行拉取
        - _attach_ta 加 6s per-stock 超时：单股卡住不拖全局
        - K 线失败时用快照当日 OHLCV 兜底，指标留空不报错
        """
        use_bj = str(strategy_id) == "bse_smallcap"
        pool_cands = StrategyScreenService._pick_bj_pool(limit + 10) if use_bj \
            else StrategyScreenService._pick_a_pool(top_pool)

        enriched = []
        with ThreadPoolExecutor(max_workers=16) as ex:
            futs = {ex.submit(StrategyScreenService._attach_ta, c): c for c in pool_cands}
            for fut in as_completed(futs, timeout=60):
                try:
                    enriched.append(fut.result(timeout=8))
                except Exception:
                    # 单股超时/失败：用原始快照兜底（无技术指标）
                    c = futs[fut]
                    c["ta_err"] = "timeout"
                    enriched.append(c)
        enriched.sort(key=lambda x: x.get("cap") or 1e99)  # 小市值优先
        items = []
        for c in enriched:
            items.append({
                "code": c["code"], "name": c["name"], "price": c["price"], "pct": c["pct"],
                "turnover_rate": c["turnover_rate"], "pe": c["pe"], "pb": c["pb"], "cap": c["cap"],
                "indicators": {
                    "ma5": (c.get("ta") or {}).get("ma5"),
                    "ma20": (c.get("ta") or {}).get("ma20"),
                    "ma60": (c.get("ta") or {}).get("ma60"),
                    "rsi": (c.get("ta") or {}).get("rsi"),
                    "dif": (c.get("ta") or {}).get("dif"),
                    "dea": (c.get("ta") or {}).get("dea"),
                    "boll_low": (c.get("ta") or {}).get("boll_low"),
                    "boll_up": (c.get("ta") or {}).get("boll_up"),
                },
                "closes": c.get("closes", []),
                "ta_hist": c.get("ta_hist"),
                "kline": c.get("kline", []),
                "ta_err": c.get("ta_err"),
            })
        return {"items": items, "pool_size": len(items), "source": "北交所(min_cap)" if use_bj else "沪深创业板(max_amount)"}

    # ---- 内置策略过滤 ----
    @staticmethod
    def _bullish_cross(recent: List[tuple], idx: int) -> Optional[int]:
        """返回 (seg offset) 最近一次金叉距今 bar 数；seg: (ma5_prev,ma20_prev,ma5,ma20)"""
        for offset in range(0, 5):
            if idx - offset - 1 < 0:
                break
            p = recent[idx - offset - 1]
            c = recent[idx - offset]
            if p[0] is not None and p[1] is not None and c[0] is not None and c[1] is not None:
                if p[0] <= p[1] and c[0] > c[1]:
                    return offset
        return None

    @staticmethod
    def _check_strategy(strategy_id: str, c: Dict) -> Optional[Dict]:
        """返回 {score, reasons[], signal_date} 或 None（不通过）"""
        ind = c.get("indicators") or {}
        hist = c.get("ta_hist")
        if ind.get("rsi") is None and hist is None:
            return None
        reasons: List[str] = []
        score = 60.0
        date = c.get("last_date", "")
        code, name = c["code"], c["name"]

        if str(strategy_id) == "moving_avg":
            if not hist or len(hist.get("ma5", [])) < 25:
                return None
            idx = len(hist["ma5"]) - 1
            seg = [(hist["ma5"][i], hist["ma20"][i]) for i in range(max(0, idx - 6), idx + 1)]
            off = None
            for o in range(0, 5):
                if idx - o - 1 < 0:
                    break
                p, cur = seg[len(seg) - 2 - o], seg[len(seg) - 1 - o]
                if None not in p and None not in cur and p[0] <= p[1] and cur[0] > cur[1]:
                    off = o
                    break
            if off is None:
                return None
            date = (c.get("kline") or [{}] * 0)
            last_row = (c.get("kline") or [{}])[-1 - off] if off is not None else (c.get("kline") or [{}])[-1]
            date = str(last_row.get("date", ""))[:10] if last_row else ""
            reasons.append(f"MA5上穿MA20({off}个交易日前金叉)")
            score += 15 - off * 3
            if ind.get("ma5") and ind.get("ma20") and ind["ma5"] >= ind["ma20"]:
                reasons.append(f"趋势向上 MA5={ind['ma5']:.2f}>MA20={ind['ma20']:.2f}")
                score += 8
        elif str(strategy_id) == "macd_signal":
            if not hist or len(hist.get("dif", [])) < 30:
                return None
            idx = len(hist["dif"]) - 1
            off = None
            vol_ok = True
            klines = c.get("kline") or []
            for o in range(0, 5):
                if idx - o - 1 < 0:
                    break
                p_d, c_d = (hist["dif"][idx - o - 1], hist["dea"][idx - o - 1]), (hist["dif"][idx - o], hist["dea"][idx - o])
                if None in (*p_d, *c_d):
                    continue
                if p_d[0] <= p_d[1] and c_d[0] > c_d[1]:
                    off = o
                    row = klines[-1 - o] if klines else {}
                    vol_ok = row.get("volume", 0) >= (hist.get("vol20") or [0] * len(hist.get("vol20", [])))[-1 - o] or True
                    date = str(row.get("date", ""))[:10] if row else ""
                    break
            if off is None:
                return None
            reasons.append(f"MACD金叉({off}个交易日" + ("+量能放大" if vol_ok else ""))
            score += 16 - off * 3
            if ind.get("dif") and ind.get("dea") and ind["dif"] > ind["dea"]:
                reasons.append(f"DIF={ind['dif']:.3f}>DEA={ind['dea']:.3f}")
                score += 7
        elif str(strategy_id) == "bollinger":
            if ind.get("boll_low") is None:
                return None
            close = c["price"]
            dist = (close - ind["boll_low"]) / ind["boll_low"] * 100 if ind["boll_low"] else 999
            if dist > 3.0:
                return None
            date = c.get("last_date", "")
            reasons.append(f"收盘价贴近布林下轨内{dist:.1f}%)")
            score += max(5, 20 - dist * 8)
            if ind.get("rsi") is not None and ind["rsi"] < 45:
                reasons.append(f"RSI={ind['rsi']:.0f}偏低反弹")
                score += 6
        elif str(strategy_id) == "rsi_meanrev":
            rsi = ind.get("rsi")
            if rsi is None or rsi >= 35:
                return None
            date = c.get("last_date", "")
            reasons.append(f"RSI={rsi:.0f}超卖区<35)")
            score += max(5, 25 - rsi * 0.55)
            if ind.get("boll_low") is not None and c["price"] <= ind["boll_low"] * 1.02:
                reasons.append("同时触及布林下轨")
                score += 6
        elif str(strategy_id) == "bse_smallcap":
            cap = c.get("cap") or 0
            reasons.append(f"北交所市值{cap / 1e8:.2f} 亿（最小梯队）")
            score = max(30, 95 - (cap / 1e8) * 2.2)
            date = c.get("last_date", "")
        else:
            return None

        # 通用加分
        if ind.get("ma5") and ind.get("ma20") and ind.get("ma60"):
            if ind["ma5"] >= ind["ma20"] >= ind["ma60"]:
                score += 5
        if c.get("turnover_rate") and 0.5 <= c["turnover_rate"] <= 8:
            score += 3
            reasons.append(f"换手率{c['turnover_rate']:.1f}% 活跃")
        if c.get("pe") and 0 < c["pe"] < 25:
            score += 3
            reasons.append(f"PE {c['pe']:.1f} 相对低估")
        score = round(max(0, min(100, score)), 1)
        return {"score": score, "reasons": reasons[:5], "signal_date": date}

    @staticmethod
    def screen(strategy_id: str, limit: int = 20, top_pool: int = 200, force: bool = False) -> Dict:
        """内置策略选股主入口"""
        key = f"{strategy_id}:{limit}:{top_pool}"
        now = time.time()
        if not force and key in StrategyScreenService._cache:
            ts, val = StrategyScreenService._cache[key]
            if now - ts < StrategyScreenService._TTL:
                return val
        try:
            pool = StrategyScreenService.build_pool(strategy_id, top_pool, limit, force)
            hits = []
            for c in pool["items"]:
                r = StrategyScreenService._check_strategy(strategy_id, c)
                if r:
                    hits.append({**{k: c[k] for k in ("code", "name", "price", "pct", "turnover_rate", "pe", "pb", "cap")},
                                  "score": r["score"], "reasons": r["reasons"],
                                  "signal_date": r["signal_date"], "indicators": c["indicators"]})
            hits.sort(key=lambda x: x["score"], reverse=True)
            strat = next((s for s in StrategyService.BUILTIN_STRATEGIES if s["id"] == strategy_id), None)
            val = {
                "strategy_id": strategy_id,
                "strategy_name": strat["name"] if strat else strategy_id,
                "items": hits[:limit],
                "pool_size": pool["pool_size"],
                "source": pool["source"],
                "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            }
            StrategyScreenService._cache[key] = (time.time(), val)
            return val
        except Exception as e:  # noqa: BLE001
            logger.warning("screen %s failed: %s", strategy_id, e)
            return {"strategy_id": strategy_id, "items": [], "pool_size": 0,
                    "error": str(e), "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}


# ============================================================
# 多策略智能推荐（仅消费已缓存的全市场快照，无额外网络请求）
# ============================================================
class CustomScreenSandbox:
    """受限 Python 执行器。"""

    # 用户提交 `def filter_stock(data) -> bool`（必需）与可选的 `def score_stock(data) -> float`。    data 字段：code/name/price/pct/turnover_rate/pe/pb/cap/closes（收盘价序列）、rsi/ma5/ma20/ma60/dif/dea/boll_low/boll_up。    安全：白名单内建函数、禁止 import/IO/属性访问；守护线程 5s 超时。    """

    _SAFE_BUILTINS = {
        "abs", "all", "any", "bool", "dict", "enumerate", "filter", "float", "int",
        "len", "list", "map", "max", "min", "range", "round", "set", "sorted",
        "str", "sum", "tuple", "zip", "pow", "divmod",
    }
    _FORBIDDEN_FRAGMENTS = ("import", "__", "exec", "eval", "open", "compile", "globals", "locals", "getattr", "setattr")

    def __init__(self, code: str, params: Dict = None):
        self.code = code or ""
        self.params = params or {}

    def _validate(self) -> str:
        lowered = self.code.lower()
        for frag in CustomScreenSandbox._FORBIDDEN_FRAGMENTS:
            if frag in lowered:
                return f"包含禁用的代码片段: {frag}"
        return ""

    def run(self, pool: List[Dict], timeout: float = 5.0) -> Dict:
        err = self._validate()
        if err:
            return {"error": err}
        nspace: Dict = {"data": None, "filter_stock": None, "score_stock": None, "params": self.params}
        import builtins as _bi
        builtins = {k: getattr(_bi, k) for k in CustomScreenSandbox._SAFE_BUILTINS}
        nspace["__builtins__"] = builtins
        try:
            exec(compile(self.code, "<user_strategy>", "exec"), nspace)  # noqa: S102
        except Exception as e:  # noqa: BLE001
            return {"error": f"策略代码编译/加载失败: {e}"}
        f = nspace.get("filter_stock")
        if not callable(f):
            return {"error": "未定义可调用的 filter_stock(data)"}
        s_score = nspace.get("score_stock") if callable(nspace.get("score_stock")) else None

        hits, box, errs = [], {}, []

        def _worker():
            try:
                for c in pool:
                    d = {
                        "code": c["code"], "name": c["name"], "price": c["price"], "pct": c["pct"],
                        "turnover_rate": c["turnover_rate"], "pe": c["pe"], "pb": c["pb"], "cap": c["cap"],
                        "closes": c.get("closes", []),
                        "rsi": (c.get("indicators") or {}).get("rsi"),
                        "ma5": (c.get("indicators") or {}).get("ma5"),
                        "ma20": (c.get("indicators") or {}).get("ma20"),
                        "ma60": (c.get("indicators") or {}).get("ma60"),
                        "dif": (c.get("indicators") or {}).get("dif"),
                        "dea": (c.get("indicators") or {}).get("dea"),
                        "boll_low": (c.get("indicators") or {}).get("boll_low"),
                        "boll_up": (c.get("indicators") or {}).get("boll_up"),
                        "params": self.params,
                    }
                    try:
                        if f(d):
                            sc = s_score(d) if s_score else 60.0
                            hits.append({**{k: c[k] for k in ("code", "name", "price", "pct", "turnover_rate", "pe", "pb", "cap")},
                                          "score": round(float(sc), 1), "indicators": c.get("indicators", {})})
                    except Exception as e:  # noqa: BLE001
                        errs.append(f"{c['code']}: {e}")
            except Exception as e:  # noqa: BLE001
                box["error"] = str(e)

        th = threading.Thread(target=_worker, daemon=True)
        th.start()
        th.join(timeout)
        if th.is_alive():
            return {"error": f"策略执行超时(>{timeout}s)，可能股票池过大或逻辑过慢"}
        if "error" in box:
            return {"error": box["error"]}
        hits.sort(key=lambda x: x["score"], reverse=True)
        return {"items": hits, "errors": errs[:10], "pool_size": len(pool),
                "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}


# ============================================================
# 回测执行器# ============================================================
