# -*- coding: utf-8 -*-
from __future__ import annotations

import time
import logging
import threading
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
from typing import Dict, List

logger = logging.getLogger("quant")

from ..datasources.astock import AStockDataService
from ..services.bigscreen import BigScreenService
from ..services.news import NewsRadarService
from ..services.research import ResearchReportService
from ..services.strategy import StrategyService


from fastapi import APIRouter

router = APIRouter()

DASH_TTL = 30.0                   # 新鲜期：30s 内直接复用
_dash_cache: List = [0.0, None]   # [ts, payload]
_dash_lock = threading.Lock()
_dash_building = False            # 后台刷新中，避免并发重复构建


def _safe(fn, label: str, default):
    """单个子数据源失败不影响整张看板（上游被风控时降级为空值而非 500）。"""
    try:
        return fn()
    except Exception as e:  # noqa: BLE001
        logger.warning("dashboard %s failed: %s", label, e)
        return default


def _build_dashboard() -> Dict:
    """并行拉取各子数据块（彼此独立），总耗时由「各段之和」降为「最慢一段」。"""
    empty_pools = {"date": "", "prev_date": "", "limit_up": [], "limit_down": [], "zhaban": []}
    with ThreadPoolExecutor(max_workers=9) as ex:
        f_pools = ex.submit(_safe, AStockDataService.get_limit_pools, "limit_pools", empty_pools)
        f_quotes = ex.submit(_safe, AStockDataService.get_stock_list, "stock_list", [])
        f_news = ex.submit(_safe, lambda: NewsRadarService.get_latest_news(5), "news", [])
        f_sec = ex.submit(_safe, NewsRadarService.get_sector_heat, "sector_heat", [])
        f_strat = ex.submit(_safe, StrategyService.list_strategies, "strategies", [])
        f_board = ex.submit(_safe, AStockDataService.get_board_progress, "board_progress", [])
        f_trend = ex.submit(_safe, lambda: AStockDataService.get_sector_trends(days=30, top=8), "sector_trends", [])
        f_flow = ex.submit(_safe, AStockDataService.get_market_fund_flow, "fund_flow", {})
        f_rep = ex.submit(_safe, lambda: ResearchReportService.get_latest_reports(10), "reports", [])

        pools = f_pools.result()
        quotes = f_quotes.result()
        news = f_news.result()
        sectors = f_sec.result()
        strategies = f_strat.result()
        board_progress = f_board.result()
        sector_trends = f_trend.result()
        fund_flow = f_flow.result()
        reports = f_rep.result()

    up = sum(1 for q in quotes if q.get("change_percent", 0) > 0)
    down = sum(1 for q in quotes if q.get("change_percent", 0) < 0)

    return {
        "code": 0, "data": {
            "market": {"total": len(quotes), "up": up, "down": down, "updated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")},
            "hot_stocks": sorted(quotes, key=lambda x: abs(x.get("change_percent", 0)), reverse=True)[:8],
            "sectors": sectors,
            "latest_news": news,
            "strategies": strategies,
            "limit_pools": pools,
            "board_progress": board_progress,
            "sector_trends": sector_trends,
            "fund_flow": fund_flow,
            "research_reports": reports,
        }
    }


def _store_dashboard(payload: Dict):
    with _dash_lock:
        _dash_cache[0] = time.time()
        _dash_cache[1] = payload


def _refresh_dashboard_bg():
    """后台刷新过期快照：前端立刻拿到旧数据，无需等待上游。"""
    global _dash_building
    with _dash_lock:
        if _dash_building:
            return
        _dash_building = True
    try:
        _store_dashboard(_build_dashboard())
    except Exception as e:  # noqa: BLE001
        logger.error("dashboard background refresh failed: %s", e)
    finally:
        with _dash_lock:
            _dash_building = False


@router.get("/api/quant/dashboard")
def get_dashboard():
    """量化数据看板（含涨停/跌停池、板块曲线、资金流、晋级数等）。
    采用 stale-while-revalidate：有旧快照先返回旧数据，后台线程再刷新。"""
    now = time.time()
    if _dash_cache[1] is not None:
        if now - _dash_cache[0] < DASH_TTL:
            return _dash_cache[1]
        threading.Thread(target=_refresh_dashboard_bg, daemon=True).start()
        return _dash_cache[1]
    # 首次无缓存：同步构建（仅此一次）
    payload = _build_dashboard()
    _store_dashboard(payload)
    return payload

@router.get("/api/quant/dashboard/snapshot")
def get_bigscreen_snapshot(market: str = "a"):
    """监控大屏快照（真实 A 股行情驱动）。market 预留全球扩展，当前仅支持 a。"""
    if market != BigScreenService.MARKET:
        return {"code": 1, "message": f"暂不支持市场 {market}，当前仅 A 股"}
    try:
        return {"code": 0, "data": BigScreenService.snapshot()}
    except Exception as e:  # noqa: BLE001
        logger.error("bigscreen snapshot failed: %s", e)
        return {"code": 1, "message": f"快照构建失败: {e}"}

@router.get("/api/quant/qmt/status")
def qmt_status():
    """QMT连接状态（模拟）。"""
    """QMT连接状态（模拟）"""
    return {"code": 0, "data": {"connected": False, "account": "", "available": True, "message": "QMT未连接，请先启动QMT客户端"}}

@router.post("/api/quant/qmt/connect")
def qmt_connect(req: Dict = {}):
    """连接QMT（模拟）"""
    return {"code": 0, "data": {"connected": True, "account": "sim_001", "balance": 1000000, "message": "模拟连接成功"}}
