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
from ..services.ai import AIAnalysisService
from ..services.backtest import BACKTEST_STRATEGY_META, BacktestEngine, run_backtest_task
from ..state import _backtest_store, _task_store


from fastapi import APIRouter

router = APIRouter()

@router.post("/api/quant/backtest/run")
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

@router.get("/api/quant/backtest/status/{task_id}")
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

@router.get("/api/quant/backtest/result/{task_id}")
def get_backtest_result(task_id: str):
    """获取回测结果"""
    result = _backtest_store.get(task_id)
    if not result:
        return {"code": 404, "message": "回测结果不存在或任务已完成"}
    return {"code": 0, "data": asdict(result)}

@router.post("/api/quant/backtest/quick")
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
