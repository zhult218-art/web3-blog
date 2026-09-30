"""
Web3 量化交易平台 - FastAPI 微服务
整合功能：
1. A股行情数据（多源：mootdx/腾讯/东方财富/新浪/akshare）
2. 策略回测（记录管理，支持多策略）
3. 多Agent AI投研分析（7分析师+多空辩论）
4. 持仓管理
5. 自选股与持仓状态
6. 策略管理（小市值/网格/CTA等）
7. 数据导出（Markdown/PDF/Excel）
"""

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
from fastapi import FastAPI, HTTPException, Query, Request, Body
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, StreamingResponse
from pydantic import BaseModel

# 同花顺金融数据 API（HiThink-Tech/Financial-API，fuyao 渠道）
try:
    from hithink_client import HithinkClient
except Exception:  # noqa: BLE001
    HithinkClient = None

# ============================================================
# 日志配置
# ============================================================
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(name)s: %(message)s'
)
logger = logging.getLogger("quant")

# ============================================================
# 配置
# ============================================================
def _load_dotenv(path: str = "./.env") -> None:
    """轻量 .env 加载：依次读取仓库根 ../.env 与本地 ./.env（仅填充尚未设置的环境变量）。"""
    candidates = [Path("../.env"), Path(path)]
    for p in candidates:
        try:
            if not p.exists():
                continue
            for line in p.read_text(encoding="utf-8").splitlines():
                line = line.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                k, _, v = line.partition("=")
                k, v = k.strip(), v.strip().strip('"').strip("'")
                if k and not os.environ.get(k):
                    os.environ[k] = v
        except Exception:  # noqa: BLE001
            pass

_load_dotenv()

QUANT_DATA_DIR = Path(os.environ.get("QUANT_DATA_DIR", "./data"))
QUANT_OUTPUT_DIR = Path(os.environ.get("QUANT_OUTPUT_DIR", "./output"))
QUANT_CACHE_DIR = Path(os.path.expanduser("~/.quant-platform"))
for d in [QUANT_DATA_DIR, QUANT_OUTPUT_DIR, QUANT_CACHE_DIR]:
    d.mkdir(parents=True, exist_ok=True)

# ============================================================
# 数据模型
# ============================================================
class TaskStatus(str, Enum):
    PENDING = "pending"
    RUNNING = "running"
    COMPLETED = "completed"
    FAILED = "failed"

class SignalType(str, Enum):
    BUY = "BUY"
    SELL = "SELL"
    HOLD = "HOLD"

@dataclass
class StockQuote:
    symbol: str
    name: str
    price: float
    change: float
    change_percent: float
    volume: float
    turnover: float
    high: float
    low: float
    open: float
    amplitude: float
    turnover_rate: float
    pe: Optional[float] = None
    pb: Optional[float] = None
    market_cap: Optional[float] = None
    updated_at: str = ""

@dataclass
class KLineData:
    date: str
    open: float
    close: float
    high: float
    low: float
    volume: float
    turnover: float
    change_percent: float

@dataclass
class TradeSignal:
    signal: str
    symbol: str
    name: str
    price: float
    reason: str
    confidence: float
    timestamp: str = ""
    analysts: List[str] = field(default_factory=list)

@dataclass
class BacktestResult:
    strategy_name: str
    initial_cash: float
    final_value: float
    total_return: float
    annual_return: float
    max_drawdown: float
    sharpe_ratio: float
    win_rate: float
    trade_count: int
    equity_curve: List[float] = field(default_factory=list)
    transactions: List[Dict] = field(default_factory=list)

@dataclass
class AnalysisTask:
    task_id: str
    symbol: str
    status: TaskStatus
    progress: int = 0
    current_stage: str = ""
    result: Optional[Dict] = None
    error: Optional[str] = None
    created_at: str = ""
    completed_at: Optional[str] = None

# 内存任务存储
_task_store: Dict[str, AnalysisTask] = {}
_backtest_store: Dict[str, BacktestResult] = {}
_portfolio_store: Dict[str, Dict] = {}


