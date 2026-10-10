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

class SupabaseStore:
    URL = os.environ.get("SUPABASE_URL", "").rstrip("/")
    KEY = os.environ.get("SUPABASE_SERVICE_KEY", "")
    ENABLED = bool(URL and KEY)
    _HEADERS = {
        "apikey": KEY,
        "Authorization": f"Bearer {KEY}",
        "Content-Type": "application/json",
    } if ENABLED else {}
    _upsert_buf: Dict[str, list] = {}
    _buf_lock = threading.Lock()
    _log_interval = 300.0
    _last_log = 0.0
    _PAGE_SIZE = 1000
    # K线读库熔断：SSL重置/超时后短时间内跳过，避免每次请求都等满超时
    _kline_fail_until = 0.0
    _KLINE_FAIL_BACKOFF = 600.0  # 失败后退避 10 分钟

    # ---------------- 基础读写 ----------------
    @staticmethod
    def _base() -> str:
        return f"{SupabaseStore.URL}/rest/v1"

    @staticmethod
    def _table(t: str) -> str:
        return f"{SupabaseStore._base()}/{t}"

    @staticmethod
    def upsert(table: str, rows: list, on_conflict: str = "id", max_chunk: int = 400) -> bool:
        """按 on_conflict 做 upsert；空列表直接成功。失败返回 False（调用方降级）。"""
        if not SupabaseStore.ENABLED or not rows:
            return False
        ok = True
        for i in range(0, len(rows), max_chunk):
            chunk = rows[i:i + max_chunk]
            try:
                r = requests.post(
                    SupabaseStore._table(table),
                    params={"on_conflict": on_conflict},
                    headers={**SupabaseStore._HEADERS, "Prefer": "resolution=merge-duplicates,return=minimal"},
                    json=chunk, timeout=20,
                )
                r.raise_for_status()
            except Exception as e:  # noqa: BLE001
                logger.warning("supabase upsert %s (%d rows) failed: %s", table, len(chunk), e)
                ok = False
        return ok

    @staticmethod
    def _select(table: str, select: str, params: dict, timeout: int = 15):
        if not SupabaseStore.ENABLED:
            return None
        r = requests.get(SupabaseStore._table(table), headers=SupabaseStore._HEADERS,
                         params={"select": select, **params}, timeout=timeout)
        r.raise_for_status()
        return r.json()

    # ---------------- 个股基本面历史（财务/股东/两融/资金流/分红/大宗） ----------------
    # 通用 jsonb 表 quant_stock_history（kind, code, date_key, payload），
    # 建表 SQL：supabase/quant_stock_history.sql（Supabase Dashboard SQL Editor 执行一次）
    STOCK_HISTORY_TABLE = "quant_stock_history"
    _stock_hist_fail_until = 0.0  # 表缺失时的退避时间戳（避免每次请求都吃 404）

    @staticmethod
    def save_stock_history(kind: str, code: str, rows: list) -> int:
        """个股历史数据落库（jsonb 通用表，date_key 从 date/period 字段提取）。返回写入行数。

        注意：写入失败（超时等瞬时错误）不设读取退避——只有确认表缺失才影响读路径。
        """
        if not SupabaseStore.ENABLED or not rows:
            return 0
        out = []
        for r in rows:
            if not isinstance(r, dict):
                continue
            dk = ""
            for k in ("date", "period", "day", "trade_date"):
                v = r.get(k)
                if v and str(v).lower() not in ("none", "nan", "null"):
                    dk = str(v)[:10]
                    break
            if not dk:
                continue
            out.append({"kind": kind, "code": code, "date_key": dk, "payload": r})
        if not out:
            return 0
        ok = SupabaseStore.upsert(SupabaseStore.STOCK_HISTORY_TABLE, out, "kind,code,date_key")
        return len(out) if ok else 0

    @staticmethod
    def load_stock_history(kind: str, code: str, limit: int = 600) -> Optional[list]:
        """读个股历史（按 date_key 升序 payload 列表）。表缺失/未启用返回 None（调用方走实时抓取）。"""
        if not SupabaseStore.ENABLED or time.time() < SupabaseStore._stock_hist_fail_until:
            return None
        try:
            rows = SupabaseStore._select(
                SupabaseStore.STOCK_HISTORY_TABLE, "date_key,payload",
                {"kind": f"eq.{kind}", "code": f"eq.{code}",
                 "order": "date_key.desc", "limit": str(limit)})
            out = [{"date_key": r["date_key"], **(r.get("payload") or {})} for r in (rows or [])]
            out.reverse()
            return out
        except requests.HTTPError as e:
            body = str(getattr(getattr(e, "response", None), "text", "") or "")
            if "PGRST205" in body or "Could not find" in body:
                SupabaseStore._stock_hist_fail_until = time.time() + 600
                logger.warning("supabase %s 表不存在：请在 Supabase SQL Editor 执行 supabase/quant_stock_history.sql（10分钟内不再重试）",
                               SupabaseStore.STOCK_HISTORY_TABLE)
            return None
        except Exception as e:  # noqa: BLE001
            logger.warning("supabase stock history read %s/%s fail: %s", kind, code, e)
            return None

    @staticmethod
    def stock_history_ready() -> bool:
        """探测 quant_stock_history 表是否可用（供状态接口展示）。表存在返回 True。"""
        return SupabaseStore.load_stock_history("margin", "__probe__", 1) is not None

    # ---------------- 全市场快照 ----------------
    @staticmethod
    def load_snapshot(trade_date: str, limit: int = 8) -> Optional[list]:
        """读某日全市场快照（最新 limit 行）。返回 [ {code,name,price,change_percent,...} ] 或 None。"""
        try:
            rows = SupabaseStore._select(
                "quant_daily_snapshot", "*",
                {"trade_date": f"eq.{trade_date}", "order": "change_percent.desc", "limit": str(limit)})
            return rows or []
        except Exception as e:
            logger.warning("supabase snapshot read fail: %s", e)
            return None

    @staticmethod
    def save_snapshot(trade_date: str, df, src: str = "") -> bool:
        """将全市场快照 DataFrame 落库（字段映射为英文列）"""
        try:
            if df is None or len(df) == 0:
                return False

            def _g(r, *keys, default=0.0):
                for k in keys:
                    v = r.get(k)
                    if v is None or v == "":
                        continue
                    try:
                        return float(v)
                    except (TypeError, ValueError):
                        pass
                return default

            rows = []
            for _, r in df.iterrows():
                code = str(r.get("代码") or "").strip()
                if not code:
                    continue
                rows.append({
                    "trade_date": trade_date,
                    "code": code,
                    "name": str(r.get("名称") or "")[:50],
                    "price": _g(r, "最新价", "trade", "price"),
                    "change": _g(r, "涨跌额", "change"),
                    "change_percent": _g(r, "涨跌幅", "changepercent"),
                    "volume": _g(r, "成交量", "volume"),
                    "turnover": _g(r, "成交额", "amount"),
                    "high": _g(r, "最高", "high"),
                    "low": _g(r, "最低", "low"),
                    "open": _g(r, "今开", "open"),
                    "prev_close": _g(r, "昨收"),
                    "amplitude": _g(r, "振幅"),
                    "turnover_rate": _g(r, "换手率"),
                    "pe": _g(r, "市盈率", "市盈率-动态", "pe") or None,
                    "pb": _g(r, "市净率", "pb") or None,
                    "market_cap": _g(r, "总市值", "mktcap"),
                    "source": str(src)[:30],
                })
            # 分片写入避免单批次过大
            ok = SupabaseStore.upsert("quant_daily_snapshot", rows, "trade_date,code")
            if ok:
                logger.info("supabase snapshot saved: %s (%d rows)", trade_date, len(rows))
            return ok
        except Exception as e:  # noqa: BLE001
            logger.warning("save_snapshot fail: %s", e)
            return False

    # ---------------- 个股K线 ----------------
    @staticmethod
    def get_kline(code: str, period: str = "1d", days: int = 60) -> Optional[list]:
        """读个股历史K线（按 code+日期倒序取最近 days 条）。返回 [{date,...}] 或 None（库无数据/不可用）。"""
        # 熔断检查：近期失败则直接跳过，不等超时
        if not SupabaseStore.ENABLED or time.time() < SupabaseStore._kline_fail_until:
            return None
        try:
            params = {"code": f"eq.{code}", "period": f"eq.{period}",
                      "order": "trade_date.desc", "limit": str(max(days, 5))}
            rows = SupabaseStore._select("quant_daily_kline", "*", params, timeout=3)
            if not rows:
                return None
            rows.sort(key=lambda x: x.get("trade_date") or "")
            return [{
                "date": r["trade_date"], "open": float(r.get("open") or 0),
                "close": float(r.get("close") or 0), "high": float(r.get("high") or 0),
                "low": float(r.get("low") or 0), "volume": float(r.get("volume") or 0),
                "turnover": float(r.get("turnover") or 0), "amplitude": float(r.get("amplitude") or 0),
                "change_percent": float(r.get("change_percent") or 0),
            } for r in rows[-days:]]
        except Exception as e:
            # 连接类异常（SSL重置/超时/拒绝）触发熔断，10 分钟内跳过 Supabase
            if any(k in str(e).lower() for k in ("ssl", "reset", "timeout", "refused", "unreachable", "10054")):
                SupabaseStore._kline_fail_until = time.time() + SupabaseStore._KLINE_FAIL_BACKOFF
                logger.warning("supabase kline circuit OPEN (10min skip): %s", e)
            else:
                logger.warning("supabase kline read fail: %s", e)
            return None

    @staticmethod
    def save_kline(code: str, period: str, rows: list, src: str = "") -> bool:
        """个股K线落库。rows: [{date,open,close,high,low,volume,turnover,change_percent,...}]"""
        try:
            if not rows:
                return False
            to_save = []
            for r in rows:
                to_save.append({
                    "code": code,
                    "trade_date": str(r["date"])[:10],
                    "period": period,
                    "open": float(r.get("open") or 0), "close": float(r.get("close") or 0),
                    "high": float(r.get("high") or 0), "low": float(r.get("low") or 0),
                    "volume": float(r.get("volume") or 0), "turnover": float(r.get("turnover") or 0),
                    "change_percent": float(r.get("change_percent") or 0),
                    "amplitude": float(r.get("amplitude") or 0),
                    "source": src,
                })
            return SupabaseStore.upsert("quant_daily_kline", to_save, "code,trade_date,period")
        except Exception as e:  # noqa: BLE001
            logger.warning("save_kline fail: %s", e)
            return False

    # ---------------- 板块走势 ----------------
    @staticmethod
    def get_sector_daily(sector: str, days: int = 30) -> Optional[list]:
        """读某板块近 days 日收盘（按日期倒序取最近 days 条）。返回 [{date,close,pct}] 或 None。"""
        try:
            params = {"sector": f"eq.{sector}",
                      "order": "trade_date.desc", "limit": str(max(days, 5))}
            rows = SupabaseStore._select("quant_sector_daily", "*", params)
            if not rows:
                return None
            rows.sort(key=lambda x: x.get("trade_date") or "")
            return [{"date": r["trade_date"], "close": float(r.get("close") or 0),
                     "pct": float(r.get("pct") or 0)} for r in rows[-days:]]
        except Exception as e:
            logger.warning("supabase sector read fail: %s", e)
            return None

    @staticmethod
    def save_sector_daily(sector: str, rows: list, src: str = "") -> bool:
        """板块每日收盘落库。rows: [{date,close,pct}]"""
        try:
            if not rows:
                return False
            to_save = [{"sector": sector, "trade_date": r["date"][:10],
                        "close": float(r.get("close") or 0), "pct": float(r.get("pct") or 0),
                        "source": src} for r in rows]
            return SupabaseStore.upsert("quant_sector_daily", to_save, "sector,trade_date")
        except Exception as e:  # noqa: BLE001
            logger.warning("save_sector_daily fail: %s", e)
            return False

    # ---------------- 娑ㄥ仠姹?杩炴澘/璧勯噾娴?----------------
    @staticmethod
    def get_pool(kind: str, trade_date: str) -> Optional[dict]:
        """读某日某类看板快照 payload。返回 dict 或 None。"""
        try:
            rows = SupabaseStore._select("quant_daily_pool", "payload",
                                         {"kind": f"eq.{kind}", "trade_date": f"eq.{trade_date}", "limit": "1"})
            if not rows:
                return None
            return rows[0].get("payload")
        except Exception as e:
            logger.warning("supabase pool read fail: %s", e)
            return None

    @staticmethod
    def save_pool(kind: str, trade_date: str, payload: dict) -> bool:
        """看板快照落库（JSONB）。"""
        try:
            if payload is None:
                return False
            return SupabaseStore.upsert("quant_daily_pool",
                                        [{"kind": kind, "trade_date": trade_date, "payload": payload}],
                                        "kind,trade_date")
        except Exception as e:  # noqa: BLE001
            logger.warning("save_pool fail: %s", e)
            return False

    @staticmethod
    def save_reports(rows: list) -> bool:
        """券商研报元数据落库（PK report_id，upsert 幂等）。"""
        try:
            if not rows:
                return False
            to_save = []
            for r in rows:
                rid = str(r.get("report_id") or "")
                if not rid:
                    continue
                to_save.append({
                    "report_id": rid,
                    "title": str(r.get("title") or "")[:200],
                    "stock_code": str(r.get("stock_code") or "")[:10],
                    "stock_name": str(r.get("stock_name") or "")[:20],
                    "org_name": str(r.get("org_name") or "")[:50],
                    "author": str(r.get("author") or "")[:50],
                    "publish_date": str(r.get("publish_date") or "")[:10],
                    "rating": str(r.get("rating") or "")[:10],
                    "rating_change": str(r.get("rating_change") or "")[:20],
                    "target_price": r.get("target_price"),
                    "eps_y1": r.get("eps_y1"), "eps_y2": r.get("eps_y2"), "eps_y3": r.get("eps_y3"),
                    "summary": str(r.get("summary") or "")[:200],
                    "url": str(r.get("url") or "")[:200],
                })
            return SupabaseStore.upsert("quant_research_reports", to_save, "report_id") if to_save else False
        except Exception as e:  # noqa: BLE001
            logger.warning("save_reports fail: %s", e)
            return False

    @staticmethod
    def save_news(rows: list) -> bool:
        """财经新闻落库（PK news_id，upsert 幂等）。"""
        try:
            if not rows:
                return False
            to_save = []
            for r in rows:
                nid = str(r.get("id") or r.get("news_id") or "")
                if not nid:
                    continue
                to_save.append({
                    "news_id": nid,
                    "title": str(r.get("title") or "")[:200],
                    "source": str(r.get("source") or "")[:20],
                    "category": str(r.get("category") or "")[:10],
                    "sentiment": str(r.get("sentiment") or "")[:10],
                    "impact": str(r.get("impact") or "")[:10],
                    "news_time": str(r.get("timestamp") or r.get("news_time") or "")[:19],
                    "summary": str(r.get("summary") or "")[:300],
                    "url": str(r.get("url") or "")[:300],
                })
            return SupabaseStore.upsert("quant_news", to_save, "news_id") if to_save else False
        except Exception as e:  # noqa: BLE001
            logger.warning("save_news fail: %s", e)
            return False

    @staticmethod
    def save_dragon_tiger(trade_date: str, rows: list) -> bool:
        """龙虎榜落库（PK trade_date+code）。"""
        try:
            if not rows:
                return False
            to_save = []
            for r in rows:
                code = str(r.get("code") or "")
                if not code:
                    continue
                to_save.append({
                    "trade_date": str(trade_date or "")[:10],
                    "code": code,
                    "name": str(r.get("name") or "")[:20],
                    "close": r.get("close"),
                    "change_percent": r.get("change_percent"),
                    "buy_amount": r.get("buy_amount"),
                    "sell_amount": r.get("sell_amount"),
                    "net_amount": r.get("net_amount"),
                    "seats": r.get("seats"),
                    "reason": str(r.get("reason") or "")[:100],
                })
            return SupabaseStore.upsert("quant_dragon_tiger", to_save, "trade_date,code") if to_save else False
        except Exception as e:  # noqa: BLE001
            logger.warning("save_dragon_tiger fail: %s", e)
            return False

    # ---------------- 同步日志 ----------------
    @staticmethod
    def log_sync(kind: str, trade_date: str, status: str, detail: str = "") -> None:
        try:
            if not SupabaseStore.ENABLED:
                return
            now_t = time.time()
            if now_t - SupabaseStore._last_log < SupabaseStore._log_interval:
                return
            SupabaseStore._last_log = now_t
            SupabaseStore._select("quant_sync_log", "id", {"limit": "1"})  # 探测连通性
            requests.post(SupabaseStore._table("quant_sync_log"),
                          headers=SupabaseStore._HEADERS, timeout=8,
                          json=[{"kind": kind, "trade_date": trade_date,
                                 "status": status, "detail": (detail or "")[:500]}])
        except Exception as e:  # noqa: BLE001
            logger.debug("sync log fail: %s", e)

    # ---------------- 每日同步计数（调度判定用） ----------------
    @staticmethod
    def update_count(trade_date: str, table_name: str, row_count: int,
                     status: str = "ok", detail: str = "") -> None:
        """记录某日某类数据落库数量/状态（quant_daily_count），供定量调度与前台核验。"""
        try:
            if not SupabaseStore.ENABLED:
                return
            requests.post(SupabaseStore._table("quant_daily_count"),
                          params={"on_conflict": "trade_date,table_name"},
                          headers={**SupabaseStore._HEADERS,
                                   "Prefer": "resolution=merge-duplicates,return=minimal"},
                          json=[{"trade_date": trade_date, "table_name": table_name,
                                 "row_count": int(row_count), "status": status,
                                 "detail": (detail or "")[:300],
                                 "updated_at": datetime.now().strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"}],
                          timeout=8)
        except Exception as e:  # noqa: BLE001
            logger.debug("update_count fail: %s", e)

    @staticmethod
    def day_synced(trade_date: str) -> bool:
        """当日该类数据已成功落库（以库状态为准，进程重启不丢失）。"""
        try:
            if not SupabaseStore.ENABLED:
                return False
            rows = SupabaseStore._select(
                "quant_daily_count", "status,row_count",
                {"trade_date": f"eq.{trade_date}", "table_name": "eq.snapshot", "limit": "5"})
            if not rows:
                return False
            return any(str(r.get("status")) == "ok" and int(r.get("row_count") or 0) > 0 for r in rows)
        except Exception:  # noqa: BLE001
            return False

    # ---------------- 行情同步告警 ----------------
    @staticmethod
    def save_alert(trade_date: str, detail: str, level: str = "error") -> None:
        """写入/更新告警（每天每交易日仅一条 open）。"""
        try:
            if not SupabaseStore.ENABLED:
                return
            requests.post(
                SupabaseStore._table("quant_sync_alert"),
                params={"on_conflict": "trade_date"},
                headers={**SupabaseStore._HEADERS, "Prefer": "resolution=merge-duplicates,return=minimal"},
                json=[{"trade_date": trade_date, "level": level, "status": "open",
                       "detail": (detail or "")[:500],
                       "resolved_at": None}],
                timeout=8)
        except Exception as e:  # noqa: BLE001
            logger.debug("save_alert fail: %s", e)

    @staticmethod
    def resolve_alerts(trade_date: str) -> None:
        """成功落库后关闭当日所有 open 告警。"""
        try:
            if not SupabaseStore.ENABLED:
                return
            requests.patch(
                SupabaseStore._table("quant_sync_alert"),
                params={"trade_date": f"eq.{trade_date}", "status": "eq.open"},
                headers={**SupabaseStore._HEADERS, "Prefer": "return=minimal"},
                json={"status": "resolved", "resolved_at": datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ")},
                timeout=8)
        except Exception as e:  # noqa: BLE001
            logger.debug("resolve_alerts fail: %s", e)

    @staticmethod
    def get_open_alerts(limit: int = 10) -> list:
        """读取未解决的同步告警（前台告警轮询）。"""
        try:
            if not SupabaseStore.ENABLED:
                return []
            rows = SupabaseStore._select(
                "quant_sync_alert", "*",
                {"status": "eq.open", "order": "trade_date.desc", "limit": str(limit)})
            return rows or []
        except Exception as e:  # noqa: BLE001
            logger.debug("get_open_alerts fail: %s", e)
            return []

