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

from ..services.portfolio import PortfolioService


from fastapi import APIRouter

router = APIRouter()

@router.get("/api/quant/portfolio")
def get_portfolio():
    """获取持仓及盈亏"""
    perf = PortfolioService.get_performance()
    return {"code": 0, "data": perf}

@router.post("/api/quant/portfolio/holding")
def add_holding(req: HoldingRequest):
    """添加持仓"""
    h = PortfolioService.add_holding(asdict(req))
    return {"code": 0, "data": h}

@router.delete("/api/quant/portfolio/holding/{holding_id}")
def delete_holding(holding_id: str):
    """删除持仓"""
    PortfolioService.remove_holding(holding_id)
    return {"code": 0, "message": "已删除"}

@router.post("/api/quant/portfolio/refresh")
def refresh_portfolio():
    """刷新持仓盈亏"""
    perf = PortfolioService.get_performance()
    return {"code": 0, "data": perf}
