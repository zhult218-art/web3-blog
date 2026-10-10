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
from ..datasources.supabase import SupabaseStore


_FUND_HIST_STATE = {"running": False, "total": 0, "done": 0, "cur": "", "errors": [],
                    "started": 0.0, "last_sync_date": "", "results": {}}


class FundHistoryService:
    """个股基本面历史服务：Supabase 落库优先 + 实时兜底 + 写穿缓存。

    目的：① 东财/同花顺对 IP 间歇风控 → 历史数据落库后不再依赖实时抓取；
          ② 每天收盘后定时刷新当日增量；③ 历史数据可供回测/复盘直接从库读。
    数据种类：finance 财务摘要 / holders 股东户数 / margin 两融明细 /
              fundflow 资金流向 / dividend 分红 / blocktrade 大宗交易。
    """

    KINDS = {
        "finance":    {"endpoint": "finance",     "fresh_days": 100},  # 季报，最长间隔约 92 天
        "holders":    {"endpoint": "holders",     "fresh_days": 50},   # 季度披露（披露延迟数周）
        "margin":     {"endpoint": "margin",      "fresh_days": 2},    # 日频
        "fundflow":   {"endpoint": "fund-flow",   "fresh_days": 2},    # 日频
        "dividend":   {"endpoint": "dividend",    "fresh_days": 100},  # 年度/半年度事件
        "blocktrade": {"endpoint": "block-trade", "fresh_days": 2},    # 日频
    }
    _fail: Dict[str, tuple] = {}  # "kind:code" -> (fail_until, error)

    @staticmethod
    def _vibe_url(kind: str) -> str:
        return f"{AStockDataService.VIBE_BASE}/api/{FundHistoryService.KINDS[kind]['endpoint']}"

    @staticmethod
    def fetch_one(kind: str, code: str) -> list:
        """从 Vibe-Research 实时抓一种数据（失败抛异常）。"""
        r = requests.get(FundHistoryService._vibe_url(kind), params={"code": code}, timeout=45)
        r.raise_for_status()
        d = r.json().get("data")
        return d if isinstance(d, list) else []

    @staticmethod
    def _age_days(date_key: str) -> int:
        try:
            return (datetime.now() - datetime.strptime(date_key[:10], "%Y-%m-%d")).days
        except ValueError:
            return 9999

    @staticmethod
    def collect_one(code: str, kinds: Optional[list] = None) -> dict:
        """单股全量采集并写穿落库。返回 {kind: 写入行数}，-1 表示该类抓取失败。"""
        kinds = kinds or list(FundHistoryService.KINDS)
        result = {}
        for kind in kinds:
            try:
                rows = FundHistoryService.fetch_one(kind, code)
                result[kind] = SupabaseStore.save_stock_history(kind, code, rows)
            except Exception as e:  # noqa: BLE001
                result[kind] = -1
                FundHistoryService._fail[f"{kind}:{code}"] = (time.time() + 300, str(e)[:120])
            time.sleep(0.25)
        return result

    @staticmethod
    def read(kind: str, code: str) -> tuple:
        """读优先：库命中且新鲜直接回；命中但过期 → 回旧值 + 后台刷新；缺失 → 实时抓 + 写穿。

        返回 (rows, source)：source ∈ supabase / supabase-refreshing / live / empty。
        """
        fresh_days = FundHistoryService.KINDS[kind]["fresh_days"]
        rows = SupabaseStore.load_stock_history(kind, code)
        if rows:
            latest = (rows[-1].get("date_key") or "")[:10]
            if latest and FundHistoryService._age_days(latest) <= fresh_days:
                return rows, "supabase"
            # 数据过期：先回旧值，后台增量刷新（界面零等待）
            threading.Thread(target=FundHistoryService.collect_one,
                             args=(code, [kind]), daemon=True).start()
            return rows, "supabase-refreshing"
        # 库里没有：实时抓（同步等待），成功即写穿
        try:
            live = FundHistoryService.fetch_one(kind, code)
        except Exception:  # noqa: BLE001
            live = []
        if live:
            threading.Thread(target=SupabaseStore.save_stock_history,
                             args=(kind, code, live), daemon=True).start()
        return live, ("live" if live else "empty")


def _fund_history_worker(codes: list):
    """全池基本面历史采集（后台线程）：每天一次全量刷新 + 一次性历史回填。"""
    _FUND_HIST_STATE.update(running=True, total=len(codes), done=0, cur="",
                            errors=[], started=time.time())
    _FUND_HIST_STATE["last_sync_date"] = datetime.now().strftime("%Y-%m-%d")
    for i, sym in enumerate(codes):
        if not _FUND_HIST_STATE["running"]:
            break
        _FUND_HIST_STATE["cur"] = sym
        code = AStockDataService._to_code(sym)
        try:
            res = FundHistoryService.collect_one(code)
            _FUND_HIST_STATE["results"][code] = res
            bad = [k for k, n in res.items() if n < 0]
            if bad:
                _FUND_HIST_STATE["errors"].append(f"{sym}:{','.join(bad)}")
        except Exception as e:  # noqa: BLE001
            logger.warning("fund-history collect %s fail: %s", sym, e)
            _FUND_HIST_STATE["errors"].append(f"{sym}:{e}")
        _FUND_HIST_STATE["done"] = i + 1
        time.sleep(0.8)
    _FUND_HIST_STATE["running"] = False


def _schedule_fund_history_sync():
    """后台线程：交易日 16:30 后自动刷新全池基本面历史（每天一次，以内存标记防重）。"""
    while True:
        try:
            now = datetime.now()
            today = now.strftime("%Y-%m-%d")
            after_close = now.hour > 16 or (now.hour == 16 and now.minute >= 30)
            if (now.weekday() < 5 and after_close and not _FUND_HIST_STATE["running"]
                    and _FUND_HIST_STATE["last_sync_date"] != today
                    and SupabaseStore.ENABLED):
                codes = list(AStockDataService.STOCK_POOL.keys())
                _FUND_HIST_STATE["last_sync_date"] = today
                threading.Thread(target=_fund_history_worker, args=(codes,), daemon=True).start()
        except Exception as e:  # noqa: BLE001
            logger.warning("fund-history scheduler error: %s", e)
        threading.Event().wait(1800)
