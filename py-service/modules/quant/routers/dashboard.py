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
from ..services.bigscreen import BigScreenService
from ..services.news import NewsRadarService
from ..services.research import ResearchReportService
from ..services.strategy import StrategyService


from fastapi import APIRouter

router = APIRouter()

_dash_cache: List = [0, None]

@router.get("/api/quant/dashboard")
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
