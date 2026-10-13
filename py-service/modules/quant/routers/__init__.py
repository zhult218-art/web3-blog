# -*- coding: utf-8 -*-
"""聚合各域路由为单个 APIRouter。"""
from fastapi import APIRouter

from .market import router as market_router
from .data import router as data_router
from .data_ext import router as data_ext_router
from .hithink import router as hithink_router
from .research import router as research_router
from .strategy import router as strategy_router
from .backtest import router as backtest_router
from .ai import router as ai_router
from .portfolio import router as portfolio_router
from .dashboard import router as dashboard_router
from .holdings import router as holdings_router

router = APIRouter()
for _r in (market_router, data_router, data_ext_router, hithink_router, research_router, strategy_router, backtest_router, ai_router, portfolio_router, dashboard_router, holdings_router):
    router.include_router(_r)
