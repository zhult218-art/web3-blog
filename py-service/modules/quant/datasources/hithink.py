# -*- coding: utf-8 -*-
"""
HithinkClient —— 同花顺金融数据 API（HiThink-Tech/Financial-API，fuyao 渠道）封装
文档：https://fuyao.aicubes.cn/docs/api-reference/overview/
Base URL: https://fuyao.aicubes.cn
鉴权：Header X-api-key，环境变量 HITHINK_FINANCE_API_KEY

能力：A股行情快照、历史K线、龙虎榜、涨跌停池、连板天梯、热股榜、飙升榜、异动原因。
所有响应统一信封 {code, message, request_id, data}，code=0 为成功。
"""
import os
import time
import logging
import threading
from typing import Optional, List, Dict, Any

import requests

logger = logging.getLogger("hithink")

_BASE = os.environ.get("HITHINK_BASE_URL", "https://fuyao.aicubes.cn")
# 注意：不在模块加载时缓存 key，避免 import 早于 .env 加载导致读不到。
# 每次请求时从 os.environ 实时读取。


def _api_key() -> str:
    return os.environ.get("HITHINK_FINANCE_API_KEY", "")

# 缓存：避免短时间内重复请求（行情 10s、K线 5min、榜单 60s）
_cache: Dict[str, tuple] = {}
_cache_lock = threading.Lock()


def _cache_get(key: str):
    rec = _cache.get(key)
    if rec and time.time() - rec[0] < rec[2]:
        return rec[1]
    return None


def _cache_set(key: str, value, ttl: float):
    with _cache_lock:
        _cache[key] = (time.time(), value, ttl)


def _to_thscode(symbol: str) -> str:
    """把内部 symbol（sh600519 / 600519）规整为同花顺 thscode（600519.SH）。"""
    s = str(symbol or "").strip().lower()
    if s.startswith(("sh", "sz", "bj")):
        code, prefix = s[2:], s[:2]
    else:
        code = s
        prefix = "sh" if code.startswith(("6", "9")) else "bj" if code.startswith(("4", "8", "920")) else "sz"
    code = "".join(ch for ch in code if ch.isdigit())[:6].zfill(6)
    suffix = {"sh": "SH", "sz": "SZ", "bj": "BJ"}[prefix]
    return f"{code}.{suffix}"


