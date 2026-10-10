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
from ..services.data_sync import _BACKFILL_STATE, _backfill_worker
from ..services.fund_history import _FUND_HIST_STATE, _fund_history_worker


from fastapi import APIRouter

router = APIRouter()

@router.get("/api/quant/data/fund-history/status")
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

@router.post("/api/quant/data/fund-history/collect")
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

@router.post("/api/quant/data/backfill")
def start_backfill(period: str = "daily"):
    """全池股票历史补全（从1990/上市至今），后台线程执行。"""
    if _BACKFILL_STATE["running"]:
        return {"code": 0, "data": _BACKFILL_STATE}
    codes = list(AStockDataService.STOCK_POOL.keys())
    _BACKFILL_STATE.update(running=True, total=len(codes), done=0, cur="", errors=[], started=time.time())
    threading.Thread(target=_backfill_worker, args=(codes, period), daemon=True).start()
    return {"code": 0, "data": _BACKFILL_STATE}

@router.post("/api/quant/data/backfill/{symbol}")
def start_backfill_one(symbol: str, period: str = "daily"):
    """单支股票历史补全"""
    if _BACKFILL_STATE["running"]:
        return {"code": 0, "data": _BACKFILL_STATE}
    code = AStockDataService._to_code(symbol) or symbol.replace("sh", "").replace("sz", "")
    codes = [AStockDataService._to_symbol(code)]
    _BACKFILL_STATE.update(running=True, total=1, done=0, cur="", errors=[], started=time.time())
    threading.Thread(target=_backfill_worker, args=(codes, period), daemon=True).start()
    return {"code": 0, "data": _BACKFILL_STATE}

@router.get("/api/quant/data/backfill/status")
def backfill_status():
    """补全任务状态。"""
    return {"code": 0, "data": _BACKFILL_STATE}
