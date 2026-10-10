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

from ..datasources.supabase import SupabaseStore


class ResearchReportService:
    """券商研报元数据：标题/机构/分析师/评级/目标价/EPS预测/原文链接。"""
    # 合规边界：仅采集列表页元数据（reportapi.eastmoney.com），
    # 不下载、不缓存研报 PDF 全文；示例字段与源站点会调整，需容错。    """

    _cache: Dict[str, tuple] = {}
    _REPORT_TTL = 300  # 5 分钟

    _HEADERS = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                      "(KHTML, like Gecko) Chrome/126.0 Safari/537.36",
        "Referer": "https://data.eastmoney.com/report/",
    }

    @staticmethod
    def _norm(x) -> Optional[float]:
        if x in (None, "", "-"):
            return None
        try:
            return float(str(x).replace(",", "").replace("万", "").strip())
        except (TypeError, ValueError):
            return None

    @staticmethod
    def _authors(row: Dict) -> str:
        a = row.get("author")
        if isinstance(a, list):
            parts = []
            for it in a:
                if isinstance(it, str):
                    nm = it.split(".")[-1] if "." in it else it
                elif isinstance(it, dict):
                    nm = it.get("name") or ""
                else:
                    nm = ""
                if nm:
                    parts.append(nm)
            return "、".join(parts)
        s = row.get("authorName") or ""
        if isinstance(s, str):
            return s.replace("|", "、")
        return ""

    @staticmethod
    def _fetch_report_page(code: str = "", page_no: int = 1, q_type: str = "0",
                           page_size: int = 30, begin: str = "", end: str = "") -> List[Dict]:
        """东财研报中心列表接口直连（qType: 0个股/1行业/2策略）。"""
        try:
            url = "https://reportapi.eastmoney.com/report/list"
            params = {
                "cb": "", "pageSize": str(page_size), "pageNo": str(page_no),
                "qType": q_type, "code": code, "industryCode": "*", "rating": "*",
                "ratingChange": "*",
                "beginTime": begin or (datetime.now() - timedelta(days=90)).strftime("%Y-%m-%d"),
                "endTime": end or datetime.now().strftime("%Y-%m-%d"),
                "_": str(int(time.time() * 1000)),
            }
            r = requests.get(url, params=params, headers=ResearchReportService._HEADERS, timeout=12)
            r.raise_for_status()
            payload = r.json()
        except Exception as e:  # noqa: BLE001
            logger.warning("research report list failed: %s", e)
            return []
        rows = (payload or {}).get("data") or []
        if isinstance(rows, dict):
            rows = rows.get("list") or []
        if not isinstance(rows, list):
            rows = []
        out = []
        for x in rows[:page_size]:
            if not isinstance(x, dict):
                continue
            code = str(x.get("stockCode") or "")
            if not code:
                continue
            title = str(x.get("title") or "").strip()
            if not title:
                continue
            rating = x.get("emRatingName") or x.get("sRatingName") or x.get("newRatingName") or ""
            info_code = str(x.get("infoCode") or "")
            out.append({
                "report_id": info_code,
                "title": title,
                "stock_code": code,
                "stock_name": str(x.get("stockName") or "").strip(),
                "org_name": str(x.get("orgSName") or x.get("orgName") or "").strip(),
                "author": ResearchReportService._authors(x),
                "publish_date": str(x.get("publishDate") or "")[:10],
                "rating": str(rating),
                "rating_change": str(x.get("emRatingChange") or ""),
                "target_price": ResearchReportService._norm(x.get("indvAimPrice") or x.get("aimPrice")),
                "eps_y1": ResearchReportService._norm(x.get("predictThisYearEps")),
                "eps_y2": ResearchReportService._norm(x.get("predictNextYearEps")),
                "eps_y3": ResearchReportService._norm(x.get("predictThreeYearEps")),
                "summary": str(x.get("summary") or x.get("jiYin") or "")[:200],
                "url": "https://data.eastmoney.com/report/info/" + info_code + ".html",
            })
        return out

    @staticmethod
    def get_reports(code: str = "", page_no: int = 1, limit: int = 20,
                    q_type: str = "0", force: bool = False) -> List[Dict]:
        """获取研报列表，可按股票代码/分析师过滤；带 5 分钟缓存。"""
        key = f"reports_{code}_{page_no}_{q_type}"
        now = time.time()
        if not force and key in ResearchReportService._cache:
            ts, val = ResearchReportService._cache[key]
            if now - ts < ResearchReportService._REPORT_TTL:
                return val[:limit]
        rows = ResearchReportService._fetch_report_page(code=code, page_no=page_no, q_type=q_type,
                                                        page_size=max(limit * 2, 30))
        ResearchReportService._cache[key] = (now, rows)
        # 异步落库（所有获取到的研报元数据均入库，便于回测/复盘）
        if rows:
            threading.Thread(target=lambda: SupabaseStore.save_reports(rows), daemon=True).start()
        return rows[:limit]

    @staticmethod
    def get_latest_reports(limit: int = 20, force: bool = False) -> List[Dict]:
        """最新全市场个股研报速递。"""
        return ResearchReportService.get_reports(code="", limit=limit, force=force)

    @staticmethod
    def get_industry_reports(limit: int = 20, force: bool = False) -> List[Dict]:
        """行业研报速递。"""
        return ResearchReportService.get_reports(code="", limit=limit, q_type="1", force=force)


# ============================================================
# 回测引擎
# ============================================================
# 内置回测策略清单（含中文说明，供前端展示；id 与 BacktestEngine.STRATEGIES 一一对应）