class HithinkClient:
    """同花顺金融数据 REST 客户端。"""

    TIMEOUT = 15

    @staticmethod
    def enabled() -> bool:
        return bool(_api_key())

    @staticmethod
    def _headers() -> dict:
        k = _api_key()
        return {"X-api-key": k} if k else {}

    @staticmethod
    def _get(path: str, params: Optional[dict] = None, ttl: float = 10.0):
        """统一 GET 请求，带缓存与错误处理。失败返回 None。"""
        if not _api_key():
            return None
        ck = f"{path}:{params}"
        hit = _cache_get(ck)
        if hit is not None:
            return hit
        try:
            r = requests.get(_BASE + path, params=params, headers=HithinkClient._headers(),
                             timeout=HithinkClient.TIMEOUT)
            r.raise_for_status()
            body = r.json()
            if body.get("code") != 0:
                logger.warning("hithink %s biz error code=%s msg=%s", path, body.get("code"), body.get("message"))
                return None
            data = body.get("data")
            _cache_set(ck, data, ttl)
            return data
        except Exception as e:  # noqa: BLE001
            logger.warning("hithink %s failed: %s", path, e)
            return None

    # ──────────────── 行情快照 ────────────────
    @staticmethod
    def snapshot(symbols: List[str]) -> Optional[List[Dict]]:
        """批量行情快照。symbols 为内部 symbol 列表（sh600519）。返回对齐 StockQuote 的 dict 列表。"""
        if not symbols:
            return None
        thscodes = ",".join(_to_thscode(s) for s in symbols)
        data = HithinkClient._get("/api/a-share/prices/snapshot", {"thscodes": thscodes}, ttl=8.0)
        if not data:
            return None
        items = data.get("item") or []
        out = []
        for it in items:
            thscode = it.get("thscode", "")
            ticker = thscode.split(".")[0]
            prefix = "sh" if thscode.endswith(".SH") else "bj" if thscode.endswith(".BJ") else "sz"
            out.append({
                "symbol": f"{prefix}{ticker}",
                "thscode": thscode,
                "price": it.get("last_price"),
                "change": it.get("price_change"),
                "change_percent": it.get("price_change_ratio_pct"),
                "open": it.get("open_price"),
                "high": it.get("high_price"),
                "low": it.get("low_price"),
                "prev_close": it.get("prev_price"),
                "volume": it.get("volume"),
                "turnover": it.get("turnover"),
            })
        return out

    # ──────────────── 历史 K 线 ────────────────
    @staticmethod
    def kline(symbol: str, days: int = 120, adjust: str = "forward") -> Optional[List[Dict]]:
        """单只标的日线 K线。days 为回溯天数（窗口上限 10 年）。返回 [{date,open,close,high,low,volume,turnover}]。"""
        thscode = _to_thscode(symbol)
        end_ms = int(time.time() * 1000)
        start_ms = end_ms - days * 86400 * 1000
        data = HithinkClient._get("/api/a-share/prices/historical", {
            "thscode": thscode, "interval": "1d",
            "start": start_ms, "end": end_ms, "adjust": adjust,
        }, ttl=300.0)
        if not data:
            return None
        items = data.get("item") or []
        out = []
        for it in items:
            ts = it.get("date_ms")
            date = time.strftime("%Y-%m-%d", time.localtime(ts / 1000)) if ts else ""
            out.append({
                "date": date,
                "open": it.get("open_price"),
                "close": it.get("close_price"),
                "high": it.get("high_price"),
                "low": it.get("low_price"),
                "volume": it.get("volume"),
                "turnover": it.get("turnover"),
            })
        return out

    # ──────────────── 龙虎榜 ────────────────
    @staticmethod
    def dragon_tiger_list(trade_date: str = "", board_type: str = "all") -> Optional[List[Dict]]:
        """龙虎榜榜单。board_type: all/org(机构)/hot(游资)。trade_date 为 yyyyMMdd 或 yyyy-MM-dd。"""
        params = {"board_type": board_type}
        if trade_date:
            params["trade_date"] = trade_date.replace("-", "")
        data = HithinkClient._get("/api/a-share/special-data/dragon-tiger-list", params, ttl=60.0)
        return data.get("item") if data else None

    # ──────────────── 涨跌停池 ────────────────
    @staticmethod
    def limit_up_pool(trade_date: str = "") -> Optional[List[Dict]]:
        """涨停股票池。"""
        params = {}
        if trade_date:
            params["trade_date"] = trade_date.replace("-", "")
        data = HithinkClient._get("/api/a-share/special-data/limit-up-pool", params, ttl=60.0)
        return data.get("item") if data else None

    @staticmethod
    def limit_down_pool(trade_date: str = "") -> Optional[List[Dict]]:
        """跌停股票池。"""
        params = {}
        if trade_date:
            params["trade_date"] = trade_date.replace("-", "")
        data = HithinkClient._get("/api/a-share/special-data/limit-down-pool", params, ttl=60.0)
        return data.get("item") if data else None

    # ──────────────── 连板天梯 ────────────────
    @staticmethod
    def limit_up_ladder() -> Optional[List[Dict]]:
        """近 30 个交易日连板梯队矩阵。"""
        data = HithinkClient._get("/api/a-share/special-data/limit-up-ladder", ttl=120.0)
        return data.get("item") if data else None

    # ──────────────── 热股榜 / 飙升榜 ────────────────
    @staticmethod
    def hot_stock_list(period: str = "24h") -> Optional[List[Dict]]:
        """A股热股榜 Top30。period: 24h / 1h。"""
        data = HithinkClient._get("/api/a-share/special-data/hot-stock-list",
                                  {"period": period}, ttl=60.0)
        return data.get("item") if data else None

    @staticmethod
    def skyrocket_list(period: str = "24h") -> Optional[List[Dict]]:
        """飙升榜 Top30。period: 24h / 1h。"""
        data = HithinkClient._get("/api/a-share/special-data/skyrocket-list",
                                  {"period": period}, ttl=60.0)
        return data.get("item") if data else None

    # ──────────────── 标的检索 ────────────────
    @staticmethod
    def search_ticker(query: str) -> Optional[List[Dict]]:
        """按 thscode / ticker / 名称检索标的。"""
        data = HithinkClient._get("/api/meta/tickers/search", {"query": query}, ttl=300.0)
        return data.get("item") if data else None