# ============================================================
# Supabase 行情存储（PostgREST）。环境变量：SUPABASE_URL / SUPABASE_SERVICE_KEY
# 用途：全市场快照、日K、板块曲线/涨停池·连板·资金流 历史落库。#       历史数据从库读，少量拉当天更新；不可用时降级内存缓存。# ============================================================
class SupabaseStore:
    URL = os.environ.get("SUPABASE_URL", "").rstrip("/")
    KEY = os.environ.get("SUPABASE_SERVICE_KEY", "")
    ENABLED = bool(URL and KEY)
    _HEADERS = {
        "apikey": KEY,
        "Authorization": f"Bearer {KEY}",
        "Content-Type": "application/json",
    } if ENABLED else {}
    _upsert_buf: Dict[str, list] = {}
    _buf_lock = threading.Lock()
    _log_interval = 300.0
    _last_log = 0.0
    _PAGE_SIZE = 1000
    # K线读库熔断：SSL重置/超时后短时间内跳过，避免每次请求都等满超时
    _kline_fail_until = 0.0
    _KLINE_FAIL_BACKOFF = 600.0  # 失败后退避 10 分钟

    # ---------------- 基础读写 ----------------
    @staticmethod
    def _base() -> str:
        return f"{SupabaseStore.URL}/rest/v1"

    @staticmethod
    def _table(t: str) -> str:
        return f"{SupabaseStore._base()}/{t}"

    @staticmethod
    def upsert(table: str, rows: list, on_conflict: str = "id", max_chunk: int = 400) -> bool:
        """按 on_conflict 做 upsert；空列表直接成功。失败返回 False（调用方降级）。"""
        if not SupabaseStore.ENABLED or not rows:
            return False
        ok = True
        for i in range(0, len(rows), max_chunk):
            chunk = rows[i:i + max_chunk]
            try:
                r = requests.post(
                    SupabaseStore._table(table),
                    params={"on_conflict": on_conflict},
                    headers={**SupabaseStore._HEADERS, "Prefer": "resolution=merge-duplicates,return=minimal"},
                    json=chunk, timeout=20,
                )
                r.raise_for_status()
            except Exception as e:  # noqa: BLE001
                logger.warning("supabase upsert %s (%d rows) failed: %s", table, len(chunk), e)
                ok = False
        return ok

    @staticmethod
    def _select(table: str, select: str, params: dict, timeout: int = 15):
        if not SupabaseStore.ENABLED:
            return None
        r = requests.get(SupabaseStore._table(table), headers=SupabaseStore._HEADERS,
                         params={"select": select, **params}, timeout=timeout)
        r.raise_for_status()
        return r.json()

    # ---------------- 个股基本面历史（财务/股东/两融/资金流/分红/大宗） ----------------
    # 通用 jsonb 表 quant_stock_history（kind, code, date_key, payload），
    # 建表 SQL：supabase/quant_stock_history.sql（Supabase Dashboard SQL Editor 执行一次）
    STOCK_HISTORY_TABLE = "quant_stock_history"
    _stock_hist_fail_until = 0.0  # 表缺失时的退避时间戳（避免每次请求都吃 404）

    @staticmethod
    def save_stock_history(kind: str, code: str, rows: list) -> int:
        """个股历史数据落库（jsonb 通用表，date_key 从 date/period 字段提取）。返回写入行数。

        注意：写入失败（超时等瞬时错误）不设读取退避——只有确认表缺失才影响读路径。
        """
        if not SupabaseStore.ENABLED or not rows:
            return 0
        out = []
        for r in rows:
            if not isinstance(r, dict):
                continue
            dk = ""
            for k in ("date", "period", "day", "trade_date"):
                v = r.get(k)
                if v and str(v).lower() not in ("none", "nan", "null"):
                    dk = str(v)[:10]
                    break
            if not dk:
                continue
            out.append({"kind": kind, "code": code, "date_key": dk, "payload": r})
        if not out:
            return 0
        ok = SupabaseStore.upsert(SupabaseStore.STOCK_HISTORY_TABLE, out, "kind,code,date_key")
        return len(out) if ok else 0

    @staticmethod
    def load_stock_history(kind: str, code: str, limit: int = 600) -> Optional[list]:
        """读个股历史（按 date_key 升序 payload 列表）。表缺失/未启用返回 None（调用方走实时抓取）。"""
        if not SupabaseStore.ENABLED or time.time() < SupabaseStore._stock_hist_fail_until:
            return None
        try:
            rows = SupabaseStore._select(
                SupabaseStore.STOCK_HISTORY_TABLE, "date_key,payload",
                {"kind": f"eq.{kind}", "code": f"eq.{code}",
                 "order": "date_key.desc", "limit": str(limit)})
            out = [{"date_key": r["date_key"], **(r.get("payload") or {})} for r in (rows or [])]
            out.reverse()
            return out
        except requests.HTTPError as e:
            body = str(getattr(getattr(e, "response", None), "text", "") or "")
            if "PGRST205" in body or "Could not find" in body:
                SupabaseStore._stock_hist_fail_until = time.time() + 600
                logger.warning("supabase %s 表不存在：请在 Supabase SQL Editor 执行 supabase/quant_stock_history.sql（10分钟内不再重试）",
                               SupabaseStore.STOCK_HISTORY_TABLE)
            return None
        except Exception as e:  # noqa: BLE001
            logger.warning("supabase stock history read %s/%s fail: %s", kind, code, e)
            return None

    @staticmethod
    def stock_history_ready() -> bool:
        """探测 quant_stock_history 表是否可用（供状态接口展示）。表存在返回 True。"""
        return SupabaseStore.load_stock_history("margin", "__probe__", 1) is not None

    # ---------------- 全市场快照 ----------------
    @staticmethod
    def load_snapshot(trade_date: str, limit: int = 8) -> Optional[list]:
        """读某日全市场快照（最新 limit 行）。返回 [ {code,name,price,change_percent,...} ] 或 None。"""
        try:
            rows = SupabaseStore._select(
                "quant_daily_snapshot", "*",
                {"trade_date": f"eq.{trade_date}", "order": "change_percent.desc", "limit": str(limit)})
            return rows or []
        except Exception as e:
            logger.warning("supabase snapshot read fail: %s", e)
            return None

    @staticmethod
    def save_snapshot(trade_date: str, df, src: str = "") -> bool:
        """将全市场快照 DataFrame 落库（字段映射为英文列）"""
        try:
            if df is None or len(df) == 0:
                return False

            def _g(r, *keys, default=0.0):
                for k in keys:
                    v = r.get(k)
                    if v is None or v == "":
                        continue
                    try:
                        return float(v)
                    except (TypeError, ValueError):
                        pass
                return default

            rows = []
            for _, r in df.iterrows():
                code = str(r.get("代码") or "").strip()
                if not code:
                    continue
                rows.append({
                    "trade_date": trade_date,
                    "code": code,
                    "name": str(r.get("名称") or "")[:50],
                    "price": _g(r, "最新价", "trade", "price"),
                    "change": _g(r, "涨跌额", "change"),
                    "change_percent": _g(r, "涨跌幅", "changepercent"),
                    "volume": _g(r, "成交量", "volume"),
                    "turnover": _g(r, "成交额", "amount"),
                    "high": _g(r, "最高", "high"),
                    "low": _g(r, "最低", "low"),
                    "open": _g(r, "今开", "open"),
                    "prev_close": _g(r, "昨收"),
                    "amplitude": _g(r, "振幅"),
                    "turnover_rate": _g(r, "换手率"),
                    "pe": _g(r, "市盈率", "市盈率-动态", "pe") or None,
                    "pb": _g(r, "市净率", "pb") or None,
                    "market_cap": _g(r, "总市值", "mktcap"),
                    "source": str(src)[:30],
                })
            # 分片写入避免单批次过大
            ok = SupabaseStore.upsert("quant_daily_snapshot", rows, "trade_date,code")
            if ok:
                logger.info("supabase snapshot saved: %s (%d rows)", trade_date, len(rows))
            return ok
        except Exception as e:  # noqa: BLE001
            logger.warning("save_snapshot fail: %s", e)
            return False

    # ---------------- 个股K线 ----------------
    @staticmethod
    def get_kline(code: str, period: str = "1d", days: int = 60) -> Optional[list]:
        """读个股历史K线（按 code+日期倒序取最近 days 条）。返回 [{date,...}] 或 None（库无数据/不可用）。"""
        # 熔断检查：近期失败则直接跳过，不等超时
        if not SupabaseStore.ENABLED or time.time() < SupabaseStore._kline_fail_until:
            return None
        try:
            params = {"code": f"eq.{code}", "period": f"eq.{period}",
                      "order": "trade_date.desc", "limit": str(max(days, 5))}
            rows = SupabaseStore._select("quant_daily_kline", "*", params, timeout=3)
            if not rows:
                return None
            rows.sort(key=lambda x: x.get("trade_date") or "")
            return [{
                "date": r["trade_date"], "open": float(r.get("open") or 0),
                "close": float(r.get("close") or 0), "high": float(r.get("high") or 0),
                "low": float(r.get("low") or 0), "volume": float(r.get("volume") or 0),
                "turnover": float(r.get("turnover") or 0), "amplitude": float(r.get("amplitude") or 0),
                "change_percent": float(r.get("change_percent") or 0),
            } for r in rows[-days:]]
        except Exception as e:
            # 连接类异常（SSL重置/超时/拒绝）触发熔断，10 分钟内跳过 Supabase
            if any(k in str(e).lower() for k in ("ssl", "reset", "timeout", "refused", "unreachable", "10054")):
                SupabaseStore._kline_fail_until = time.time() + SupabaseStore._KLINE_FAIL_BACKOFF
                logger.warning("supabase kline circuit OPEN (10min skip): %s", e)
            else:
                logger.warning("supabase kline read fail: %s", e)
            return None

    @staticmethod
    def save_kline(code: str, period: str, rows: list, src: str = "") -> bool:
        """个股K线落库。rows: [{date,open,close,high,low,volume,turnover,change_percent,...}]"""
        try:
            if not rows:
                return False
            to_save = []
            for r in rows:
                to_save.append({
                    "code": code,
                    "trade_date": str(r["date"])[:10],
                    "period": period,
                    "open": float(r.get("open") or 0), "close": float(r.get("close") or 0),
                    "high": float(r.get("high") or 0), "low": float(r.get("low") or 0),
                    "volume": float(r.get("volume") or 0), "turnover": float(r.get("turnover") or 0),
                    "change_percent": float(r.get("change_percent") or 0),
                    "amplitude": float(r.get("amplitude") or 0),
                    "source": src,
                })
            return SupabaseStore.upsert("quant_daily_kline", to_save, "code,trade_date,period")
        except Exception as e:  # noqa: BLE001
            logger.warning("save_kline fail: %s", e)
            return False

    # ---------------- 板块走势 ----------------
    @staticmethod
    def get_sector_daily(sector: str, days: int = 30) -> Optional[list]:
        """读某板块近 days 日收盘（按日期倒序取最近 days 条）。返回 [{date,close,pct}] 或 None。"""
        try:
            params = {"sector": f"eq.{sector}",
                      "order": "trade_date.desc", "limit": str(max(days, 5))}
            rows = SupabaseStore._select("quant_sector_daily", "*", params)
            if not rows:
                return None
            rows.sort(key=lambda x: x.get("trade_date") or "")
            return [{"date": r["trade_date"], "close": float(r.get("close") or 0),
                     "pct": float(r.get("pct") or 0)} for r in rows[-days:]]
        except Exception as e:
            logger.warning("supabase sector read fail: %s", e)
            return None

    @staticmethod
    def save_sector_daily(sector: str, rows: list, src: str = "") -> bool:
        """板块每日收盘落库。rows: [{date,close,pct}]"""
        try:
            if not rows:
                return False
            to_save = [{"sector": sector, "trade_date": r["date"][:10],
                        "close": float(r.get("close") or 0), "pct": float(r.get("pct") or 0),
                        "source": src} for r in rows]
            return SupabaseStore.upsert("quant_sector_daily", to_save, "sector,trade_date")
        except Exception as e:  # noqa: BLE001
            logger.warning("save_sector_daily fail: %s", e)
            return False

    # ---------------- 娑ㄥ仠姹?杩炴澘/璧勯噾娴?----------------
    @staticmethod
    def get_pool(kind: str, trade_date: str) -> Optional[dict]:
        """读某日某类看板快照 payload。返回 dict 或 None。"""
        try:
            rows = SupabaseStore._select("quant_daily_pool", "payload",
                                         {"kind": f"eq.{kind}", "trade_date": f"eq.{trade_date}", "limit": "1"})
            if not rows:
                return None
            return rows[0].get("payload")
        except Exception as e:
            logger.warning("supabase pool read fail: %s", e)
            return None

    @staticmethod
    def save_pool(kind: str, trade_date: str, payload: dict) -> bool:
        """看板快照落库（JSONB）。"""
        try:
            if payload is None:
                return False
            return SupabaseStore.upsert("quant_daily_pool",
                                        [{"kind": kind, "trade_date": trade_date, "payload": payload}],
                                        "kind,trade_date")
        except Exception as e:  # noqa: BLE001
            logger.warning("save_pool fail: %s", e)
            return False

    @staticmethod
    def save_reports(rows: list) -> bool:
        """券商研报元数据落库（PK report_id，upsert 幂等）。"""
        try:
            if not rows:
                return False
            to_save = []
            for r in rows:
                rid = str(r.get("report_id") or "")
                if not rid:
                    continue
                to_save.append({
                    "report_id": rid,
                    "title": str(r.get("title") or "")[:200],
                    "stock_code": str(r.get("stock_code") or "")[:10],
                    "stock_name": str(r.get("stock_name") or "")[:20],
                    "org_name": str(r.get("org_name") or "")[:50],
                    "author": str(r.get("author") or "")[:50],
                    "publish_date": str(r.get("publish_date") or "")[:10],
                    "rating": str(r.get("rating") or "")[:10],
                    "rating_change": str(r.get("rating_change") or "")[:20],
                    "target_price": r.get("target_price"),
                    "eps_y1": r.get("eps_y1"), "eps_y2": r.get("eps_y2"), "eps_y3": r.get("eps_y3"),
                    "summary": str(r.get("summary") or "")[:200],
                    "url": str(r.get("url") or "")[:200],
                })
            return SupabaseStore.upsert("quant_research_reports", to_save, "report_id") if to_save else False
        except Exception as e:  # noqa: BLE001
            logger.warning("save_reports fail: %s", e)
            return False

    @staticmethod
    def save_news(rows: list) -> bool:
        """财经新闻落库（PK news_id，upsert 幂等）。"""
        try:
            if not rows:
                return False
            to_save = []
            for r in rows:
                nid = str(r.get("id") or r.get("news_id") or "")
                if not nid:
                    continue
                to_save.append({
                    "news_id": nid,
                    "title": str(r.get("title") or "")[:200],
                    "source": str(r.get("source") or "")[:20],
                    "category": str(r.get("category") or "")[:10],
                    "sentiment": str(r.get("sentiment") or "")[:10],
                    "impact": str(r.get("impact") or "")[:10],
                    "news_time": str(r.get("timestamp") or r.get("news_time") or "")[:19],
                    "summary": str(r.get("summary") or "")[:300],
                    "url": str(r.get("url") or "")[:300],
                })
            return SupabaseStore.upsert("quant_news", to_save, "news_id") if to_save else False
        except Exception as e:  # noqa: BLE001
            logger.warning("save_news fail: %s", e)
            return False

    @staticmethod
    def save_dragon_tiger(trade_date: str, rows: list) -> bool:
        """龙虎榜落库（PK trade_date+code）。"""
        try:
            if not rows:
                return False
            to_save = []
            for r in rows:
                code = str(r.get("code") or "")
                if not code:
                    continue
                to_save.append({
                    "trade_date": str(trade_date or "")[:10],
                    "code": code,
                    "name": str(r.get("name") or "")[:20],
                    "close": r.get("close"),
                    "change_percent": r.get("change_percent"),
                    "buy_amount": r.get("buy_amount"),
                    "sell_amount": r.get("sell_amount"),
                    "net_amount": r.get("net_amount"),
                    "seats": r.get("seats"),
                    "reason": str(r.get("reason") or "")[:100],
                })
            return SupabaseStore.upsert("quant_dragon_tiger", to_save, "trade_date,code") if to_save else False
        except Exception as e:  # noqa: BLE001
            logger.warning("save_dragon_tiger fail: %s", e)
            return False

    # ---------------- 同步日志 ----------------
    @staticmethod
    def log_sync(kind: str, trade_date: str, status: str, detail: str = "") -> None:
        try:
            if not SupabaseStore.ENABLED:
                return
            now_t = time.time()
            if now_t - SupabaseStore._last_log < SupabaseStore._log_interval:
                return
            SupabaseStore._last_log = now_t
            SupabaseStore._select("quant_sync_log", "id", {"limit": "1"})  # 探测连通性
            requests.post(SupabaseStore._table("quant_sync_log"),
                          headers=SupabaseStore._HEADERS, timeout=8,
                          json=[{"kind": kind, "trade_date": trade_date,
                                 "status": status, "detail": (detail or "")[:500]}])
        except Exception as e:  # noqa: BLE001
            logger.debug("sync log fail: %s", e)

    # ---------------- 每日同步计数（调度判定用） ----------------
    @staticmethod
    def update_count(trade_date: str, table_name: str, row_count: int,
                     status: str = "ok", detail: str = "") -> None:
        """记录某日某类数据落库数量/状态（quant_daily_count），供定量调度与前台核验。"""
        try:
            if not SupabaseStore.ENABLED:
                return
            requests.post(SupabaseStore._table("quant_daily_count"),
                          params={"on_conflict": "trade_date,table_name"},
                          headers={**SupabaseStore._HEADERS,
                                   "Prefer": "resolution=merge-duplicates,return=minimal"},
                          json=[{"trade_date": trade_date, "table_name": table_name,
                                 "row_count": int(row_count), "status": status,
                                 "detail": (detail or "")[:300],
                                 "updated_at": datetime.now().strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"}],
                          timeout=8)
        except Exception as e:  # noqa: BLE001
            logger.debug("update_count fail: %s", e)

    @staticmethod
    def day_synced(trade_date: str) -> bool:
        """当日该类数据已成功落库（以库状态为准，进程重启不丢失）。"""
        try:
            if not SupabaseStore.ENABLED:
                return False
            rows = SupabaseStore._select(
                "quant_daily_count", "status,row_count",
                {"trade_date": f"eq.{trade_date}", "table_name": "eq.snapshot", "limit": "5"})
            if not rows:
                return False
            return any(str(r.get("status")) == "ok" and int(r.get("row_count") or 0) > 0 for r in rows)
        except Exception:  # noqa: BLE001
            return False

    # ---------------- 行情同步告警 ----------------
    @staticmethod
    def save_alert(trade_date: str, detail: str, level: str = "error") -> None:
        """写入/更新告警（每天每交易日仅一条 open）。"""
        try:
            if not SupabaseStore.ENABLED:
                return
            requests.post(
                SupabaseStore._table("quant_sync_alert"),
                params={"on_conflict": "trade_date"},
                headers={**SupabaseStore._HEADERS, "Prefer": "resolution=merge-duplicates,return=minimal"},
                json=[{"trade_date": trade_date, "level": level, "status": "open",
                       "detail": (detail or "")[:500],
                       "resolved_at": None}],
                timeout=8)
        except Exception as e:  # noqa: BLE001
            logger.debug("save_alert fail: %s", e)

    @staticmethod
    def resolve_alerts(trade_date: str) -> None:
        """成功落库后关闭当日所有 open 告警。"""
        try:
            if not SupabaseStore.ENABLED:
                return
            requests.patch(
                SupabaseStore._table("quant_sync_alert"),
                params={"trade_date": f"eq.{trade_date}", "status": "eq.open"},
                headers={**SupabaseStore._HEADERS, "Prefer": "return=minimal"},
                json={"status": "resolved", "resolved_at": datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ")},
                timeout=8)
        except Exception as e:  # noqa: BLE001
            logger.debug("resolve_alerts fail: %s", e)

    @staticmethod
    def get_open_alerts(limit: int = 10) -> list:
        """读取未解决的同步告警（前台告警轮询）。"""
        try:
            if not SupabaseStore.ENABLED:
                return []
            rows = SupabaseStore._select(
                "quant_sync_alert", "*",
                {"status": "eq.open", "order": "trade_date.desc", "limit": str(limit)})
            return rows or []
        except Exception as e:  # noqa: BLE001
            logger.debug("get_open_alerts fail: %s", e)
            return []


# ============================================================
# A股数据服务# ============================================================
try:
    import akshare as ak
except Exception:
    ak = None

# ---- 备用数据源（用户指定多源容灾：Baostock TCP / Ashare 单文件 / adata / Tushare / eodhd）----
try:
    import baostock as bs
except Exception:
    bs = None

# Ashare 单文件行情库（github.com/mpquant/Ashare）：新浪主+腾讯备，原生支持 1d/1w/1M
try:
    from Ashare import get_price as _ashare_get_price
except Exception:
    _ashare_get_price = None

# adata / tushare 依赖较重或需 token，采用函数内懒加载


class AStockDataService:
    """A股数据服务 - 多源聚合 (akshare: 东方财富/新浪财经/腾讯/同花顺)，Vibe/mock 降级。"""

    VIBE_BASE = os.environ.get("VIBE_BASE", "http://127.0.0.1:8900")
    STOCK_POOL = {
        "sh600519": "贵州茅台", "sz000858": "五粮液", "sh601318": "中国平安",
        "sz000001": "平安银行", "sh600036": "招商银行", "sz002594": "比亚迪",
        "sh601899": "紫金矿业", "sz000333": "美的集团", "sh600900": "长江电力",
        "sz300059": "东方财富", "sh688017": "心脉医疗", "sz300750": "宁德时代",
        "sh603259": "药明康德", "sz002475": "立讯精密", "sh601012": "隆基绿能",
    }
    _quote_cache: Dict[str, StockQuote] = {}
    _cache_ts = 0.0
    _CACHE_TTL = 30.0
    _vibe_down_since = 0.0  # Vibe 熔断时间戳（失败后 60s 内直接走腾讯/新浪）
    _all_df = None
    _all_src = ""
    _all_ts = 0.0
    _ALL_TTL = 120.0
    _snap_lock = threading.Lock()
    _batch_lock = threading.Lock()
    # ---- 缓存加固 ----
    _cache_ttl_val = 30.0          # 报价批次实际 TTL（写入时加抖动，防雪崩）
    _all_ttl_val = 120.0           # 全市场快照实际 TTL（写入时加抖动，防雪崩）
    NEG_TTL = 30.0                 # 负缓存基础 TTL（空结果/失败短缓存，防穿透）
    _neg_cache: Dict[str, tuple] = {}   # key -> (timestamp, jittered_ttl)

    @staticmethod
    def _neg_get(key: str) -> bool:
        rec = AStockDataService._neg_cache.get(key)
        return bool(rec and time.time() - rec[0] < rec[1])

    @staticmethod
    def _neg_set(key: str):
        # 写入即抖动，不同 key 错峰过期，避免负缓存集中失效
        AStockDataService._neg_cache[key] = (
            time.time(), AStockDataService.NEG_TTL * random.uniform(0.85, 1.15))

    KLINE_PERIOD_MAP = {"1m": "1", "5m": "5", "15m": "15", "30m": "30", "60m": "60"}
    DAILY_PERIOD_MAP = {"1d": "daily", "d": "daily", "day": "daily", "daily": "daily",
                        "1w": "weekly", "w": "weekly", "week": "weekly", "weekly": "weekly",
                        "1mo": "monthly", "m": "monthly", "month": "monthly", "monthly": "monthly",
                        "all": "daily"}
    FULL_HISTORY_DAYS = 8000  # 请求天数达到该值时全量从上市拉取
    # 任意窗口 K 线内存缓存(5min)：日线/周线/月线切换秒回，不必每次打上游或读 Supabase
    _recent_kline_memo: Dict[str, tuple] = {}  # (code,p)->(ts,rows)
    RECENT_KLINE_TTL = 300
    # 日历窗口系数：days 根周期 bar 需要的日历天数（月≈31、周≈8、日≈2.2 含节假日）
    _PERIOD_CAL_DAYS = {"daily": 2.5, "weekly": 9.0, "monthly": 32.0}
    _full_kline_memo: Dict[str, tuple] = {}  # (code,p)->(ts,rows) 鍏ㄩ噺K绾垮唴瀛樼紦瀛?30min)
    # ---- Baostock 兜底状态：非线程安全，所有调用经 _bs_lock 串行；失败 60s 熔断 ----
    _bs_lock = threading.Lock()
    _bs_logged = False
    _bs_fail_until = 0.0
    _BS_FREQ = {"daily": "d", "weekly": "w", "monthly": "m"}
    _ADATA_KTYPE = {"daily": 1, "weekly": 2, "monthly": 3}  # adata: 1日/2周/3月

    @staticmethod
    def _to_code(symbol: str) -> str:
        s = str(symbol or "")
        if s[:2] in ("sh", "sz", "bj"):
            s = s[2:]
        return AStockDataService._clean_code(s)

    @staticmethod
    def _clean_code(code) -> str:
        """把代码规整为 6 位数字：处理 float 形式（如 '920992.0'）、去掉 sh/sz/bj、补零。"""
        s = str(code or "").strip()
        if s[:2] in ("sh", "sz", "bj"):
            s = s[2:]
        # float 字符串会带 .0（如 920992.0），数字代码直接取整数部分
        if s.endswith(".0"):
            try:
                s = str(int(float(s)))
            except (ValueError, TypeError):
                pass
        # 保留纯数字，去掉残留非数字
        digits = "".join(ch for ch in s if ch.isdigit())
        if not digits:
            return ""
        # 若长度 >6 通常含 B 或美股后缀，截断到 6 位
        if len(digits) > 6:
            digits = digits[:6]
        return digits.zfill(6)

    @staticmethod
    def _to_symbol(code: str) -> str:
        code = AStockDataService._clean_code(code)
        if not code:
            return str(code or "")
        # 北交所 20 / 4 / 8 开头
        if code.startswith(("4", "8", "920")):
            return "bj" + code
        prefix = "sh" if code.startswith(("6", "9")) else "sz"
        return prefix + code

    @staticmethod
    def _with_retry(fn, tries=2, delay=0.8):
        last = None
        for i in range(tries):
            try:
                return fn()
            except Exception as e:
                last = e
                if i < tries - 1:
                    time.sleep(delay)
        raise last

    @staticmethod
    def _vibe_quote_codes() -> List[str]:
        return [AStockDataService._to_code(s) for s in AStockDataService.STOCK_POOL]

    @staticmethod
    def _fetch_vibe_quotes() -> Dict[str, Dict]:
        try:
            url = f"{AStockDataService.VIBE_BASE}/api/quote"
            r = requests.get(url, params={"codes": ",".join(AStockDataService._vibe_quote_codes())}, timeout=8)
            r.raise_for_status()
            return r.json().get("data") or {}
        except Exception as e:
            logger.warning("Vibe quote 源不可用: %s", e)
            return {}

    @staticmethod
    def _run_with_deadline(fn, budget: float):
        """守护线程执行 fn，超时 budget 秒即放弃（僵尸线程留在后台，不阻塞进程）。"""
        box = {}

        def runner():
            try:
                box["value"] = fn()
            except Exception as e:  # noqa: BLE001
                box["error"] = e

        th = threading.Thread(target=runner, daemon=True)
        th.start()
        th.join(budget)
        if "value" in box:
            return box["value"]
        if "error" in box:
            raise box["error"]
        raise TimeoutError(f"fetch exceeded {budget}s")

    @staticmethod
    def _em_snapshot_direct() -> pd.DataFrame:
        """东方财富 clist 直连拉取全市场快照（主域名被 WAF 拦时切换 push2delay 镜像）。"""
        url_tpl = "https://{host}/api/qt/clist/get"
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                          "(KHTML, like Gecko) Chrome/126.0 Safari/537.36",
            "Referer": "https://quote.eastmoney.com/",
        }
        fields = "f2,f3,f4,f5,f6,f7,f8,f9,f12,f14,f15,f16,f17,f18,f20,f23"
        col_map = {"f12": "代码", "f14": "名称", "f2": "最新价", "f3": "涨跌幅", "f4": "涨跌额", "f5": "成交量", "f6": "成交额", "f7": "振幅", "f8": "换手率", "f9": "市盈率", "f15": "最高", "f16": "最低", "f17": "今开", "f18": "昨收", "f20": "总市值", "f23": "市净率"}
        rows = []

        def parse_diff(diff):
            out = []
            for d in diff:
                row = {}
                for k, v in d.items():
                    col = col_map.get(k)
                    if not col:
                        continue
                    if isinstance(v, str):
                        try:
                            v = float(v) if v not in ("-", "") else 0.0
                        except ValueError:
                            pass
                    row[col] = v
                code = AStockDataService._clean_code(row.get("代码") or "")
                if code:
                    row["代码"] = code
                out.append(row)
            return out

        def fetch_page(host, pn):
            """东财 clist 单页抓取（返回原始 data 节点）。"""
            params = {"pn": pn, "pz": 100, "po": 1, "np": 1, "fltt": 2, "invt": 2,
                      "fid": "f12", "fs": "m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048",
                      "fields": fields}
            r = requests.get(url_tpl.format(host=host), params=params, headers=headers, timeout=20)
            r.raise_for_status()
            return (r.json() or {}).get("data") or {}

        def fetch_page_retry(host, pn, retries=3):
            """单页抓取，带退避重试（东财对高频请求易拒绝连接）。"""
            last = None
            for attempt in range(retries):
                try:
                    return fetch_page(host, pn)
                except Exception as e:  # noqa: BLE001
                    last = e
                    time.sleep(0.6 * (attempt + 1))
            raise last

        # 选出可用主机（push2delay 镜像优先；主域名对非浏览器 TLS 指纹拦截）
        host, first = None, None
        for h in ("push2delay.eastmoney.com", "push2.eastmoney.com"):
            try:
                first = fetch_page_retry(h, 1)
                host = h
                break
            except Exception as e:  # noqa: BLE001
                logger.warning("snapshot host %s unavailable: %s", h, e)
        if not host:
            raise RuntimeError("all eastmoney hosts unavailable")
        rows.extend(parse_diff(first.get("diff") or []))
        total = int(first.get("total") or 0)
        last_page = min((total + 99) // 100, 70) if total else 1
        if last_page > 1:
            with ThreadPoolExecutor(max_workers=4) as ex:
                futs = {ex.submit(fetch_page_retry, host, pn): pn for pn in range(2, last_page + 1)}
                page_rows = {}
                for fut in as_completed(futs):
                    pn = futs[fut]
                    try:
                        page_rows[pn] = parse_diff(fut.result().get("diff") or [])
                    except Exception as e:  # noqa: BLE001
                        logger.warning("snapshot page %s failed: %s", pn, e)
                for pn in sorted(page_rows):
                    rows.extend(page_rows[pn])
        if len(rows) < max(int(total * 0.5), 100) and total:
            raise RuntimeError(f"incomplete snapshot {len(rows)}/{total}")
        if not rows:
            raise RuntimeError("empty snapshot")
        return pd.DataFrame(rows)

    @staticmethod
    def _sina_snapshot() -> pd.DataFrame:
        """新浪财经全量 A 股快照（东财被 WAF 拦截时的备用数据源）。

        通过 vip.stock.finance.sina.com.cn 的 JSON 接口分页拉取沪深京 A 股，
        每页 100 条，返回字段与东财快照对齐。
        """
        import json, math
        headers = {"User-Agent": "Mozilla/5.0", "Referer": "https://finance.sina.com.cn"}
        base = "https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData"
        count_url = "https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeStockCount"
        rows = []
        # 获取总数
        r = requests.get(count_url, params={"node": "hs_a"}, headers=headers, timeout=8)
        total = int(r.text.strip('"') or 0)
        if not total:
            raise RuntimeError("sina count=0")
        pages = math.ceil(total / 100)
        logger.info("sina snapshot: total=%s pages=%s", total, pages)

        def fetch_page(pn):
            r = requests.get(base, params={"page": pn, "num": 100, "node": "hs_a"},
                             headers=headers, timeout=10)
            items = json.loads(r.text) if r.text else []
            out = []
            for item in items:
                code = str(item.get("code") or "")
                if not code:
                    continue
                # 转为带交易所前缀的 symbol
                prefix = "sh" if code.startswith(("50", "51", "52", "53", "60", "68", "9")) else "sz"
                if code.startswith(("8", "4", "920")):
                    prefix = "bj"
                symbol = f"{prefix}{code}"
                name = item.get("name") or code
                price = float(item.get("trade") or 0)
                prev_close = float(item.get("settlement") or 0)
                change_pct = float(item.get("changepercent") or 0)
                change = float(item.get("pricechange") or 0)
                rows.append({
                    "代码": code, "名称": name,
                    "最新价": price, "涨跌幅": change_pct, "涨跌额": change,
                    "成交量": float(item.get("volume") or 0),
                    "成交额": float(item.get("amount") or 0),
                    "振幅": float(item.get("amplitude") or 0),
                    "换手率": float(item.get("turnoverratio") or 0),
                    "市盈率": float(item.get("pe") or 0),
                    "最高": float(item.get("high") or 0),
                    "最低": float(item.get("low") or 0),
                    "今开": float(item.get("open") or 0),
                    "昨收": prev_close,
                    "总市值": float(item.get("mktcap") or 0),
                    "市净率": float(item.get("pb") or 0),
                })
            return items

        with ThreadPoolExecutor(max_workers=4) as ex:
            futs = {ex.submit(fetch_page, pn): pn for pn in range(1, pages + 1)}
            for fut in as_completed(futs):
                try:
                    fut.result()
                except Exception as e:
                    logger.warning("sina page %s failed: %s", futs[fut], e)
        logger.info("sina snapshot done: rows=%s", len(rows))
        if len(rows) < max(int(total * 0.5), 100):
            raise RuntimeError(f"sina incomplete {len(rows)}/{total}")
        return pd.DataFrame(rows)

    @staticmethod
    def _fetch_snapshot_once():
        """依次尝试：东财直连 -> 新浪财经 -> akshare 东财；任一成功即返回 (df, src)，均失败返回 (None, '')。
        注：同花顺快照不返回名称/PE/PB/总市值，仅用于单股报价与特色数据端点，不作全市场快照源。"""
        attempts = [
            (lambda: AStockDataService._em_snapshot_direct(), "东财直连", 25.0),
            (lambda: AStockDataService._sina_snapshot(), "新浪财经", 30.0),
        ]
        if ak is not None:
            attempts.append((lambda: ak.stock_zh_a_spot_em(), "东方财富(akshare)", 8.0))
        for fn, src, budget in attempts:
            t0 = time.time()
            try:
                df = AStockDataService._run_with_deadline(fn, budget)
                if df is not None and len(df):
                    logger.info("snapshot OK from %s rows=%s %.1fs", src, len(df), time.time() - t0)
                    return df, src
            except Exception as e:  # noqa: BLE001
                logger.warning("snapshot %s failed %.1fs: %s", src, time.time() - t0, e)
        return None, ""

    @staticmethod
    def _load_all_snapshot(force=False):
        # 跳过缓存直接抓取
        now = time.time()
        if not force and AStockDataService._all_df is not None \
                and now - AStockDataService._all_ts < AStockDataService._all_ttl_val:
            return AStockDataService._all_df, AStockDataService._all_src
        lock = AStockDataService._snap_lock
        if lock.acquire(blocking=False):
            try:
                # 拿到锁后二次检查（可能别的请求刚写完缓存）
                if not force and AStockDataService._all_df is not None \
                        and time.time() - AStockDataService._all_ts < AStockDataService._all_ttl_val:
                    return AStockDataService._all_df, AStockDataService._all_src
                df, src = AStockDataService._fetch_snapshot_once()
                if df is not None and len(df):
                    AStockDataService._all_df = df
                    AStockDataService._all_src = src
                    AStockDataService._all_ts = time.time()
                    # TTL 抖动防雪崩：快照错峰过期
                    AStockDataService._all_ttl_val = AStockDataService._ALL_TTL * random.uniform(0.85, 1.15)
                    # 异步把当天全市场快照落库（不阻塞请求返回）
                    threading.Thread(
                        target=lambda: SupabaseStore.save_snapshot(
                            datetime.now().strftime("%Y-%m-%d"), df, src),
                        daemon=True).start()
                    return df, src
            finally:
                lock.release()
        else:
            # 已有请求在拉取：最多等 12s 共享其结果，超时走旧缓存/失败兜底
            if lock.acquire(timeout=12):
                try:
                    return AStockDataService._all_df, AStockDataService._all_src
                finally:
                    lock.release()
        # 拉取失败但有历史快照 -> 返回过期数据兜底（总比挂死/空页面好）
        if AStockDataService._all_df is not None:
            return AStockDataService._all_df, (AStockDataService._all_src or "snapshot") + "(stale)"
        return None, ""

    @staticmethod
    def _row_num(row: Dict, *keys) -> float:
        for k in keys:
            v = row.get(k)
            if v is None:
                continue
            try:
                return float(v)
            except (TypeError, ValueError):
                pass
        return 0.0

    @staticmethod
    def _row_name(row: Dict, fallback: str) -> str:
        for k in ("名称", "name"):
            v = row.get(k)
            if v:
                return str(v)
        return fallback

    @staticmethod
    def _snapshot_quote(row: Dict, symbol: str, name: str) -> Optional[StockQuote]:
        price = AStockDataService._row_num(row, "最新价", "trade", "price")
        prev = AStockDataService._row_num(row, "昨收", "settlement")
        change_pct = AStockDataService._row_num(row, "涨跌幅", "changepercent")
        change = AStockDataService._row_num(row, "涨跌额", "change")
        if not change_pct and prev:
            change_pct = (price - prev) / prev * 100 if prev else 0
        if not change and prev:
            change = price - prev
        return StockQuote(
            symbol=symbol, name=AStockDataService._row_name(row, name),
            price=price, change=change, change_percent=change_pct,
            volume=AStockDataService._row_num(row, "成交量", "volume"),
            turnover=AStockDataService._row_num(row, "成交额", "amount"),
            high=AStockDataService._row_num(row, "最高", "high"),
            low=AStockDataService._row_num(row, "最低", "low"),
            open=AStockDataService._row_num(row, "今开", "open"),
            amplitude=AStockDataService._row_num(row, "振幅", "amplitude"),
            turnover_rate=AStockDataService._row_num(row, "换手率", "turnoverratio"),
            pe=AStockDataService._row_num(row, "市盈率", "pe") or None,
            pb=AStockDataService._row_num(row, "市净率", "pb") or None,
            market_cap=AStockDataService._row_num(row, "总市值", "mktcap"),
            updated_at=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        )

    @staticmethod
    def _use_vibe_quote(symbol: str, name: str) -> Optional[StockQuote]:
        try:
            url = f"{AStockDataService.VIBE_BASE}/api/quote"
            vcode = AStockDataService._to_code(symbol)
            r = requests.get(url, params={"codes": vcode}, timeout=8)
            r.raise_for_status()
            q = (r.json().get("data") or {}).get(vcode)
            if q:
                ts = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                return StockQuote(
                    symbol=symbol, name=q.get("name") or name,
                    price=float(q.get("price") or 0), change=float(q.get("change_amt") or 0),
                    change_percent=float(q.get("change_pct") or 0),
                    volume=float(q.get("amount_wan") or 0) * 10000,
                    turnover=float(q.get("amount_wan") or 0) * 10000,
                    high=float(q.get("high") or 0), low=float(q.get("low") or 0),
                    open=float(q.get("open") or 0),
                    amplitude=float(q.get("amplitude_pct") or 0),
                    turnover_rate=float(q.get("turnover_pct") or 0),
                    pe=q.get("pe_ttm"), pb=q.get("pb"),
                    market_cap=float(q.get("mcap_yi") or 0) * 10000,
                    updated_at=ts,
                )
        except Exception as e:
            logger.warning("Vibe single quote failed for %s: %s", symbol, e)
        return None

    @staticmethod
    def _mock_quote(symbol: str, name: str = "") -> StockQuote:
        seed = int(hashlib.md5(symbol.encode()).hexdigest()[:8], 16)
        rng = np.random.RandomState(seed)
        base_price = rng.uniform(20, 2000)
        change_pct = rng.uniform(-3, 3)
        change = base_price * change_pct / 100
        close = base_price + change
        high = max(base_price, close) + rng.uniform(0, 2)
        low = min(base_price, close) - rng.uniform(0, 2)
        volume = rng.uniform(5000, 200000)
        turnover = volume * close
        amplitude = ((high - low) / base_price) * 100 if base_price > 0 else 0
        turnover_rate = rng.uniform(0.1, 8)
        pe = rng.uniform(5, 80)
        pb = rng.uniform(0.5, 15)
        return StockQuote(
            symbol=symbol, name=name or symbol,
            price=round(close, 2), change=round(change, 2),
            change_percent=round(change_pct, 2),
            volume=round(volume, 0), turnover=round(turnover, 2),
            high=round(high, 2), low=round(low, 2),
            open=round(base_price, 2), amplitude=round(amplitude, 2),
            turnover_rate=round(turnover_rate, 2),
            pe=round(pe, 2), pb=round(pb, 2),
            market_cap=round(close * (rng.uniform(5, 500)), 2),
            updated_at=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        )

    @staticmethod
    def get_stock_list(limit: int = 0) -> List[Dict]:
        df, src = AStockDataService._load_all_snapshot()
        if df is not None and len(df):
            result = []
            for row in df.to_dict("records"):
                code = str(row.get("代码") or row.get("code") or "")
                if not code:
                    continue
                symbol = AStockDataService._to_symbol(code)
                name = AStockDataService.STOCK_POOL.get(symbol) or AStockDataService._row_name(row, code)
                q = AStockDataService._snapshot_quote(row, symbol, name)
                if q is None:
                    continue
                d = asdict(q)
                d["source"] = src
                result.append(d)
                if limit > 0 and len(result) >= limit:
                    break
            if result:
                AStockDataService.STOCK_POOL.update({d["symbol"]: d["name"] for d in result})
                return result
        pool = AStockDataService._load_quote_batch()
        result = [asdict(pool[s]) for s in AStockDataService.STOCK_POOL if s in pool]
        if result:
            return result
        # 彻底离线兜底：mock 数据保证页面有内容
        return [asdict(AStockDataService._mock_quote(s, n)) for s, n in AStockDataService.STOCK_POOL.items()]

    @staticmethod
    def _tencent_batch_quotes(symbols: List[str]) -> Dict[str, StockQuote]:
        """腾讯 qt.gtimg.cn 批量实时行情（单次请求，比较稳）。"""
        out: Dict[str, StockQuote] = {}
        if not symbols:
            return out
        try:
            url = "https://qt.gtimg.cn/q=" + ",".join(symbols)
            r = requests.get(url, timeout=6, headers={"User-Agent": "Mozilla/5.0"})
            r.encoding = "gbk"
        except Exception as e:
            logger.warning("tencent batch failed: %s", e)
            return out
        ts = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        def num(f, i):
            if i < len(f) and f[i] not in ("", "-"):
                try:
                    return float(f[i])
                except ValueError:
                    pass
            return 0.0

        for line in r.text.split(";"):
            line = line.strip()
            if "=" not in line:
                continue
            key, _, raw = line.partition("=")
            sym = key.strip().replace("v_", "")
            f = raw.strip().strip('"').split("~")
            if len(f) < 35 or not f[3]:
                continue
            price = num(f, 3)
            prev = num(f, 4)
            out[sym] = StockQuote(
                symbol=sym, name=f[1] or AStockDataService.STOCK_POOL.get(sym, sym),
                price=price,
                change=num(f, 31) or (price - prev),
                change_percent=num(f, 32) or ((price - prev) / prev * 100 if prev else 0),
                volume=num(f, 36), turnover=num(f, 37) * 10000,
                high=num(f, 33), low=num(f, 34), open=num(f, 5),
                amplitude=num(f, 43), turnover_rate=num(f, 38),
                pe=num(f, 39) or None, pb=num(f, 46) or None,
                market_cap=num(f, 45) * 1e8, updated_at=ts,
            )
        return out

    @staticmethod
    def _load_quote_batch(force=False) -> Dict[str, StockQuote]:
        now = time.time()
        if not force and now - AStockDataService._cache_ts < AStockDataService._cache_ttl_val and AStockDataService._quote_cache:
            return AStockDataService._quote_cache
        lock = AStockDataService._batch_lock
        if not lock.acquire(blocking=False):
            # 已有请求正在刷新：最多等 10s 共享结果
            if lock.acquire(timeout=10) :
                try:
                    return AStockDataService._quote_cache
                finally:
                    lock.release()
            return AStockDataService._quote_cache
        try:
            if not force and time.time() - AStockDataService._cache_ts < AStockDataService._cache_ttl_val \
                    and AStockDataService._quote_cache:
                return AStockDataService._quote_cache
            cache = AStockDataService._tencent_batch_quotes(list(AStockDataService.STOCK_POOL.keys()))
            misses = [s for s in AStockDataService.STOCK_POOL if s not in cache][:10]
            for sym in misses:
                try:
                    q = AStockDataService.get_realtime_quote(sym, AStockDataService.STOCK_POOL[sym])
                    if q:
                        cache[sym] = q
                except Exception as e:  # noqa: BLE001
                    logger.warning("single quote %s failed: %s", sym, e)
            if cache:
                AStockDataService._quote_cache = cache
                AStockDataService._cache_ts = time.time()
                # TTL 抖动：不同批次错峰过期，防止集中失效引发雪崩
                AStockDataService._cache_ttl_val = AStockDataService._CACHE_TTL * random.uniform(0.85, 1.15)
            return cache
        finally:
            lock.release()

    @staticmethod
    def get_realtime_quote(symbol: str, name: str = "") -> StockQuote:
        """实时行情: 腾讯(akshare hist_tx) -> 东财/新浪快照 -> Vibe -> mock"""
        code = AStockDataService._to_code(symbol)
        sym = AStockDataService._to_symbol(code)
        name = name or AStockDataService.STOCK_POOL.get(sym, symbol)
        if ak is not None:
            try:
                today = datetime.now().strftime("%Y%m%d")
                start = (datetime.now() - timedelta(days=10)).strftime("%Y%m%d")
                df = AStockDataService._with_retry(
                    lambda: ak.stock_zh_a_hist_tx(symbol=sym, start_date=start, end_date=today), 2, 0.8)
                if df is not None and len(df):
                    last = df.iloc[-1]
                    prev = float(last["close"]) if len(df) > 1 else float(last["open"])
                    price = float(last["close"])
                    change = price - prev
                    change_pct = change / prev * 100 if prev else 0
                    return StockQuote(
                        symbol=sym, name=name, price=price, change=change, change_percent=change_pct,
                        volume=float(last.get("volume") or 0), turnover=float(last.get("amount") or 0),
                        high=float(last["high"]), low=float(last["low"]), open=float(last["open"]),
                        amplitude=0.0, turnover_rate=0.0,
                        updated_at=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                    )
            except Exception as e:
                logger.warning("TX quote %s failed: %s", sym, e)
            df, src = AStockDataService._load_all_snapshot()
            if df is not None and len(df):
                rows = df.to_dict("records")
                for row in rows:
                    c = str(row.get("代码") or row.get("code") or "")
                    if c and AStockDataService._to_symbol(c) == sym:
                        q = AStockDataService._snapshot_quote(row, sym, name)
                        if q:
                            return q
        vibe = AStockDataService._use_vibe_quote(sym, name)
        if vibe:
            return vibe
        return AStockDataService._mock_quote(sym, name)

    @staticmethod
    def _tag(rows, source):
        if rows:
            rows[0]["source"] = source
        return rows

    @staticmethod
    def _resample_daily(rows: List[Dict], mode: str) -> List[Dict]:
        """按重采样为周K/月K。"""
        if not rows or mode not in ("weekly", "monthly"):
            return list(rows or [])
        rule = "ME" if pd.__version__ >= "2.2" else "M"
        if mode == "weekly":
            rule = "W-FRI"
        df = pd.DataFrame(rows)
        df["date"] = pd.to_datetime(df["date"])
        df = df.set_index("date").sort_index()
        if df.empty:
            return []
        agg = df.resample(rule).agg({
            "open": "first", "high": "max", "low": "min",
            "close": "last", "volume": "sum", "turnover": "sum",
        }).dropna(subset=["open"])
        out = []
        for dt, r in agg.iterrows():
            out.append({
                "date": dt.strftime("%Y-%m-%d"), "open": round(float(r["open"]), 2),
                "close": round(float(r["close"]), 2), "high": round(float(r["high"]), 2),
                "low": round(float(r["low"]), 2), "volume": float(r["volume"] or 0),
                "turnover": float(r["turnover"] or 0), "amplitude": 0.0, "change_percent": 0.0,
            })
        return out

    @staticmethod
    def _tx_dt_str(v) -> str:
        """腾讯/新浪时间串规整为 YYYY-MM-DD HH:MM；纯日期则返回 YYYY-MM-DD。"""
        s = str(v or "").strip()
        if "-" in s:
            return s[:16]
        s = "".join(ch for ch in s if ch.isdigit())
        if len(s) == 8:
            return f"{s[:4]}-{s[4:6]}-{s[6:8]}"
        if len(s) >= 12:
            return f"{s[:4]}-{s[4:6]}-{s[6:8]} {s[8:10]}:{s[10:12]}"
        return s

    @staticmethod
    def _tx_minute_intraday(symbol: str) -> List[Dict]:
        """腾讯当日分时: 每分 价格/均价/成交量/成交额，均价由累计额/累计量算出。"""
        try:
            url = "https://web.ifzq.gtimg.cn/appstock/app/minute/query"
            r = requests.get(url, params={"code": symbol},
                             headers={"User-Agent": "Mozilla/5.0"}, timeout=10)
            r.raise_for_status()
            node = r.json().get("data", {}).get(symbol, {}).get("data", {}) or {}
            lines = node.get("data") or []
            day = AStockDataService._tx_dt_str(node.get("date") or datetime.now().strftime("%Y-%m-%d"))
            if len(day) > 10:
                day = day[:10]
            out = []
            prev_vol, prev_amt = 0.0, 0.0
            has_cum = False
            for line in lines:
                parts = [x for x in re.split(r"[\s\t,]+", str(line)) if x]
                if len(parts) < 4:
                    continue
                t = parts[0]
                try:
                    price = float(parts[1])
                    vol_cum = float(parts[2])  # 累计成交量
                    amt_cum = float(parts[3])  # 累计成交额
                except (TypeError, ValueError):
                    continue
                if not has_cum:
                    has_cum = True
                    minute_vol = vol_cum
                    minute_amt = amt_cum
                else:
                    minute_vol = vol_cum - prev_vol if vol_cum >= prev_vol else vol_cum
                    minute_amt = amt_cum - prev_amt if amt_cum >= prev_amt else amt_cum
                prev_vol, prev_amt = vol_cum, amt_cum
                if minute_vol < 0:
                    minute_vol = 0.0
                if minute_amt < 0:
                    minute_amt = 0.0
                avg = (amt_cum / (vol_cum * 100)) if vol_cum else price
                hm = f"{t[:2]}:{t[2:4]}" if len(t) == 4 and t.isdigit() else str(t)
                out.append({"date": day, "datetime": f"{day} {hm}", "open": price,
                            "high": price, "low": price, "close": price, "avg": round(avg, 2),
                            "volume": round(minute_vol * 100, 0), "turnover": round(minute_amt, 2),
                            "amplitude": 0.0, "change_percent": 0.0})
            return out
        except Exception as e:
            logger.warning("TX intraday %s failed: %s", symbol, e)
            return []

    @staticmethod
    def _sina_minute_batch(symbol: str, bars: int = 1100) -> List[Dict]:
        """新浪 getKLineData 拉取1分钟K: datalen 最多1100条(约5天数据)。"""
        try:
            url = "https://quotes.sina.cn/cn/api/jsonp_v2.php/var%20_=/CN_MarketDataService.getKLineData"
            r = requests.get(url, params={"symbol": symbol, "scale": 1, "ma": "no", "datalen": int(bars)},
                             headers={"User-Agent": "Mozilla/5.0", "Referer": "https://finance.sina.com.cn"},
                             timeout=12)
            r.raise_for_status()
            txt = r.text
            start, end = txt.find("["), txt.rfind("]")
            if start < 0 or end <= start:
                return []
            data = json.loads(txt[start:end + 1])
            out = []
            for row in data:
                day = str(row.get("day") or "")[:19]
                if not day:
                    continue
                out.append({
                    "date": day[:10], "datetime": day,
                    "open": float(row.get("open") or 0), "close": float(row.get("close") or 0),
                    "high": float(row.get("high") or 0), "low": float(row.get("low") or 0),
                    "volume": float(row.get("volume") or 0),
                    "turnover": float(row.get("amount") or 0),
                    "avg": 0.0, "amplitude": 0.0, "change_percent": 0.0,
                })
            return out
        except Exception as e:
            logger.warning("SINA batch minute %s failed: %s", symbol, e)
            return []

    @staticmethod
    def _tx_mkline(symbol: str, m: str = "1", bars: int = 1200) -> List[Dict]:
        """腾讯分钟K: m in 1/5/15/30/60；m1 最多320条，m5 以上较少。"""
        try:
            count = max(120, min(int(bars), 3000))
            url = "https://ifzq.gtimg.cn/appstock/app/kline/mkline"
            r = requests.get(url, params={"param": f"{symbol},m{m},,{count}"},
                             headers={"User-Agent": "Mozilla/5.0"}, timeout=12)
            r.raise_for_status()
            node = r.json().get("data", {}).get(symbol, {})
            arr = node.get(f"m{m}") or node.get(str(m)) or []
            out = []
            for f in arr:
                if not f or len(f) < 5:
                    continue
                try:
                    price = float(f[1])
                except (TypeError, ValueError):
                    continue
                ts = AStockDataService._tx_dt_str(f[0])
                day = ts[:10]
                volume = 0.0
                for vi in (5, 6, 7):
                    try:
                        volume = float(f[vi])
                        if volume:
                            break
                    except (TypeError, ValueError, IndexError):
                        continue
                out.append({"date": day, "datetime": ts, "open": price,
                            "close": float(f[2]), "high": float(f[3]), "low": float(f[4]),
                            "avg": 0.0, "volume": round(volume * 100, 0), "turnover": 0.0,
                            "amplitude": 0.0, "change_percent": 0.0})
            return out
        except Exception as e:
            logger.warning("TX mkline %s m%s failed: %s", symbol, m, e)
            return []

    @staticmethod
    def _fetch_minute(sym: str, p: str, days: int) -> tuple:
        """分钟K: 新浪优先，腾讯 mK 兜底。返回 (rows, source)。"""
        if ak is not None:
            try:
                df = AStockDataService._with_retry(
                    lambda: ak.stock_zh_a_minute(symbol=sym, period=AStockDataService.KLINE_PERIOD_MAP[p]), 2, 0.8)
                if df is not None and len(df):
                    tail = df.tail(min(len(df), max(days * 4, 240)))
                    rows = []
                    for _, r in tail.iterrows():
                        rows.append({
                            "date": str(r["day"])[:19], "open": float(r["open"]), "close": float(r["close"]),
                            "high": float(r["high"]), "low": float(r["low"]),
                            "volume": float(r.get("volume") or 0), "turnover": float(r.get("amount") or 0),
                            "amplitude": 0.0, "change_percent": 0.0,
                        })
                    if rows:
                        return rows, "新浪财经"
            except Exception as e:
                logger.warning("SINA minute %s %s failed: %s", sym, p, e)
        m = AStockDataService.KLINE_PERIOD_MAP[p]
        if p == "1m":
            rows = AStockDataService._tx_mkline(sym, "1", bars=max(240, days * 480))
            source = "腾讯分时"
        else:
            rows = AStockDataService._tx_mkline(sym, m, bars=max(160, days * 96))
            source = f"腾讯{m}分"
        return rows, source

    @staticmethod
    def _mock_minute(sym: str, p: str, days: int) -> List[Dict]:
        """分钟K兜底（极端异常时）。"""
        seed = int(hashlib.md5(f"min_{sym}_{p}".encode()).hexdigest()[:8], 16)
        rng = np.random.RandomState(seed)
        per_day = {"1m": 240, "5m": 48, "15m": 16, "30m": 8, "60m": 4}
        step = per_day.get(p, 60)
        n = min(max(days * step, step), 1200)
        base = rng.uniform(10, 500)
        now = datetime.now()
        out = []
        days_back = 0
        while len(out) < n and days_back < days + 21:
            d = now - timedelta(days=days_back)
            days_back += 1
            if d.weekday() >= 5:
                continue
            t = datetime(d.year, d.month, d.day, 9, 30)
            end = datetime(d.year, d.month, d.day, 15, 0)
            while t < end and len(out) < n:
                if (t.hour, t.minute) >= (11, 31) and (t.hour, t.minute) <= (12, 59):
                    t = t.replace(hour=13, minute=0)
                change = rng.uniform(-0.5, 0.5)
                close = base + change
                out.append({
                    "date": d.strftime("%Y-%m-%d"), "datetime": t.strftime("%Y-%m-%d %H:%M"),
                    "open": round(base, 2), "high": round(max(base, close) + rng.uniform(0, 0.3), 2),
                    "low": round(min(base, close) - rng.uniform(0, 0.3), 2), "close": round(close, 2),
                    "avg": round(close, 2), "volume": round(rng.uniform(500, 8000), 0),
                    "turnover": 0.0, "amplitude": 0.0, "change_percent": 0.0,
                })
                base = close
                t += timedelta(minutes=step)
        return out

    # ---------------- 多源兜底（Baostock TCP / Ashare / adata / Tushare） ----------------
    @staticmethod
    def _bs_code(symbol: str) -> str:
        """sh600519 -> sh.600519（baostock 代码格式）"""
        s = AStockDataService._to_symbol(symbol)
        return f"{s[:2]}.{s[2:]}" if len(s) > 2 else s

    @staticmethod
    def _bs_login():
        """Baostock 懒登录（必须已持有 _bs_lock）。"""
        if not AStockDataService._bs_logged:
            lg = bs.login()
            if lg is not None and getattr(lg, "error_code", "1") == "0":
                AStockDataService._bs_logged = True
            else:
                raise RuntimeError(f"baostock login fail: {getattr(lg, 'error_msg', '')}")

    @staticmethod
    def _baostock_kline(code: str, ak_period: str, start_date: str, end_date: str) -> List[Dict]:
        """Baostock 历史 K 线（自有 TCP 协议免 HTTP 风控；日/周/月+前复权，可拉 1990 至今全历史）。
        返回统一 rows；不可用时抛异常。volume 单位换算为手（baostock 为股）。"""
        if bs is None:
            raise RuntimeError("baostock not installed")
        if time.time() < AStockDataService._bs_fail_until:
            raise RuntimeError("baostock circuit open (recent failure)")

        def _query():
            AStockDataService._bs_login()
            rs = bs.query_history_k_data_plus(
                AStockDataService._bs_code(code),
                "date,open,high,low,close,volume,amount,pctChg",
                start_date=start_date, end_date=end_date,
                frequency=AStockDataService._BS_FREQ.get(ak_period, "d"),
                adjustflag="2")  # 2=前复权
            if getattr(rs, "error_code", "1") != "0":
                raise RuntimeError(f"baostock query fail: {getattr(rs, 'error_msg', '')}")
            return rs

        with AStockDataService._bs_lock:
            try:
                rs = _query()
            except Exception:
                # 会话失效：重置登录态后重试一次
                AStockDataService._bs_logged = False
                rs = _query()
            rows = []
            while rs.next():
                d = rs.get_row_data()
                try:
                    o = float(d[1])
                except (TypeError, ValueError):
                    continue  # 停牌等空行跳过
                rows.append({
                    "date": d[0][:10], "open": o, "close": float(d[4]),
                    "high": float(d[2]), "low": float(d[3]),
                    "volume": round(float(d[5] or 0) / 100, 0),  # 股 -> 手，与东财口径一致
                    "turnover": float(d[6] or 0), "amplitude": 0.0,
                    "change_percent": float(d[7] or 0),
                })
            return rows

    @staticmethod
    def _ashare_kline(sym: str, ak_period: str, count: int) -> List[Dict]:
        """Ashare 单文件源（新浪主/腾讯备独立实现，原生 1d/1w/1M；单次上限约 800 根，不适合全量历史）。"""
        if _ashare_get_price is None:
            raise RuntimeError("Ashare unavailable")
        freq = {"daily": "1d", "weekly": "1w", "monthly": "1M"}.get(ak_period)
        if not freq:
            raise RuntimeError(f"ashare unsupported period: {ak_period}")
        df = _ashare_get_price(sym, count=max(int(count), 10), frequency=freq)
        if df is None or not len(df):
            return []
        rows = []
        for idx, r in df.iterrows():
            rows.append({
                "date": str(idx)[:10], "open": float(r["open"]), "close": float(r["close"]),
                "high": float(r["high"]), "low": float(r["low"]),
                "volume": float(r.get("volume") or 0), "turnover": 0.0,
                "amplitude": 0.0, "change_percent": 0.0,
            })
        return rows

    @staticmethod
    def _adata_kline(code: str, ak_period: str, start_date: str, end_date: str) -> List[Dict]:
        """adata 兜底（东财族源，风控解除后可用；懒加载避免 py_mini_racer 拖累启动）。"""
        try:
            import adata
        except Exception:
            raise RuntimeError("adata not installed")
        df = adata.stock.market.get_market(
            stock_code=code, start_date=start_date, end_date=end_date,
            k_type=AStockDataService._ADATA_KTYPE.get(ak_period, 1), adjust_type=1)
        if df is None or not len(df):
            return []
        cols = list(df.columns)

        def _s(r, *names):
            for c in names:
                if c in cols and r[c] not in (None, ""):
                    return str(r[c])
            return ""

        def _f(r, *names):
            for c in names:
                if c in cols:
                    try:
                        v = r[c]
                        if v not in (None, "") and str(v).lower() != "nan":
                            return float(v)
                    except (TypeError, ValueError):
                        pass
            return 0.0

        return [{
            "date": _s(r, "trade_date", "date")[:10],
            "open": _f(r, "open"), "close": _f(r, "close"),
            "high": _f(r, "high"), "low": _f(r, "low"),
            "volume": _f(r, "volume"), "turnover": _f(r, "amount", "turnover"),
            "amplitude": _f(r, "amplitude"), "change_percent": _f(r, "change_percent", "pct_change"),
        } for _, r in df.iterrows()]

    @staticmethod
    def _tushare_kline(code: str, ak_period: str, start_date: str, end_date: str) -> List[Dict]:
        """Tushare 门控兜底：仅在设置 TUSHARE_TOKEN 环境变量后启用（免费额度 200 次/日）。"""
        token = os.environ.get("TUSHARE_TOKEN", "").strip()
        if not token:
            raise RuntimeError("TUSHARE_TOKEN not set")
        import tushare as ts
        sym = AStockDataService._to_symbol(code)
        suffix = "SH" if sym[:2] == "sh" else ("BJ" if sym[:2] == "bj" else "SZ")
        ts.set_token(token)
        daily = ts.pro_api().daily(ts_code=f"{sym[2:]}.{suffix}",
                                   start_date=start_date.replace("-", ""),
                                   end_date=end_date.replace("-", ""))
        if daily is None or not len(daily):
            return []
        daily = daily.sort_values("trade_date")  # tushare 倒序返回
        rows = [{
            "date": str(r["trade_date"])[:10], "open": float(r["open"]), "close": float(r["close"]),
            "high": float(r["high"]), "low": float(r["low"]),
            "volume": float(r.get("vol") or 0),  # 手
            "turnover": float(r.get("amount") or 0) * 1000,  # 千元 -> 元
            "amplitude": float(r.get("amp") or 0), "change_percent": float(r.get("pct_chg") or 0),
        } for _, r in daily.iterrows()]
        return rows if ak_period == "daily" else AStockDataService._resample_daily(rows, ak_period)

    @staticmethod
    def get_kline(symbol: str, period: str = "1d", days: int = 60,
                  start_date: str = "", end_date: str = "") -> List[Dict]:
        """K绾? 鍒嗘椂(1m/5m/15m/30m/60m) / 鏃ュ懆鏈?/ 鍏ㄩ噺鍘嗗彶"""

#        优先从 Supabase 历史库（分钟实时走接口，日线增量补库）；
#        intraday 返回当日分时线，5d 返回近5日分钟K；        start_date/end_date 则按窗口分页（前台渐进加载历史）。            
        sym = AStockDataService._to_symbol(symbol)
        code = AStockDataService._to_code(sym)
        p = (period or "1d").lower()
        full_req = days >= AStockDataService.FULL_HISTORY_DAYS or p == "all"
        w_start = start_date or ""
        w_end = end_date or datetime.now().strftime("%Y-%m-%d")

        def _window_out(result: list):
            """start_date 分页窗口：按日期过滤；否则全量(full_req)/最近(days)。"""
            if w_start:
                return [r for r in result if (r.get("date") or "")[:10] >= w_start and (r.get("date") or "")[:10] <= w_end]
            return result if full_req else result[-days:]

        # 腾讯当日分时 / 新浪1分钟兜底（实时接口，不落库）
        if p == "intraday":
            rows = AStockDataService._tx_minute_intraday(sym)
            source = "腾讯分时"
            if len(rows) < 2:
                rows = AStockDataService._sina_minute_batch(sym, 1100)
                source = "新浪分时"
            if len(rows) < 2:
                rows = AStockDataService._tx_mkline(sym, "1", 1250)
                source = "腾讯分钟"
            if len(rows) < 2:
                # 全部真实源失败才用 mock，并打兜底标签（端点会据此返回空）
                rows = AStockDataService._mock_minute(sym, "intraday", 1)
                source = "分时兜底"
            return AStockDataService._tag(rows, source)

        # ---- 分时/分钟K：新浪/腾讯 或 mock（绝不落入日线源）----
        if p in AStockDataService.KLINE_PERIOD_MAP:
            rows, source = AStockDataService._fetch_minute(sym, p, days)
            if rows:
                return AStockDataService._tag(rows, source)
            logger.warning("minute %s %s all sources failed, use mock", sym, p)
            return AStockDataService._tag(AStockDataService._mock_minute(sym, p, days), "mock")

        # ---- 鏃?鍛?鏈圞 ----
        if p not in AStockDataService.DAILY_PERIOD_MAP:
            return []

        ak_period = AStockDataService.DAILY_PERIOD_MAP[p]
        cal_factor = AStockDataService._PERIOD_CAL_DAYS.get(ak_period, 2.5)

        def _memo_hit():
            """任意窗口内存缓存命中（TTL 5min）：切周期秒回。"""
            memo = AStockDataService._recent_kline_memo.get((code, p))
            if memo and time.time() - memo[0] < AStockDataService.RECENT_KLINE_TTL:
                rows = memo[1]
                # 窗口覆盖校验：缓存最早日期需覆盖本次请求窗口（防止小窗口缓存被当大窗口用）
                if rows and len(rows) >= days:
                    return rows
                earliest = (rows[0].get("date") or "")[:10] if rows else ""
                need_start = (datetime.now() - timedelta(days=days * cal_factor)).strftime("%Y-%m-%d")
                if rows and earliest and earliest <= need_start:
                    return rows
            return None

        hit_rows = _memo_hit()
        if hit_rows:
            return AStockDataService._tag(_window_out(list(hit_rows)), "cache")

        # 读库优先；全量请求走单例内存缓存，避免每次全量重抓
        try:
            if full_req:
                memo = AStockDataService._full_kline_memo.get((code, p))
                if memo and time.time() - memo[0] < 1800:
                    return AStockDataService._tag(_window_out(memo[1]), "supabase")
            else:
                cached = SupabaseStore.get_kline(code, p, days)
                # 覆盖校验：库内最早日期需覆盖请求窗口（旧数据只有近几个月的月K时视为缺失）
                if cached:
                    earliest = (cached[0].get("date") or "")[:10]
                    need_start = (datetime.now() - timedelta(days=days * cal_factor)).strftime("%Y-%m-%d")
                    if earliest and earliest <= need_start:
                        AStockDataService._recent_kline_memo[(code, p)] = (time.time(), list(cached))
                        return AStockDataService._tag(_window_out(cached), "supabase")
        except Exception as e:
            logger.warning("supabase cache read %s %s failed: %s", code, p, e)

        now = datetime.now()
        if full_req:
            em_start, sina_start, tx_start, fq_beg = "19900101", "1990-01-01", "19900101", "1990-01-01"
        else:
            span = max(int(days * cal_factor) + 60, 150)
            em_start = (now - timedelta(days=span)).strftime("%Y%m%d")
            sina_start = (now - timedelta(days=span)).strftime("%Y-%m-%d")
            tx_start = (now - timedelta(days=span)).strftime("%Y%m%d")
            fq_beg = (now - timedelta(days=span)).strftime("%Y-%m-%d")
        # start_date 覆盖窗口起点；end_date 覆盖终点
        if w_start:
            em_start = w_start.replace("-", "")
            sina_start = w_start
            tx_start = w_start.replace("-", "")
            fq_beg = w_start
        today = w_end.replace("-", "")

        def _remember(rows: list):
            if not rows:
                return
            # 任意窗口都进 5min 内存缓存（切周期秒回）；全量另进 30min 全量缓存
            AStockDataService._recent_kline_memo[(code, p)] = (time.time(), list(rows))
            if full_req:
                AStockDataService._full_kline_memo[(code, p)] = (time.time(), rows)

        # 1) 东财原生日/周/月（日线重试3次；周/月降为2次快败，失败立即走日K重采样）
        if ak is not None:
            try:
                em_tries, em_delay = (3, 1.2) if ak_period == "daily" else (2, 0.8)
                df = AStockDataService._with_retry(
                    lambda: ak.stock_zh_a_hist(symbol=code, period=ak_period, start_date=em_start,
                                               end_date=today, adjust="qfq"), em_tries, em_delay)
                if df is not None and len(df):
                    source = "东方财富"
                    result = []
                    for _, r in df.iterrows():
                        result.append({
                            "date": str(r["日期"])[:10], "open": float(r["开盘"]), "close": float(r["收盘"]),
                            "high": float(r["最高"]), "low": float(r["最低"]),
                            "volume": float(r.get("成交量") or 0), "turnover": float(r.get("成交额") or 0),
                            "amplitude": float(r.get("振幅") or 0),
                            "change_percent": float(r.get("涨跌幅") or 0),
                        })
                    if result:
                        # 全量异步落库（按 upsert，历史缺口补全）
                        threading.Thread(
                            target=lambda: SupabaseStore.save_kline(code, p, result, source),
                            daemon=True).start()
                    _remember(result)
                    return AStockDataService._tag(_window_out(result), source)
            except Exception as e:
                logger.warning("EM hist %s %s failed: %s", sym, ak_period, e)
        # 1.5) Ashare 兜底（新浪/腾讯独立实现，原生周/月；单次上限不足，全量历史请求跳过）
        if not full_req:
            try:
                if w_start:
                    span_days = max((datetime.now() - datetime.strptime(w_start, "%Y-%m-%d")).days, 30)
                else:
                    span_days = int(days * cal_factor) + 40
                result = AStockDataService._ashare_kline(sym, ak_period, int(span_days / cal_factor) + 10)
                if result:
                    threading.Thread(
                        target=lambda: SupabaseStore.save_kline(code, p, result, "Ashare"),
                        daemon=True).start()
                    _remember(result)
                    return AStockDataService._tag(_window_out(result), "Ashare")
            except Exception as e:
                logger.warning("Ashare kline %s %s failed: %s", sym, ak_period, e)
        # 1.6) Baostock 兜底（自有 TCP 免 HTTP 风控，日/周/月+前复权，支持全历史）
        try:
            bs_start = w_start or ("1990-01-01" if full_req else
                                   (now - timedelta(days=int(days * cal_factor) + 60)).strftime("%Y-%m-%d"))
            result = AStockDataService._baostock_kline(code, ak_period, bs_start, w_end)
            if result:
                threading.Thread(
                    target=lambda: SupabaseStore.save_kline(code, p, result, "Baostock"),
                    daemon=True).start()
                _remember(result)
                return AStockDataService._tag(_window_out(result), "Baostock")
        except Exception as e:
            AStockDataService._bs_fail_until = time.time() + 60  # 失败短熔断，避免每次请求都撞登录超时
            logger.warning("Baostock kline %s %s failed: %s", sym, ak_period, e)
        # 1.7) adata 兜底（东财族源，风控解除后可用）
        try:
            ad_start = w_start or ("1990-01-01" if full_req else
                                   (now - timedelta(days=int(days * cal_factor) + 60)).strftime("%Y-%m-%d"))
            result = AStockDataService._adata_kline(code, ak_period, ad_start, w_end)
            if result:
                threading.Thread(
                    target=lambda: SupabaseStore.save_kline(code, p, result, "AData"),
                    daemon=True).start()
                _remember(result)
                return AStockDataService._tag(_window_out(result), "AData")
        except Exception as e:
            logger.warning("AData kline %s failed: %s", sym, e)
        # 2) 新浪日线（周/月重采样，日线直出）
        if ak is not None:
            try:
                today_s = now.strftime("%Y-%m-%d")
                df = AStockDataService._with_retry(
                    lambda: ak.stock_zh_a_daily(symbol=sym, start_date=sina_start, end_date=today_s, adjust="qfq"), 2, 0.8)
                if df is not None and len(df):
                    daily_rows = []
                    for _, r in df.iterrows():
                        daily_rows.append({
                            "date": str(r["date"])[:10], "open": float(r["open"]), "close": float(r["close"]),
                            "high": float(r["high"]), "low": float(r["low"]),
                            "volume": float(r.get("volume") or 0), "turnover": float(r.get("amount") or 0),
                            "amplitude": 0.0, "change_percent": 0.0,
                        })
                    period_rows = AStockDataService._resample_daily(daily_rows, ak_period)
                    if period_rows:
                        threading.Thread(
                            target=lambda: SupabaseStore.save_kline(code, p, period_rows, "新浪财经"),
                            daemon=True).start()
                    _remember(period_rows)
                    return AStockDataService._tag(period_rows if full_req else period_rows[-days:], "新浪财经")
            except Exception as e:
                logger.warning("SINA daily %s failed: %s", sym, e)
        # 3) 腾讯日线（周/月重采样）
        if ak is not None:
            try:
                df = AStockDataService._with_retry(
                    lambda: ak.stock_zh_a_hist_tx(symbol=sym, start_date=tx_start, end_date=today), 2, 0.8)
                if df is not None and len(df):
                    daily_rows = []
                    for _, r in df.iterrows():
                        daily_rows.append({
                            "date": str(r["date"])[:10], "open": float(r["open"]), "close": float(r["close"]),
                            "high": float(r["high"]), "low": float(r["low"]),
                            "volume": float(r.get("volume") or 0), "turnover": float(r.get("amount") or 0),
                            "amplitude": 0.0, "change_percent": 0.0,
                        })
                    period_rows = daily_rows if ak_period == "daily" else AStockDataService._resample_daily(daily_rows, ak_period)
                    if period_rows:
                        threading.Thread(
                            target=lambda: SupabaseStore.save_kline(code, p, period_rows, "腾讯"),
                            daemon=True).start()
                    _remember(period_rows)
                    return AStockDataService._tag(period_rows if full_req else period_rows[-days:], "腾讯")
            except Exception as e:
                logger.warning("TX hist %s failed: %s", sym, e)

        # 4) Vibe 拉取（周/月重采样，熔断失败后 60s 内跳过直达腾讯）
        if time.time() - AStockDataService._vibe_down_since > 60:
            try:
                vcode = AStockDataService._to_code(sym)
                url = f"{AStockDataService.VIBE_BASE}/api/kline"
                r = requests.get(url, params={"code": vcode, "category": 4, "offset": max(days, 1)}, timeout=6)
                r.raise_for_status()
                rows = r.json().get("data") or []
                if rows:
                    source = "Vibe"
                    result = []
                    for row in rows:
                        result.append({
                            "date": str(row.get("date"))[:10],
                            "open": float(row.get("open") or 0), "close": float(row.get("close") or 0),
                            "high": float(row.get("high") or 0), "low": float(row.get("low") or 0),
                            "volume": float(row.get("volume") or 0),
                            "turnover": float(row.get("turnover") or 0) if row.get("turnover") else 0,
                            "amplitude": 0.0, "change_percent": 0.0,
                        })
                    period_rows = result if ak_period == "daily" else AStockDataService._resample_daily(result, ak_period)
                    if period_rows:
                        threading.Thread(
                            target=lambda: SupabaseStore.save_kline(code, p, period_rows, source),
                            daemon=True).start()
                    _remember(period_rows)
                    return AStockDataService._tag(period_rows if full_req else period_rows[-days:], source)
                AStockDataService._vibe_down_since = 0.0
            except Exception as e:
                logger.warning("Vibe kline failed for %s: %s", symbol, e)
                AStockDataService._vibe_down_since = time.time()

        # 5) 腾讯 fqkline（直连 HTTP，不依赖 akshare，可拉多年日线）
        try:
            tx_sym = sym if sym[:2] in ("sh", "sz", "bj") else AStockDataService._to_symbol(code)
            end_d = now.strftime("%Y-%m-%d")
            url = "https://ifzq.gtimg.cn/appstock/app/fqkline/get"
            r = requests.get(url, params={"param": f"{tx_sym},day,{fq_beg},{end_d},{min(days, 700)},qfq"},
                             headers={"User-Agent": "Mozilla/5.0"}, timeout=12)
            r.raise_for_status()
            dd = r.json().get("data", {}).get(tx_sym, {})
            arr = dd.get("qfqday") or dd.get("day") or []
            if arr:
                daily_rows = []
                for f in arr:
                    daily_rows.append({
                        "date": str(f[0])[:10], "open": float(f[1] or 0), "close": float(f[2] or 0),
                        "high": float(f[3] or 0), "low": float(f[4] or 0),
                        "volume": float(f[5] or 0), "turnover": 0.0,
                        "amplitude": 0.0, "change_percent": 0.0,
                    })
                period_rows = daily_rows if ak_period == "daily" else AStockDataService._resample_daily(daily_rows, ak_period)
                if period_rows:
                    threading.Thread(
                        target=lambda: SupabaseStore.save_kline(code, p, period_rows, "腾讯"),
                        daemon=True).start()
                    _remember(period_rows)
                    return AStockDataService._tag(period_rows if full_req else period_rows[-days:], "腾讯")
        except Exception as e:
            logger.warning("TX fqkline %s failed: %s", sym, e)

        # 5.5) Tushare 门控兜底（仅当设置 TUSHARE_TOKEN 环境变量后启用）
        try:
            ts_start = w_start or (now - timedelta(days=int(days * cal_factor) + 60)).strftime("%Y-%m-%d")
            result = AStockDataService._tushare_kline(code, ak_period, ts_start, w_end)
            if result:
                threading.Thread(
                    target=lambda: SupabaseStore.save_kline(code, p, result, "Tushare"),
                    daemon=True).start()
                _remember(result)
                return AStockDataService._tag(_window_out(result), "Tushare")
        except Exception as e:
            logger.warning("Tushare kline %s failed: %s", sym, e)

        # 6) mock 兜底（仅极端异常；超长请求截断到 1500 根）
        seed = int(hashlib.md5(f"d_{sym}_{p}".encode()).hexdigest()[:8], 16)
        rng = np.random.RandomState(seed)
        base_price = rng.uniform(10, 500)
        result = []
        n = min(max(days, 30), 1500)
        for i in range(n):
            date = (now - timedelta(days=n - i)).strftime("%Y-%m-%d")
            change_pct = rng.uniform(-3, 3)
            change = base_price * change_pct / 100
            close = base_price + change
            high = max(base_price, close) + rng.uniform(0, base_price * 0.02)
            low = min(base_price, close) - rng.uniform(0, base_price * 0.02)
            volume = rng.uniform(5000, 200000)
            turnover = volume * close
            result.append({
                "date": date, "open": round(base_price, 2),
                "close": round(close, 2), "high": round(high, 2),
                "low": round(low, 2), "volume": round(volume, 0),
                "turnover": round(turnover, 2), "amplitude": 0.0, "change_percent": round(change_pct, 2),
            })
            base_price = close
        return AStockDataService._tag(result, "mock")

    @staticmethod
    def compute_indicators(kline: List[Dict]) -> Dict:
        """基于真实K线计算技术指标: MA5/10/20/60, RSI14, MACD(12,26,9)"""
        closes = [float(k["close"]) for k in kline]
        n = len(closes)
        out: Dict = {}

        def _ma(p: int):
            return round(sum(closes[-p:]) / p, 2) if n >= p else None

        for p in (5, 10, 20, 60):
            out[f"ma{p}"] = _ma(p)

        if n >= 15:
            gains, losses = [], []
            for i in range(1, n):
                diff = closes[i] - closes[i - 1]
                gains.append(max(diff, 0))
                losses.append(max(-diff, 0))
            avg_g = sum(gains[-14:]) / 14
            avg_l = sum(losses[-14:]) / 14
            rs = avg_g / avg_l if avg_l > 0 else 999
            out["rsi"] = round(100 - 100 / (1 + rs), 2)
        else:
            out["rsi"] = None

        if n >= 26:
            def _ema(arr, period):
                k = 2 / (period + 1)
                e = arr[0]
                for v in arr[1:]:
                    e = v * k + e * (1 - k)
                return e

            dif = round(_ema(closes, 12) - _ema(closes, 26), 4)
            dea = round(_ema([closes[i] for i in range(n)], 9), 4) if n >= 35 else None
            if dea is not None:
                hist = round((dif - dea) * 2, 4)
            else:
                hist = None
            out["macd"] = {"dif": dif, "dea": dea, "hist": hist}
        else:
            out["macd"] = {"dif": None, "dea": None, "hist": None}
        return out

    @staticmethod
    def get_stock_info(symbol: str) -> Dict:
        quote = AStockDataService.get_realtime_quote(symbol)
        kline = AStockDataService.get_kline(symbol, period="daily", days=80)
        return {
            "quote": asdict(quote),
            "kline": kline[-10:],
            "indicators": AStockDataService.compute_indicators(kline),
        }

    _recommend_cache: Dict[str, tuple] = {}
    _RECOMMEND_TTL = 900.0  # 15 分钟

    @staticmethod
    def _kline_positions(symbol: str) -> Dict:
        """基于近 250+ 日计算 52周相对位置/距高点等技面因子。"""
        kl = AStockDataService.get_kline(symbol, period="daily", days=260)
        if len(kl) < 60:
            return {}
        closes = [float(k["close"]) for k in kl]
        hi = max(closes)
        lo = min(closes)
        last = closes[-1]
        pos52 = (last - lo) / (hi - lo) * 100 if hi > lo else 100.0
        ma5 = sum(closes[-5:]) / 5
        ma20 = sum(closes[-20:]) / 20
        ma60 = sum(closes[-60:]) / 60
        up_trend = ma5 >= ma20 >= ma60
        down20 = (closes[-20] - closes[-1]) / closes[-20] * 100 if closes[-20] else 0
        return {
            "pos52": round(pos52, 1), "drawdown_max": round((last - hi) / hi * 100, 1),
            "up_trend": up_trend, "ma5": round(ma5, 2), "ma20": round(ma20, 2), "ma60": round(ma60, 2),
            "rsi": AStockDataService.compute_indicators(kl).get("rsi"),
            "vol20": round(down20, 2),
        }

    @staticmethod
    def get_recommendations(limit: int = 10, force: bool = False) -> Dict:
        """低位荐股：全市场快照（基本面 PE/PB/市值）+ 近2周低位 + 研报覆盖评分。"""
        key = f"recommend_{limit}"
        now = time.time()
        if not force and key in AStockDataService._recommend_cache:
            ts, val = AStockDataService._recommend_cache[key]
            if now - ts < AStockDataService._RECOMMEND_TTL:
                return val
        try:
            df, _src = AStockDataService._load_all_snapshot()
            rows = df.to_dict("records")[:3000] if df is not None and len(df) else []
            # 基本面初筛：PE 0~80、市值20亿~3000亿、价格2~300
            cands = []
            for r in rows:
                pe = AStockDataService._row_num(r, "市盈率", "市盈率-动态", "pe")
                mc = AStockDataService._row_num(r, "总市值", "mktcap")
                price = AStockDataService._row_num(r, "最新价", "trade", "price")
                pct = AStockDataService._row_num(r, "涨跌幅", "changepercent")
                code = str(r.get("代码") or r.get("code") or "")
                name = AStockDataService._row_name(r, "")
                if not code:
                    continue
                if not (code.startswith(("6", "0", "3"))):  # 仅沪深主板+创业板+科创板
                    continue
                if not (0 < pe <= 80):
                    continue
                if not (2 <= price <= 300):
                    continue
                if mc and not (2e9 <= mc <= 3e11):
                    continue
                if abs(pct) > 9.5:  # 剔除涨停/跌停
                    continue
                cands.append({"code": code, "name": name, "pe": pe, "pb": float(r.get("市净率") or 0),
                              "cap": mc, "price": price, "pct": pct})
            # 并发拉研报覆盖评分
            pool = cands[:30]  # 初筛后取 30 只有代表性
            scored = []

            def _score_one(c: Dict):
                symbol = AStockDataService._to_symbol(c["code"])
                pos = AStockDataService._kline_positions(symbol)
                if not pos or pos.get("pos52", 100) > 40:  # 仅低<40%位置
                    return None
                reports = ResearchReportService.get_reports(code=c["code"], limit=6)
                r_cnt = len(reports)
                total_target = sum(1 for x in reports if (x.get("target_price") or 0) > c["price"])
                # 评分：位置越低越好，研报越多/看多越多越好，RSI适中偏低，PE低加分
                score = 100.0 - pos["pos52"] + min(r_cnt * 4, 24)
                if total_target > 0:
                    score += 6
                if pos.get("rsi") is not None:
                    score += 5 if 40 <= pos["rsi"] <= 68 else (0 if pos["rsi"] < 75 else -15)
                score -= max(0, 30 - c["pe"]) * 0.3     # PE过低有陷阱
                score += 5 if c["pe"] < 20 else 0
                if c["pb"] and c["pb"] < 3:
                    score += 3
                return {
                    "code": c["code"], "name": c["name"], "price": c["price"], "pct": c["pct"],
                    "pe": c["pe"], "pb": c["pb"] or 0, "cap": c["cap"],
                    "pos52": pos["pos52"], "rsi": pos.get("rsi"),
                    "up_trend": bool(pos.get("up_trend")), "ma_bull": pos["ma5"] >= pos["ma20"],
                    "report_cnt": r_cnt, "total_target": total_target,
                    "score": round(score, 1),
                    "reasons": [
                        f"52周相对位置仅{pos['pos52']:.0f}%（低位）",
                        f"PE {c['pe']:.1f}" + ("估值低于20倍" if c["pe"] < 20 else ""),
                        f"{r_cnt} 篇研报覆盖" + ("，目标价高于现价" if total_target > 0 else ""),
                        "均线多头排列(MA5>MA20>MA60)" if pos["up_trend"] else "",
                    ],
                }

            with ThreadPoolExecutor(max_workers=6) as ex:
                for r_ in ex.map(_score_one, pool):
                    if r_:
                        scored.append(r_)
            scored.sort(key=lambda x: x["score"], reverse=True)
            val = {
                "items": scored[:limit],
                "pool_size": len(cands),
                "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                "source": "tech_fund_report",
            }
            AStockDataService._recommend_cache[key] = (time.time(), val)
            return val
        except Exception as e:
            logger.warning("recommend failed: %s", e)
            return {"items": [], "pool_size": 0, "error": str(e),
                    "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

    _spark_cache: Dict[str, List] = {}
    _spark_ts = 0.0
    _SPARK_TTL = 180.0

    @staticmethod
    def get_sparklines_batch(codes: List[str], limit: int = 40) -> Dict[str, List[float]]:
        """并发批量获取近期收盘价走势（自带缓存限时）。"""
        from concurrent.futures import ThreadPoolExecutor
        now = time.time()
        if now - AStockDataService._spark_ts > AStockDataService._SPARK_TTL:
            AStockDataService._spark_cache = {}
        out: Dict[str, List[float]] = dict(AStockDataService._spark_cache)
        need = [c for c in codes if c and c not in out][:limit]
        if not need:
            return out

        def _one(c: str):
            try:
                kl = AStockDataService.get_kline(AStockDataService._to_symbol(c), "daily", 12)
                if kl:
                    return c, [float(k["close"]) for k in kl]
            except Exception as e:
                logger.warning("sparkline %s failed: %s", c, e)
            return None

        with ThreadPoolExecutor(max_workers=4) as ex:
            for item in ex.map(_one, need):
                if item:
                    out[item[0]] = item[1]
                    AStockDataService._spark_cache[item[0]] = item[1]
        AStockDataService._spark_ts = now
        return out

    @staticmethod
    def search_stocks(q: str = "") -> List[Dict]:
        """全市场搜索（东财/新浪快照），仅用缓存快照，避免每次输入都全量拉取）。"""
        df, src = AStockDataService._load_all_snapshot()
        if df is None or not len(df):
            return [{"symbol": s, "name": n} for s, n in AStockDataService.STOCK_POOL.items()
                    if not q or q in s or q in n]
        results = []
        for row in df.to_dict("records"):
            code = str(row.get("代码") or row.get("code") or "")
            name = AStockDataService._row_name(row, "")
            if not code:
                continue
            if q and q not in code and q not in name:
                continue
            results.append({"symbol": AStockDataService._to_symbol(code), "name": name, "source": src})
            if len(results) >= 50:
                break
        return results

    # ------------------------------------------------------------------
    # MSN Finance 数据源（A股指数/历史K线/基本面/搜索）    # ------------------------------------------------------------------
    _MSN_KEY = "0QfOX3Vn51YCzitbLaRkTTBadtWpgTN8NZLW0C1SEM"
    _MSN_HEADERS = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126.0 Safari/537.36 Edg/126.0",
        "Referer": "https://www.msn.cn/zh-cn/finance",
        "Origin": "https://www.msn.cn",
    }
    # 申万行业级指数映射 MSN 指数（通过 "399xxx code" / "CSI SWS <英文> Index" 搜索定位 SecId）。
    _SWS_SECTORS = {
        "电子": "Electronics", "医药生物": "Pharmaceuticals and Biotechnology",
        "食品饮料": "Food, Beverages and Tobacco", "化工": "Chemicals",
        "电力设备": "Power Equipment", "计算机": "Computer", "医药医疗": "Medical Devices",
        "汽车": "Automobiles", "国防军工": "Aerospace and Defense", "银行": "Banks",
        "保险": "Insurance", "传媒": "Media", "通信设备": "Telecommunications Equipment",
        "煤炭": "Coal", "钢铁": "Steel", "房地产": "Real Estate", "服装": "Apparel and Luxury Goods",
        "农业": "Agricultural Products", "家电": "Home Appliances", "石油石化": "Oil, Gas and Fossil Fuels",
    }

    @staticmethod
    def _msn_find_index_secid(sector_name_cn: str) -> str:
        """搜索申万行业指数 SecId：先查 CSI SWS <英文> Index，再兜底返回缓存的电子指数。"""
        if not sector_name_cn:
            return ""
        en = AStockDataService._SWS_SECTORS.get(sector_name_cn, "")
        cands = []
        if en:
            cands.append(f"CSI SWS {en} Index")
            cands.append(f"SWS {en} Index")
        cands.append("CSI SWS Index")
        for q in cands:
            secid = AStockDataService._msn_search_secid(q)
            if secid:
                return secid
        return ""
    _sws_cache: Dict[str, dict] = {}
    _sws_ts = 0.0

    @staticmethod
    def _msn_search_secid(query: str) -> str:
        """MSN Finance 搜索 读取/定位 A股指数 SecId。"""
        try:
            r = requests.get("https://services.bingapis.com/contentservices-finance.csautosuggest/api/v1/Query",
                             params={"query": query, "market": "zh-cn", "count": 4},
                             headers=AStockDataService._MSN_HEADERS, timeout=10)
            stocks = ((r.json() or {}).get("data") or {}).get("stocks") or []
            for raw in stocks:
                d = json.loads(raw)
                secid = d.get("SecId") or ""
                full = d.get("FullInstrument") or ""
                mic = d.get("ExMicCode") or ""
                if "XSHG_CSI" in mic or ".10." in full or ".10." in full:
                    return secid
                if secid:
                    return secid
        except Exception:
            pass
        return ""

    @staticmethod
    def _msn_charts_10yh1(secid: str, days: int = 30) -> List[Dict]:
        """MSN Charts 10YH1 → 近 N 日日K"""
        try:
            r = requests.get("https://assets.msn.com/service/Finance/Charts",
                             params={"ids": secid, "type": "10YH1", "apikey": AStockDataService._MSN_KEY,
                                     "wrapodata": "false", "ocid": "finance-utils-peregrine"},
                             headers=AStockDataService._MSN_HEADERS, timeout=12)
            j = r.json()
            arr = j if isinstance(j, list) else [j]
            series = arr[0].get("series", {}) if arr else {}
            prices = series.get("prices") or []
            opens = series.get("openPrices") or []
            highs = series.get("pricesHigh") or []
            lows = series.get("pricesLow") or []
            dates = series.get("timeStamps") or []
            vols = series.get("volumes") or []
            n = min(len(dates), len(prices))
            out = []
            for i in range(max(0, n - days), n):
                c = prices[i] if i < len(prices) else 0
                o = opens[i] if i < len(opens) else c
                h = highs[i] if i < len(highs) else c
                lo = lows[i] if i < len(lows) else c
                v = vols[i] if i < len(vols) else 0
                out.append({"date": str(dates[i])[:10], "open": float(o), "close": float(c),
                            "high": float(h), "low": float(lo), "volume": float(v),
                            "turnover": 0.0, "amplitude": 0.0, "change_percent": 0.0})
            return out
        except Exception as e:
            logger.warning("MSN charts %s failed: %s", secid, e)
            return []

    @staticmethod
    def _msn_quote_summary(secid: str) -> Dict:
        """MSN QuoteSummary 拉取行情/财务概要。"""
        try:
            r = requests.get("https://assets.msn.com/service/Finance/QuoteSummary",
                             params={"ids": secid, "apikey": AStockDataService._MSN_KEY,
                                     "wrapodata": "false", "ocid": "finance-utils-peregrine",
                                     "cm": "zh-cn", "it": "web"},
                             headers=AStockDataService._MSN_HEADERS, timeout=10)
            data = r.json()
            if isinstance(data, list) and data:
                return data[0]
            return data
        except Exception as e:
            logger.warning("MSN quote %s failed: %s", secid, e)
            return {}

    @staticmethod
    def get_sector_heat_ths() -> Dict[str, float]:
        """同花顺行业板块涨跌。"""
        if ak is None:
            return {}
        try:
            df = AStockDataService._with_retry(lambda: ak.stock_board_industry_summary_ths(), 2, 0.8)
            if df is not None and len(df):
                out = {}
                for _, r in df.head(30).iterrows():
                    out[str(r["板块"])] = float(r["涨跌幅"])
                return out
        except Exception as e:
            logger.warning("THS board failed: %s", e)
        return {}

    # ------------------------------------------------------------------
    # 智能看板扩展数据：涨停/跌停/炸板池、板块涨跌曲线、主力资金流、连板晋级    # 全部走东方财富直连 HTTP（不依赖 akshare）    # ------------------------------------------------------------------

    _EM = "https://push2delay.eastmoney.com"
    _EMEX = "https://push2ex.eastmoney.com"
    _EMHIS = "https://push2his.eastmoney.com"
    _ZX_UT = "7eea3edcaed734bea9cbfc24409ed989"
    _KT_HEADERS = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                      "(KHTML, like Gecko) Chrome/126.0 Safari/537.36",
        "Referer": "https://quote.eastmoney.com/",
    }
    _pool_cache: Dict[str, tuple] = {}
    _trend_cache: Dict[str, tuple] = {}
    _fflow_cache: Dict[str, tuple] = {}

    @staticmethod
    def _mkymd(dt: datetime) -> str:
        return dt.strftime("%Y%m%d")

    @staticmethod
    def _recent_trade_dates(n: int = 5) -> List[str]:
        """取 n 个自然日候选交易日（不只按日历单判定）。"""
        out, d = [], datetime.now()
        while len(out) < n and d >= datetime.now() - timedelta(days=14):
            if d.weekday() < 5:
                out.append(AStockDataService._mkymd(d))
            d -= timedelta(days=1)
        return out

    @staticmethod
    def _em_pool(date: str, mode: str) -> list:
        """东财涨停(dpt=wz.ztzt)/跌停/炸板池原始列表; mode in ('zt','dt','zb')"""
        if ak is not None:
            try:
                if mode == "zt":
                    df = ak.stock_zt_pool_em(date=date)
                    if df is not None and len(df):
                        return df.to_dict("records")
                elif mode == "dt":
                    df = ak.stock_zt_pool_dtgc_em(date=date)
                    if df is not None and len(df):
                        return df.to_dict("records")
                elif mode == "zb":
                    df = ak.stock_zt_pool_zbgc_em(date=date)
                    if df is not None and len(df):
                        return df.to_dict("records")
            except Exception as e:
                logger.warning("em_pool akshare %s failed: %s", mode, e)
        paths = {"zt": "/getTopicZTPool", "dt": "/getTopicDTPool", "zb": "/getTopicZBPool"}
        sorts = {"zt": "fbt:asc", "dt": "fund:asc", "zb": "zbc:desc"}
        try:
            url = AStockDataService._EMEX + paths[mode]
            params = {"ut": AStockDataService._ZX_UT, "dpt": "wz.ztzt",
                      "Pageindex": 0, "pagesize": 500, "sort": sorts[mode], "date": date}
            r = requests.get(url, params=params, headers=AStockDataService._KT_HEADERS, timeout=12)
            r.raise_for_status()
            j = r.json()
            pool = ((j or {}).get("data") or {}).get("pool") or []
            if pool:
                return pool
        except Exception as e:
            logger.warning("em_pool %s %s failed: %s", mode, date, e)
        return []

    @staticmethod
    def _em_pool_items(date: str, mode: str) -> List[Dict]:
        """涨停/跌停/炸板池，统一结构列表。"""
        rows = AStockDataService._em_pool(date, mode)
        out = []
        for x in rows:
            if isinstance(x, dict) and "c" in x:
                code = AStockDataService._clean_code(x.get("c") or "")
                if not code:
                    continue
                out.append({
                    "code": code,
                    "name": x.get("n") or "",
                    "price": float(x.get("p") or 0),
                    "pct": float(x.get("zdp") or 0) if x.get("zdp") is not None else 0.0,
                    "amount": float(x.get("amount") or 0) if x.get("amount") is not None else 0.0,
                    "fund": float(x.get("fund") or 0) if x.get("fund") is not None else 0.0,
                    "seal": float(x.get("fund") or 0) if x.get("fund") is not None else 0.0,
                    "hs": float(x.get("hs") or 0) if x.get("hs") is not None else 0.0,
                    "fbt": str(x.get("fbt") or ""),
                    "lbt": str(x.get("lbt") or ""),
                    "zbc": int(x.get("zbc") or 0) if x.get("zbc") is not None else 0,
                    "lbc": int(x.get("lbc") or 1) if x.get("lbc") is not None else 1,
                    "zttj": str(x.get("zttj") or ""),
                    "hybk": str(x.get("hybk") or ""),
                })
            elif isinstance(x, dict):
                # DataFrame 形态（akshare 场景）
                code = AStockDataService._clean_code(x.get("代码") or x.get("code") or "")
                if not code:
                    continue
                out.append({
                    "code": code,
                    "name": x.get("名称") or x.get("name") or "",
                    "price": float(x.get("最新价") or x.get("price") or 0),
                    "pct": float(x.get("涨跌幅") or x.get("pct") or 0),
                    "amount": float(x.get("成交额") or 0),
                    "fund": float(x.get("封板资金") or x.get("fund") or 0),
                    "seal": float(x.get("封板资金") or x.get("fund") or 0),
                    "hs": float(x.get("换手率") or 0),
                    "fbt": str(x.get("首次封板时间") or ""),
                    "lbt": str(x.get("最后封板时间") or ""),
                    "zbc": int(x.get("炸板次数") or 0),
                    "lbc": int(x.get("连板数") or 1),
                    "zttj": str(x.get("涨停统计") or ""),
                    "hybk": str(x.get("所属行业") or ""),
                })
            if len(out) >= 200:
                break
        return out

    @staticmethod
    def get_limit_pools(force: bool = False) -> Dict:
        """涨停/跌停/炸板 + 最近交易日；带缓存 60s。"""
        key = "limit_pools"
        now = time.time()
        if not force and key in AStockDataService._pool_cache:
            ts, val = AStockDataService._pool_cache[key]
            if now - ts < 60:
                return val
        date = AStockDataService._recent_trade_dates(5)[0]
        zuotian = AStockDataService._recent_trade_dates(7)[1] if len(AStockDataService._recent_trade_dates(7)) > 1 else date
        zt, dt, zb = [], [], []
        zt_rows, okdates = [], []
        for d in AStockDataService._recent_trade_dates(5):
            s = AStockDataService._em_pool_items(d, "zt")
            if s:
                zt_rows.append((d, s))
                break
        if zt_rows:
            date, zt = zt_rows[0]
        for d in AStockDataService._recent_trade_dates(5):
            s = AStockDataService._em_pool_items(d, "dt")
            if s:
                dt = s
                break
        for d in AStockDataService._recent_trade_dates(5):
            s = AStockDataService._em_pool_items(d, "zb")
            if s:
                zb = s
                break
        val = {"date": date, "prev_date": zuotian,
               "limit_up": zt, "limit_down": dt, "zhaban": zb}
        # 异步落库当天涨停池快照
        if val.get("date"):
            threading.Thread(
                target=lambda: SupabaseStore.save_pool("limit_up", val["date"], {
                    "date": val["date"], "prev_date": val["prev_date"],
                    "limit_up": zt, "limit_down": dt, "zhaban": zb}),
                daemon=True).start()
        AStockDataService._pool_cache[key] = (time.time(), val)
        return val

    @staticmethod
    def get_board_progress(force: bool = False) -> List[Dict]:
        """连板晋级：1板 / 2板 / 3板；用最近两日涨停池比较。"""
        key = "board_progress"
        now = time.time()
        if not force and key in AStockDataService._pool_cache:
            ts, val = AStockDataService._pool_cache[key]
            if now - ts < 120:
                return val
        rows = []
        try:
            dates = AStockDataService._recent_trade_dates(7)
            pools = []
            for d in dates:
                items = AStockDataService._em_pool_items(d, "zt")
                if items:
                    pools.append((d, items))
                if len(pools) >= 2:
                    break
            if not pools:
                return []
            today_date, today = pools[0]
            # 昨日涨停池（连板：lbc >= 1）
            prev = None
            for d, items in pools[1:]:
                if d != today_date:
                    prev = (d, items)
                    break
            # 今日各板位数量与具体个股（仅统计今日涨停还在连板的）
            today_boards: Dict[int, int] = {}
            today_stocks: Dict[int, List[Dict]] = {}
            for it in today:
                lb = int(it.get("lbc") or 1)
                today_boards[lb] = today_boards.get(lb, 0) + 1
                today_stocks.setdefault(lb, []).append({
                    "code": it.get("code") or "",
                    "name": it.get("name") or "",
                    "pct": float(it.get("pct") or 0),
                    "hybk": it.get("hybk") or "",
                })
            # 昨日各板位数量
            prev_boards: Dict[int, int] = {}
            if prev:
                for it in prev[1]:
                    lb = int(it.get("lbc") or 1)
                    prev_boards[lb] = prev_boards.get(lb, 0) + 1
            for n in range(1, 8):
                jinri = today_boards.get(n + 1, 0)
                zuori = prev_boards.get(n, 0) if prev else today_boards.get(n, 0)
                base = zuori or 0
                rate = round((jinri / base * 100), 1) if base else 0.0
                rows.append({
                    "name": f"{n}进{n + 1}板",
                    "from_n": n,
                    "count": jinri,
                    "prev_count": base,
                    "rate": rate,
                    "stocks": today_stocks.get(n + 1, []),
                })
            rows = [r for r in rows if r["count"] or r["prev_count"]]
            # 全局连板梯队（lbc >= 2，按板位分组含股明细）
            ladders: List[Dict] = []
            for lb in sorted(today_boards, reverse=True):
                if lb < 2:
                    continue
                ladders.append({
                    "lb": lb,
                    "count": today_boards[lb],
                    "stocks": today_stocks.get(lb, []),
                })
            rows.append({"ladders": ladders})
            if today_date:
                threading.Thread(
                    target=lambda: SupabaseStore.save_pool("board_progress", today_date, {"rows": rows,
                                                                                          "pin_date": today_date}),
                    daemon=True).start()
            AStockDataService._pool_cache[key] = (time.time(), rows)
        except Exception as e:
            logger.warning("board_progress failed: %s", e)
        return rows

    @staticmethod
    def get_limit_pool_by_board() -> Dict:
        """涨停池按连板数分层（首板 / 1进2 / 2进3 / ... / 6进7），duanxianxia 风格。
        返回 {date, boards:[{label, lbc, count, stocks:[{code,name,pct,seal,amount,hybk,fbt}]}],
               total, seal_total}"""
        pools = AStockDataService.get_limit_pools()
        zt = pools.get("limit_up") or []
        date = pools.get("date") or ""
        groups: Dict[int, List[Dict]] = {}
        seal_total = 0.0
        amount_total = 0.0
        for it in zt:
            lb = int(it.get("lbc") or 1)
            groups.setdefault(lb, []).append({
                "code": it.get("code") or "",
                "name": it.get("name") or "",
                "pct": float(it.get("pct") or 0),
                "price": float(it.get("price") or 0),
                "seal": float(it.get("seal") or it.get("fund") or 0),
                "amount": float(it.get("amount") or 0),
                "hs": float(it.get("hs") or 0),
                "hybk": it.get("hybk") or "",
                "fbt": it.get("fbt") or "",
            })
            seal_total += float(it.get("seal") or it.get("fund") or 0)
            amount_total += float(it.get("amount") or 0)
        boards = []
        for n in range(1, 8):
            stocks = groups.get(n, [])
            label = "首板" if n == 1 else f"{n - 1}进{n}"
            boards.append({
                "label": label, "lbc": n, "count": len(stocks),
                "stocks": sorted(stocks, key=lambda x: x.get("seal", 0), reverse=True),
            })
        return {
            "date": date,
            "total": len(zt),
            "seal_total": round(seal_total, 2),
            "amount_total": round(amount_total, 2),
            "boards": boards,
        }

    @staticmethod
    def get_sector_hist_em(board_code: str, days: int = 30, beg: str = "") -> list:
        """东财板块指数日K（secid=90.BKxxxx）；返回 [{date,close,open,high,low,pct}]"""
        try:
            beg = beg or (datetime.now() - timedelta(days=days * 2)).strftime("%Y%m%d")
            end = datetime.now().strftime("%Y%m%d")
            url = AStockDataService._EMHIS + "/api/qt/stock/kline/get"
            params = {"secid": f"90.{board_code}", "ut": AStockDataService._ZX_UT,
                      "fields1": "f1,f2,f3,f4,f5,f6",
                      "fields2": "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61",
                      "klt": "101", "fqt": "1", "beg": beg, "end": end,
                      "lmt": str(days)}
            r = requests.get(url, params=params, headers=AStockDataService._KT_HEADERS, timeout=12)
            r.raise_for_status()
            data = ((r.json() or {}).get("data") or {})
            klines = data.get("klines") or []
            out = []
            for k in klines:
                f = k.split(",")
                if len(f) < 11:
                    continue
                out.append({
                    "date": f[0],
                    "open": float(f[1] or 0), "close": float(f[2] or 0),
                    "high": float(f[3] or 0), "low": float(f[4] or 0),
                    "volume": float(f[5] or 0), "amount": float(f[6] or 0),
                    "amp": float(f[7] or 0), "pct": float(f[8] or 0),
                    "change": float(f[9] or 0), "turnover": float(f[10] or 0),
                })
            return out[-days:] if days and days > 0 else out
        except Exception as e:
            logger.warning("sector hist %s failed: %s", board_code, e)
            return []

    @staticmethod
    def _sector_trends_ths(days: int = 30, top: int = 0) -> Dict:
        """同花顺行业板块近 days 日日K（全量约 90 行业，稳定不受东财限流影响）。"""
        if ak is None:
            return {}
        try:
            names = AStockDataService._with_retry(
                lambda: ak.stock_board_industry_name_ths(), 2, 0.8)
            if names is None or not len(names):
                return {}
            limit = int(top or len(names))
            selected = [str(r["name"]).strip() for _, r in names.head(limit).iterrows() if str(r.get("name") or "").strip()]
            end = datetime.now().strftime("%Y%m%d")
            beg = (datetime.now() - timedelta(days=days * 2)).strftime("%Y%m%d")
            results: Dict[str, List[Dict]] = {}
            with ThreadPoolExecutor(max_workers=5) as ex:
                futs = {ex.submit(AStockDataService._ths_board_hist, nm, beg, end, days): nm for nm in selected}
                for fut in as_completed(futs):
                    nm = futs[fut]
                    try:
                        hist = fut.result()
                        if hist:
                            results[nm] = hist
                    except Exception as e:
                        logger.warning("sector trend ths %s failed: %s", nm, e)
            if not results:
                return {}
            dates: List[str] = []
            series: List[Dict] = []
            for nm, hist in results.items():
                if not series:
                    dates = [h["date"] for h in hist]
                closes = [h["close"] for h in hist]
                pcts = [h["pct"] for h in hist]
                series.append({
                    "name": nm,
                    "closes": closes,
                    "pcts": pcts,
                    "today_pct": pcts[-1] if pcts else 0,
                })
            return {"dates": dates, "series": series, "source": "ths",
                    "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
        except Exception as e:
            logger.warning("ths sector trends failed: %s", e)
            return {}

    @staticmethod
    def _ths_board_hist(sector_name: str, beg: str, end: str, days: int = 30) -> List[Dict]:
        """同花顺单板块指数日K（列位置映射：日期/收盘；涨跌幅由收盘序列推算）。"""
        try:
            df = ak.stock_board_industry_index_ths(symbol=sector_name, start_date=beg, end_date=end)
            if df is None or not len(df):
                return []
            rows = [list(r) for _, r in df.iterrows()]
            out = []
            prev = None
            for row in rows:
                date = str(row[0]) if row else ""
                close = float(row[4] or 0) if len(row) > 4 else 0
                pct = 0.0
                if prev:
                    pct = round((close - prev) / prev * 100, 2) if prev else 0.0
                prev = close
                out.append({"date": date, "close": close, "pct": pct})
            return out[-days:]
        except Exception as e:
            logger.warning("ths board hist %s failed: %s", sector_name, e)
            return []

    @staticmethod
    def _save_sector_trends(val: Dict) -> None:
        """板块曲线结果（dates+series）按板块落库；用作定时同步（可靠），失败静默。"""
        try:
            series = val.get("series") or []
            dates = val.get("dates") or []
            if not series or not dates:
                return
            for s in series:
                rows = [{"date": dates[i], "close": s["closes"][i],
                         "pct": s["pcts"][i] if i < len(s["pcts"]) else 0}
                        for i in range(len(dates))
                        if i < len(s["closes"])]
                ok = False
                for _ in range(2):
                    if SupabaseStore.save_sector_daily(s["name"], rows, val.get("source") or "ths"):
                        ok = True
                        break
                if not ok:
                    logger.warning("sector save retry exhausted: %s", s.get("name"))
        except Exception as e:  # noqa: BLE001
            logger.warning("save_sector_trends fail: %s", e)

    @staticmethod
    def _sector_trends_from_db(days: int = 30, top: int = 0) -> Optional[Dict]:
        """从Supabase杣捧瞿捧瞿（可选）当日板块趋势。"""

        try:
            if not SupabaseStore.ENABLED:
                return None
            name_df = None
            if ak is not None:
                try:
                    name_df = AStockDataService._with_retry(
                        lambda: ak.stock_board_industry_name_ths(), 1, 0.5)
                except Exception:  # noqa: BLE001
                    name_df = None
            if name_df is None or not len(name_df):
                return None
            limit = int(top or len(name_df))
            names = [str(r["name"]).strip() for _, r in name_df.head(limit).iterrows()
                     if str(r.get("name") or "").strip()]
            # 分页批量读取（PostgREST 单页 limit 上限 1000），单次 HTTP 一次取全量
            beg_date = (datetime.now() - timedelta(days=days * 2 + 7)).strftime("%Y-%m-%d")
            try:
                rows = []
                offset = 0
                while True:
                    page = SupabaseStore._select(
                        "quant_sector_daily", "*",
                        {"trade_date": f"gte.{beg_date}", "limit": str(SupabaseStore._PAGE_SIZE),
                         "offset": str(offset)})
                    if not page:
                        break
                    rows.extend(page)
                    if len(page) < SupabaseStore._PAGE_SIZE:
                        break
                    offset += SupabaseStore._PAGE_SIZE
            except Exception as e:  # noqa: BLE001
                logger.warning("sector batch read fail: %s", e)
                return None
            if not rows:
                return None
            by_sector: Dict[str, list] = {}
            for r in rows:
                by_sector.setdefault(r.get("sector"), []).append(r)
            series = []
            dates = []
            for nm in names:
                hist = by_sector.get(nm)
                if not hist:
                    return None
                hist.sort(key=lambda x: x.get("trade_date") or "")
                hist = hist[-days:]
                if len(hist) < max(days * 0.8, 15):
                    # 数据不足：整批放弃，交给实时抓取兜底
                    return None
                if not dates:
                    dates = [h["trade_date"] for h in hist]
                series.append({
                    "name": nm,
                    "closes": [float(h.get("close") or 0) for h in hist],
                    "pcts": [float(h.get("pct") or 0) for h in hist],
                    "today_pct": float(hist[-1].get("pct") or 0) if hist else 0,
                })
            if not series:
                return None
            return {"dates": dates, "series": series, "source": "supabase",
                    "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
        except Exception as e:  # noqa: BLE001
            logger.warning("sector_trends_from_db fail: %s", e)
            return None

    @staticmethod
    def get_sector_trends(days: int = 60, top: int = 10, force: bool = False) -> Dict:
        """板块涨跌曲线（外层硬超时包装）：15 秒内未完成则回退缓存/DB/空结构。"""
        key = f"sector_trends_{days}_{top}"
        now = time.time()
        if not force and key in AStockDataService._trend_cache:
            ts, val = AStockDataService._trend_cache[key]
            if now - ts < 300:
                return val
        try:
            with ThreadPoolExecutor(max_workers=1) as ex:
                fut = ex.submit(AStockDataService._get_sector_trends_impl, days, top, force)
                return fut.result(timeout=15)
        except TimeoutError:
            logger.warning("sector_trends timeout (15s), fallback to cache/db/empty")
            ts, val = AStockDataService._trend_cache.get(key, (0, None))
            if val:
                return val
            db_val = AStockDataService._sector_trends_from_db(days, top)
            if db_val:
                return db_val
            return {"dates": [], "series": [], "source": "timeout",
                    "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
        except Exception as e:
            logger.warning("sector_trends outer wrapper failed: %s", e)
            ts, val = AStockDataService._trend_cache.get(key, (0, None))
            if val:
                return val
            db_val = AStockDataService._sector_trends_from_db(days, top)
            if db_val:
                return db_val
            return {"dates": [], "series": [], "source": "error",
                    "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

    @staticmethod
    def _get_sector_trends_impl(days: int = 60, top: int = 10, force: bool = False) -> Dict:
        """板块涨跌曲线：行业板块列表 + 每板块近 days 日收盘涨跌幅。"""
        # 优先走同花顺全量板块（akshare stock_board_industry_*_ths，稳定覆盖约 90 个行业）；        失败回退到东财 clist + BK 列表。        """
        key = f"sector_trends_{days}_{top}"
        now = time.time()
        if not force and key in AStockDataService._trend_cache:
            ts, val = AStockDataService._trend_cache[key]
            if now - ts < 300:
                return val
        # 读库优先：10+ 板块近期数据齐时，直接由 Supabase 组装（不再整批抓取）
        db_val = AStockDataService._sector_trends_from_db(days, top)
        if db_val:
            AStockDataService._trend_cache[key] = (time.time(), db_val)
            return db_val
        ths_val = AStockDataService._sector_trends_ths(days, top)
        if ths_val and ths_val.get("series"):
            # 抓取结果异步落库
            threading.Thread(target=lambda: AStockDataService._save_sector_trends(ths_val), daemon=True).start()
            AStockDataService._trend_cache[key] = (time.time(), ths_val)
            return ths_val
        logger.warning("sector_trends ths empty, fallback to eastmoney")
        try:
            url = AStockDataService._EM + "/api/qt/clist/get"
            params = {"pn": 1, "pz": max(int(top) * 2, 20), "po": 1, "np": 1,
                      "fltt": 2, "invt": 2, "fid": "f3",
                      "fs": "m:90+t:2+f:!50",  # 行业板块
                      "fields": "f12,f14,f3,f20,f8"}
            r = requests.get(url, params=params, headers=AStockDataService._KT_HEADERS, timeout=12)
            r.raise_for_status()
            diff = ((r.json() or {}).get("data") or {}).get("diff") or []
            boards = []
            for x in diff:
                code = (x.get("f12") or "").strip()
                nm = (x.get("f14") or "").strip()
                if code.startswith("BK") and nm and len(boards) < int(top):
                    boards.append({"code": code, "name": nm,
                                   "pct": float(x.get("f3") or 0),
                                   "amount": float(x.get("f20") or 0),
                                   "hs": float(x.get("f8") or 0)})
            if not boards:
                raise RuntimeError("empty sector list")
            dates: List[str] = []
            series: List[Dict] = []
            results = {}
            # 分批小并发抓取历史，batch=2 控制并发避免限流；外层 15s 硬超时兜底
            batch = 2
            for i in range(0, len(boards), batch):
                chunk = boards[i:i + batch]
                with ThreadPoolExecutor(max_workers=batch) as ex:
                    futs = {ex.submit(AStockDataService.get_sector_hist_em, b["code"], max(days, 15)): b
                            for b in chunk}
                    for fut in as_completed(futs):
                        b = futs[fut]
                        try:
                            hist = fut.result()
                            if hist:
                                results[b["name"]] = {"board": b, "hist": hist}
                        except Exception as e:
                            logger.warning("sector trend %s failed: %s", b["name"], e)
                time.sleep(0.25)
            names = list(results.keys())
            for i, nm in enumerate(names):
                hist = results[nm]["hist"]
                if not series:
                    dates = [h["date"] for h in hist]
                closes = [h["close"] for h in hist]
                pcts = [h["pct"] for h in hist]
                series.append({
                    "name": nm,
                    "closes": closes,
                    "pcts": pcts,
                    "today_pct": results[nm]["board"]["pct"],
                })
            val = {"dates": dates, "series": series, "source": "eastmoney",
                   "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
            if series:
                AStockDataService._trend_cache[key] = (time.time(), val)
                return val
            logger.warning("sector_trends eastmoney series empty, fallback to MSN")
        except Exception as e:
            logger.warning("sector_trends failed: %s", e)
        # ---- MSN SWS 行业指数兜底 ----
        try:
            now2 = time.time()
            if now2 - AStockDataService._sws_ts < 600 and AStockDataService._sws_cache:
                return AStockDataService._sws_cache
            msn_boards = []
            with ThreadPoolExecutor(max_workers=4) as ex:
                def _fetch_sws(name):
                    secid = AStockDataService._msn_find_index_secid(name)
                    return name, secid
                futs = {ex.submit(_fetch_sws, name): name
                        for name in list(AStockDataService._SWS_SECTORS)[: max(int(top) * 2, 10)]}
                for fut in as_completed(futs):
                    name, secid = fut.result()
                    if secid:
                        hist = AStockDataService._msn_charts_10yh1(secid, max(days, 15))
                        if hist and len(hist) > 5:
                            closes = [h["close"] for h in hist]
                            pcts = [0.0] + [round((closes[i] - closes[i-1]) / closes[i-1] * 100, 2)
                                            if closes[i-1] else 0 for i in range(1, len(closes))]
                            msn_boards.append({"name": name, "closes": closes, "pcts": pcts,
                                               "today_pct": pcts[-1] if pcts else 0.0,
                                               "dates": [h["date"] for h in hist]})
            if msn_boards:
                msn_boards.sort(key=lambda b: abs(b.get("today_pct", 0)), reverse=True)
                msn_boards = msn_boards[:top]
                dates = msn_boards[0]["dates"]
                series = [{"name": b["name"], "closes": b["closes"], "pcts": b["pcts"],
                           "today_pct": b["today_pct"]} for b in msn_boards]
                val = {"dates": dates, "series": series, "source": "msn_sws",
                       "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
                AStockDataService._trend_cache[key] = (time.time(), val)
                AStockDataService._sws_cache = val
                AStockDataService._sws_ts = time.time()
                return val
        except Exception as e2:
            logger.warning("MSN sector fallback failed: %s", e2)
        return {"dates": [], "series": [], "source": "eastmoney",
                "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

    @staticmethod
    def _em_fflow(fid_secids: str, extra_fs: str = "", po: int = 1) -> list:
        """东财资金流排名单；fid: f62（主力净流入排名）；po=1 取流入，po=0 取流出。"""
        out = []
        try:
            url = AStockDataService._EM + "/api/qt/clist/get"
            params = {"pn": 1, "pz": 20, "po": po, "np": 1, "fltt": 2, "invt": 2,
                      "fid": fid_secids or "f62",
                      "fs": extra_fs or "m:90+t:2+f:!50",
                      "fields": "f12,f14,f2,f3,f62,f184,f66,f69,f72,f75,f78,f81,f84,f87,f124"}
            r = requests.get(url, params=params, headers=AStockDataService._KT_HEADERS, timeout=12)
            r.raise_for_status()
            diff = ((r.json() or {}).get("data") or {}).get("diff") or []
            for x in diff:
                def F(k):
                    v = x.get(k)
                    return float(v) if v is not None else 0.0
                out.append({
                    "code": str(x.get("f12") or ""),
                    "name": str(x.get("f14") or ""),
                    "price": F("f2"),
                    "pct": F("f3"),
                    "main": F("f62"),
                    "main_pct": F("f184"),
                    "super": F("f66"),
                    "big": F("f72"),
                    "medium": F("f78"),
                    "small": F("f84"),
                    "lead": str(x.get("f124") or ""),
                })
        except Exception as e:
            logger.warning("em_fflow %s failed: %s", fid_secids, e)
        return out

    @staticmethod
    def get_market_fund_flow(force: bool = False) -> Dict:
        """主力净流入流出：板块资金流入/流出榜 + 个股榜 + 大盘指数资金流。"""
        key = "market_fund_flow"
        now = time.time()
        if not force and key in AStockDataService._fflow_cache:
            ts, val = AStockDataService._fflow_cache[key]
            if now - ts < 60:
                return val
        try:
            sectors_in = AStockDataService._em_fflow("f62", "m:90+t:2+f:!50", po=1)
            sectors_out = AStockDataService._em_fflow("f62", "m:90+t:2+f:!50", po=0)
            stocks_in = AStockDataService._em_fflow(
                "f62", "m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048", po=1)
            stocks_out = AStockDataService._em_fflow(
                "f62", "m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048", po=0)
            # 大盘指数资金流（上证 1.000001 + 深证 0.399001）
            indices = []
            for secid, secid2, idxname in (("1.000001", "0.399001", "上证指数"),
                                           ("0.399001", "1.000001", "深证成指")):
                try:
                    url = AStockDataService._EM + "/api/qt/stock/fflow/kline/get"
                    params = {"ut": AStockDataService._ZX_UT,
                              "lmt": "0", "klt": "1", "secid": secid, "secid2": secid2,
                              "fields1": "f1,f2,f3,f7",
                              "fields2": "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f62,f63,f64,f65"}
                    r = requests.get(url, params=params, headers=AStockDataService._KT_HEADERS, timeout=12)
                    r.raise_for_status()
                    kl = ((r.json() or {}).get("data") or {}).get("klines") or []
                    if not kl:
                        continue
                    f = kl[-1].split(",")
                    if len(f) >= 6:
                        indices.append({
                            "name": idxname,
                            "date": f[0],
                            "main": float(f[1] or 0),
                            "small": float(f[2] or 0),
                            "medium": float(f[3] or 0),
                            "big": float(f[4] or 0),
                            "super": float(f[5] or 0),
                        })
                except Exception as e:
                    logger.warning("index fflow %s failed: %s", secid, e)
            val = {
                "indices": indices,
                "sectors_in": sectors_in[:12],
                "sectors_out": sectors_out[:12],
                "stocks_in": stocks_in[:12],
                "stocks_out": stocks_out[:12],
                "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            }
            threading.Thread(
                target=lambda: SupabaseStore.save_pool(
                    "fund_flow", datetime.now().strftime("%Y-%m-%d"), val),
                daemon=True).start()
            AStockDataService._fflow_cache[key] = (time.time(), val)
            return val
        except Exception as e:
            logger.warning("market_fund_flow failed: %s", e)
            return {"indices": [], "sectors_in": [], "sectors_out": [],
                    "stocks_in": [], "stocks_out": [],
                    "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

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
class ResearchReportService:
    """券商研报元数据：标题/机构/分析师/评级/目标价/EPS预测/原文链接。"""
    # 合规边界：仅采集列表页元数据（reportapi.eastmoney.com），
    # 不下载、不缓存研报 PDF 全文；示例字段与源站点会调整，需容错。    """

    _cache: Dict[str, tuple] = {}
    _REPORT_TTL = 300  # 5 分钟

    _HEADERS = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                      "(KHTML, like Gecko) Chrome/126.0 Safari/537.36",
        "Referer": "https://data.eastmoney.com/report/",
    }

    @staticmethod
    def _norm(x) -> Optional[float]:
        if x in (None, "", "-"):
            return None
        try:
            return float(str(x).replace(",", "").replace("万", "").strip())
        except (TypeError, ValueError):
            return None

    @staticmethod
    def _authors(row: Dict) -> str:
        a = row.get("author")
        if isinstance(a, list):
            parts = []
            for it in a:
                if isinstance(it, str):
                    nm = it.split(".")[-1] if "." in it else it
                elif isinstance(it, dict):
                    nm = it.get("name") or ""
                else:
                    nm = ""
                if nm:
                    parts.append(nm)
            return "、".join(parts)
        s = row.get("authorName") or ""
        if isinstance(s, str):
            return s.replace("|", "、")
        return ""

    @staticmethod
    def _fetch_report_page(code: str = "", page_no: int = 1, q_type: str = "0",
                           page_size: int = 30, begin: str = "", end: str = "") -> List[Dict]:
        """东财研报中心列表接口直连（qType: 0个股/1行业/2策略）。"""
        try:
            url = "https://reportapi.eastmoney.com/report/list"
            params = {
                "cb": "", "pageSize": str(page_size), "pageNo": str(page_no),
                "qType": q_type, "code": code, "industryCode": "*", "rating": "*",
                "ratingChange": "*",
                "beginTime": begin or (datetime.now() - timedelta(days=90)).strftime("%Y-%m-%d"),
                "endTime": end or datetime.now().strftime("%Y-%m-%d"),
                "_": str(int(time.time() * 1000)),
            }
            r = requests.get(url, params=params, headers=ResearchReportService._HEADERS, timeout=12)
            r.raise_for_status()
            payload = r.json()
        except Exception as e:  # noqa: BLE001
            logger.warning("research report list failed: %s", e)
            return []
        rows = (payload or {}).get("data") or []
        if isinstance(rows, dict):
            rows = rows.get("list") or []
        if not isinstance(rows, list):
            rows = []
        out = []
        for x in rows[:page_size]:
            if not isinstance(x, dict):
                continue
            code = str(x.get("stockCode") or "")
            if not code:
                continue
            title = str(x.get("title") or "").strip()
            if not title:
                continue
            rating = x.get("emRatingName") or x.get("sRatingName") or x.get("newRatingName") or ""
            info_code = str(x.get("infoCode") or "")
            out.append({
                "report_id": info_code,
                "title": title,
                "stock_code": code,
                "stock_name": str(x.get("stockName") or "").strip(),
                "org_name": str(x.get("orgSName") or x.get("orgName") or "").strip(),
                "author": ResearchReportService._authors(x),
                "publish_date": str(x.get("publishDate") or "")[:10],
                "rating": str(rating),
                "rating_change": str(x.get("emRatingChange") or ""),
                "target_price": ResearchReportService._norm(x.get("indvAimPrice") or x.get("aimPrice")),
                "eps_y1": ResearchReportService._norm(x.get("predictThisYearEps")),
                "eps_y2": ResearchReportService._norm(x.get("predictNextYearEps")),
                "eps_y3": ResearchReportService._norm(x.get("predictThreeYearEps")),
                "summary": str(x.get("summary") or x.get("jiYin") or "")[:200],
                "url": "https://data.eastmoney.com/report/info/" + info_code + ".html",
            })
        return out

    @staticmethod
    def get_reports(code: str = "", page_no: int = 1, limit: int = 20,
                    q_type: str = "0", force: bool = False) -> List[Dict]:
        """获取研报列表，可按股票代码/分析师过滤；带 5 分钟缓存。"""
        key = f"reports_{code}_{page_no}_{q_type}"
        now = time.time()
        if not force and key in ResearchReportService._cache:
            ts, val = ResearchReportService._cache[key]
            if now - ts < ResearchReportService._REPORT_TTL:
                return val[:limit]
        rows = ResearchReportService._fetch_report_page(code=code, page_no=page_no, q_type=q_type,
                                                        page_size=max(limit * 2, 30))
        ResearchReportService._cache[key] = (now, rows)
        # 异步落库（所有获取到的研报元数据均入库，便于回测/复盘）
        if rows:
            threading.Thread(target=lambda: SupabaseStore.save_reports(rows), daemon=True).start()
        return rows[:limit]

    @staticmethod
    def get_latest_reports(limit: int = 20, force: bool = False) -> List[Dict]:
        """最新全市场个股研报速递。"""
        return ResearchReportService.get_reports(code="", limit=limit, force=force)

    @staticmethod
    def get_industry_reports(limit: int = 20, force: bool = False) -> List[Dict]:
        """行业研报速递。"""
        return ResearchReportService.get_reports(code="", limit=limit, q_type="1", force=force)


# ============================================================
# 回测引擎
# ============================================================
# 内置回测策略清单（含中文说明，供前端展示；id 与 BacktestEngine.STRATEGIES 一一对应）
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
LLM_PROVIDERS = [
    {"key": "deepseek", "name": "DeepSeek", "base_url": "https://api.deepseek.com/v1",
     "models": ["deepseek-chat", "deepseek-reasoner"]},
    {"key": "openai", "name": "OpenAI", "base_url": "https://api.openai.com/v1",
     "models": ["gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-4.1", "gpt-4.1-mini"]},
    {"key": "qwen", "name": "通义千问", "base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
     "models": ["qwen3-max", "qwen3-plus", "qwen-plus", "qwen-turbo"]},
    {"key": "zhipu", "name": "智谱GLM", "base_url": "https://open.bigmodel.cn/api/paas/v4",
     "models": ["glm-4.7-flash", "glm-4-plus", "glm-4-air"]},
    {"key": "minimax", "name": "MiniMax", "base_url": "https://api.minimaxi.com/v1",
     "models": ["MiniMax-M2.7", "MiniMax-M2.7-highspeed", "MiniMax-Text-01"]},
    {"key": "siliconflow", "name": "硅基流动(免费)", "base_url": "https://api.siliconflow.cn/v1",
     "models": ["deepseek-chat", "Qwen/Qwen2.5-7B-Instruct", "Qwen/Qwen2.5-14B-Instruct"]},
    {"key": "kimi", "name": "Kimi(Moonshot)", "base_url": "https://api.moonshot.cn/v1",
     "models": ["moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k"]},
    {"key": "openrouter", "name": "OpenRouter", "base_url": "https://openrouter.ai/api/v1",
     "models": ["deepseek/deepseek-chat", "anthropic/claude-3.5-sonnet", "openai/gpt-4o"]},
    {"key": "ollama", "name": "Ollama(本地)", "base_url": "http://localhost:11434/v1",
     "models": ["qwen3:latest", "llama3.1:8b", "deepseek-r1:7b"]},
]

# 服务端启动时读取 LLM env 配置；AI 深研 tab 选择时自动使用。
LLM_DEFAULTS: Dict[str, str] = {
    "base_url": os.environ.get("LLM_BASE_URL", "").strip(),
    "api_key":  os.environ.get("LLM_API_KEY", "").strip(),
    "model":    os.environ.get("LLM_MODEL", "").strip(),
}


def _as_llm_endpoint(base_url: str) -> str:
    """把用户填写的 base url 规范成 /chat/completions 完整端点"""
    url = (base_url or "").strip().rstrip("/")
    if not url:
        return ""
    if url.endswith("/chat/completions"):
        return url
    return url + "/chat/completions"


def fetch_llm_models(base_url: str, api_key: str = "", timeout: float = 15.0) -> List[str]:
    """根据 base_url + api_key 拉取 OpenAI 兼容网关 /v1/models 的模型列表"""
    endpoint = _as_llm_endpoint(base_url)
    if not endpoint:
        return []
    models_url = endpoint.replace("/chat/completions", "").rstrip("/") + "/models"
    headers = {"Authorization": f"Bearer {api_key.strip()}"} if api_key and str(api_key).strip() else {}
    try:
        r = requests.get(models_url, headers=headers, timeout=timeout)
        r.raise_for_status()
        data = r.json()
        items = data.get("data") or []
        ids: List[str] = []
        for it in items:
            if isinstance(it, dict) and it.get("id"):
                ids.append(str(it["id"]))
            elif isinstance(it, str):
                ids.append(it)
        return sorted(set(ids))
    except Exception as e:  # noqa: BLE001
        logger.warning("LLM models fetch failed (%s): %s", models_url, e)
        return []


def _extract_json(text: str) -> Optional[Dict]:
    """从 LLM 输出中稳健提取 JSON 对象（兼容 markdown 围栏与前后杂文本）。"""
    if not text:
        return None
    s = text.strip()
    if s.startswith("```"):
        s = re.sub(r"^```[a-zA-Z]*\s*", "", s)
        s = re.sub(r"\s*```$", "", s)
    start, end = s.find("{"), s.rfind("}")
    if start < 0 or end <= start:
        return None
    try:
        obj = json.loads(s[start:end + 1])
        return obj if isinstance(obj, dict) else None
    except Exception:  # noqa: BLE001
        return None


def call_llm_json(base_url: str, api_key: str, model: str,
                  system_prompt: str, user_prompt: str,
                  timeout: float = 90.0) -> Optional[Dict]:
    """调用 OpenAI 兼容 /chat/completions，要求返回 JSON 对象。失败返回 None，上层兜底走 mock。"""
    endpoint = _as_llm_endpoint(base_url)
    if not endpoint or not (model or "").strip():
        return None
    headers = {"Content-Type": "application/json"}
    if api_key and str(api_key).strip():
        headers["Authorization"] = f"Bearer {api_key.strip()}"
    payload = {
        "model": model.strip(),
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
        "temperature": 0.3,
        "stream": False,
    }
    # response_format=json_object 部分兼容网关不支持：失败后去掉该参数重试。
    for payload0, retry in ((dict(payload, response_format={"type": "json_object"}), True),
                            (payload, False)):
        try:
            r = requests.post(endpoint, json=payload0, headers=headers, timeout=timeout)
            r.raise_for_status()
            data = r.json()
            content = (data.get("choices") or [{}])[0].get("message", {}).get("content") or ""
            obj = _extract_json(content)
            if obj is not None:
                return obj
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM call failed (model=%s endpoint=%s): %s", model, endpoint, e)
            if not retry:
                return None
    return None


# ============================================================
# 多Agent AI分析服务
# ============================================================
class AIAnalysisService:
    """多Agent AI深度投研：优先调用真实LLM（OpenAI兼容接口），失败/无Key时自动降级为本地mock，保证界面可用。"""

    ANALYSTS = [
        ("market_analyst", "市场分析", "K线形态、技术指标、量价关系分析"),
        ("social_analyst", "情绪分析", "社交媒体情绪、散户讨论热度分析"),
        ("news_analyst", "新闻事件分析", "行业新闻、公告、宏观事件影响"),
        ("fundamentals_analyst", "基本面分析", "财报分析、盈利能力、估值水平"),
        ("policy_analyst", "政策分析", "监管政策、产业政策、窗口指导"),
        ("hot_money_analyst", "游资追踪", "龙虎榜、大单流向、主力资金"),
        ("lockup_analyst", "解禁监控", "限售解禁、大股东减持、股权质押"),
    ]

    @staticmethod
    def _load_context(symbol: str) -> Dict:
        """聚合真实行情/指标/研报/新闻/资金流/板块上下文供 LLM 使用（全程容错降级）。"""
        ctx: Dict = {}
        try:
            ctx["info"] = AStockDataService.get_stock_info(symbol)
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context info failed: %s", e)
        try:
            ctx["news"] = NewsRadarService.get_latest_news(10)
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context news failed: %s", e)
        try:
            ctx["reports"] = ResearchReportService.get_reports(
                code=AStockDataService._to_code(symbol), limit=6)
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context reports failed: %s", e)
        try:
            ctx["industry"] = ResearchReportService.get_industry_reports(6)
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context industry failed: %s", e)
        try:
            ctx["fund_flow"] = AStockDataService.get_market_fund_flow()
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context fund_flow failed: %s", e)
        try:
            ctx["sector_heat"] = AStockDataService.get_sector_heat_ths()
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context sector_heat failed: %s", e)
        try:
            ctx["limit_pools"] = AStockDataService.get_limit_pools()
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context limit_pools failed: %s", e)
        try:
            ctx["sector_trends"] = AStockDataService.get_sector_trends(days=10, top=5)
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context sector_trends failed: %s", e)
        try:
            ctx["quote"] = asdict(AStockDataService.get_realtime_quote(symbol))
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context realtime failed: %s", e)
        return ctx

    @staticmethod
    def _format_context(ctx: Dict) -> str:
        """将真实数据上下文压缩为紧凑文本喂给 LLM"""
        parts = []
        info = ctx.get("info") or {}
        quote = info.get("quote") or ctx.get("quote") or {}
        if quote:
            parts.append(
                "【实时行情】%s %s 现价%.2f 涨跌%.2f(%.2f%%) 市盈率%s 市净率%s 市值%.0f亿"
                % (quote.get("symbol", ""), quote.get("name", ""),
                   float(quote.get("price") or 0), float(quote.get("change") or 0),
                   float(quote.get("change_percent") or 0),
                   quote.get("pe") or "-", quote.get("pb") or "-",
                   float(quote.get("market_cap") or 0)))
        ind = info.get("indicators") or {}
        if ind:
            macd = ind.get("macd") or {}
            parts.append(
                "【技术指标】MA5=%s MA10=%s MA20=%s MA60=%s RSI=%s MACD(DIF=%s DEA=%s HIST=%s)"
                % (ind.get("ma5"), ind.get("ma10"), ind.get("ma20"), ind.get("ma60"),
                   ind.get("rsi"), macd.get("dif"), macd.get("dea"), macd.get("hist")))
        kl = info.get("kline") or []
        if kl:
            recent = [f"{k.get('date')}:收{float(k.get('close') or 0)}" for k in kl[-20:]]
            parts.append("【近20日收盘】" + " | ".join(recent))
        news = ctx.get("news") or []
        if news:
            parts.append("【财经新闻】" + " ".join(
                f"[{n.get('source', '')}/{n.get('sentiment', '')}] {n.get('title', '')}"
                for n in news[:10]))
        reports = ctx.get("reports") or []
        if reports:
            parts.append("【个股研报】" + " ".join(
                f"{r.get('org_name', '')}《{r.get('title', '')}》评级:{r.get('rating', '-')}"
                f"目标价{r.get('target_price') or '-'}" for r in reports[:6]))
        industry = ctx.get("industry") or []
        if industry:
            parts.append("【行业研报】" + " ".join(
                f"{i.get('org_name', '')}《{i.get('title', '')}》评级:{i.get('rating', '-')}"
                for i in industry[:4]))
        ff = ctx.get("fund_flow") or {}
        if ff:
            parts.append("【市场资金流】" + json.dumps(ff, ensure_ascii=False)[:400])
        sh = ctx.get("sector_heat") or {}
        if sh:
            top = [f"{k}({v}%)" for k, v in sorted(sh.items(), key=lambda x: float(x[1] or 0), reverse=True)[:8]]
            parts.append("【板块热点TOP8】" + " | ".join(top))
        lp = ctx.get("limit_pools") or {}
        rows = lp.get("data") or lp.get("pools") or []
        if rows:
            body = " ".join(f"{r.get('name', '')}{r.get('code', '')}:{r.get('reason', '')}" for r in rows[:8])
            parts.append("【涨停池】" + body)
        st = ctx.get("sector_trends") or {}
        if st and isinstance(st.get("series"), list):
            trend_body = " ".join(
                f"{s.get('name', '')}近{len(s.get('closes', []) or [])}日由{s.get('closes', [None, None])[0]}→{s.get('closes', [None, None])[-1]}"
                for s in st["series"][:5])
            if trend_body:
                parts.append("【板块趋势】" + trend_body)
        return "\n".join(parts)

    # A股交易约束（TradingAgents-astock 的 trader/PM 必含）
    A_STOCK_RULES = (
        "- T+1 交易制度：当日买入次日才能卖出\n"
        "- 涨跌停制度：主板±10%，创业板/科创板±20%，ST股±5%\n"
        "- 最小交易单位：主板100股（1手），科创板/创业板200股\n"
        "- 交易时间：09:30-11:30 / 13:00-15:00（北京时间）\n"
        "- ST/*ST 表示存在退市风险，需谨慎控制仓位\n"
        "- 北向资金（沪深港通）流入流出及游资（龙虎榜席位）动向是需要关注的信号指标\n"
        "（以上参数仅供技术研究参考，不构成投资建议）"
    )

    _ANALYST_SYSTEM = {
        "market_analyst": (
            "你是A股资深技术分析师。基于给定行情数据输出JSON，字段："
            "{\"score\":0-10,\"verdict\":\"看多/看空/中性\",\"summary\":\"100-180字结论\","
            "\"key_points\":[\"3-5条要点\"],\"confidence\":0.0-1.0,"
            "\"trend\":\"上升趋势/下降趋势/震荡\",\"support\":数,\"resistance\":数,"
            "\"indicators\":{\"RSI\":数,\"MACD\":\"金叉/死叉/粘合\",\"KDJ\":\"超买/超卖/中性\"}}"
            "只输出JSON，不要其它文字"),
        "social_analyst": (
            "你是A股舆情情绪分析师。基于新闻与行业氛围判断散户情绪，输出JSON："
            "{\"score\":0-10,\"verdict\":\"看多/看空/中性\",\"summary\":\"100-180字\","
            "\"key_points\":[\"3-5条\"],\"confidence\":0.0-1.0,"
            "\"sentiment\":\"乐观/谨慎/悲观\",\"discussion_heat\":0-100}只输出JSON"),
        "news_analyst": (
            "你是A股新闻事件分析师，评估新闻对个股与板块的影响与风险，输出JSON："
            "{\"score\":0-10,\"verdict\":\"看多/看空/中性\",\"summary\":\"100-180字\","
            "\"key_points\":[\"3-5条\"],\"confidence\":0.0-1.0,\"event_impact\":\"利好/利空/中性\"}"
            "只输出JSON"),
        "fundamentals_analyst": (
            "你是A股基本面分析师，基于估值、市值与券商研报评级/EPS预测分析，输出JSON："
            "{\"score\":0-10,\"verdict\":\"看多/看空/中性\",\"summary\":\"100-180字\","
            "\"key_points\":[\"3-5条\"],\"confidence\":0.0-1.0,"
            "\"valuation\":\"低估/合理/高估\",\"eps_forecast\":\"一句话\"}只输出JSON"),
        "policy_analyst": (
            "你是A股政策分析师（A股是政策市），基于行业研报与财经新闻评估政策面影响，输出JSON："
            "{\"score\":0-10,\"verdict\":\"看多/看空/中性\",\"summary\":\"100-180字\","
            "\"key_points\":[\"3-5条\"],\"confidence\":0.0-1.0,"
            "\"policy_impact\":\"利好/利空/中性\",\"key_policies\":[\"相关具体政策\"]}只输出JSON"),
        "hot_money_analyst": (
            "你是A股游资资金流分析师，结合市场资金流向与题材热点评估短线资金动向，输出JSON："
            "{\"score\":0-10,\"verdict\":\"看多/看空/中性\",\"summary\":\"100-180字\","
            "\"key_points\":[\"3-5条\"],\"confidence\":0.0-1.0,"
            "\"capital_flow\":\"大幅流入/小幅流入/平衡/小幅流出/大幅流出\","
            "\"dragon_tiger\":\"机构买入/游资介入/无明显异动\"}只输出JSON"),
        "lockup_analyst": (
            "你是A股解禁减持监控分析师（数据有限时基于已有信息合理推断），输出JSON："
            "{\"score\":0-10,\"verdict\":\"看多/看空/中性\",\"summary\":\"100-180字\","
            "\"key_points\":[\"3-5条\"],\"confidence\":0.0-1.0,"
            "\"lockup_risk\":\"高/中/低\",\"major_shareholder_activity\":\"一句话\"}只输出JSON"),
    }

    # 分析师报告保留的字段白名单（防止模型夹带无关字段）
    _REPORT_KEYS = (
        "score", "verdict", "summary", "key_points", "confidence",
        "trend", "support", "resistance", "indicators",
        "sentiment", "discussion_heat", "event_impact",
        "valuation", "eps_forecast", "policy_impact", "key_policies",
        "capital_flow", "dragon_tiger", "lockup_risk", "major_shareholder_activity",
    )

    @staticmethod
    def _mock_report(stage_id: str, name: str, task_id: str) -> Dict:
        """mock 分析师报告（LLM 无配置或调用失败时兜底）"""
        seed = int(hashlib.md5(f"{task_id}_{stage_id}".encode()).hexdigest()[:8], 16)
        rng = np.random.RandomState(seed)
        hint = "模拟数据（未配置模型接口）"
        if stage_id == "market_analyst":
            return {
                "trend": rng.choice(["上升趋势", "下降趋势", "震荡"], p=[0.4, 0.2, 0.4]),
                "support": round(rng.uniform(80, 150), 2),
                "resistance": round(rng.uniform(160, 300), 2),
                "indicators": {"RSI": round(rng.uniform(30, 70), 1), "MACD": rng.choice(["金叉", "死叉", "粘合"]),
                               "KDJ": rng.choice(["超买", "超卖", "中性"])},
                "score": round(rng.uniform(5, 9), 1),
                "verdict": rng.choice(["看多", "看空", "中性"], p=[0.45, 0.2, 0.35]),
                "summary": "基于K线与量价形态的综合分析。" + hint,
                "key_points": [hint],
            }
        if stage_id == "policy_analyst":
            return {
                "policy_impact": rng.choice(["利好", "利空", "中性"], p=[0.35, 0.15, 0.5]),
                "key_policies": ["货币政策调整", "行业监管政策变化"],
                "score": round(rng.uniform(5, 9), 1),
                "verdict": rng.choice(["看多", "看空", "中性"], p=[0.4, 0.15, 0.45]),
                "summary": "基于政策与产业趋势的综合分析。" + hint,
                "key_points": [hint],
            }
        if stage_id == "hot_money_analyst":
            return {
                "capital_flow": rng.choice(["大幅流入", "小幅流入", "平衡", "小幅流出", "大幅流出"]),
                "dragon_tiger": rng.choice(["机构买入", "游资介入", "无明显异动"]),
                "score": round(rng.uniform(4, 9), 1),
                "verdict": rng.choice(["看多", "看空", "中性"], p=[0.35, 0.2, 0.45]),
                "summary": "基于资金流与龙虎榜的综合分析。" + hint,
                "key_points": [hint],
            }
        return {
            "score": round(rng.uniform(5, 9), 1),
            "verdict": rng.choice(["看多", "看空", "中性"], p=[0.4, 0.2, 0.4]),
            "summary": f"{name}完成分析，综合评分{round(rng.uniform(5, 9), 1)}。" + hint,
            "key_points": [f"关键要点{i + 1}" for i in range(3)],
        }

    @staticmethod
    async def _run_analyst(stage_id: str, name: str, desc: str, symbol: str,
                           ctx: Dict, llm_cfg: Optional[Dict], task_id: str) -> Dict:
        """运行单个分析师：优先真实 LLM（OpenAI 兼容），输出非法或无配置时 mock 兜底"""
        cfg = llm_cfg or {}
        if cfg.get("base_url") and cfg.get("model"):
            system = AIAnalysisService._ANALYST_SYSTEM.get(
                stage_id, AIAnalysisService._ANALYST_SYSTEM["news_analyst"])
            user = (f"股票代码：{symbol}\n请以「{name}」（{desc}）的视角，分析以下真实数据并给出专业判断：\n"
                    + AIAnalysisService._format_context(ctx))
            raw = await asyncio.to_thread(
                call_llm_json, cfg.get("base_url"), cfg.get("api_key"), cfg.get("model"),
                system, user)
            if raw:
                report = {k: raw[k] for k in AIAnalysisService._REPORT_KEYS if k in raw}
                if not report.get("summary"):
                    v = report.get("verdict", "中性")
                    report["summary"] = f"{name}完成分析：{v}"
                if "verdict" not in report:
                    report["verdict"] = "中性"
                if "score" not in report:
                    report["score"] = 5.0
                if not report.get("key_points"):
                    report["key_points"] = f"LLM完成{name}分析"
                return report
            logger.warning("analyst %s LLM 无有效JSON，降级mock", stage_id)
        return AIAnalysisService._mock_report(stage_id, name, task_id)

    @staticmethod
    def _report_line(r: Dict) -> str:
        rep = r["report"]
        return (f"- {r['name']}({rep.get('verdict', '中性')}) score={rep.get('score', 0)}: "
                f"{rep.get('summary', '')}")

    @staticmethod
    async def _quality_gate(analysts_reports: Dict, symbol: str,
                            ctx_text: str, call, task_id: str) -> Dict:
        """数据质量门控：TradingAgents Quality Gate，硬规则 + LLM 复核"""
        grades = {}
        notes = []
        fail_markers = ("无法获取", "I cannot retrieve", "I don't have access",
                        "unable to fetch", "工具调用失败", "模拟数据")
        for stage_id, r in analysts_reports.items():
            rep = r["report"]
            text = str(rep.get("summary") or "") + " " + str(rep.get("verdict") or "")
            text += " " + " ".join(str(k) for k in (rep.get("key_points") or []))
            length = len(text.strip())
            fails = sum(1 for m in fail_markers if m in str(rep.get("summary") or ""))
            if length < 20 and fails:
                g, note = "D", "报告过短且含失败信息"
            elif fails:
                g, note = "C", "关键信息缺失/失败提示，建议人工复核"
            elif "score" not in rep or "verdict" not in rep:
                g, note = "B", "缺少结构化评分子段"
            else:
                g, note = "A", f"完整({length}字)"
            grades[stage_id] = g
            notes.append(f"- {r['name']}: [{g}] {note}")
        hard_summary = "\n".join(notes)

        review = ""
        if call is not None:
            system = ("你是A股数据质量复核员。核对 7 位分析师报告的完整性与合理性，"
                      "输出JSON：{\"grade\":\"A/B/C/D\",\"confidence\":\"高/中/低\","
                      "\"suggestion\":\"40-100字建议\"}只输出JSON。")
            raw = await call(system,
                             f"股票代码{symbol}\n检测结果：\n{hard_summary}\n\n分析师摘要：\n"
                             + "\n".join(AIAnalysisService._report_line(r) for r in analysts_reports.values()))
            if raw:
                review = (f"[LLM复核] 等级{raw.get('grade', '-')} 置信度:{raw.get('confidence', '-')} "
                          f"建议:{raw.get('suggestion', '')}")
        gate_summary = ("## 数据质量门控结果\n\n### 硬规则\n" + hard_summary
                        + ("\n\n" + review if review else ""))
        return {"grades": grades, "summary": gate_summary}

    @staticmethod
    async def _debate(analysts_reports: Dict, symbol: str, ctx_text: str,
                      bull_score: float, bear_score: float, bull_pct: float,
                      call, task_id: str) -> Dict:
        """多空研究员辩论：TradingAgents Bull/Bear Researcher，各1轮辩论"""
        reports_text = "\n".join(AIAnalysisService._report_line(r) for r in analysts_reports.values())
        bull_arg = bear_arg = ""

        bull_system = (
            "你是A股看多研究员（Bull Analyst），为建仓该A股构建证据充分的多头观点。"
            "重点：政策加持（专精特新/国家战略行业）、北向资金持续流入、游资接力（连板+成交量确认题材归属）、"
            "估值与成长故事（前向PE/PEG/消化年限）、解禁利空出尽。"
            "必须用具体数值（现价/指标/评分）反击空头要点。只输出JSON："
            "{\"argument\":\"80-160字多头论点\",\"key_catalysts\":[\"3-5条催化\"],\"risk_mitigation\":\"40-80字\"}"
        )
        bull_user = (f"股票代码{symbol}\n{ctx_text}\n\n分析师观点：\n{reports_text}\n"
                     f"多头得分{bull_score:.1f} 空头得分{bear_score:.1f}（多方占比{round(bull_pct*100,1)}%）")
        if call is not None:
            raw = await call(bull_system, bull_user)
            if raw:
                bull_arg = str(raw.get("argument") or "").strip()
        if not bull_arg or not call:
            bulls = [f"{r['name']}:{r['report'].get('summary', '')}"
                     for r in analysts_reports.values() if r["report"].get("verdict") == "看多"]
            bull_arg = ("多头核心论点：" + (" | ".join(bulls) if bulls else "多方证据偏弱")
                        + f"（多方占比{round(bull_pct*100,1)}%）")

        bear_system = (
            "你是A股看空研究员（Bear Analyst），构建证据充分的空头核验观点。"
            "重点：估值消化年限不足、政策不及预期、北向资金撤离、游资撤退（连板/题材熄火）、解禁高峰将至。"
            "必须用具体数值反击多头要点。只输出JSON："
            "{\"argument\":\"80-160字空头论点\",\"key_risks\":[\"3-5条风险\"],\"counter_to_bull\":\"40-80字\"}"
        )
        bear_user = (f"股票代码{symbol}\n{ctx_text}\n\n分析师观点：\n{reports_text}\n"
                     f"多头得分{bull_score:.1f} 空头得分{bear_score:.1f}（空方占比{round((1-bull_pct)*100,1)}%）"
                     f"\n看多研究员最新观点：{bull_arg}")
        if call is not None:
            raw = await call(bear_system, bear_user)
            if raw:
                bear_arg = str(raw.get("argument") or "").strip()
        if not bear_arg or not call:
            bears = [f"{r['name']}:{r['report'].get('summary', '')}"
                     for r in analysts_reports.values() if r["report"].get("verdict") == "看空"]
            bear_arg = ("空头核心论点：" + (" | ".join(bears) if bears else "空方证据偏弱")
                        + f"（空方占比{round((1-bull_pct)*100,1)}%）")

        return {
            "bull_history": [bull_arg] if bull_arg else [],
            "bear_history": [bear_arg] if bear_arg else [],
            "history": (["Bull: " + bull_arg, "Bear: " + bear_arg] if bull_arg and bear_arg
                        else [bull_arg or bear_arg]),
            "bull_score": round(bull_score, 1), "bear_score": round(bear_score, 1),
            "bull_percentage": round(bull_pct * 100, 1),
        }

    @staticmethod
    async def _research_manager(debate: Dict, symbol: str, ctx_text: str,
                                call, task_id: str) -> Dict:
        """研究经理（Research Manager）：把辩论收敛为投资计划（Buy/Overweight/Hold/Underweight/Sell）"""
        history = "\n".join(debate.get("history", []))
        plan = {
            "recommendation": "",
            "rationale": "",
            "strategic_actions": "",
        }
        system = (
            "你是A股研究经理兼辩论主持人。综合多空辩论给出明确的投资计划和行动指引。"
            "评级只取其一：Buy(多头强势)/Overweight(偏多)/Hold(均衡)/Underweight(偏空)/Sell(空方强势)。"
            "注意A股：政策市属性、北向与游资动向、解禁减持。只输出JSON："
            "{\"recommendation\":\"Buy/Overweight/Hold/Underweight/Sell\",\"rationale\":\"80-150字\","
            "\"strategic_actions\":\"60-120字可执行步骤与仓位指引\"}"
        )
        user = (f"股票代码{symbol}\n{ctx_text}\n\nA股特殊约束\n{AIAnalysisService.A_STOCK_RULES}\n\n辩论记录：\n{history}")
        if call is not None:
            raw = await call(system, user)
            if raw:
                rating = str(raw.get("recommendation") or "")[:1].upper()
                full = str(raw.get("recommendation") or "")
                low = full.lower()
                if "buy" in low and "over" not in low:
                    rating = "Buy"
                elif "overweight" in low:
                    rating = "Overweight"
                elif "hold" in low:
                    rating = "Hold"
                elif "underweight" in low:
                    rating = "Underweight"
                elif "sell" in low:
                    rating = "Sell"
                if rating in ("Buy", "Overweight", "Hold", "Underweight", "Sell"):
                    plan["recommendation"] = rating
                plan["rationale"] = str(raw.get("rationale") or "")
                plan["strategic_actions"] = str(raw.get("strategic_actions") or "")

        if not plan["recommendation"]:
            bp = float(debate.get("bull_percentage") or 50)
            if bp >= 65:
                plan["recommendation"] = "Buy"
            elif bp >= 55:
                plan["recommendation"] = "Overweight"
            elif bp <= 35:
                plan["recommendation"] = "Sell"
            elif bp <= 45:
                plan["recommendation"] = "Underweight"
            else:
                plan["recommendation"] = "Hold"
            plan["rationale"] = f"多方占比{debate.get('bull_percentage', 50)}%，依据多空计票给出{plan['recommendation']}评级。"
            plan["strategic_actions"] = "结合多方占比决定仓位：评级越高仓位上限越高，严格控制单票风险。"
        return plan

    @staticmethod
    async def _trader(analysts_reports: Dict, plan: Dict, symbol: str, ctx_text: str,
                      call, task_id: str) -> Dict:
        """交易员（Trader）：把投资计划转成可执行的交易提案（Buy/Hold/Sell + 入场/止损/仓位）"""
        proposal = {"action": "Hold", "reasoning": "", "entry_price": None,
                    "stop_loss": None, "position_sizing": ""}
        reports_text = "\n".join(AIAnalysisService._report_line(r) for r in analysts_reports.values())
        system = (
            "你是专注A股的交易台执行交易员。把研究经理的投资计划转成具体交易提案。"
            "A股限制：T+1、涨跌停（主板±10%、创业科创±20%、ST±5%）、100股（科创/创业200股）。"
            "只输出JSON：{\"action\":\"Buy/Hold/Sell\",\"reasoning\":\"40-100字\","
            "\"entry_price\":\"数字或null\",\"stop_loss\":\"数字或null\",\"position_sizing\":\"百分比仓位\"}"
        )
        user = (f"股票代码{symbol}\n{ctx_text}\n\n{AIAnalysisService.A_STOCK_RULES}\n\n投资计划：\n"
                f"评级：{plan.get('recommendation')}\n理由：{plan.get('rationale')}\n行动：{plan.get('strategic_actions')}\n"
                f"\n分析师观点：\n{reports_text}")
        if call is not None:
            raw = await call(system, user)
            if raw:
                action = str(raw.get("action") or "").strip()
                low = action.lower()
                if "buy" in low:
                    proposal["action"] = "Buy"
                elif "sell" in low:
                    proposal["action"] = "Sell"
                else:
                    proposal["action"] = "Hold"
                proposal["reasoning"] = str(raw.get("reasoning") or "")
                for k in ("entry_price", "stop_loss"):
                    v = raw.get(k)
                    if v not in (None, "", "null"):
                        try:
                            proposal[k] = round(float(v), 2)
                        except (TypeError, ValueError):
                            proposal[k] = None
                proposal["position_sizing"] = str(raw.get("position_sizing") or "")
        if not proposal["reasoning"] and not call:
            plan_rating = plan.get("recommendation", "Hold")
            proposal["action"] = {"Buy": "Buy", "Overweight": "Buy"}.get(plan_rating, "Hold")
            proposal["reasoning"] = f"基于研究经理评级{plan_rating}给出的默认交易方向。"
        return proposal

    @staticmethod
    async def _risk_debate(trader_proposal: Dict, analysts_reports: Dict, symbol: str,
                           ctx_text: str, call, task_id: str) -> Dict:
        """风险位持仓辩论：Aggressive/Conservative/Neutral 三角色，TradingAgents Risk Debate 一轮"""
        reports_text = "\n".join(AIAnalysisService._report_line(r) for r in analysts_reports.values())
        trader_text = (f"交易方向：{trader_proposal.get('action')}；理由：{trader_proposal.get('reasoning')}"
                       f"；入场{trader_proposal.get('entry_price')} 止损{trader_proposal.get('stop_loss')}"
                       f" 仓位{trader_proposal.get('position_sizing')}")
        speakers = ["Aggressive", "Conservative", "Neutral"]
        frames = {
            "Aggressive": {
                "role": "激进风险分析师，主张把握高收益高弹性机会（连板效应、政策市、游资共识、北向双认可），用数据反驳保守与中性观点。",
                "system_key": "aggressive",
            },
            "Conservative": {
                "role": "保守风险分析师，强调回撤控制、解禁减持与估值消化风险，要求更厚安全边际，用数据反驳激进与中性观点。",
                "system_key": "conservative",
            },
            "Neutral": {
                "role": "中性风险分析师，平衡激进与保守，给出折中仓位与风控建议。",
                "system_key": "neutral",
            },
        }
        histories = {k: [] for k in ("aggressive", "conservative", "neutral")}
        last_others = {s: "" for s in speakers}

        for speaker in speakers:
            other_lines = [f"{s}: {last_others[s]}" for s in speakers if s != speaker and last_others[s]]
            system = (
                f"你是在风险辩论环节发言的{['激进', '保守', '中性'][speakers.index(speaker)]}风险分析师。{frames[speaker]['role']} "
                "结合交易提案与分析师报告发言。只输出JSON："
                "{\"stance\":\"看多/谨慎/中性\",\"argument\":\"60-140字\",\"suggested_position\":\"0-100数字\",\"suggested_stop_loss\":\"数字或null\"}"
            )
            user = (f"股票代码{symbol}\n{ctx_text}\n\n{speaker} 当前需回应：\n交易提案：{trader_text}\n"
                    + (("\n其他立场" + " | ".join(other_lines)) if other_lines else "")
                    + f"\n\n分析师观点：\n{reports_text}")
            arg_text = ""
            if call is not None:
                raw = await call(system, user)
                if raw:
                    histories[frames[speaker]["system_key"]].append(str(raw.get("argument") or ""))
                    arg_text = f"{speaker}: {raw.get('argument', '')}"
            if not arg_text:
                arg_text = f"{speaker}: 结合交易提案{trader_text[:60]}给出{'激进看多' if speaker == 'Aggressive' else ('谨慎控制风险' if speaker == 'Conservative' else '中性平衡')}观点。"
                histories[frames[speaker]["system_key"]].append(arg_text)
            last_others[speaker] = arg_text

        return {
            "history": [last_others[s] for s in speakers if last_others[s]],
            "aggressive_history": histories["aggressive"],
            "conservative_history": histories["conservative"],
            "neutral_history": histories["neutral"],
            "trader_reference": trader_text,
        }

    @staticmethod
    async def _portfolio_manager(risk_debate: Dict, plan: Dict, trader_proposal: Dict,
                                 symbol: str, ctx_text: str, past_context: str,
                                 call, task_id: str) -> Dict:
        """投资组合经理（Portfolio Manager）：综合风险辩论给出最终决策（Rating/执行摘要/逻辑/目标价/周期）"""
        decision = {"rating": "", "executive_summary": "", "investment_thesis": "",
                    "price_target": None, "time_horizon": ""}
        risk_history = "\n".join(risk_debate.get("history", []))
        lessons = (f"- 过往投资历史与复盘：\n{past_context}\n" if past_context else "")
        system = (
            "你是A股投资组合经理，综合全部信息给出最终决策。评级只取其一："
            "Buy(强烈建仓)/Overweight(偏多加仓)/Hold(持有)/Underweight(减仓)/Sell(清仓回避)。"
            "必须考虑A股约束与过往教训。只输出JSON："
            "{\"rating\":\"Buy/Overweight/Hold/Underweight/Sell\",\"executive_summary\":\"50-100字执行摘要(入场/仓位/关键风险/持有周期)\","
            "\"investment_thesis\":\"80-160字投资逻辑\",\"price_target\":\"数字或null\",\"time_horizon\":\"3-6个月\"}"
        )
        user = (
            f"股票代码{symbol}\n{ctx_text}\n\n{AIAnalysisService.A_STOCK_RULES}\n{lessons}"
            f"\n研究经理投资计划：{plan.get('recommendation')} {plan.get('rationale')}\n"
            f"\n交易员提案：{trader_proposal.get('action')} {trader_proposal.get('reasoning')} "
            f"仓{trader_proposal.get('position_sizing')}\n\n风险辩论：\n{risk_history}")
        if call is not None:
            raw = await call(system, user)
            if raw:
                low = str(raw.get("rating") or "").lower()
                rating = next((r for r in ("buy", "overweight", "hold", "underweight", "sell") if r in low), "")
                if rating:
                    decision["rating"] = {"buy": "Buy", "overweight": "Overweight", "hold": "Hold",
                                          "underweight": "Underweight", "sell": "Sell"}[rating]
                decision["executive_summary"] = str(raw.get("executive_summary") or "")
                decision["investment_thesis"] = str(raw.get("investment_thesis") or "")
                for k in ("price_target",):
                    v = raw.get(k)
                    if v not in (None, "", "null"):
                        try:
                            decision[k] = round(float(v), 2)
                        except (TypeError, ValueError):
                            decision[k] = None
                decision["time_horizon"] = str(raw.get("time_horizon") or "")
        if not decision["rating"]:
            decision["rating"] = plan.get("recommendation", "Hold")
            decision["executive_summary"] = f"维持研究经理评级{decision['rating']}；参考交易员提案{trader_proposal.get('action', 'Hold')}。"
            decision["investment_thesis"] = "综合多空辩论、交易提案与风险讨论后的审慎结论。"
        return decision

    @staticmethod
    def _parse_rating(text: str or Dict, default: str = "Hold") -> str:
        """从 PM 决策/文本解析 5 档评级"""
        rating = ""
        if isinstance(text, dict):
            rating = text.get("rating") or text.get("recommendation") or ""
        elif text:
            rating = text
        low = str(rating).lower()
        for key, mapped in (("overweight", "Overweight"), ("underweight", "Underweight"),
                            ("buy", "Buy"), ("sell", "Sell"), ("hold", "Hold")):
            if key in low:
                return mapped
        return default

    @staticmethod
    def _memory_load(symbol: str) -> str:
        """读取该标的历史决策记忆（近 5 条）作为 PM 的 past_context。"""
        try:
            with open(QUANT_CACHE_DIR / "ai_memory.json", encoding="utf-8") as f:
                mem = json.load(f)
            entries = mem.get(symbol, [])[-5:]
            if not entries:
                return ""
            return "\n".join(
                f"- {e.get('date', '')}: 评级{e.get('rating', '-')} -> {e.get('outcome', '待验证')} | {e.get('thesis', '')[:120]}" 
                for e in entries)
        except (FileNotFoundError, json.JSONDecodeError, KeyError):
            return ""

    @staticmethod
    def _memory_save(symbol: str, rating: str, thesis: str, signal: str, confidence: float):
        """记录本次决策到记忆文件（便于下次分析复盘）。"""
        try:
            path = QUANT_CACHE_DIR / "ai_memory.json"
            mem: Dict = {}
            try:
                with open(path, encoding="utf-8") as f:
                    mem = json.load(f)
            except (FileNotFoundError, json.JSONDecodeError):
                mem = {}
            entries = mem.setdefault(symbol, [])
            entries.append({
                "date": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                "rating": rating, "signal": signal, "confidence": confidence,
                "thesis": thesis, "outcome": None,
            })
            mem[symbol] = entries[-20:]
            with open(path, "w", encoding="utf-8") as f:
                json.dump(mem, f, ensure_ascii=False, indent=2)
        except Exception as e:  # noqa: BLE001
            logger.warning("ai memory save failed: %s", e)

    @staticmethod
    async def _run_full_pipeline(analysts_reports: Dict, symbol: str, ctx: Dict,
                                 llm_cfg: Optional[Dict], task_id: str) -> Dict:
        """TradingAgents-astock 完整流水线：质量校验 -> 多空辩论 -> 研究经理 -> 交易员        -> 三位风险辩手 -> 组合经理最终决策 -> 信号。各阶段 LLM 失败有降级。"""
        cfg = llm_cfg or {}
        has_llm = bool(cfg.get("base_url")) and bool(cfg.get("model"))

        async def call(system: str, user: str) -> Optional[Dict]:
            if not has_llm:
                return None
            return await asyncio.to_thread(
                call_llm_json, cfg.get("base_url"), cfg.get("api_key"), cfg.get("model"),
                system, user)

        bull_score = sum(r["report"]["score"] for r in analysts_reports.values()
                         if r["report"].get("verdict") == "看多")
        bear_score = sum(r["report"]["score"] for r in analysts_reports.values()
                         if r["report"].get("verdict") == "看空")
        total = bull_score + bear_score or 1
        bull_pct = bull_score / total
        ctx_text = AIAnalysisService._format_context(ctx)

        gate = await AIAnalysisService._quality_gate(
            analysts_reports, symbol, ctx_text, call if has_llm else None, task_id)
        debate = await AIAnalysisService._debate(
            analysts_reports, symbol, ctx_text, bull_score, bear_score, bull_pct,
            call if has_llm else None, task_id)
        plan = await AIAnalysisService._research_manager(
            debate, symbol, ctx_text, call if has_llm else None, task_id)
        trader_proposal = await AIAnalysisService._trader(
            analysts_reports, plan, symbol, ctx_text, call if has_llm else None, task_id)
        risk_debate = await AIAnalysisService._risk_debate(
            trader_proposal, analysts_reports, symbol, ctx_text,
            call if has_llm else None, task_id)
        past_context = AIAnalysisService._memory_load(symbol)
        pm = await AIAnalysisService._portfolio_manager(
            risk_debate, plan, trader_proposal, symbol, ctx_text, past_context,
            call if has_llm else None, task_id)

        return {
            "gate": gate,
            "debate": debate,
            "investment_plan": plan,
            "trader_proposal": trader_proposal,
            "risk_debate": risk_debate,
            "pm_decision": pm,
            "past_context": past_context,
        }

    @staticmethod
    async def analyze_stock(symbol: str, task_id: str,
                            llm_config: Optional[Dict] = None) -> Dict:
        """TradingAgents-astock 完整投研流水线入口。
        7分析师并发产出报告 -> 数据质量门 -> 多空研究员辩论 -> 研究经理投资计划
        -> 交易员交易提案 -> 三位风险辩手（激进/保守/中性） -> 组合经理 5 档评级
        -> 信号与记忆落盘。配置 llm_config（base_url + model）时各阶段用真实
        LLM；未配置或调用失败自动降级为规则/计票推断。"""
        cfg = llm_config or {}
        has_llm = bool(cfg.get("base_url")) and bool(cfg.get("model"))
        ctx: Dict = {}
        if has_llm:
            ctx = await asyncio.to_thread(AIAnalysisService._load_context, symbol)

        reports = await asyncio.gather(*[
            AIAnalysisService._run_analyst(stage_id, name, desc, symbol, ctx, llm_config, task_id)
            for stage_id, name, desc in AIAnalysisService.ANALYSTS
        ])

        analysts_reports = {}
        for (stage_id, name, desc), report in zip(AIAnalysisService.ANALYSTS, reports):
            analysts_reports[stage_id] = {
                "id": stage_id, "name": name, "description": desc,
                "status": "completed", "report": report,
            }

        pipeline = await AIAnalysisService._run_full_pipeline(
            analysts_reports, symbol, ctx, llm_config, task_id)

        gate = pipeline.get("gate") or {}
        debate = pipeline.get("debate") or {}
        plan = pipeline.get("investment_plan") or {}
        trader_proposal = pipeline.get("trader_proposal") or {}
        risk_debate = pipeline.get("risk_debate") or {}
        pm = pipeline.get("pm_decision") or {}
        past_context = pipeline.get("past_context") or ""
        debate_history = debate.get("history") or []

        rating = AIAnalysisService._parse_rating(pm, "Hold")
        if rating in ("Buy", "Overweight"):
            signal_name, direction, score = "BUY", "bull", 85
        elif rating in ("Sell", "Underweight"):
            signal_name, direction, score = "SELL", "bear", 25
        else:
            signal_name, direction, score = "HOLD", "neutral", 55

        bull_pct = debate.get("bull_percentage")
        base_conf = (bull_pct / 100.0) if isinstance(bull_pct, (int, float)) else 0.5
        if rating in ("Buy", "Overweight"):
            conf = 0.5 + base_conf / 2
        elif rating in ("Sell", "Underweight"):
            conf = 0.5 + (1 - base_conf) / 2
        else:
            conf = 0.5
        conf = min(0.95, max(0.3, conf))

        bull_args = debate.get("bull_history") or []
        bear_args = debate.get("bear_history") or []

        reason_parts = [p for p in (pm.get("executive_summary") or "",
                                    pm.get("investment_thesis") or "") if p]
        reason = " | ".join(reason_parts) or plan.get("rationale") or ""
        if not reason and debate_history:
            reason = debate_history[-1]

        final_signal = {
            "signal": signal_name,
            "direction": direction,
            "score": score,
            "confidence": round(conf, 3),
            "reason": reason,
            "bull_argument": bull_args[-1] if bull_args else "",
            "bear_argument": bear_args[-1] if bear_args else "",
            "rating": rating,
            "bull_score": debate.get("bull_score", 0),
            "bear_score": debate.get("bear_score", 0),
            "bull_percentage": debate.get("bull_percentage", 50.0),
        }

        AIAnalysisService._memory_save(
            symbol, rating,
            pm.get("investment_thesis") or plan.get("rationale") or "",
            signal_name, round(conf, 3))

        return {
            "symbol": symbol,
            "task_id": task_id,
            "status": "completed",
            "model": cfg.get("model") or "",
            "llm_used": has_llm,
            "analysts": analysts_reports,
            "debate": debate,
            "final_signal": final_signal,
            "quality_gate": gate,
            "investment_plan": plan,
            "trader_proposal": trader_proposal,
            "risk_debate": risk_debate,
            "pm_decision": pm,
            "signal_rating": rating,
            "confidence": round(conf, 3),
            "memory_context": past_context,
            "completed_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        }


# ============================================================
# 持仓管理
# ============================================================
class PortfolioService:
    """持仓管理服务"""

    PF_FILE = QUANT_CACHE_DIR / "portfolio.json"

    @staticmethod
    def get_portfolio() -> Dict:
        try:
            with open(PortfolioService.PF_FILE, encoding="utf-8") as f:
                return json.load(f)
        except (FileNotFoundError, json.JSONDecodeError):
            return {"holdings": [], "last_refresh": None}

    @staticmethod
    def save_portfolio(data: Dict):
        with open(PortfolioService.PF_FILE, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

    @staticmethod
    def add_holding(holding: Dict) -> Dict:
        data = PortfolioService.get_portfolio()
        holding["id"] = str(uuid.uuid4())[:8]
        holding["created_at"] = datetime.now().strftime("%Y-%m-%d %H:%M")
        data.setdefault("holdings", []).append(holding)
        PortfolioService.save_portfolio(data)
        return holding

    @staticmethod
    def remove_holding(holding_id: str) -> bool:
        data = PortfolioService.get_portfolio()
        data["holdings"] = [h for h in data.get("holdings", []) if h.get("id") != holding_id]
        PortfolioService.save_portfolio(data)
        return True

    @staticmethod
    def get_performance() -> Dict:
        """计算持仓收益"""
        data = PortfolioService.get_portfolio()
        total_cost = sum(h.get("cost_price", 0) * h.get("shares", 0) for h in data.get("holdings", []))
        current_prices = {}
        for h in data.get("holdings", []):
            sym = h.get("symbol", "")
            name = AStockDataService.STOCK_POOL.get(sym, sym)
            quote = AStockDataService.get_realtime_quote(sym, name)
            current_prices[sym] = quote.price

        total_value = sum(h.get("shares", 0) * current_prices.get(h.get("symbol", ""), 0)
                          for h in data.get("holdings", []))
        pnl = total_value - total_cost if total_cost > 0 else 0
        pnl_pct = (pnl / total_cost * 100) if total_cost > 0 else 0

        holdings_detail = []
        for h in data.get("holdings", []):
            sym = h.get("symbol", "")
            name = AStockDataService.STOCK_POOL.get(sym, sym)
            quote = AStockDataService.get_realtime_quote(sym, name)
            p = quote.price - h.get("cost_price", 0)
            holdings_detail.append({
                "id": h.get("id"), "symbol": sym, "name": name,
                "shares": h.get("shares", 0), "cost_price": h.get("cost_price", 0),
                "current_price": quote.price, "pnl": round(p * h.get("shares", 0), 2),
                "pnl_percent": round(p / h.get("cost_price", 1) * 100, 2),
                "market_value": round(quote.price * h.get("shares", 0), 2),
            })

        return {
            "total_value": round(total_value, 2),
            "total_cost": round(total_cost, 2),
            "total_pnl": round(pnl, 2),
            "total_pnl_percent": round(pnl_pct, 2),
            "holdings": holdings_detail,
            "last_refresh": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        }


# ============================================================
# 策略管理
# ============================================================
class StrategyService:
    """量化策略管理"""

    BUILTIN_STRATEGIES = [
        {"id": "bse_smallcap", "name": "北交所小市值", "description": "选择北交所市值最小的N只股票等权配置，定期调仓",
         "category": "多因子", "risk_level": "MEDIUM", "tags": "小市值,北交所,等权"},
        {"id": "grid_okx", "name": "OKX网格交易", "description": "在价格区间内低吸高抛，赚取波动收益",
         "category": "高频", "risk_level": "LOW", "tags": "网格,BTC,ETH,OKX"},
        {"id": "moving_avg", "name": "双均线策略", "description": "MA5上穿MA20金叉买入，死叉卖出",
         "category": "趋势策略", "risk_level": "MEDIUM", "tags": "均线,趋势"},
        {"id": "macd_signal", "name": "MACD信号策略", "description": "MACD金叉买入，死叉卖出，配合成交量确认",
         "category": "趋势策略", "risk_level": "MEDIUM", "tags": "MACD,趋势"},
        {"id": "bollinger", "name": "布林带策略", "description": "价格触及下轨买入，触及上轨卖出",
         "category": "趋势策略", "risk_level": "LOW", "tags": "布林带,均线,回归"},
        {"id": "rsi_meanrev", "name": "RSI均值回归", "description": "RSI超卖时买入，超买时卖出",
         "category": "趋势策略", "risk_level": "LOW", "tags": "RSI,均值回归"},
        {"id": "qmt_auto", "name": "QMT自动化交易", "description": "基于迅投QMT平台的自动化策略接口",
         "category": "实盘", "risk_level": "HIGH", "tags": "QMT,实盘,Python"},
    ]

    @staticmethod
    def list_strategies() -> List[Dict]:
        return StrategyService.BUILTIN_STRATEGIES

    @staticmethod
    def get_strategy(strategy_id: str) -> Optional[Dict]:
        for s in StrategyService.BUILTIN_STRATEGIES:
            if s["id"] == strategy_id:
                return s
        return None


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
app = FastAPI(
    title="Web3 Quant Platform API",
    description="量化交易平台 - 整合TradingAgents多Agent分析、回测引擎、行情数据、持仓管理",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ---- 请求模型 ----
class BacktestRequest(BaseModel):
    strategy_id: str = "bse_smallcap"
    symbols: List[str] = ["sh600519", "sz000858"]
    initial_cash: float = 1000000
    stop_loss: float = -0.10
    max_position: float = 0.05
    days: int = 120
    include_ai: bool = False
    # 策略管理(quant-service/Java)创建的自定义策略：strategy_id 为用户策略 ID
    # code 为该策略代码（用户只能在「策略管理」里维护），name 为策略名称
    code: Optional[str] = None
    name: Optional[str] = None

class ScreenCustomRequest(BaseModel):
    code: str
    top_pool: int = 200
    limit: int = 20
    params: Dict = {}

class AnalyzeRequest(BaseModel):
    symbol: str
    include_ai: bool = True
    llm_config: Optional[Dict] = None

class ModelsRequest(BaseModel):
    base_url: str
    api_key: Optional[str] = ""

class HoldingRequest(BaseModel):
    symbol: str
    name: str
    shares: int
    cost_price: float


# ---- 健康检查 ----
@app.get("/health")
def health():
    return {"ok": True, "service": "quant-py", "version": "1.0.0", "time": datetime.now().isoformat()}


# ---- 行情数据 ----
@app.get("/api/quant/market/quotes")
def get_quotes(limit: int = 0, page: int = 0, page_size: int = 30):
    """获取 A 股行情列表。limit>0 取前 limit 支；page>0 时支持分页（返回 total/page/page_size 切片）。"""
    data = AStockDataService.get_stock_list(limit)
    if page and page > 0:
        start = (page - 1) * page_size
        total = len(data)
        return {"code": 0, "data": data[start:start + page_size], "total": total, "page": page, "page_size": page_size}
    return {"code": 0, "data": data}


@app.get("/api/quant/market/quote/{symbol}")
def get_quote(symbol: str):
    """单只股票实时行情"""
    name = AStockDataService.STOCK_POOL.get(symbol, symbol)
    quote = AStockDataService.get_realtime_quote(symbol, name)
    return {"code": 0, "data": asdict(quote)}


_FUND_HIST_STATE = {"running": False, "total": 0, "done": 0, "cur": "", "errors": [],
                    "started": 0.0, "last_sync_date": "", "results": {}}


class FundHistoryService:
    """个股基本面历史服务：Supabase 落库优先 + 实时兜底 + 写穿缓存。

    目的：① 东财/同花顺对 IP 间歇风控 → 历史数据落库后不再依赖实时抓取；
          ② 每天收盘后定时刷新当日增量；③ 历史数据可供回测/复盘直接从库读。
    数据种类：finance 财务摘要 / holders 股东户数 / margin 两融明细 /
              fundflow 资金流向 / dividend 分红 / blocktrade 大宗交易。
    """

    KINDS = {
        "finance":    {"endpoint": "finance",     "fresh_days": 100},  # 季报，最长间隔约 92 天
        "holders":    {"endpoint": "holders",     "fresh_days": 50},   # 季度披露（披露延迟数周）
        "margin":     {"endpoint": "margin",      "fresh_days": 2},    # 日频
        "fundflow":   {"endpoint": "fund-flow",   "fresh_days": 2},    # 日频
        "dividend":   {"endpoint": "dividend",    "fresh_days": 100},  # 年度/半年度事件
        "blocktrade": {"endpoint": "block-trade", "fresh_days": 2},    # 日频
    }
    _fail: Dict[str, tuple] = {}  # "kind:code" -> (fail_until, error)

    @staticmethod
    def _vibe_url(kind: str) -> str:
        return f"{AStockDataService.VIBE_BASE}/api/{FundHistoryService.KINDS[kind]['endpoint']}"

    @staticmethod
    def fetch_one(kind: str, code: str) -> list:
        """从 Vibe-Research 实时抓一种数据（失败抛异常）。"""
        r = requests.get(FundHistoryService._vibe_url(kind), params={"code": code}, timeout=45)
        r.raise_for_status()
        d = r.json().get("data")
        return d if isinstance(d, list) else []

    @staticmethod
    def _age_days(date_key: str) -> int:
        try:
            return (datetime.now() - datetime.strptime(date_key[:10], "%Y-%m-%d")).days
        except ValueError:
            return 9999

    @staticmethod
    def collect_one(code: str, kinds: Optional[list] = None) -> dict:
        """单股全量采集并写穿落库。返回 {kind: 写入行数}，-1 表示该类抓取失败。"""
        kinds = kinds or list(FundHistoryService.KINDS)
        result = {}
        for kind in kinds:
            try:
                rows = FundHistoryService.fetch_one(kind, code)
                result[kind] = SupabaseStore.save_stock_history(kind, code, rows)
            except Exception as e:  # noqa: BLE001
                result[kind] = -1
                FundHistoryService._fail[f"{kind}:{code}"] = (time.time() + 300, str(e)[:120])
            time.sleep(0.25)
        return result

    @staticmethod
    def read(kind: str, code: str) -> tuple:
        """读优先：库命中且新鲜直接回；命中但过期 → 回旧值 + 后台刷新；缺失 → 实时抓 + 写穿。

        返回 (rows, source)：source ∈ supabase / supabase-refreshing / live / empty。
        """
        fresh_days = FundHistoryService.KINDS[kind]["fresh_days"]
        rows = SupabaseStore.load_stock_history(kind, code)
        if rows:
            latest = (rows[-1].get("date_key") or "")[:10]
            if latest and FundHistoryService._age_days(latest) <= fresh_days:
                return rows, "supabase"
            # 数据过期：先回旧值，后台增量刷新（界面零等待）
            threading.Thread(target=FundHistoryService.collect_one,
                             args=(code, [kind]), daemon=True).start()
            return rows, "supabase-refreshing"
        # 库里没有：实时抓（同步等待），成功即写穿
        try:
            live = FundHistoryService.fetch_one(kind, code)
        except Exception:  # noqa: BLE001
            live = []
        if live:
            threading.Thread(target=SupabaseStore.save_stock_history,
                             args=(kind, code, live), daemon=True).start()
        return live, ("live" if live else "empty")


def _fund_history_worker(codes: list):
    """全池基本面历史采集（后台线程）：每天一次全量刷新 + 一次性历史回填。"""
    _FUND_HIST_STATE.update(running=True, total=len(codes), done=0, cur="",
                            errors=[], started=time.time())
    _FUND_HIST_STATE["last_sync_date"] = datetime.now().strftime("%Y-%m-%d")
    for i, sym in enumerate(codes):
        if not _FUND_HIST_STATE["running"]:
            break
        _FUND_HIST_STATE["cur"] = sym
        code = AStockDataService._to_code(sym)
        try:
            res = FundHistoryService.collect_one(code)
            _FUND_HIST_STATE["results"][code] = res
            bad = [k for k, n in res.items() if n < 0]
            if bad:
                _FUND_HIST_STATE["errors"].append(f"{sym}:{','.join(bad)}")
        except Exception as e:  # noqa: BLE001
            logger.warning("fund-history collect %s fail: %s", sym, e)
            _FUND_HIST_STATE["errors"].append(f"{sym}:{e}")
        _FUND_HIST_STATE["done"] = i + 1
        time.sleep(0.8)
    _FUND_HIST_STATE["running"] = False


def _schedule_fund_history_sync():
    """后台线程：交易日 16:30 后自动刷新全池基本面历史（每天一次，以内存标记防重）。"""
    while True:
        try:
            now = datetime.now()
            today = now.strftime("%Y-%m-%d")
            after_close = now.hour > 16 or (now.hour == 16 and now.minute >= 30)
            if (now.weekday() < 5 and after_close and not _FUND_HIST_STATE["running"]
                    and _FUND_HIST_STATE["last_sync_date"] != today
                    and SupabaseStore.ENABLED):
                codes = list(AStockDataService.STOCK_POOL.keys())
                _FUND_HIST_STATE["last_sync_date"] = today
                threading.Thread(target=_fund_history_worker, args=(codes,), daemon=True).start()
        except Exception as e:  # noqa: BLE001
            logger.warning("fund-history scheduler error: %s", e)
        threading.Event().wait(1800)


@app.get("/api/quant/data/fund-history/status")
def fund_history_status():
    """基本面历史落库状态：表可用性 + 采集任务进度 + 各股各类数据的最新日期。"""
    data = {**_FUND_HIST_STATE, "supabase_enabled": SupabaseStore.ENABLED,
            "table_ready": bool(SupabaseStore.ENABLED) and SupabaseStore.stock_history_ready()}
    if SupabaseStore.ENABLED:
        try:
            rows = SupabaseStore._select(
                SupabaseStore.STOCK_HISTORY_TABLE, "kind,code,date_key",
                {"order": "date_key.desc", "limit": "800"})
            latest = {}
            for r in rows or []:
                key = f"{r['code']}:{r['kind']}"
                latest.setdefault(key, r["date_key"])
            data["coverage"] = latest
        except Exception:  # noqa: BLE001
            data["coverage"] = {}
    return {"code": 0, "data": data}


@app.post("/api/quant/data/fund-history/collect")
def fund_history_collect(symbol: str = Query("all")):
    """手动触发基本面历史采集：symbol=all 全池 / 单股（sh600519 或 600519）。"""
    if not SupabaseStore.ENABLED:
        return {"code": 0, "data": {"enabled": False, "message": "Supabase 未配置，无法落库"}}
    if _FUND_HIST_STATE["running"]:
        return {"code": 0, "data": _FUND_HIST_STATE}
    if symbol == "all":
        codes = list(AStockDataService.STOCK_POOL.keys())
    else:
        codes = [AStockDataService._to_symbol(AStockDataService._to_code(symbol))]
    _FUND_HIST_STATE["last_sync_date"] = datetime.now().strftime("%Y-%m-%d")
    threading.Thread(target=_fund_history_worker, args=(codes,), daemon=True).start()
    return {"code": 0, "data": _FUND_HIST_STATE}


# ---- 个股基本面历史读取端点（Supabase 优先，实时兜底写穿） ----
@app.get("/api/quant/market/finance/{symbol}")
def py_stock_finance(symbol: str):
    """季报财务摘要（同花顺按报告期 8 期，Supabase 持久化）。"""
    code = AStockDataService._to_code(symbol)
    rows, src = FundHistoryService.read("finance", code)
    return {"code": 0, "data": rows or [], "source": src}


@app.get("/api/quant/market/holders/{symbol}")
def py_stock_holders(symbol: str):
    """股东户数历史（东财全量历史，Supabase 持久化）。"""
    code = AStockDataService._to_code(symbol)
    rows, src = FundHistoryService.read("holders", code)
    return {"code": 0, "data": rows or [], "source": src}


@app.get("/api/quant/market/margin/{symbol}")
def py_stock_margin(symbol: str):
    """融资融券明细（东财日级，Supabase 持久化）。"""
    code = AStockDataService._to_code(symbol)
    rows, src = FundHistoryService.read("margin", code)
    # 库中为升序，前端表格期望最新在前
    return {"code": 0, "data": (rows or [])[::-1], "source": src}


@app.get("/api/quant/market/fund-flow/{symbol}")
def py_stock_fund_flow(symbol: str):
    """个股资金流向（东财 120 日主力净流入，Supabase 持久化）。"""
    code = AStockDataService._to_code(symbol)
    rows, src = FundHistoryService.read("fundflow", code)
    return {"code": 0, "data": rows or [], "source": src}


@app.get("/api/quant/market/dividend/{symbol}")
def py_stock_dividend(symbol: str):
    """分红送配历史（Supabase 持久化）。"""
    code = AStockDataService._to_code(symbol)
    rows, src = FundHistoryService.read("dividend", code)
    return {"code": 0, "data": rows or [], "source": src}


@app.get("/api/quant/market/block-trade/{symbol}")
def py_stock_block_trade(symbol: str):
    """大宗交易记录（Supabase 持久化）。"""
    code = AStockDataService._to_code(symbol)
    rows, src = FundHistoryService.read("blocktrade", code)
    return {"code": 0, "data": rows or [], "source": src}


@app.get("/api/quant/market/dragon-tiger/{symbol}")
def py_stock_dragon_tiger(symbol: str, days: int = 30):
    """龙虎榜：代理 Vibe 获取并落库 Supabase（PK trade_date+code）。"""
    code = AStockDataService._to_code(symbol)
    try:
        url = f"{AStockDataService.VIBE_BASE}/api/dragon-tiger"
        r = requests.get(url, params={"code": code, "days": days}, timeout=15)
        r.raise_for_status()
        data = (r.json() or {}).get("data") or {}
        records = data.get("records") or []
        if records:
            latest_date = records[0].get("date") or ""
            save_rows = [{
                "code": code,
                "name": AStockDataService.STOCK_POOL.get(AStockDataService._to_symbol(code), ""),
                "close": None,
                "change_percent": None,
                "buy_amount": None,
                "sell_amount": None,
                "net_amount": float(rec.get("net_buy") or 0),
                "seats": data.get("seats"),
                "reason": rec.get("reason") or "",
            } for rec in records[:1]]  # 仅存最近一条（含席位明细）
            if latest_date:
                threading.Thread(
                    target=lambda: SupabaseStore.save_dragon_tiger(latest_date, save_rows),
                    daemon=True).start()
        return {"code": 0, "data": data, "source": "vibe"}
    except Exception as e:
        logger.warning("dragon-tiger %s failed: %s", code, e)
        return {"code": 0, "data": {"records": [], "seats": {"buy": [], "sell": []},
                                      "institution": {"buy_amt": 0, "sell_amt": 0, "net_amt": 0}},
                "source": "empty"}


# ============================================================
# 同花顺金融数据 API 特色数据端点（HiThink-Tech/Financial-API）
# 付费数据源，数据质量优于免费源；HITHINK_FINANCE_API_KEY 未配置时返回 disabled。
# ============================================================
@app.get("/api/quant/hithink/status")
def py_hithink_status():
    """同花顺数据源连通性状态。"""
    enabled = HithinkClient is not None and HithinkClient.enabled()
    return {"code": 0, "data": {"enabled": enabled, "source": "hithink"}}


@app.get("/api/quant/hithink/quote")
def py_hithink_quote(symbols: str = Query(..., description="逗号分隔 symbol，如 sh600519,sz000001")):
    """同花顺批量行情快照。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    syms = [s.strip() for s in symbols.split(",") if s.strip()]
    rows = HithinkClient.snapshot(syms)
    return {"code": 0, "data": rows or [], "source": "hithink"}


@app.get("/api/quant/hithink/kline/{symbol}")
def py_hithink_kline(symbol: str, days: int = Query(120, ge=1, le=3650), adjust: str = "forward"):
    """同花顺个股历史 K线（日线）。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    rows = HithinkClient.kline(symbol, days=days, adjust=adjust)
    return {"code": 0, "data": rows or [], "source": "hithink"}


@app.get("/api/quant/hithink/dragon-tiger")
def py_hithink_dragon_tiger(trade_date: str = "", board_type: str = "all"):
    """同花顺龙虎榜榜单（全市场，按交易日）。board_type: all/org/hot。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    rows = HithinkClient.dragon_tiger_list(trade_date, board_type)
    return {"code": 0, "data": rows or [], "source": "hithink"}


@app.get("/api/quant/hithink/limit-up")
def py_hithink_limit_up(trade_date: str = ""):
    """同花顺涨停股票池。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    rows = HithinkClient.limit_up_pool(trade_date)
    return {"code": 0, "data": rows or [], "source": "hithink"}


@app.get("/api/quant/hithink/limit-down")
def py_hithink_limit_down(trade_date: str = ""):
    """同花顺跌停股票池。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    rows = HithinkClient.limit_down_pool(trade_date)
    return {"code": 0, "data": rows or [], "source": "hithink"}


@app.get("/api/quant/hithink/limit-ladder")
def py_hithink_limit_ladder():
    """同花顺连板天梯（近 30 日连板梯队矩阵）。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    rows = HithinkClient.limit_up_ladder()
    return {"code": 0, "data": rows or [], "source": "hithink"}


@app.get("/api/quant/hithink/hot-stocks")
def py_hithink_hot_stocks(period: str = "24h"):
    """同花顺 A 股热股榜 Top30。period: 24h / 1h。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    rows = HithinkClient.hot_stock_list(period)
    return {"code": 0, "data": rows or [], "source": "hithink"}


@app.get("/api/quant/hithink/skyrocket")
def py_hithink_skyrocket(period: str = "24h"):
    """同花顺飙升榜 Top30。period: 24h / 1h。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    rows = HithinkClient.skyrocket_list(period)
    return {"code": 0, "data": rows or [], "source": "hithink"}


@app.get("/api/quant/hithink/search")
def py_hithink_search(query: str = Query(..., min_length=1)):
    """同花顺标的检索（按代码/名称）。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    rows = HithinkClient.search_ticker(query)
    return {"code": 0, "data": rows or [], "source": "hithink"}


_BACKFILL_STATE = {"running": False, "total": 0, "done": 0, "cur": "", "errors": [], "started": 0.0}

def _backfill_worker(codes: list, period: str = "daily"):
    today = datetime.now().strftime("%Y%m%d")
    for i, sym in enumerate(codes):
        if not _BACKFILL_STATE["running"]:
            break
        _BACKFILL_STATE["cur"] = sym
        try:
            code = AStockDataService._to_code(sym)
            rows, src = [], "补全(东财qfq)"
            try:
                df = AStockDataService._with_retry(
                    lambda: ak.stock_zh_a_hist(symbol=code, period=period, start_date="19900101",
                                               end_date=today, adjust="qfq"), 2, 1.0)
                if df is not None and len(df):
                    for _, r in df.iterrows():
                        rows.append({
                            "date": str(r["日期"])[:10], "open": float(r["开盘"]), "close": float(r["收盘"]),
                            "high": float(r["最高"]), "low": float(r["最低"]),
                            "volume": float(r.get("成交量") or 0), "turnover": float(r.get("成交额") or 0),
                            "amplitude": float(r.get("振幅") or 0),
                            "change_percent": float(r.get("涨跌幅") or 0),
                        })
            except Exception as em_err:
                logger.warning("backfill %s EM fail: %s", sym, em_err)
            if not rows:
                # 东财风控/失败 → Baostock TCP 全历史兜底（免 HTTP 风控）
                today_iso = f"{today[:4]}-{today[4:6]}-{today[6:]}"
                bs_rows = AStockDataService._baostock_kline(code, period, "1990-01-01", today_iso)
                if bs_rows:
                    rows, src = bs_rows, "补全(baostock)"
            if rows:
                SupabaseStore.save_kline(code, period, rows, src)
        except Exception as e:
            logger.warning("backfill %s fail: %s", sym, e)
            _BACKFILL_STATE["errors"].append(f"{sym}:{e}")
        _BACKFILL_STATE["done"] = i + 1
        time.sleep(1.2)
    _BACKFILL_STATE["running"] = False


@app.post("/api/quant/data/backfill")
def start_backfill(period: str = "daily"):
    """全池股票历史补全（从1990/上市至今），后台线程执行。"""
    if _BACKFILL_STATE["running"]:
        return {"code": 0, "data": _BACKFILL_STATE}
    codes = list(AStockDataService.STOCK_POOL.keys())
    _BACKFILL_STATE.update(running=True, total=len(codes), done=0, cur="", errors=[], started=time.time())
    threading.Thread(target=_backfill_worker, args=(codes, period), daemon=True).start()
    return {"code": 0, "data": _BACKFILL_STATE}


@app.post("/api/quant/data/backfill/{symbol}")
def start_backfill_one(symbol: str, period: str = "daily"):
    """单支股票历史补全"""
    if _BACKFILL_STATE["running"]:
        return {"code": 0, "data": _BACKFILL_STATE}
    code = AStockDataService._to_code(symbol) or symbol.replace("sh", "").replace("sz", "")
    codes = [AStockDataService._to_symbol(code)]
    _BACKFILL_STATE.update(running=True, total=1, done=0, cur="", errors=[], started=time.time())
    threading.Thread(target=_backfill_worker, args=(codes, period), daemon=True).start()
    return {"code": 0, "data": _BACKFILL_STATE}


@app.get("/api/quant/data/backfill/status")
def backfill_status():
    """补全任务状态。"""
    return {"code": 0, "data": _BACKFILL_STATE}


@app.get("/api/quant/market/kline/{symbol}")
def get_kline(symbol: str, period: str = "1d", days: int = 60):
    """K线数据：负缓存防穿透——全部真实源失败的代码短缓存，不再重复打上游"""
    nkey = f"kline:{str(symbol).lower()}:{(period or '1d').lower()}"
    if AStockDataService._neg_get(nkey):
        return {"code": 0, "data": [], "negative": True}
    data = AStockDataService.get_kline(symbol, period, days)
    src0 = data[0].get("source") if data and isinstance(data[0], dict) else ""
    if not data or src0 in ("mock", "分时兜底"):
        # 所有真实数据源都失败（如不存在的代码）：写负缓存并返回空，不向前台输出伪造行情
        AStockDataService._neg_set(nkey)
        return {"code": 0, "data": []}
    return {"code": 0, "data": data}


@app.get("/api/quant/market/sparklines")
def get_sparklines(codes: str = ""):
    """批量股票近12日收盘价走势 (codes=600519,000858,...)"""
    code_list = [c.strip() for c in codes.split(",") if c.strip()]
    data = AStockDataService.get_sparklines_batch(code_list) if code_list else {}
    return {"code": 0, "data": data}


@app.get("/api/quant/market/alerts")
def get_sync_alerts():
    """获取未解决的行情同步告警（供前台 NoticePanel 轮询展示）。"""
    return {"code": 0, "data": SupabaseStore.get_open_alerts(limit=10)}


@app.get("/api/quant/market/search")
def search_stocks(q: str = ""):
    """搜索股票（akshare 全市场快照）"""
    return {"code": 0, "data": AStockDataService.search_stocks(q)}


@app.get("/api/quant/market/info/{symbol}")
def get_stock_info(symbol: str):
    """股票详细信息 + 实时行情。"""
    info = AStockDataService.get_stock_info(symbol)
    return {"code": 0, "data": info}


# ---- 板块数据 ----
@app.get("/api/quant/market/sectors")
def get_sectors():
    """板块热度（同花顺 source, 兜底 mock）"""
    ths = AStockDataService.get_sector_heat_ths()
    if ths:
        return {"code": 0, "data": ths, "source": "同花顺"}
    return {"code": 0, "data": NewsRadarService.get_sector_heat()}


@app.get("/api/quant/market/limit-pools")
def get_limit_pools():
    """涨停/跌停/炸板 + 交易日。"""
    return {"code": 0, "data": AStockDataService.get_limit_pools()}


@app.get("/api/quant/market/board-progress")
def get_board_progress():
    """连板晋级：1板 / 2板 / 3板。"""
    return {"code": 0, "data": AStockDataService.get_board_progress()}


@app.get("/api/quant/market/limit-pool-boards")
def get_limit_pool_boards():
    """涨停池按连板数分层（首板 / 1进2 / ... / 6进7），duanxianxia 风格。"""
    return {"code": 0, "data": AStockDataService.get_limit_pool_by_board()}


@app.get("/api/quant/market/sector-trends")
def get_sector_trends(days: int = 60, top: int = 10):
    """板块涨跌曲线（行业板块近 N 日收盘涨跌幅）。外层 15s 硬超时；失败快速返回空结构。"""
    try:
        data = AStockDataService.get_sector_trends(days=days, top=top)
        if not isinstance(data, dict):
            data = {"dates": [], "series": [], "source": "empty"}
        return {"code": 0, "data": data}
    except Exception as e:  # noqa: BLE001
        logger.warning("sector_trends endpoint failed: %s", e)
        return {"code": 0, "data": {"dates": [], "series": [], "source": "error",
                                    "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}}


@app.get("/api/quant/market/fund-flow")
def get_market_fund_flow():
    """主力净流入流出（板块榜/大盘榜）。"""
    return {"code": 0, "data": AStockDataService.get_market_fund_flow()}


@app.get("/api/quant/market/news")
def get_news(limit: int = 20):
    """财经新闻。"""
    return {"code": 0, "data": NewsRadarService.get_latest_news(limit)}


# ---- eodhd 全球财经新闻（用户指定数据源；A股代码返回空，仅对美股/全球代码有效）----
_EODHD_TOKEN = os.environ.get("EODHD_TOKEN", "6aae8aba0e5001.11785774")
_eodhd_news_cache: Dict[str, tuple] = {}  # key -> (ts, rows)
_EODHD_LOCK = threading.Lock()


@app.get("/api/quant/market/news/global")
def get_global_news(symbol: str = "", limit: int = 30):
    """eodhd 全球新闻：symbol 空则返回全市场要闻；传美股代码如 AAPL（自动补 .US）或 AAPL.US。"""
    sym = (symbol or "").strip().upper()
    if sym and "." not in sym:
        sym = f"{sym}.US"
    key = f"{sym}:{min(max(limit, 1), 100)}"
    with _EODHD_LOCK:
        hit = _eodhd_news_cache.get(key)
        if hit and time.time() - hit[0] < 600:
            return {"code": 0, "data": hit[1], "source": "eodhd-cache"}
    try:
        params = {"api_token": _EODHD_TOKEN, "fmt": "json", "limit": min(max(limit, 1), 100)}
        if sym:  # 空符号不能传 s（eodhd 422），省略则返回全市场要闻
            params["s"] = sym
        r = requests.get("https://eodhd.com/api/news", params=params, timeout=12)
        r.raise_for_status()
        raw = r.json() or []
        rows = [{
            "date": (item.get("date") or "")[:19].replace("T", " "),
            "title": item.get("title") or "",
            "summary": (item.get("content") or "")[:300],
            "url": item.get("link") or "",
            "symbols": item.get("symbols") or [],
            "sentiment": (item.get("sentiment") or {}).get("polarity") if isinstance(item.get("sentiment"), dict) else None,
        } for item in raw if isinstance(item, dict)]
        with _EODHD_LOCK:
            _eodhd_news_cache[key] = (time.time(), rows)
        return {"code": 0, "data": rows, "source": "eodhd"}
    except Exception as e:
        logger.warning("eodhd news %s failed: %s", sym or "ALL", e)
        return {"code": 0, "data": [], "source": "eodhd-error"}


# ---- 券商研报（元数据，合规不下载全文）----
@app.get("/api/quant/research/reports")
def get_research_reports(code: str = "", page_no: int = 1, limit: int = 20, q_type: str = "0"):
    """研报列表：code=股票代码（空则全市场）；qType=0个股/1行业/2策略。    仅返回元数据（标题/机构/分析师/评级/目标价/EPS/原文链接）。"""
    return {"code": 0, "data": ResearchReportService.get_reports(code=code, page_no=page_no,
                                                                 limit=limit, q_type=q_type,
                                                                 force=bool(code))}


@app.get("/api/quant/research/reports/latest")
def get_latest_research_reports(limit: int = 20):
    """最新研报速递（全市场个股研报）。"""
    return {"code": 0, "data": ResearchReportService.get_latest_reports(limit)}


@app.get("/api/quant/research/reports/industry")
def get_industry_research_reports(limit: int = 20):
    """行业研报速递。"""
    return {"code": 0, "data": ResearchReportService.get_industry_reports(limit)}


# ---- 智能荐股（技术面 + 基本面 + 研报覆盖，低位筛选） ----
@app.get("/api/quant/recommend")
def get_recommendations(limit: int = 10, force: bool = False):
    """低位荐股：技术面（52周位置/均线/MACD）+ 基本面 PE/市净率 + 研报覆盖 综合评分。"""
    data = AStockDataService.get_recommendations(limit=limit, force=force)
    return {"code": 0, "data": data}


@app.get("/api/quant/recommend/strategies")
def get_recommend_strategy_list(force: bool = False):
    """多策略推荐：基于已缓存全市场快照的 8 种推荐策略（10分钟缓存，force 强制刷新）。
    返回 [{id,name,desc,items:[{code,name,price,change_pct,score,reasons[]}],disabled,disabled_reason}]"""
    return {"code": 0, "data": RecommendStrategyService.recommend_all(force=force)}


# ---- 策略列表 ----
@app.get("/api/quant/strategies")
def list_strategies():
    """策略列表"""
    return {"code": 0, "data": StrategyService.list_strategies()}


@app.get("/api/quant/strategies/{strategy_id}")
def get_strategy(strategy_id: str):
    """策略详情"""
    s = StrategyService.get_strategy(strategy_id)
    if not s:
        raise HTTPException(404, "策略不存在")
    return {"code": 0, "data": s}


@app.get("/api/quant/strategies/{strategy_id}/screen")
def screen_strategy(strategy_id: str, limit: int = 20, top_pool: int = 80, force: bool = False):
    """内置策略选股：按策略条件从全市场筛选当前值得买入的标的"""
    if not StrategyService.get_strategy(strategy_id):
        raise HTTPException(404, "策略不存在")
    return {"code": 0, "data": StrategyScreenService.screen(strategy_id, limit=limit, top_pool=top_pool, force=force)}


@app.post("/api/quant/screen/custom")
def screen_custom(req: ScreenCustomRequest):
    """自定义 Python 策略选股（沙箱受限执行）：def filter_stock(data)->bool + 可选 def score_stock(data)->float。"""
    pool = StrategyScreenService.build_pool("moving_avg", req.top_pool, req.limit + 10)
    sandbox = CustomScreenSandbox(req.code, req.params)
    result = sandbox.run(pool["items"][: max(req.top_pool, 20)], timeout=6.0)
    result["pool_source"] = pool["source"]
    if "error" in result:
        return {"code": 0, "data": result}
    return {"code": 0, "data": {"items": result["items"][: req.limit], "errors": result.get("errors", []),
                                "pool_size": result["pool_size"], "generated_at": result["generated_at"]}}


# ---- 回测 ----
@app.post("/api/quant/backtest/run")
async def run_backtest(req: BacktestRequest):
    """提交回测任务"""
    task_id = f"bt_{uuid.uuid4().hex[:12]}"
    symbols = req.symbols if req.symbols else ["sh600519"]
    config = {
        "strategy_id": req.strategy_id,
        "initial_cash": req.initial_cash,
        "stop_loss": req.stop_loss,
        "max_position": req.max_position,
        "commission": 0.0003,
        "slippage": 0.001,
        "days": req.days,
        "code": req.code or "",
        "name": req.name or "",
    }
    asyncio.create_task(run_backtest_task(task_id, req.strategy_id, symbols, config))
    return {"code": 0, "data": {"task_id": task_id, "status": "pending", "message": "回测任务已提交",
                                "strategies": BACKTEST_STRATEGY_META}}


@app.get("/api/quant/backtest/status/{task_id}")
def get_backtest_status(task_id: str):
    """查询回测任务状态。"""
    task = _task_store.get(task_id)
    if not task:
        return {"code": 0, "data": {"task_id": task_id, "status": "not_found"}}
    return {
        "code": 0, "data": {
            "task_id": task.task_id, "symbol": task.symbol,
            "status": task.status.value, "progress": task.progress,
            "current_stage": task.current_stage,
            "result": task.result, "error": task.error,
            "created_at": task.created_at, "completed_at": task.completed_at,
        }
    }


@app.get("/api/quant/backtest/result/{task_id}")
def get_backtest_result(task_id: str):
    """获取回测结果"""
    result = _backtest_store.get(task_id)
    if not result:
        return {"code": 404, "message": "回测结果不存在或任务已完成"}
    return {"code": 0, "data": asdict(result)}


@app.post("/api/quant/backtest/quick")
async def quick_backtest(req: BacktestRequest):
    """快速回测（同步等待完成）。"""
    symbols = req.symbols if req.symbols else ["sh600519"]
    config = {
        "strategy_id": req.strategy_id,
        "initial_cash": req.initial_cash,
        "stop_loss": req.stop_loss,
        "max_position": req.max_position,
        "commission": 0.0003,
        "slippage": 0.001,
        "days": req.days,
    }
    price_data = {}
    for sym in symbols:
        price_data[sym] = AStockDataService.get_kline(sym, days=config["days"])

    engine = BacktestEngine(config)
    metrics = engine.run(req.strategy_id, symbols, price_data)

    # 同时做AI分析
    ai_result = {}
    if req.include_ai:
        tid = f"ai_{uuid.uuid4().hex[:8]}"
        ai_result = await AIAnalysisService.analyze_stock(symbols[0], tid)

    return {"code": 0, "data": {"backtest": metrics, "ai_analysis": ai_result}}


# ---- AI分析 ----
@app.get("/api/quant/ai/llm-providers")
def get_llm_providers():
    """预置 OpenAI 兼容模型网关列表（前台下拉填充 base_url / 模型，可编辑）。"""
    return {"code": 0, "data": LLM_PROVIDERS}


@app.post("/api/quant/ai/models")
def ai_models(req: ModelsRequest):
    """根据 base_url + api_key 实时拉取网关的模型列表（v1/models）。"""
    models = fetch_llm_models(req.base_url, req.api_key or "")
    return {"code": 0, "data": models}


@app.post("/api/quant/ai/analyze")
async def ai_analyze(req: AnalyzeRequest):
    """AI多Agent深度投研（7 分析师），配置 llm_config 时走真实LLM，否则 mock。"""
    task_id = f"ai_{uuid.uuid4().hex[:12]}"
    llm_config = req.llm_config
    if not llm_config and LLM_DEFAULTS.get("base_url") and LLM_DEFAULTS.get("model"):
        llm_config = LLM_DEFAULTS
    result = await AIAnalysisService.analyze_stock(req.symbol, task_id, llm_config)
    return {"code": 0, "data": result}


@app.get("/api/quant/ai/llm-status")
def llm_status():
    """服务端启动的 LLM 配置状态（不暴露 api_key）。"""
    cfg = LLM_DEFAULTS
    configured = bool(cfg.get("base_url") and cfg.get("model"))
    provider = ""
    if configured:
        base = cfg["base_url"].rstrip("/")
        for p in LLM_PROVIDERS:
            if p["base_url"].rstrip("/") in base or base in p["base_url"].rstrip("/"):
                provider = p["name"]
                break
    return {"code": 0, "data": {
        "configured": configured,
        "base_url": cfg.get("base_url", ""),
        "model": cfg.get("model", ""),
        "provider": provider or ("自定义网关" if configured else ""),
        "has_api_key": bool(cfg.get("api_key")),
    }}


@app.get("/api/quant/ai/signal/{symbol}")
def get_signal(symbol: str):
    """主动获取交易信号"""
    name = AStockDataService.STOCK_POOL.get(symbol, symbol)
    quote = AStockDataService.get_realtime_quote(symbol, name)
    rng = np.random.RandomState(int(hashlib.md5(symbol.encode()).hexdigest()[:8], 16))
    signal = rng.choice(["BUY", "SELL", "HOLD"], p=[0.35, 0.15, 0.5])
    confidence = round(rng.uniform(0.55, 0.92), 2)
    reasons = {
        "BUY": "资金面与情绪面共振，多个分析师看多",
        "SELL": "风险信号增多，建议减仓离场",
        "HOLD": "多空分歧，等待更明确信号",
    }
    return {
        "code": 0, "data": {
            "symbol": symbol, "name": name, "price": quote.price,
            "signal": signal, "confidence": confidence,
            "reason": reasons[signal],
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        }
    }


# ---- 持仓管理 ----
@app.get("/api/quant/portfolio")
def get_portfolio():
    """获取持仓及盈亏"""
    perf = PortfolioService.get_performance()
    return {"code": 0, "data": perf}


@app.post("/api/quant/portfolio/holding")
def add_holding(req: HoldingRequest):
    """添加持仓"""
    h = PortfolioService.add_holding(asdict(req))
    return {"code": 0, "data": h}


@app.delete("/api/quant/portfolio/holding/{holding_id}")
def delete_holding(holding_id: str):
    """删除持仓"""
    PortfolioService.remove_holding(holding_id)
    return {"code": 0, "message": "已删除"}


@app.post("/api/quant/portfolio/refresh")
def refresh_portfolio():
    """刷新持仓盈亏"""
    perf = PortfolioService.get_performance()
    return {"code": 0, "data": perf}


# ---- 数据看板 ----
_dash_cache: List = [0, None]

@app.get("/api/quant/dashboard")
def get_dashboard():
    """量化数据看板（含涨停/跌停池、板块曲线、资金流、晋级数等）。"""
    now = time.time()
    if now - _dash_cache[0] < 30 and _dash_cache[1] is not None:
        return _dash_cache[1]
    try:
        pools = AStockDataService.get_limit_pools()
    except Exception:
        pools = {"date": "", "prev_date": "", "limit_up": [], "limit_down": [], "zhaban": []}
    quotes = AStockDataService.get_stock_list()
    news = NewsRadarService.get_latest_news(5)
    sectors = NewsRadarService.get_sector_heat()
    strategies = StrategyService.list_strategies()

    up = sum(1 for q in quotes if q["change_percent"] > 0)
    down = sum(1 for q in quotes if q["change_percent"] < 0)

    result = {
        "code": 0, "data": {
            "market": {"total": len(quotes), "up": up, "down": down, "updated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")},
            "hot_stocks": sorted(quotes, key=lambda x: abs(x["change_percent"]), reverse=True)[:8],
            "sectors": sectors,
            "latest_news": news,
            "strategies": strategies,
            "limit_pools": pools,
            "board_progress": AStockDataService.get_board_progress(),
            "sector_trends": AStockDataService.get_sector_trends(days=30, top=8),
            "fund_flow": AStockDataService.get_market_fund_flow(),
            "research_reports": ResearchReportService.get_latest_reports(10),
        }
    }
    _dash_cache[0] = time.time()
    _dash_cache[1] = result
    return result


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


@app.get("/api/quant/dashboard/snapshot")
def get_bigscreen_snapshot(market: str = "a"):
    """监控大屏快照（真实 A 股行情驱动）。market 预留全球扩展，当前仅支持 a。"""
    if market != BigScreenService.MARKET:
        return {"code": 1, "message": f"暂不支持市场 {market}，当前仅 A 股"}
    try:
        return {"code": 0, "data": BigScreenService.snapshot()}
    except Exception as e:  # noqa: BLE001
        logger.error("bigscreen snapshot failed: %s", e)
        return {"code": 1, "message": f"快照构建失败: {e}"}


# ---- QMT接口 ----
@app.get("/api/quant/qmt/status")
def qmt_status():
    """QMT连接状态（模拟）。"""
    """QMT连接状态（模拟）"""
    return {"code": 0, "data": {"connected": False, "account": "", "available": True, "message": "QMT未连接，请先启动QMT客户端"}}

@app.post("/api/quant/qmt/connect")
def qmt_connect(req: Dict = {}):
    """连接QMT（模拟）"""
    return {"code": 0, "data": {"connected": True, "account": "sim_001", "balance": 1000000, "message": "模拟连接成功"}}


# ============================================================
# 辅助函数
# ============================================================

def _daily_incremental_sync():
    """每日增量同步：全市场快照 + 板块走势落库（每天收盘后调度跑，避免重复拉全量）。"""
    try:
        today = datetime.now().strftime("%Y-%m-%d")
        logger.info("daily incremental sync start: %s", today)
        errors: list[str] = []
        # 1) 全市场快照（异步落库在 _load_all_snapshot 内）
        df, src = AStockDataService._fetch_snapshot_once()
        snap_rows = 0
        if df is not None and len(df):
            snap_rows = len(df)
            if SupabaseStore.save_snapshot(today, df, src):
                SupabaseStore.update_count(today, "snapshot", snap_rows, "ok", f"src={src}")
            else:
                SupabaseStore.update_count(today, "snapshot", snap_rows, "fail", "save_snapshot returns False")
                errors.append("snapshot落库失败")
        else:
            errors.append("snapshot拉取失败")
        # 2) 板块走势：抓取后同步落库（保证写入完成，重启不丢）
        sect_rows = 0
        try:
            sect = AStockDataService.get_sector_trends(days=30, top=0, force=True)
            if sect and sect.get("series"):
                sect_rows = len(sect["series"])
                AStockDataService._save_sector_trends(sect)
                SupabaseStore.update_count(today, "sector", sect_rows, "ok", f"src={sect.get('source') or ''}")
        except Exception as e:  # noqa: BLE001
            logger.warning("incremental sector sync fail: %s", e)
            SupabaseStore.update_count(today, "sector", sect_rows, "fail", str(e)[:300])
            errors.append(f"sector失败: {e}")
        # 3) 告警
        if errors:
            SupabaseStore.save_alert(today, " \n".join(errors), "error")
            SupabaseStore.log_sync("daily", today, "fail", " \n".join(errors))
        else:
            SupabaseStore.resolve_alerts(today)
            SupabaseStore.log_sync("daily", today, "ok")
        logger.info("daily incremental sync done: %s (snapshot=%s sector=%s) errors=%d",
                    today, snap_rows, sect_rows, len(errors))
    except Exception as e:  # noqa: BLE001
        logger.warning("daily incremental sync failed: %s", e)
        SupabaseStore.save_alert(datetime.now().strftime("%Y-%m-%d"), str(e)[:500], "error")
        SupabaseStore.log_sync("daily", datetime.now().strftime("%Y-%m-%d"), "fail", str(e))
    # 增量同步完成后，后台刷新全池最近 10 日 K 线（不阻塞）
    if not _DAILY_KLINE_STATE["running"]:
        codes = list(AStockDataService.STOCK_POOL.keys())
        _DAILY_KLINE_STATE.update(running=True, total=len(codes), done=0, saved=0, errors=[])
        threading.Thread(target=_daily_kline_refresh_worker, args=(codes,), daemon=True).start()


# 每日 K 线刷新状态
_DAILY_KLINE_STATE = {"running": False, "total": 0, "done": 0, "saved": 0, "errors": []}


def _daily_kline_refresh_worker(codes: list):
    """每日收盘后刷新全池股票最近 10 个交易日的日 K 线（增量 upsert 到 Supabase）。"""
    global _DAILY_KLINE_STATE
    today = datetime.now().strftime("%Y%m%d")
    start_date = (datetime.now() - timedelta(days=20)).strftime("%Y%m%d")
    saved = 0
    for i, sym in enumerate(codes):
        if not _DAILY_KLINE_STATE["running"]:
            break
        try:
            code = AStockDataService._to_code(sym)
            rows = []
            try:
                df = AStockDataService._with_retry(
                    lambda: ak.stock_zh_a_hist(symbol=code, period="daily",
                                               start_date=start_date, end_date=today, adjust="qfq"),
                    2, 0.8)
                if df is not None and len(df):
                    for _, r in df.iterrows():
                        rows.append({
                            "date": str(r["日期"])[:10], "open": float(r["开盘"]),
                            "close": float(r["收盘"]), "high": float(r["最高"]),
                            "low": float(r["最低"]), "volume": float(r.get("成交量") or 0),
                            "turnover": float(r.get("成交额") or 0),
                            "amplitude": float(r.get("振幅") or 0),
                            "change_percent": float(r.get("涨跌幅") or 0),
                        })
            except Exception:
                pass
            if not rows:
                today_iso = f"{today[:4]}-{today[4:6]}-{today[6:]}"
                start_iso = f"{start_date[:4]}-{start_date[4:6]}-{start_date[6:]}"
                bs_rows = AStockDataService._baostock_kline(code, "daily", start_iso, today_iso)
                if bs_rows:
                    rows = bs_rows
            if rows and SupabaseStore.save_kline(code, "daily", rows, "daily-sync"):
                saved += len(rows)
        except Exception as e:  # noqa: BLE001
            logger.warning("daily kline refresh %s fail: %s", sym, e)
            _DAILY_KLINE_STATE["errors"].append(f"{sym}:{e}")
        _DAILY_KLINE_STATE["done"] = i + 1
        time.sleep(0.5)
    _DAILY_KLINE_STATE["running"] = False
    _DAILY_KLINE_STATE["saved"] = saved
    SupabaseStore.log_sync("daily-kline", datetime.now().strftime("%Y-%m-%d"), "ok",
                           f"refreshed {saved} rows for {len(codes)} stocks")
    logger.info("daily kline refresh done: %s rows saved", saved)


def _backfill_sector_history(days: int = 120):
    """板块历史回填：一次性把近 days 日全部板块走势写入 Supabase。"""
    try:
        logger.info("sector history backfill start: days=%s", days)
        sect = AStockDataService.get_sector_trends(days=max(days, 30), top=0, force=True)
        if not sect or not sect.get("series"):
            logger.warning("backfill sector empty")
            return 0
        AStockDataService._save_sector_trends(sect)
        n = len(sect.get("series") or [])
        SupabaseStore.log_sync("backfill_sector", datetime.now().strftime("%Y-%m-%d"), "ok",
                               f"series={n} days={days}")
        logger.info("sector history backfill done: %s series", n)
        return n
    except Exception as e:  # noqa: BLE001
        logger.warning("sector backfill failed: %s", e)
        return 0

def _schedule_daily_sync():
    """后台线程：每 30 分钟检查一次，交易日 15:30 后且当日尚未落库（以库中 quant_daily_count 为准，    进程重启不丢）时执行增量；基准为 8:30 起的任意时刻，避免重复同步。"""
    while True:
        now = datetime.now()
        today = now.strftime("%Y-%m-%d")
        is_trade_day = now.weekday() < 5
        after_close = now.hour > 15 or (now.hour == 15 and now.minute >= 30)
        if is_trade_day and after_close and not SupabaseStore.day_synced(today):
            _daily_incremental_sync()
        threading.Event().wait(1800)


@app.on_event("startup")
def _warm_snapshot_cache():
    """后台预热全市场行情缓存，避免首页接口等待抓取。"""
    threading.Thread(target=lambda: AStockDataService._load_all_snapshot(), daemon=True).start()
    # 预热监控大屏快照（净值历史/基准指数/持仓成本），避免首次打开大屏等待
    threading.Thread(target=lambda: BigScreenService.snapshot(), daemon=True).start()
    # 每日增量同步线程（Supabase 落库）
    threading.Thread(target=_schedule_daily_sync, daemon=True).start()
    # 基本面历史：定时刷新线程 + 启动即全量回填（历史落库，规避上游间歇风控）
    threading.Thread(target=_schedule_fund_history_sync, daemon=True).start()

    def _initial_fund_backfill():
        threading.Event().wait(20)  # 等待行情预热与 Vibe 服务就绪
        if not SupabaseStore.ENABLED:
            return
        # 表就绪且任一池内股缺少资金流历史 → 视为首次启动，做一次全量回填
        try:
            probe = SupabaseStore.load_stock_history("fundflow", AStockDataService._to_code("sh600519"))
        except Exception:  # noqa: BLE001
            probe = None
        if probe is not None and not probe and not _FUND_HIST_STATE["running"]:
            logger.info("首次启动：开始全池基本面历史回填（财务/股东/两融/资金流/分红/大宗）")
            _fund_history_worker(list(AStockDataService.STOCK_POOL.keys()))

    threading.Thread(target=_initial_fund_backfill, daemon=True).start()


# ---- 增量同步接口 ----
@app.post("/api/quant/market/sync")
def manual_incremental_sync(force: bool = Query(False), backfill: bool = Query(False),
                            backfill_days: int = Query(120)):
    """手动触发同步：当日全市场快照 + 板块走势落库；backfill=true 时回填 backfill_days 日板块历史"""
    if not SupabaseStore.ENABLED:
        return {"code": 0, "data": {"enabled": False, "message": "Supabase 未配置 SUPABASE_URL / SUPABASE_SERVICE_KEY"}}
    try:
        today = datetime.now().strftime("%Y-%m-%d")
        df, src = AStockDataService._fetch_snapshot_once()
        snap_ok = False
        if df is not None:
            snap_ok = SupabaseStore.save_snapshot(today, df, src)
        SupabaseStore.update_count(
            today, "snapshot", int(len(df)) if df is not None else 0,
            "ok" if snap_ok else "fail", f"src={src}")
        sect = AStockDataService.get_sector_trends(days=30, top=0, force=True)
        sect_saved = 0
        if sect and sect.get("series"):
            sect_saved = len(sect["series"])
            AStockDataService._save_sector_trends(sect)
            SupabaseStore.update_count(today, "sector", sect_saved, "ok", f"src={sect.get('source') or ''}")
        resp = {
            "enabled": True, "date": today,
            "snapshot_saved": bool(snap_ok),
            "snapshot_rows": int(len(df)) if df is not None else 0,
            "sector_series": sect_saved,
        }
        if backfill:
            n = _backfill_sector_history(max(backfill_days, 30))
            resp["backfill_days"] = max(backfill_days, 30)
            resp["backfill_series"] = n
        if snap_ok or sect_saved:
            SupabaseStore.resolve_alerts(today)
        return {"code": 0, "data": resp}
    except Exception as e:
        logger.warning("manual sync failed: %s", e)
        SupabaseStore.save_alert(datetime.now().strftime("%Y-%m-%d"), f"手动同步失败: {e}", "error")
        return {"code": 0, "data": {"enabled": True, "error": str(e)}}


# ============================================================
# 用户持仓管理（user_holdings 表，MySQL）
# JWT 鉴权：从 Authorization 头解析 userId / username / role
# 普通用户只能看自己的持仓，管理员可查看所有用户
# ============================================================

import jwt as _pyjwt
import pymysql as _pymysql

def _get_jwt_claims(request: Request) -> dict:
    """从 FastAPI Request 的 Authorization 头解析 JWT claims。失败抛 HTTPException(401)。"""
    auth = request.headers.get("Authorization", "")
    if not auth.startswith("Bearer "):
        raise HTTPException(401, "未登录")
    token = auth[7:]
    secret = os.environ.get("JWT_SECRET", "")
    if not secret:
        raise HTTPException(500, "JWT_SECRET 未配置")
    try:
        return _pyjwt.decode(token, secret, algorithms=["HS256"])
    except Exception:
        raise HTTPException(401, "令牌已过期或无效")

def _holdings_conn():
    """获取 MySQL 连接（web3_portal 库）"""
    return _pymysql.connect(
        host=os.environ.get("MYSQL_HOST", "127.0.0.1"),
        port=int(os.environ.get("MYSQL_PORT", "3306")),
        user=os.environ.get("MYSQL_USERNAME", "root"),
        password=os.environ.get("MYSQL_PASSWORD", "root"),
        database=os.environ.get("MYSQL_DATABASE", "web3_portal"),
        charset="utf8mb4",
        cursorclass=_pymysql.cursors.DictCursor,
    )

def _row_to_dict(row: dict) -> dict:
    """将 MySQL 行转为前端需要的格式"""
    return {
        "id": row["id"],
        "user_id": row["user_id"],
        "username": row.get("username", ""),
        "stock_code": row["stock_code"],
        "stock_name": row.get("stock_name", ""),
        "shares": row["shares"],
        "cost_price": float(row["cost_price"]) if row.get("cost_price") else 0,
        "buy_date": str(row["buy_date"]) if row.get("buy_date") else None,
        "notes": row.get("notes", ""),
        "created_at": str(row["created_at"]) if row.get("created_at") else "",
        "updated_at": str(row["updated_at"]) if row.get("updated_at") else "",
    }

@app.get("/api/quant/my-holdings")
def get_my_holdings(request: Request):
    """获取当前用户的持仓列表"""
    claims = _get_jwt_claims(request)
    user_id = claims.get("userId")
    is_admin = "ROLE_ADMIN" in (claims.get("authorities", "") or "")
    username = claims.get("username", "")

    conn = _holdings_conn()
    try:
        cur = conn.cursor()
        if is_admin:
            # 管理员：查看所有用户持仓
            cur.execute("SELECT * FROM user_holdings ORDER BY user_id, stock_code")
        else:
            cur.execute("SELECT * FROM user_holdings WHERE user_id = %s ORDER BY stock_code", (user_id,))
        rows = cur.fetchall()
        items = [_row_to_dict(r) for r in rows]
        return {"code": 0, "data": {"holdings": items, "is_admin": is_admin, "username": username}}
    except Exception as e:
        logger.warning("get_my_holdings failed: %s", e)
        raise HTTPException(500, str(e))
    finally:
        conn.close()

@app.post("/api/quant/my-holdings")
def save_holding(request: Request, body: dict = Body(...)):
    """新增或更新持仓（同一 user_id + stock_code 则 upsert）"""
    claims = _get_jwt_claims(request)
    user_id = claims.get("userId")
    username = claims.get("username", "")

    code = str(body.get("stock_code", "")).strip()
    if not code:
        raise HTTPException(400, "股票代码不能为空")

    name = str(body.get("stock_name", "")).strip()
    shares = int(body.get("shares", 0) or 0)
    cost = float(body.get("cost_price", 0) or 0)
    buy_date = body.get("buy_date")
    notes = str(body.get("notes", "")).strip()

    conn = _holdings_conn()
    try:
        cur = conn.cursor()
        cur.execute("""
            INSERT INTO user_holdings (user_id, username, stock_code, stock_name, shares, cost_price, buy_date, notes)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE
                stock_name = VALUES(stock_name),
                shares = VALUES(shares),
                cost_price = VALUES(cost_price),
                buy_date = VALUES(buy_date),
                notes = VALUES(notes)
        """, (user_id, username, code, name, shares, cost, buy_date, notes))
        conn.commit()
        return {"code": 0, "data": {"ok": True}}
    except Exception as e:
        logger.warning("save_holding failed: %s", e)
        raise HTTPException(500, str(e))
    finally:
        conn.close()

@app.delete("/api/quant/my-holdings/{holding_id}")
def delete_holding(request: Request, holding_id: int):
    """删除一条持仓（只能删自己的）"""
    claims = _get_jwt_claims(request)
    user_id = claims.get("userId")
    is_admin = "ROLE_ADMIN" in (claims.get("authorities", "") or "")

    conn = _holdings_conn()
    try:
        cur = conn.cursor()
        if is_admin:
            cur.execute("DELETE FROM user_holdings WHERE id = %s", (holding_id,))
        else:
            cur.execute("DELETE FROM user_holdings WHERE id = %s AND user_id = %s", (holding_id, user_id))
        conn.commit()
        if cur.rowcount == 0:
            raise HTTPException(404, "持仓不存在或无权删除")
        return {"code": 0, "data": {"ok": True}}
    except HTTPException:
        raise
    except Exception as e:
        logger.warning("delete_holding failed: %s", e)
        raise HTTPException(500, str(e))
    finally:
        conn.close()


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=9006)
