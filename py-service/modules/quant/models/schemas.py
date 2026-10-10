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
