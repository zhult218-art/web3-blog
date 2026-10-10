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


try:
    import akshare as ak
except Exception:  # noqa: BLE001
    ak = None


_BACKFILL_STATE = {"running": False, "total": 0, "done": 0, "cur": "", "errors": [], "started": 0.0}

def _backfill_worker(codes: list, period: str = "daily"):
    today = datetime.now().strftime("%Y%m%d")
    for i, sym in enumerate(codes):
        if not _BACKFILL_STATE["running"]:
            break
        _BACKFILL_STATE["cur"] = sym
        try:
            code = AStockDataService._to_code(sym)
            rows, src = [], "补全(东财qfq)"
            try:
                df = AStockDataService._with_retry(
                    lambda: ak.stock_zh_a_hist(symbol=code, period=period, start_date="19900101",
                                               end_date=today, adjust="qfq"), 2, 1.0)
                if df is not None and len(df):
                    for _, r in df.iterrows():
                        rows.append({
                            "date": str(r["日期"])[:10], "open": float(r["开盘"]), "close": float(r["收盘"]),
                            "high": float(r["最高"]), "low": float(r["最低"]),
                            "volume": float(r.get("成交量") or 0), "turnover": float(r.get("成交额") or 0),
                            "amplitude": float(r.get("振幅") or 0),
                            "change_percent": float(r.get("涨跌幅") or 0),
                        })
            except Exception as em_err:
                logger.warning("backfill %s EM fail: %s", sym, em_err)
            if not rows:
                # 东财风控/失败 → Baostock TCP 全历史兜底（免 HTTP 风控）
                today_iso = f"{today[:4]}-{today[4:6]}-{today[6:]}"
                bs_rows = AStockDataService._baostock_kline(code, period, "1990-01-01", today_iso)
                if bs_rows:
                    rows, src = bs_rows, "补全(baostock)"
            if rows:
                SupabaseStore.save_kline(code, period, rows, src)
        except Exception as e:
            logger.warning("backfill %s fail: %s", sym, e)
            _BACKFILL_STATE["errors"].append(f"{sym}:{e}")
        _BACKFILL_STATE["done"] = i + 1
        time.sleep(1.2)
    _BACKFILL_STATE["running"] = False
# ============================================================
# 辅助函数
# ============================================================

def _daily_incremental_sync():
    """每日增量同步：全市场快照 + 板块走势落库（每天收盘后调度跑，避免重复拉全量）。"""
    try:
        today = datetime.now().strftime("%Y-%m-%d")
        logger.info("daily incremental sync start: %s", today)
        errors: list[str] = []
        # 1) 全市场快照（异步落库在 _load_all_snapshot 内）
        df, src = AStockDataService._fetch_snapshot_once()
        snap_rows = 0
        if df is not None and len(df):
            snap_rows = len(df)
            if SupabaseStore.save_snapshot(today, df, src):
                SupabaseStore.update_count(today, "snapshot", snap_rows, "ok", f"src={src}")
            else:
                SupabaseStore.update_count(today, "snapshot", snap_rows, "fail", "save_snapshot returns False")
                errors.append("snapshot落库失败")
        else:
            errors.append("snapshot拉取失败")
        # 2) 板块走势：抓取后同步落库（保证写入完成，重启不丢）
        sect_rows = 0
        try:
            sect = AStockDataService.get_sector_trends(days=30, top=0, force=True)
            if sect and sect.get("series"):
                sect_rows = len(sect["series"])
                AStockDataService._save_sector_trends(sect)
                SupabaseStore.update_count(today, "sector", sect_rows, "ok", f"src={sect.get('source') or ''}")
        except Exception as e:  # noqa: BLE001
            logger.warning("incremental sector sync fail: %s", e)
            SupabaseStore.update_count(today, "sector", sect_rows, "fail", str(e)[:300])
            errors.append(f"sector失败: {e}")
        # 3) 告警
        if errors:
            SupabaseStore.save_alert(today, " \n".join(errors), "error")
            SupabaseStore.log_sync("daily", today, "fail", " \n".join(errors))
        else:
            SupabaseStore.resolve_alerts(today)
            SupabaseStore.log_sync("daily", today, "ok")
        logger.info("daily incremental sync done: %s (snapshot=%s sector=%s) errors=%d",
                    today, snap_rows, sect_rows, len(errors))
    except Exception as e:  # noqa: BLE001
        logger.warning("daily incremental sync failed: %s", e)
        SupabaseStore.save_alert(datetime.now().strftime("%Y-%m-%d"), str(e)[:500], "error")
        SupabaseStore.log_sync("daily", datetime.now().strftime("%Y-%m-%d"), "fail", str(e))
    # 增量同步完成后，后台刷新全池最近 10 日 K 线（不阻塞）
    if not _DAILY_KLINE_STATE["running"]:
        codes = list(AStockDataService.STOCK_POOL.keys())
        _DAILY_KLINE_STATE.update(running=True, total=len(codes), done=0, saved=0, errors=[])
        threading.Thread(target=_daily_kline_refresh_worker, args=(codes,), daemon=True).start()


