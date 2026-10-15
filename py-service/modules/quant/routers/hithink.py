# -*- coding: utf-8 -*-
from __future__ import annotations

import logging

from fastapi import Query

logger = logging.getLogger("quant")

from ..datasources.hithink import HithinkClient


from fastapi import APIRouter

router = APIRouter()

@router.get("/api/quant/hithink/status")
def py_hithink_status():
    """同花顺数据源连通性状态。"""
    enabled = HithinkClient is not None and HithinkClient.enabled()
    return {"code": 0, "data": {"enabled": enabled, "source": "hithink"}}

@router.get("/api/quant/hithink/quote")
def py_hithink_quote(symbols: str = Query(..., description="逗号分隔 symbol，如 sh600519,sz000001")):
    """同花顺批量行情快照。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    syms = [s.strip() for s in symbols.split(",") if s.strip()]
    rows = HithinkClient.snapshot(syms)
    return {"code": 0, "data": rows or [], "source": "hithink"}

@router.get("/api/quant/hithink/kline/{symbol}")
def py_hithink_kline(symbol: str, days: int = Query(120, ge=1, le=3650), adjust: str = "forward"):
    """同花顺个股历史 K线（日线）。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    rows = HithinkClient.kline(symbol, days=days, adjust=adjust)
    return {"code": 0, "data": rows or [], "source": "hithink"}

@router.get("/api/quant/hithink/dragon-tiger")
def py_hithink_dragon_tiger(trade_date: str = "", board_type: str = "all"):
    """同花顺龙虎榜榜单（全市场，按交易日）。board_type: all/org/hot。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    rows = HithinkClient.dragon_tiger_list(trade_date, board_type)
    return {"code": 0, "data": rows or [], "source": "hithink"}

@router.get("/api/quant/hithink/limit-up")
def py_hithink_limit_up(trade_date: str = ""):
    """同花顺涨停股票池。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    rows = HithinkClient.limit_up_pool(trade_date)
    return {"code": 0, "data": rows or [], "source": "hithink"}

@router.get("/api/quant/hithink/limit-down")
def py_hithink_limit_down(trade_date: str = ""):
    """同花顺跌停股票池。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    rows = HithinkClient.limit_down_pool(trade_date)
    return {"code": 0, "data": rows or [], "source": "hithink"}

@router.get("/api/quant/hithink/limit-ladder")
def py_hithink_limit_ladder():
    """同花顺连板天梯（近 30 日连板梯队矩阵）。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    rows = HithinkClient.limit_up_ladder()
    return {"code": 0, "data": rows or [], "source": "hithink"}

@router.get("/api/quant/hithink/hot-stocks")
def py_hithink_hot_stocks(period: str = "24h"):
    """同花顺 A 股热股榜 Top30。period: 24h / 1h。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    rows = HithinkClient.hot_stock_list(period)
    return {"code": 0, "data": rows or [], "source": "hithink"}

@router.get("/api/quant/hithink/skyrocket")
def py_hithink_skyrocket(period: str = "24h"):
    """同花顺飙升榜 Top30。period: 24h / 1h。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    rows = HithinkClient.skyrocket_list(period)
    return {"code": 0, "data": rows or [], "source": "hithink"}

@router.get("/api/quant/hithink/search")
def py_hithink_search(query: str = Query(..., min_length=1)):
    """同花顺标的检索（按代码/名称）。"""
    if HithinkClient is None or not HithinkClient.enabled():
        return {"code": 1, "message": "hithink not configured", "data": None}
    rows = HithinkClient.search_ticker(query)
    return {"code": 0, "data": rows or [], "source": "hithink"}
