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

class StrategyService:
    """量化策略管理"""

    BUILTIN_STRATEGIES = [
        {"id": "bse_smallcap", "name": "北交所小市值", "description": "选择北交所市值最小的N只股票等权配置，定期调仓",
         "category": "多因子", "risk_level": "MEDIUM", "tags": "小市值,北交所,等权"},
        {"id": "grid_okx", "name": "OKX网格交易", "description": "在价格区间内低吸高抛，赚取波动收益",
         "category": "高频", "risk_level": "LOW", "tags": "网格,BTC,ETH,OKX"},
        {"id": "moving_avg", "name": "双均线策略", "description": "MA5上穿MA20金叉买入，死叉卖出",
         "category": "趋势策略", "risk_level": "MEDIUM", "tags": "均线,趋势"},
        {"id": "macd_signal", "name": "MACD信号策略", "description": "MACD金叉买入，死叉卖出，配合成交量确认",
         "category": "趋势策略", "risk_level": "MEDIUM", "tags": "MACD,趋势"},
        {"id": "bollinger", "name": "布林带策略", "description": "价格触及下轨买入，触及上轨卖出",
         "category": "趋势策略", "risk_level": "LOW", "tags": "布林带,均线,回归"},
        {"id": "rsi_meanrev", "name": "RSI均值回归", "description": "RSI超卖时买入，超买时卖出",
         "category": "趋势策略", "risk_level": "LOW", "tags": "RSI,均值回归"},
        {"id": "qmt_auto", "name": "QMT自动化交易", "description": "基于迅投QMT平台的自动化策略接口",
         "category": "实盘", "risk_level": "HIGH", "tags": "QMT,实盘,Python"},
    ]

    @staticmethod
    def list_strategies() -> List[Dict]:
        return StrategyService.BUILTIN_STRATEGIES

    @staticmethod
    def get_strategy(strategy_id: str) -> Optional[Dict]:
        for s in StrategyService.BUILTIN_STRATEGIES:
            if s["id"] == strategy_id:
                return s
        return None