# 每日 K 线刷新状态
_DAILY_KLINE_STATE = {"running": False, "total": 0, "done": 0, "saved": 0, "errors": []}


def _daily_kline_refresh_worker(codes: list):
    """每日收盘后刷新全池股票最近 10 个交易日的日 K 线（增量 upsert 到 Supabase）。"""
    global _DAILY_KLINE_STATE
    today = datetime.now().strftime("%Y%m%d")
    start_date = (datetime.now() - timedelta(days=20)).strftime("%Y%m%d")
    saved = 0
    for i, sym in enumerate(codes):
        if not _DAILY_KLINE_STATE["running"]:
            break
        try:
            code = AStockDataService._to_code(sym)
            rows = []
            try:
                df = AStockDataService._with_retry(
                    lambda: ak.stock_zh_a_hist(symbol=code, period="daily",
                                               start_date=start_date, end_date=today, adjust="qfq"),
                    2, 0.8)
                if df is not None and len(df):
                    for _, r in df.iterrows():
                        rows.append({
                            "date": str(r["日期"])[:10], "open": float(r["开盘"]),
                            "close": float(r["收盘"]), "high": float(r["最高"]),
                            "low": float(r["最低"]), "volume": float(r.get("成交量") or 0),
                            "turnover": float(r.get("成交额") or 0),
                            "amplitude": float(r.get("振幅") or 0),
                            "change_percent": float(r.get("涨跌幅") or 0),
                        })
            except Exception:
                pass
            if not rows:
                today_iso = f"{today[:4]}-{today[4:6]}-{today[6:]}"
                start_iso = f"{start_date[:4]}-{start_date[4:6]}-{start_date[6:]}"
                bs_rows = AStockDataService._baostock_kline(code, "daily", start_iso, today_iso)
                if bs_rows:
                    rows = bs_rows
            if rows and SupabaseStore.save_kline(code, "daily", rows, "daily-sync"):
                saved += len(rows)
        except Exception as e:  # noqa: BLE001
            logger.warning("daily kline refresh %s fail: %s", sym, e)
            _DAILY_KLINE_STATE["errors"].append(f"{sym}:{e}")
        _DAILY_KLINE_STATE["done"] = i + 1
        time.sleep(0.5)
    _DAILY_KLINE_STATE["running"] = False
    _DAILY_KLINE_STATE["saved"] = saved
    SupabaseStore.log_sync("daily-kline", datetime.now().strftime("%Y-%m-%d"), "ok",
                           f"refreshed {saved} rows for {len(codes)} stocks")
    logger.info("daily kline refresh done: %s rows saved", saved)


def _backfill_sector_history(days: int = 120):
    """板块历史回填：一次性把近 days 日全部板块走势写入 Supabase。"""
    try:
        logger.info("sector history backfill start: days=%s", days)
        sect = AStockDataService.get_sector_trends(days=max(days, 30), top=0, force=True)
        if not sect or not sect.get("series"):
            logger.warning("backfill sector empty")
            return 0
        AStockDataService._save_sector_trends(sect)
        n = len(sect.get("series") or [])
        SupabaseStore.log_sync("backfill_sector", datetime.now().strftime("%Y-%m-%d"), "ok",
                               f"series={n} days={days}")
        logger.info("sector history backfill done: %s series", n)
        return n
    except Exception as e:  # noqa: BLE001
        logger.warning("sector backfill failed: %s", e)
        return 0

def _schedule_daily_sync():
    """后台线程：每 30 分钟检查一次，交易日 15:30 后且当日尚未落库（以库中 quant_daily_count 为准，    进程重启不丢）时执行增量；基准为 8:30 起的任意时刻，避免重复同步。"""
    while True:
        now = datetime.now()
        today = now.strftime("%Y-%m-%d")
        is_trade_day = now.weekday() < 5
        after_close = now.hour > 15 or (now.hour == 15 and now.minute >= 30)
        if is_trade_day and after_close and not SupabaseStore.day_synced(today):
            _daily_incremental_sync()
        threading.Event().wait(1800)
