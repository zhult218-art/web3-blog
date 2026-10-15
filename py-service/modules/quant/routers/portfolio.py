# -*- coding: utf-8 -*-
from __future__ import annotations

import logging
from dataclasses import asdict

logger = logging.getLogger("quant")

from ..services.portfolio import PortfolioService
from ..models.schemas import HoldingRequest

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
