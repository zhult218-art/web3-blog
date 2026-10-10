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
