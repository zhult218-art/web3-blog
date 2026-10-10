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

from ..config import QUANT_CACHE_DIR
from ..datasources.astock import AStockDataService


class PortfolioService:
    """持仓管理服务"""

    PF_FILE = QUANT_CACHE_DIR / "portfolio.json"

    @staticmethod
    def get_portfolio() -> Dict:
        try:
            with open(PortfolioService.PF_FILE, encoding="utf-8") as f:
                return json.load(f)
        except (FileNotFoundError, json.JSONDecodeError):
            return {"holdings": [], "last_refresh": None}

    @staticmethod
    def save_portfolio(data: Dict):
        with open(PortfolioService.PF_FILE, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

    @staticmethod
    def add_holding(holding: Dict) -> Dict:
        data = PortfolioService.get_portfolio()
        holding["id"] = str(uuid.uuid4())[:8]
        holding["created_at"] = datetime.now().strftime("%Y-%m-%d %H:%M")
        data.setdefault("holdings", []).append(holding)
        PortfolioService.save_portfolio(data)
        return holding

    @staticmethod
    def remove_holding(holding_id: str) -> bool:
        data = PortfolioService.get_portfolio()
        data["holdings"] = [h for h in data.get("holdings", []) if h.get("id") != holding_id]
        PortfolioService.save_portfolio(data)
        return True

    @staticmethod
    def get_performance() -> Dict:
        """计算持仓收益"""
        data = PortfolioService.get_portfolio()
        total_cost = sum(h.get("cost_price", 0) * h.get("shares", 0) for h in data.get("holdings", []))
        current_prices = {}
        for h in data.get("holdings", []):
            sym = h.get("symbol", "")
            name = AStockDataService.STOCK_POOL.get(sym, sym)
            quote = AStockDataService.get_realtime_quote(sym, name)
            current_prices[sym] = quote.price

        total_value = sum(h.get("shares", 0) * current_prices.get(h.get("symbol", ""), 0)
                          for h in data.get("holdings", []))
        pnl = total_value - total_cost if total_cost > 0 else 0
        pnl_pct = (pnl / total_cost * 100) if total_cost > 0 else 0

        holdings_detail = []
        for h in data.get("holdings", []):
            sym = h.get("symbol", "")
            name = AStockDataService.STOCK_POOL.get(sym, sym)
            quote = AStockDataService.get_realtime_quote(sym, name)
            p = quote.price - h.get("cost_price", 0)
            holdings_detail.append({
                "id": h.get("id"), "symbol": sym, "name": name,
                "shares": h.get("shares", 0), "cost_price": h.get("cost_price", 0),
                "current_price": quote.price, "pnl": round(p * h.get("shares", 0), 2),
                "pnl_percent": round(p / h.get("cost_price", 1) * 100, 2),
                "market_value": round(quote.price * h.get("shares", 0), 2),
            })

        return {
            "total_value": round(total_value, 2),
            "total_cost": round(total_cost, 2),
            "total_pnl": round(pnl, 2),
            "total_pnl_percent": round(pnl_pct, 2),
            "holdings": holdings_detail,
            "last_refresh": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        }


# ============================================================
# 策略管理
# ============================================================
