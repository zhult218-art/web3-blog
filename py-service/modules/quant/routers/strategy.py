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
from ..services.recommend import RecommendStrategyService
from ..services.screen import CustomScreenSandbox, StrategyScreenService
from ..services.strategy import StrategyService


from fastapi import APIRouter

router = APIRouter()

@router.get("/api/quant/recommend")
def get_recommendations(limit: int = 10, force: bool = False):
    """低位荐股：技术面（52周位置/均线/MACD）+ 基本面 PE/市净率 + 研报覆盖 综合评分。"""
    data = AStockDataService.get_recommendations(limit=limit, force=force)
    return {"code": 0, "data": data}

@router.get("/api/quant/recommend/strategies")
def get_recommend_strategy_list(force: bool = False):
    """多策略推荐：基于已缓存全市场快照的 8 种推荐策略（10分钟缓存，force 强制刷新）。
    返回 [{id,name,desc,items:[{code,name,price,change_pct,score,reasons[]}],disabled,disabled_reason}]"""
    return {"code": 0, "data": RecommendStrategyService.recommend_all(force=force)}

@router.get("/api/quant/strategies")
def list_strategies():
    """策略列表"""
    return {"code": 0, "data": StrategyService.list_strategies()}

@router.get("/api/quant/strategies/{strategy_id}")
def get_strategy(strategy_id: str):
    """策略详情"""
    s = StrategyService.get_strategy(strategy_id)
    if not s:
        raise HTTPException(404, "策略不存在")
    return {"code": 0, "data": s}

@router.get("/api/quant/strategies/{strategy_id}/screen")
def screen_strategy(strategy_id: str, limit: int = 20, top_pool: int = 80, force: bool = False):
    """内置策略选股：按策略条件从全市场筛选当前值得买入的标的"""
    if not StrategyService.get_strategy(strategy_id):
        raise HTTPException(404, "策略不存在")
    return {"code": 0, "data": StrategyScreenService.screen(strategy_id, limit=limit, top_pool=top_pool, force=force)}

@router.post("/api/quant/screen/custom")
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
