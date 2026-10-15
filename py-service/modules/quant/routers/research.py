# -*- coding: utf-8 -*-
from __future__ import annotations

import logging

logger = logging.getLogger("quant")

from ..services.research import ResearchReportService


from fastapi import APIRouter

router = APIRouter()

@router.get("/api/quant/research/reports")
def get_research_reports(code: str = "", page_no: int = 1, limit: int = 20, q_type: str = "0"):
    """研报列表：code=股票代码（空则全市场）；qType=0个股/1行业/2策略。    仅返回元数据（标题/机构/分析师/评级/目标价/EPS/原文链接）。"""
    return {"code": 0, "data": ResearchReportService.get_reports(code=code, page_no=page_no,
                                                                 limit=limit, q_type=q_type,
                                                                 force=bool(code))}

@router.get("/api/quant/research/reports/latest")
def get_latest_research_reports(limit: int = 20):
    """最新研报速递（全市场个股研报）。"""
    return {"code": 0, "data": ResearchReportService.get_latest_reports(limit)}

@router.get("/api/quant/research/reports/industry")
def get_industry_research_reports(limit: int = 20):
    """行业研报速递。"""
    return {"code": 0, "data": ResearchReportService.get_industry_reports(limit)}
