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
from ..models.entities import StockQuote
from ..services.research import ResearchReportService


try:
    import akshare as ak
except Exception:
    ak = None

# ---- 备用数据源（用户指定多源容灾：Baostock TCP / Ashare 单文件 / adata / Tushare / eodhd）----
try:
    import baostock as bs
except Exception:
    bs = None

# Ashare 单文件行情库（github.com/mpquant/Ashare）：新浪主+腾讯备，原生支持 1d/1w/1M
try:
    from .ashare import get_price as _ashare_get_price
except Exception:
    _ashare_get_price = None

# adata / tushare 依赖较重或需 token，采用函数内懒加载


class AStockDataService:
    """A股数据服务 - 多源聚合 (akshare: 东方财富/新浪财经/腾讯/同花顺)，Vibe/mock 降级。"""

    VIBE_BASE = os.environ.get("VIBE_BASE", "http://127.0.0.1:8900")
    STOCK_POOL = {
        "sh600519": "贵州茅台", "sz000858": "五粮液", "sh601318": "中国平安",
        "sz000001": "平安银行", "sh600036": "招商银行", "sz002594": "比亚迪",
        "sh601899": "紫金矿业", "sz000333": "美的集团", "sh600900": "长江电力",
        "sz300059": "东方财富", "sh688017": "心脉医疗", "sz300750": "宁德时代",
        "sh603259": "药明康德", "sz002475": "立讯精密", "sh601012": "隆基绿能",
    }
    _quote_cache: Dict[str, StockQuote] = {}
    _cache_ts = 0.0
    _CACHE_TTL = 30.0
    _vibe_down_since = 0.0  # Vibe 熔断时间戳（失败后 60s 内直接走腾讯/新浪）
    _all_df = None
    _all_src = ""
    _all_ts = 0.0
    _ALL_TTL = 120.0
    _snap_lock = threading.Lock()
    _batch_lock = threading.Lock()
    # ---- 缓存加固 ----
    _cache_ttl_val = 30.0          # 报价批次实际 TTL（写入时加抖动，防雪崩）
    _all_ttl_val = 120.0           # 全市场快照实际 TTL（写入时加抖动，防雪崩）
    NEG_TTL = 30.0                 # 负缓存基础 TTL（空结果/失败短缓存，防穿透）
    _neg_cache: Dict[str, tuple] = {}   # key -> (timestamp, jittered_ttl)

    @staticmethod
    def _neg_get(key: str) -> bool:
        rec = AStockDataService._neg_cache.get(key)
        return bool(rec and time.time() - rec[0] < rec[1])

    @staticmethod
    def _neg_set(key: str):
        # 写入即抖动，不同 key 错峰过期，避免负缓存集中失效
        AStockDataService._neg_cache[key] = (
            time.time(), AStockDataService.NEG_TTL * random.uniform(0.85, 1.15))

    KLINE_PERIOD_MAP = {"1m": "1", "5m": "5", "15m": "15", "30m": "30", "60m": "60"}
    DAILY_PERIOD_MAP = {"1d": "daily", "d": "daily", "day": "daily", "daily": "daily",
                        "1w": "weekly", "w": "weekly", "week": "weekly", "weekly": "weekly",
                        "1mo": "monthly", "m": "monthly", "month": "monthly", "monthly": "monthly",
                        "all": "daily"}
    FULL_HISTORY_DAYS = 8000  # 请求天数达到该值时全量从上市拉取
    # 任意窗口 K 线内存缓存(5min)：日线/周线/月线切换秒回，不必每次打上游或读 Supabase
    _recent_kline_memo: Dict[str, tuple] = {}  # (code,p)->(ts,rows)
    RECENT_KLINE_TTL = 300
    # 日历窗口系数：days 根周期 bar 需要的日历天数（月≈31、周≈8、日≈2.2 含节假日）
    _PERIOD_CAL_DAYS = {"daily": 2.5, "weekly": 9.0, "monthly": 32.0}
    _full_kline_memo: Dict[str, tuple] = {}  # (code,p)->(ts,rows) 鍏ㄩ噺K绾垮唴瀛樼紦瀛?30min)
    # ---- Baostock 兜底状态：非线程安全，所有调用经 _bs_lock 串行；失败 60s 熔断 ----
    _bs_lock = threading.Lock()
    _bs_logged = False
    _bs_fail_until = 0.0
    _BS_FREQ = {"daily": "d", "weekly": "w", "monthly": "m"}
    _ADATA_KTYPE = {"daily": 1, "weekly": 2, "monthly": 3}  # adata: 1日/2周/3月

    @staticmethod
    def _to_code(symbol: str) -> str:
        s = str(symbol or "")
        if s[:2] in ("sh", "sz", "bj"):
            s = s[2:]
        return AStockDataService._clean_code(s)

    @staticmethod
    def _clean_code(code) -> str:
        """把代码规整为 6 位数字：处理 float 形式（如 '920992.0'）、去掉 sh/sz/bj、补零。"""
        s = str(code or "").strip()
        if s[:2] in ("sh", "sz", "bj"):
            s = s[2:]
        # float 字符串会带 .0（如 920992.0），数字代码直接取整数部分
        if s.endswith(".0"):
            try:
                s = str(int(float(s)))
            except (ValueError, TypeError):
                pass
        # 保留纯数字，去掉残留非数字
        digits = "".join(ch for ch in s if ch.isdigit())
        if not digits:
            return ""
        # 若长度 >6 通常含 B 或美股后缀，截断到 6 位
        if len(digits) > 6:
            digits = digits[:6]
        return digits.zfill(6)

    @staticmethod
    def _to_symbol(code: str) -> str:
        code = AStockDataService._clean_code(code)
        if not code:
            return str(code or "")
        # 北交所 20 / 4 / 8 开头
        if code.startswith(("4", "8", "920")):
            return "bj" + code
        prefix = "sh" if code.startswith(("6", "9")) else "sz"
        return prefix + code

    @staticmethod
    def _with_retry(fn, tries=2, delay=0.8):
        last = None
        for i in range(tries):
            try:
                return fn()
            except Exception as e:
                last = e
                if i < tries - 1:
                    time.sleep(delay)
        raise last

    @staticmethod
    def _vibe_quote_codes() -> List[str]:
        return [AStockDataService._to_code(s) for s in AStockDataService.STOCK_POOL]

    @staticmethod
    def _fetch_vibe_quotes() -> Dict[str, Dict]:
        try:
            url = f"{AStockDataService.VIBE_BASE}/api/quote"
            r = requests.get(url, params={"codes": ",".join(AStockDataService._vibe_quote_codes())}, timeout=8)
            r.raise_for_status()
            return r.json().get("data") or {}
        except Exception as e:
            logger.warning("Vibe quote 源不可用: %s", e)
            return {}

    @staticmethod
    def _run_with_deadline(fn, budget: float):
        """守护线程执行 fn，超时 budget 秒即放弃（僵尸线程留在后台，不阻塞进程）。"""
        box = {}

        def runner():
            try:
                box["value"] = fn()
            except Exception as e:  # noqa: BLE001
                box["error"] = e

        th = threading.Thread(target=runner, daemon=True)
        th.start()
        th.join(budget)
        if "value" in box:
            return box["value"]
        if "error" in box:
            raise box["error"]
        raise TimeoutError(f"fetch exceeded {budget}s")

    @staticmethod
    def _em_snapshot_direct() -> pd.DataFrame:
        """东方财富 clist 直连拉取全市场快照（主域名被 WAF 拦时切换 push2delay 镜像）。"""
        url_tpl = "https://{host}/api/qt/clist/get"
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                          "(KHTML, like Gecko) Chrome/126.0 Safari/537.36",
            "Referer": "https://quote.eastmoney.com/",
        }
        fields = "f2,f3,f4,f5,f6,f7,f8,f9,f12,f14,f15,f16,f17,f18,f20,f23"
        col_map = {"f12": "代码", "f14": "名称", "f2": "最新价", "f3": "涨跌幅", "f4": "涨跌额", "f5": "成交量", "f6": "成交额", "f7": "振幅", "f8": "换手率", "f9": "市盈率", "f15": "最高", "f16": "最低", "f17": "今开", "f18": "昨收", "f20": "总市值", "f23": "市净率"}
        rows = []

        def parse_diff(diff):
            out = []
            for d in diff:
                row = {}
                for k, v in d.items():
                    col = col_map.get(k)
                    if not col:
                        continue
                    if isinstance(v, str):
                        try:
                            v = float(v) if v not in ("-", "") else 0.0
                        except ValueError:
                            pass
                    row[col] = v
                code = AStockDataService._clean_code(row.get("代码") or "")
                if code:
                    row["代码"] = code
                out.append(row)
            return out

        def fetch_page(host, pn):
            """东财 clist 单页抓取（返回原始 data 节点）。"""
            params = {"pn": pn, "pz": 100, "po": 1, "np": 1, "fltt": 2, "invt": 2,
                      "fid": "f12", "fs": "m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048",
                      "fields": fields}
            r = requests.get(url_tpl.format(host=host), params=params, headers=headers, timeout=20)
            r.raise_for_status()
            return (r.json() or {}).get("data") or {}

        def fetch_page_retry(host, pn, retries=3):
            """单页抓取，带退避重试（东财对高频请求易拒绝连接）。"""
            last = None
            for attempt in range(retries):
                try:
                    return fetch_page(host, pn)
                except Exception as e:  # noqa: BLE001
                    last = e
                    time.sleep(0.6 * (attempt + 1))
            raise last

        # 选出可用主机（push2delay 镜像优先；主域名对非浏览器 TLS 指纹拦截）
        host, first = None, None
        for h in ("push2delay.eastmoney.com", "push2.eastmoney.com"):
            try:
                first = fetch_page_retry(h, 1)
                host = h
                break
            except Exception as e:  # noqa: BLE001
                logger.warning("snapshot host %s unavailable: %s", h, e)
        if not host:
            raise RuntimeError("all eastmoney hosts unavailable")
        rows.extend(parse_diff(first.get("diff") or []))
        total = int(first.get("total") or 0)
        last_page = min((total + 99) // 100, 70) if total else 1
        if last_page > 1:
            with ThreadPoolExecutor(max_workers=4) as ex:
                futs = {ex.submit(fetch_page_retry, host, pn): pn for pn in range(2, last_page + 1)}
                page_rows = {}
                for fut in as_completed(futs):
                    pn = futs[fut]
                    try:
                        page_rows[pn] = parse_diff(fut.result().get("diff") or [])
                    except Exception as e:  # noqa: BLE001
                        logger.warning("snapshot page %s failed: %s", pn, e)
                for pn in sorted(page_rows):
                    rows.extend(page_rows[pn])
        if len(rows) < max(int(total * 0.5), 100) and total:
            raise RuntimeError(f"incomplete snapshot {len(rows)}/{total}")
        if not rows:
            raise RuntimeError("empty snapshot")
        return pd.DataFrame(rows)

    @staticmethod
    def _sina_snapshot() -> pd.DataFrame:
        """新浪财经全量 A 股快照（东财被 WAF 拦截时的备用数据源）。

        通过 vip.stock.finance.sina.com.cn 的 JSON 接口分页拉取沪深京 A 股，
        每页 100 条，返回字段与东财快照对齐。
        """
        import json, math
        headers = {"User-Agent": "Mozilla/5.0", "Referer": "https://finance.sina.com.cn"}
        base = "https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData"
        count_url = "https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeStockCount"
        rows = []
        # 获取总数
        r = requests.get(count_url, params={"node": "hs_a"}, headers=headers, timeout=8)
        total = int(r.text.strip('"') or 0)
        if not total:
            raise RuntimeError("sina count=0")
        pages = math.ceil(total / 100)
        logger.info("sina snapshot: total=%s pages=%s", total, pages)

        def fetch_page(pn):
            r = requests.get(base, params={"page": pn, "num": 100, "node": "hs_a"},
                             headers=headers, timeout=10)
            items = json.loads(r.text) if r.text else []
            out = []
            for item in items:
                code = str(item.get("code") or "")
                if not code:
                    continue
                # 转为带交易所前缀的 symbol
                prefix = "sh" if code.startswith(("50", "51", "52", "53", "60", "68", "9")) else "sz"
                if code.startswith(("8", "4", "920")):
                    prefix = "bj"
                symbol = f"{prefix}{code}"
                name = item.get("name") or code
                price = float(item.get("trade") or 0)
                prev_close = float(item.get("settlement") or 0)
                change_pct = float(item.get("changepercent") or 0)
                change = float(item.get("pricechange") or 0)
                rows.append({
                    "代码": code, "名称": name,
                    "最新价": price, "涨跌幅": change_pct, "涨跌额": change,
                    "成交量": float(item.get("volume") or 0),
                    "成交额": float(item.get("amount") or 0),
                    "振幅": float(item.get("amplitude") or 0),
                    "换手率": float(item.get("turnoverratio") or 0),
                    "市盈率": float(item.get("pe") or 0),
                    "最高": float(item.get("high") or 0),
                    "最低": float(item.get("low") or 0),
                    "今开": float(item.get("open") or 0),
                    "昨收": prev_close,
                    "总市值": float(item.get("mktcap") or 0),
                    "市净率": float(item.get("pb") or 0),
                })
            return items

        with ThreadPoolExecutor(max_workers=4) as ex:
            futs = {ex.submit(fetch_page, pn): pn for pn in range(1, pages + 1)}
            for fut in as_completed(futs):
                try:
                    fut.result()
                except Exception as e:
                    logger.warning("sina page %s failed: %s", futs[fut], e)
        logger.info("sina snapshot done: rows=%s", len(rows))
        if len(rows) < max(int(total * 0.5), 100):
            raise RuntimeError(f"sina incomplete {len(rows)}/{total}")
        return pd.DataFrame(rows)

    @staticmethod
    def _fetch_snapshot_once():
        """依次尝试：东财直连 -> 新浪财经 -> akshare 东财；任一成功即返回 (df, src)，均失败返回 (None, '')。
        注：同花顺快照不返回名称/PE/PB/总市值，仅用于单股报价与特色数据端点，不作全市场快照源。"""
        attempts = [
            (lambda: AStockDataService._em_snapshot_direct(), "东财直连", 25.0),
            (lambda: AStockDataService._sina_snapshot(), "新浪财经", 30.0),
        ]
        if ak is not None:
            attempts.append((lambda: ak.stock_zh_a_spot_em(), "东方财富(akshare)", 8.0))
        for fn, src, budget in attempts:
            t0 = time.time()
            try:
                df = AStockDataService._run_with_deadline(fn, budget)
                if df is not None and len(df):
                    logger.info("snapshot OK from %s rows=%s %.1fs", src, len(df), time.time() - t0)
                    return df, src
            except Exception as e:  # noqa: BLE001
                logger.warning("snapshot %s failed %.1fs: %s", src, time.time() - t0, e)
        return None, ""

    @staticmethod
    def _load_all_snapshot(force=False):
        # 跳过缓存直接抓取
        now = time.time()
        if not force and AStockDataService._all_df is not None \
                and now - AStockDataService._all_ts < AStockDataService._all_ttl_val:
            return AStockDataService._all_df, AStockDataService._all_src
        lock = AStockDataService._snap_lock
        if lock.acquire(blocking=False):
            try:
                # 拿到锁后二次检查（可能别的请求刚写完缓存）
                if not force and AStockDataService._all_df is not None \
                        and time.time() - AStockDataService._all_ts < AStockDataService._all_ttl_val:
                    return AStockDataService._all_df, AStockDataService._all_src
                df, src = AStockDataService._fetch_snapshot_once()
                if df is not None and len(df):
                    AStockDataService._all_df = df
                    AStockDataService._all_src = src
                    AStockDataService._all_ts = time.time()
                    # TTL 抖动防雪崩：快照错峰过期
                    AStockDataService._all_ttl_val = AStockDataService._ALL_TTL * random.uniform(0.85, 1.15)
                    # 异步把当天全市场快照落库（不阻塞请求返回）
                    threading.Thread(
                        target=lambda: SupabaseStore.save_snapshot(
                            datetime.now().strftime("%Y-%m-%d"), df, src),
                        daemon=True).start()
                    return df, src
            finally:
                lock.release()
        else:
            # 已有请求在拉取：最多等 12s 共享其结果，超时走旧缓存/失败兜底
            if lock.acquire(timeout=12):
                try:
                    return AStockDataService._all_df, AStockDataService._all_src
                finally:
                    lock.release()
        # 拉取失败但有历史快照 -> 返回过期数据兜底（总比挂死/空页面好）
        if AStockDataService._all_df is not None:
            return AStockDataService._all_df, (AStockDataService._all_src or "snapshot") + "(stale)"
        return None, ""

    @staticmethod
    def _row_num(row: Dict, *keys) -> float:
        for k in keys:
            v = row.get(k)
            if v is None:
                continue
            try:
                return float(v)
            except (TypeError, ValueError):
                pass
        return 0.0

    @staticmethod
    def _row_name(row: Dict, fallback: str) -> str:
        for k in ("名称", "name"):
            v = row.get(k)
            if v:
                return str(v)
        return fallback

    @staticmethod
    def _snapshot_quote(row: Dict, symbol: str, name: str) -> Optional[StockQuote]:
        price = AStockDataService._row_num(row, "最新价", "trade", "price")
        prev = AStockDataService._row_num(row, "昨收", "settlement")
        change_pct = AStockDataService._row_num(row, "涨跌幅", "changepercent")
        change = AStockDataService._row_num(row, "涨跌额", "change")
        if not change_pct and prev:
            change_pct = (price - prev) / prev * 100 if prev else 0
        if not change and prev:
            change = price - prev
        return StockQuote(
            symbol=symbol, name=AStockDataService._row_name(row, name),
            price=price, change=change, change_percent=change_pct,
            volume=AStockDataService._row_num(row, "成交量", "volume"),
            turnover=AStockDataService._row_num(row, "成交额", "amount"),
            high=AStockDataService._row_num(row, "最高", "high"),
            low=AStockDataService._row_num(row, "最低", "low"),
            open=AStockDataService._row_num(row, "今开", "open"),
            amplitude=AStockDataService._row_num(row, "振幅", "amplitude"),
            turnover_rate=AStockDataService._row_num(row, "换手率", "turnoverratio"),
            pe=AStockDataService._row_num(row, "市盈率", "pe") or None,
            pb=AStockDataService._row_num(row, "市净率", "pb") or None,
            market_cap=AStockDataService._row_num(row, "总市值", "mktcap"),
            updated_at=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        )

    @staticmethod
    def _use_vibe_quote(symbol: str, name: str) -> Optional[StockQuote]:
        try:
            url = f"{AStockDataService.VIBE_BASE}/api/quote"
            vcode = AStockDataService._to_code(symbol)
            r = requests.get(url, params={"codes": vcode}, timeout=8)
            r.raise_for_status()
            q = (r.json().get("data") or {}).get(vcode)
            if q:
                ts = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                return StockQuote(
                    symbol=symbol, name=q.get("name") or name,
                    price=float(q.get("price") or 0), change=float(q.get("change_amt") or 0),
                    change_percent=float(q.get("change_pct") or 0),
                    volume=float(q.get("amount_wan") or 0) * 10000,
                    turnover=float(q.get("amount_wan") or 0) * 10000,
                    high=float(q.get("high") or 0), low=float(q.get("low") or 0),
                    open=float(q.get("open") or 0),
                    amplitude=float(q.get("amplitude_pct") or 0),
                    turnover_rate=float(q.get("turnover_pct") or 0),
                    pe=q.get("pe_ttm"), pb=q.get("pb"),
                    market_cap=float(q.get("mcap_yi") or 0) * 10000,
                    updated_at=ts,
                )
        except Exception as e:
            logger.warning("Vibe single quote failed for %s: %s", symbol, e)
        return None

    @staticmethod
    def _mock_quote(symbol: str, name: str = "") -> StockQuote:
        seed = int(hashlib.md5(symbol.encode()).hexdigest()[:8], 16)
        rng = np.random.RandomState(seed)
        base_price = rng.uniform(20, 2000)
        change_pct = rng.uniform(-3, 3)
        change = base_price * change_pct / 100
        close = base_price + change
        high = max(base_price, close) + rng.uniform(0, 2)
        low = min(base_price, close) - rng.uniform(0, 2)
        volume = rng.uniform(5000, 200000)
        turnover = volume * close
        amplitude = ((high - low) / base_price) * 100 if base_price > 0 else 0
        turnover_rate = rng.uniform(0.1, 8)
        pe = rng.uniform(5, 80)
        pb = rng.uniform(0.5, 15)
        return StockQuote(
            symbol=symbol, name=name or symbol,
            price=round(close, 2), change=round(change, 2),
            change_percent=round(change_pct, 2),
            volume=round(volume, 0), turnover=round(turnover, 2),
            high=round(high, 2), low=round(low, 2),
            open=round(base_price, 2), amplitude=round(amplitude, 2),
            turnover_rate=round(turnover_rate, 2),
            pe=round(pe, 2), pb=round(pb, 2),
            market_cap=round(close * (rng.uniform(5, 500)), 2),
            updated_at=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        )

    @staticmethod
    def get_stock_list(limit: int = 0) -> List[Dict]:
        df, src = AStockDataService._load_all_snapshot()
        if df is not None and len(df):
            result = []
            for row in df.to_dict("records"):
                code = str(row.get("代码") or row.get("code") or "")
                if not code:
                    continue
                symbol = AStockDataService._to_symbol(code)
                name = AStockDataService.STOCK_POOL.get(symbol) or AStockDataService._row_name(row, code)
                q = AStockDataService._snapshot_quote(row, symbol, name)
                if q is None:
                    continue
                d = asdict(q)
                d["source"] = src
                result.append(d)
                if limit > 0 and len(result) >= limit:
                    break
            if result:
                AStockDataService.STOCK_POOL.update({d["symbol"]: d["name"] for d in result})
                return result
        pool = AStockDataService._load_quote_batch()
        result = [asdict(pool[s]) for s in AStockDataService.STOCK_POOL if s in pool]
        if result:
            return result
        # 彻底离线兜底：mock 数据保证页面有内容
        return [asdict(AStockDataService._mock_quote(s, n)) for s, n in AStockDataService.STOCK_POOL.items()]

    @staticmethod
    def _tencent_batch_quotes(symbols: List[str]) -> Dict[str, StockQuote]:
        """腾讯 qt.gtimg.cn 批量实时行情（单次请求，比较稳）。"""
        out: Dict[str, StockQuote] = {}
        if not symbols:
            return out
        try:
            url = "https://qt.gtimg.cn/q=" + ",".join(symbols)
            r = requests.get(url, timeout=6, headers={"User-Agent": "Mozilla/5.0"})
            r.encoding = "gbk"
        except Exception as e:
            logger.warning("tencent batch failed: %s", e)
            return out
        ts = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        def num(f, i):
            if i < len(f) and f[i] not in ("", "-"):
                try:
                    return float(f[i])
                except ValueError:
                    pass
            return 0.0

        for line in r.text.split(";"):
            line = line.strip()
            if "=" not in line:
                continue
            key, _, raw = line.partition("=")
            sym = key.strip().replace("v_", "")
            f = raw.strip().strip('"').split("~")
            if len(f) < 35 or not f[3]:
                continue
            price = num(f, 3)
            prev = num(f, 4)
            out[sym] = StockQuote(
                symbol=sym, name=f[1] or AStockDataService.STOCK_POOL.get(sym, sym),
                price=price,
                change=num(f, 31) or (price - prev),
                change_percent=num(f, 32) or ((price - prev) / prev * 100 if prev else 0),
                volume=num(f, 36), turnover=num(f, 37) * 10000,
                high=num(f, 33), low=num(f, 34), open=num(f, 5),
                amplitude=num(f, 43), turnover_rate=num(f, 38),
                pe=num(f, 39) or None, pb=num(f, 46) or None,
                market_cap=num(f, 45) * 1e8, updated_at=ts,
            )
        return out

    @staticmethod
    def _load_quote_batch(force=False) -> Dict[str, StockQuote]:
        now = time.time()
        if not force and now - AStockDataService._cache_ts < AStockDataService._cache_ttl_val and AStockDataService._quote_cache:
            return AStockDataService._quote_cache
        lock = AStockDataService._batch_lock
        if not lock.acquire(blocking=False):
            # 已有请求正在刷新：最多等 10s 共享结果
            if lock.acquire(timeout=10) :
                try:
                    return AStockDataService._quote_cache
                finally:
                    lock.release()
            return AStockDataService._quote_cache
        try:
            if not force and time.time() - AStockDataService._cache_ts < AStockDataService._cache_ttl_val \
                    and AStockDataService._quote_cache:
                return AStockDataService._quote_cache
            cache = AStockDataService._tencent_batch_quotes(list(AStockDataService.STOCK_POOL.keys()))
            misses = [s for s in AStockDataService.STOCK_POOL if s not in cache][:10]
            for sym in misses:
                try:
                    q = AStockDataService.get_realtime_quote(sym, AStockDataService.STOCK_POOL[sym])
                    if q:
                        cache[sym] = q
                except Exception as e:  # noqa: BLE001
                    logger.warning("single quote %s failed: %s", sym, e)
            if cache:
                AStockDataService._quote_cache = cache
                AStockDataService._cache_ts = time.time()
                # TTL 抖动：不同批次错峰过期，防止集中失效引发雪崩
                AStockDataService._cache_ttl_val = AStockDataService._CACHE_TTL * random.uniform(0.85, 1.15)
            return cache
        finally:
            lock.release()

    @staticmethod
    def get_realtime_quote(symbol: str, name: str = "") -> StockQuote:
        """实时行情: 腾讯(akshare hist_tx) -> 东财/新浪快照 -> Vibe -> mock"""
        code = AStockDataService._to_code(symbol)
        sym = AStockDataService._to_symbol(code)
        name = name or AStockDataService.STOCK_POOL.get(sym, symbol)
        if ak is not None:
            try:
                today = datetime.now().strftime("%Y%m%d")
                start = (datetime.now() - timedelta(days=10)).strftime("%Y%m%d")
                df = AStockDataService._with_retry(
                    lambda: ak.stock_zh_a_hist_tx(symbol=sym, start_date=start, end_date=today), 2, 0.8)
                if df is not None and len(df):
                    last = df.iloc[-1]
                    prev = float(last["close"]) if len(df) > 1 else float(last["open"])
                    price = float(last["close"])
                    change = price - prev
                    change_pct = change / prev * 100 if prev else 0
                    return StockQuote(
                        symbol=sym, name=name, price=price, change=change, change_percent=change_pct,
                        volume=float(last.get("volume") or 0), turnover=float(last.get("amount") or 0),
                        high=float(last["high"]), low=float(last["low"]), open=float(last["open"]),
                        amplitude=0.0, turnover_rate=0.0,
                        updated_at=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                    )
            except Exception as e:
                logger.warning("TX quote %s failed: %s", sym, e)
            df, src = AStockDataService._load_all_snapshot()
            if df is not None and len(df):
                rows = df.to_dict("records")
                for row in rows:
                    c = str(row.get("代码") or row.get("code") or "")
                    if c and AStockDataService._to_symbol(c) == sym:
                        q = AStockDataService._snapshot_quote(row, sym, name)
                        if q:
                            return q
        vibe = AStockDataService._use_vibe_quote(sym, name)
        if vibe:
            return vibe
        return AStockDataService._mock_quote(sym, name)

    @staticmethod
    def _tag(rows, source):
        if rows:
            rows[0]["source"] = source
        return rows

    @staticmethod
    def _resample_daily(rows: List[Dict], mode: str) -> List[Dict]:
        """按重采样为周K/月K。"""
        if not rows or mode not in ("weekly", "monthly"):
            return list(rows or [])
        rule = "ME" if pd.__version__ >= "2.2" else "M"
        if mode == "weekly":
            rule = "W-FRI"
        df = pd.DataFrame(rows)
        df["date"] = pd.to_datetime(df["date"])
        df = df.set_index("date").sort_index()
        if df.empty:
            return []
        agg = df.resample(rule).agg({
            "open": "first", "high": "max", "low": "min",
            "close": "last", "volume": "sum", "turnover": "sum",
        }).dropna(subset=["open"])
        out = []
        for dt, r in agg.iterrows():
            out.append({
                "date": dt.strftime("%Y-%m-%d"), "open": round(float(r["open"]), 2),
                "close": round(float(r["close"]), 2), "high": round(float(r["high"]), 2),
                "low": round(float(r["low"]), 2), "volume": float(r["volume"] or 0),
                "turnover": float(r["turnover"] or 0), "amplitude": 0.0, "change_percent": 0.0,
            })
        return out

    @staticmethod
    def _tx_dt_str(v) -> str:
        """腾讯/新浪时间串规整为 YYYY-MM-DD HH:MM；纯日期则返回 YYYY-MM-DD。"""
        s = str(v or "").strip()
        if "-" in s:
            return s[:16]
        s = "".join(ch for ch in s if ch.isdigit())
        if len(s) == 8:
            return f"{s[:4]}-{s[4:6]}-{s[6:8]}"
        if len(s) >= 12:
            return f"{s[:4]}-{s[4:6]}-{s[6:8]} {s[8:10]}:{s[10:12]}"
        return s

    @staticmethod
    def _tx_minute_intraday(symbol: str) -> List[Dict]:
        """腾讯当日分时: 每分 价格/均价/成交量/成交额，均价由累计额/累计量算出。"""
        try:
            url = "https://web.ifzq.gtimg.cn/appstock/app/minute/query"
            r = requests.get(url, params={"code": symbol},
                             headers={"User-Agent": "Mozilla/5.0"}, timeout=10)
            r.raise_for_status()
            node = r.json().get("data", {}).get(symbol, {}).get("data", {}) or {}
            lines = node.get("data") or []
            day = AStockDataService._tx_dt_str(node.get("date") or datetime.now().strftime("%Y-%m-%d"))
            if len(day) > 10:
                day = day[:10]
            out = []
            prev_vol, prev_amt = 0.0, 0.0
            has_cum = False
            for line in lines:
                parts = [x for x in re.split(r"[\s\t,]+", str(line)) if x]
                if len(parts) < 4:
                    continue
                t = parts[0]
                try:
                    price = float(parts[1])
                    vol_cum = float(parts[2])  # 累计成交量
                    amt_cum = float(parts[3])  # 累计成交额
                except (TypeError, ValueError):
                    continue
                if not has_cum:
                    has_cum = True
                    minute_vol = vol_cum
                    minute_amt = amt_cum
                else:
                    minute_vol = vol_cum - prev_vol if vol_cum >= prev_vol else vol_cum
                    minute_amt = amt_cum - prev_amt if amt_cum >= prev_amt else amt_cum
                prev_vol, prev_amt = vol_cum, amt_cum
                if minute_vol < 0:
                    minute_vol = 0.0
                if minute_amt < 0:
                    minute_amt = 0.0
                avg = (amt_cum / (vol_cum * 100)) if vol_cum else price
                hm = f"{t[:2]}:{t[2:4]}" if len(t) == 4 and t.isdigit() else str(t)
                out.append({"date": day, "datetime": f"{day} {hm}", "open": price,
                            "high": price, "low": price, "close": price, "avg": round(avg, 2),
                            "volume": round(minute_vol * 100, 0), "turnover": round(minute_amt, 2),
                            "amplitude": 0.0, "change_percent": 0.0})
            return out
        except Exception as e:
            logger.warning("TX intraday %s failed: %s", symbol, e)
            return []

    @staticmethod
    def _sina_minute_batch(symbol: str, bars: int = 1100) -> List[Dict]:
        """新浪 getKLineData 拉取1分钟K: datalen 最多1100条(约5天数据)。"""
        try:
            url = "https://quotes.sina.cn/cn/api/jsonp_v2.php/var%20_=/CN_MarketDataService.getKLineData"
            r = requests.get(url, params={"symbol": symbol, "scale": 1, "ma": "no", "datalen": int(bars)},
                             headers={"User-Agent": "Mozilla/5.0", "Referer": "https://finance.sina.com.cn"},
                             timeout=12)
            r.raise_for_status()
            txt = r.text
            start, end = txt.find("["), txt.rfind("]")
            if start < 0 or end <= start:
                return []
            data = json.loads(txt[start:end + 1])
            out = []
            for row in data:
                day = str(row.get("day") or "")[:19]
                if not day:
                    continue
                out.append({
                    "date": day[:10], "datetime": day,
                    "open": float(row.get("open") or 0), "close": float(row.get("close") or 0),
                    "high": float(row.get("high") or 0), "low": float(row.get("low") or 0),
                    "volume": float(row.get("volume") or 0),
                    "turnover": float(row.get("amount") or 0),
                    "avg": 0.0, "amplitude": 0.0, "change_percent": 0.0,
                })
            return out
        except Exception as e:
            logger.warning("SINA batch minute %s failed: %s", symbol, e)
            return []

    @staticmethod
    def _tx_mkline(symbol: str, m: str = "1", bars: int = 1200) -> List[Dict]:
        """腾讯分钟K: m in 1/5/15/30/60；m1 最多320条，m5 以上较少。"""
        try:
            count = max(120, min(int(bars), 3000))
            url = "https://ifzq.gtimg.cn/appstock/app/kline/mkline"
            r = requests.get(url, params={"param": f"{symbol},m{m},,{count}"},
                             headers={"User-Agent": "Mozilla/5.0"}, timeout=12)
            r.raise_for_status()
            node = r.json().get("data", {}).get(symbol, {})
            arr = node.get(f"m{m}") or node.get(str(m)) or []
            out = []
            for f in arr:
                if not f or len(f) < 5:
                    continue
                try:
                    price = float(f[1])
                except (TypeError, ValueError):
                    continue
                ts = AStockDataService._tx_dt_str(f[0])
                day = ts[:10]
                volume = 0.0
                for vi in (5, 6, 7):
                    try:
                        volume = float(f[vi])
                        if volume:
                            break
                    except (TypeError, ValueError, IndexError):
                        continue
                out.append({"date": day, "datetime": ts, "open": price,
                            "close": float(f[2]), "high": float(f[3]), "low": float(f[4]),
                            "avg": 0.0, "volume": round(volume * 100, 0), "turnover": 0.0,
                            "amplitude": 0.0, "change_percent": 0.0})
            return out
        except Exception as e:
            logger.warning("TX mkline %s m%s failed: %s", symbol, m, e)
            return []

    @staticmethod
    def _fetch_minute(sym: str, p: str, days: int) -> tuple:
        """分钟K: 新浪优先，腾讯 mK 兜底。返回 (rows, source)。"""
        if ak is not None:
            try:
                df = AStockDataService._with_retry(
                    lambda: ak.stock_zh_a_minute(symbol=sym, period=AStockDataService.KLINE_PERIOD_MAP[p]), 2, 0.8)
                if df is not None and len(df):
                    tail = df.tail(min(len(df), max(days * 4, 240)))
                    rows = []
                    for _, r in tail.iterrows():
                        rows.append({
                            "date": str(r["day"])[:19], "open": float(r["open"]), "close": float(r["close"]),
                            "high": float(r["high"]), "low": float(r["low"]),
                            "volume": float(r.get("volume") or 0), "turnover": float(r.get("amount") or 0),
                            "amplitude": 0.0, "change_percent": 0.0,
                        })
                    if rows:
                        return rows, "新浪财经"
            except Exception as e:
                logger.warning("SINA minute %s %s failed: %s", sym, p, e)
        m = AStockDataService.KLINE_PERIOD_MAP[p]
        if p == "1m":
            rows = AStockDataService._tx_mkline(sym, "1", bars=max(240, days * 480))
            source = "腾讯分时"
        else:
            rows = AStockDataService._tx_mkline(sym, m, bars=max(160, days * 96))
            source = f"腾讯{m}分"
        return rows, source

    @staticmethod
    def _mock_minute(sym: str, p: str, days: int) -> List[Dict]:
        """分钟K兜底（极端异常时）。"""
        seed = int(hashlib.md5(f"min_{sym}_{p}".encode()).hexdigest()[:8], 16)
        rng = np.random.RandomState(seed)
        per_day = {"1m": 240, "5m": 48, "15m": 16, "30m": 8, "60m": 4}
        step = per_day.get(p, 60)
        n = min(max(days * step, step), 1200)
        base = rng.uniform(10, 500)
        now = datetime.now()
        out = []
        days_back = 0
        while len(out) < n and days_back < days + 21:
            d = now - timedelta(days=days_back)
            days_back += 1
            if d.weekday() >= 5:
                continue
            t = datetime(d.year, d.month, d.day, 9, 30)
            end = datetime(d.year, d.month, d.day, 15, 0)
            while t < end and len(out) < n:
                if (t.hour, t.minute) >= (11, 31) and (t.hour, t.minute) <= (12, 59):
                    t = t.replace(hour=13, minute=0)
                change = rng.uniform(-0.5, 0.5)
                close = base + change
                out.append({
                    "date": d.strftime("%Y-%m-%d"), "datetime": t.strftime("%Y-%m-%d %H:%M"),
                    "open": round(base, 2), "high": round(max(base, close) + rng.uniform(0, 0.3), 2),
                    "low": round(min(base, close) - rng.uniform(0, 0.3), 2), "close": round(close, 2),
                    "avg": round(close, 2), "volume": round(rng.uniform(500, 8000), 0),
                    "turnover": 0.0, "amplitude": 0.0, "change_percent": 0.0,
                })
                base = close
                t += timedelta(minutes=step)
        return out

    # ---------------- 多源兜底（Baostock TCP / Ashare / adata / Tushare） ----------------
    @staticmethod
    def _bs_code(symbol: str) -> str:
        """sh600519 -> sh.600519（baostock 代码格式）"""
        s = AStockDataService._to_symbol(symbol)
        return f"{s[:2]}.{s[2:]}" if len(s) > 2 else s

    @staticmethod
    def _bs_login():
        """Baostock 懒登录（必须已持有 _bs_lock）。"""
        if not AStockDataService._bs_logged:
            lg = bs.login()
            if lg is not None and getattr(lg, "error_code", "1") == "0":
                AStockDataService._bs_logged = True
            else:
                raise RuntimeError(f"baostock login fail: {getattr(lg, 'error_msg', '')}")

    @staticmethod
    def _baostock_kline(code: str, ak_period: str, start_date: str, end_date: str) -> List[Dict]:
        """Baostock 历史 K 线（自有 TCP 协议免 HTTP 风控；日/周/月+前复权，可拉 1990 至今全历史）。
        返回统一 rows；不可用时抛异常。volume 单位换算为手（baostock 为股）。"""
        if bs is None:
            raise RuntimeError("baostock not installed")
        if time.time() < AStockDataService._bs_fail_until:
            raise RuntimeError("baostock circuit open (recent failure)")

        def _query():
            AStockDataService._bs_login()
            rs = bs.query_history_k_data_plus(
                AStockDataService._bs_code(code),
                "date,open,high,low,close,volume,amount,pctChg",
                start_date=start_date, end_date=end_date,
                frequency=AStockDataService._BS_FREQ.get(ak_period, "d"),
                adjustflag="2")  # 2=前复权
            if getattr(rs, "error_code", "1") != "0":
                raise RuntimeError(f"baostock query fail: {getattr(rs, 'error_msg', '')}")
            return rs

        with AStockDataService._bs_lock:
            try:
                rs = _query()
            except Exception:
                # 会话失效：重置登录态后重试一次
                AStockDataService._bs_logged = False
                rs = _query()
            rows = []
            while rs.next():
                d = rs.get_row_data()
                try:
                    o = float(d[1])
                except (TypeError, ValueError):
                    continue  # 停牌等空行跳过
                rows.append({
                    "date": d[0][:10], "open": o, "close": float(d[4]),
                    "high": float(d[2]), "low": float(d[3]),
                    "volume": round(float(d[5] or 0) / 100, 0),  # 股 -> 手，与东财口径一致
                    "turnover": float(d[6] or 0), "amplitude": 0.0,
                    "change_percent": float(d[7] or 0),
                })
            return rows

    @staticmethod
    def _ashare_kline(sym: str, ak_period: str, count: int) -> List[Dict]:
        """Ashare 单文件源（新浪主/腾讯备独立实现，原生 1d/1w/1M；单次上限约 800 根，不适合全量历史）。"""
        if _ashare_get_price is None:
            raise RuntimeError("Ashare unavailable")
        freq = {"daily": "1d", "weekly": "1w", "monthly": "1M"}.get(ak_period)
        if not freq:
            raise RuntimeError(f"ashare unsupported period: {ak_period}")
        df = _ashare_get_price(sym, count=max(int(count), 10), frequency=freq)
        if df is None or not len(df):
            return []
        rows = []
        for idx, r in df.iterrows():
            rows.append({
                "date": str(idx)[:10], "open": float(r["open"]), "close": float(r["close"]),
                "high": float(r["high"]), "low": float(r["low"]),
                "volume": float(r.get("volume") or 0), "turnover": 0.0,
                "amplitude": 0.0, "change_percent": 0.0,
            })
        return rows

    @staticmethod
    def _adata_kline(code: str, ak_period: str, start_date: str, end_date: str) -> List[Dict]:
        """adata 兜底（东财族源，风控解除后可用；懒加载避免 py_mini_racer 拖累启动）。"""
        try:
            import adata
        except Exception:
            raise RuntimeError("adata not installed")
        df = adata.stock.market.get_market(
            stock_code=code, start_date=start_date, end_date=end_date,
            k_type=AStockDataService._ADATA_KTYPE.get(ak_period, 1), adjust_type=1)
        if df is None or not len(df):
            return []
        cols = list(df.columns)

        def _s(r, *names):
            for c in names:
                if c in cols and r[c] not in (None, ""):
                    return str(r[c])
            return ""

        def _f(r, *names):
            for c in names:
                if c in cols:
                    try:
                        v = r[c]
                        if v not in (None, "") and str(v).lower() != "nan":
                            return float(v)
                    except (TypeError, ValueError):
                        pass
            return 0.0

        return [{
            "date": _s(r, "trade_date", "date")[:10],
            "open": _f(r, "open"), "close": _f(r, "close"),
            "high": _f(r, "high"), "low": _f(r, "low"),
            "volume": _f(r, "volume"), "turnover": _f(r, "amount", "turnover"),
            "amplitude": _f(r, "amplitude"), "change_percent": _f(r, "change_percent", "pct_change"),
        } for _, r in df.iterrows()]

    @staticmethod
    def _tushare_kline(code: str, ak_period: str, start_date: str, end_date: str) -> List[Dict]:
        """Tushare 门控兜底：仅在设置 TUSHARE_TOKEN 环境变量后启用（免费额度 200 次/日）。"""
        token = os.environ.get("TUSHARE_TOKEN", "").strip()
        if not token:
            raise RuntimeError("TUSHARE_TOKEN not set")
        import tushare as ts
        sym = AStockDataService._to_symbol(code)
        suffix = "SH" if sym[:2] == "sh" else ("BJ" if sym[:2] == "bj" else "SZ")
        ts.set_token(token)
        daily = ts.pro_api().daily(ts_code=f"{sym[2:]}.{suffix}",
                                   start_date=start_date.replace("-", ""),
                                   end_date=end_date.replace("-", ""))
        if daily is None or not len(daily):
            return []
        daily = daily.sort_values("trade_date")  # tushare 倒序返回
        rows = [{
            "date": str(r["trade_date"])[:10], "open": float(r["open"]), "close": float(r["close"]),
            "high": float(r["high"]), "low": float(r["low"]),
            "volume": float(r.get("vol") or 0),  # 手
            "turnover": float(r.get("amount") or 0) * 1000,  # 千元 -> 元
            "amplitude": float(r.get("amp") or 0), "change_percent": float(r.get("pct_chg") or 0),
        } for _, r in daily.iterrows()]
        return rows if ak_period == "daily" else AStockDataService._resample_daily(rows, ak_period)

    @staticmethod
    def get_kline(symbol: str, period: str = "1d", days: int = 60,
                  start_date: str = "", end_date: str = "") -> List[Dict]:
        """K绾? 鍒嗘椂(1m/5m/15m/30m/60m) / 鏃ュ懆鏈?/ 鍏ㄩ噺鍘嗗彶"""

#        优先从 Supabase 历史库（分钟实时走接口，日线增量补库）；
#        intraday 返回当日分时线，5d 返回近5日分钟K；        start_date/end_date 则按窗口分页（前台渐进加载历史）。            
        sym = AStockDataService._to_symbol(symbol)
        code = AStockDataService._to_code(sym)
        p = (period or "1d").lower()
        full_req = days >= AStockDataService.FULL_HISTORY_DAYS or p == "all"
        w_start = start_date or ""
        w_end = end_date or datetime.now().strftime("%Y-%m-%d")

        def _window_out(result: list):
            """start_date 分页窗口：按日期过滤；否则全量(full_req)/最近(days)。"""
            if w_start:
                return [r for r in result if (r.get("date") or "")[:10] >= w_start and (r.get("date") or "")[:10] <= w_end]
            return result if full_req else result[-days:]

        # 腾讯当日分时 / 新浪1分钟兜底（实时接口，不落库）
        if p == "intraday":
            rows = AStockDataService._tx_minute_intraday(sym)
            source = "腾讯分时"
            if len(rows) < 2:
                rows = AStockDataService._sina_minute_batch(sym, 1100)
                source = "新浪分时"
            if len(rows) < 2:
                rows = AStockDataService._tx_mkline(sym, "1", 1250)
                source = "腾讯分钟"
            if len(rows) < 2:
                # 全部真实源失败才用 mock，并打兜底标签（端点会据此返回空）
                rows = AStockDataService._mock_minute(sym, "intraday", 1)
                source = "分时兜底"
            return AStockDataService._tag(rows, source)

        # ---- 分时/分钟K：新浪/腾讯 或 mock（绝不落入日线源）----
        if p in AStockDataService.KLINE_PERIOD_MAP:
            rows, source = AStockDataService._fetch_minute(sym, p, days)
            if rows:
                return AStockDataService._tag(rows, source)
            logger.warning("minute %s %s all sources failed, use mock", sym, p)
            return AStockDataService._tag(AStockDataService._mock_minute(sym, p, days), "mock")

        # ---- 鏃?鍛?鏈圞 ----
        if p not in AStockDataService.DAILY_PERIOD_MAP:
            return []

        ak_period = AStockDataService.DAILY_PERIOD_MAP[p]
        cal_factor = AStockDataService._PERIOD_CAL_DAYS.get(ak_period, 2.5)

        def _memo_hit():
            """任意窗口内存缓存命中（TTL 5min）：切周期秒回。"""
            memo = AStockDataService._recent_kline_memo.get((code, p))
            if memo and time.time() - memo[0] < AStockDataService.RECENT_KLINE_TTL:
                rows = memo[1]
                # 窗口覆盖校验：缓存最早日期需覆盖本次请求窗口（防止小窗口缓存被当大窗口用）
                if rows and len(rows) >= days:
                    return rows
                earliest = (rows[0].get("date") or "")[:10] if rows else ""
                need_start = (datetime.now() - timedelta(days=days * cal_factor)).strftime("%Y-%m-%d")
                if rows and earliest and earliest <= need_start:
                    return rows
            return None

        hit_rows = _memo_hit()
        if hit_rows:
            return AStockDataService._tag(_window_out(list(hit_rows)), "cache")

        # 读库优先；全量请求走单例内存缓存，避免每次全量重抓
        try:
            if full_req:
                memo = AStockDataService._full_kline_memo.get((code, p))
                if memo and time.time() - memo[0] < 1800:
                    return AStockDataService._tag(_window_out(memo[1]), "supabase")
            else:
                cached = SupabaseStore.get_kline(code, p, days)
                # 覆盖校验：库内最早日期需覆盖请求窗口（旧数据只有近几个月的月K时视为缺失）
                if cached:
                    earliest = (cached[0].get("date") or "")[:10]
                    need_start = (datetime.now() - timedelta(days=days * cal_factor)).strftime("%Y-%m-%d")
                    if earliest and earliest <= need_start:
                        AStockDataService._recent_kline_memo[(code, p)] = (time.time(), list(cached))
                        return AStockDataService._tag(_window_out(cached), "supabase")
        except Exception as e:
            logger.warning("supabase cache read %s %s failed: %s", code, p, e)

        now = datetime.now()
        if full_req:
            em_start, sina_start, tx_start, fq_beg = "19900101", "1990-01-01", "19900101", "1990-01-01"
        else:
            span = max(int(days * cal_factor) + 60, 150)
            em_start = (now - timedelta(days=span)).strftime("%Y%m%d")
            sina_start = (now - timedelta(days=span)).strftime("%Y-%m-%d")
            tx_start = (now - timedelta(days=span)).strftime("%Y%m%d")
            fq_beg = (now - timedelta(days=span)).strftime("%Y-%m-%d")
        # start_date 覆盖窗口起点；end_date 覆盖终点
        if w_start:
            em_start = w_start.replace("-", "")
            sina_start = w_start
            tx_start = w_start.replace("-", "")
            fq_beg = w_start
        today = w_end.replace("-", "")

        def _remember(rows: list):
            if not rows:
                return
            # 任意窗口都进 5min 内存缓存（切周期秒回）；全量另进 30min 全量缓存
            AStockDataService._recent_kline_memo[(code, p)] = (time.time(), list(rows))
            if full_req:
                AStockDataService._full_kline_memo[(code, p)] = (time.time(), rows)

        # 1) 东财原生日/周/月（日线重试3次；周/月降为2次快败，失败立即走日K重采样）
        if ak is not None:
            try:
                em_tries, em_delay = (3, 1.2) if ak_period == "daily" else (2, 0.8)
                df = AStockDataService._with_retry(
                    lambda: ak.stock_zh_a_hist(symbol=code, period=ak_period, start_date=em_start,
                                               end_date=today, adjust="qfq"), em_tries, em_delay)
                if df is not None and len(df):
                    source = "东方财富"
                    result = []
                    for _, r in df.iterrows():
                        result.append({
                            "date": str(r["日期"])[:10], "open": float(r["开盘"]), "close": float(r["收盘"]),
                            "high": float(r["最高"]), "low": float(r["最低"]),
                            "volume": float(r.get("成交量") or 0), "turnover": float(r.get("成交额") or 0),
                            "amplitude": float(r.get("振幅") or 0),
                            "change_percent": float(r.get("涨跌幅") or 0),
                        })
                    if result:
                        # 全量异步落库（按 upsert，历史缺口补全）
                        threading.Thread(
                            target=lambda: SupabaseStore.save_kline(code, p, result, source),
                            daemon=True).start()
                    _remember(result)
                    return AStockDataService._tag(_window_out(result), source)
            except Exception as e:
                logger.warning("EM hist %s %s failed: %s", sym, ak_period, e)
        # 1.5) Ashare 兜底（新浪/腾讯独立实现，原生周/月；单次上限不足，全量历史请求跳过）
        if not full_req:
            try:
                if w_start:
                    span_days = max((datetime.now() - datetime.strptime(w_start, "%Y-%m-%d")).days, 30)
                else:
                    span_days = int(days * cal_factor) + 40
                result = AStockDataService._ashare_kline(sym, ak_period, int(span_days / cal_factor) + 10)
                if result:
                    threading.Thread(
                        target=lambda: SupabaseStore.save_kline(code, p, result, "Ashare"),
                        daemon=True).start()
                    _remember(result)
                    return AStockDataService._tag(_window_out(result), "Ashare")
            except Exception as e:
                logger.warning("Ashare kline %s %s failed: %s", sym, ak_period, e)
        # 1.6) Baostock 兜底（自有 TCP 免 HTTP 风控，日/周/月+前复权，支持全历史）
        try:
            bs_start = w_start or ("1990-01-01" if full_req else
                                   (now - timedelta(days=int(days * cal_factor) + 60)).strftime("%Y-%m-%d"))
            result = AStockDataService._baostock_kline(code, ak_period, bs_start, w_end)
            if result:
                threading.Thread(
                    target=lambda: SupabaseStore.save_kline(code, p, result, "Baostock"),
                    daemon=True).start()
                _remember(result)
                return AStockDataService._tag(_window_out(result), "Baostock")
        except Exception as e:
            AStockDataService._bs_fail_until = time.time() + 60  # 失败短熔断，避免每次请求都撞登录超时
            logger.warning("Baostock kline %s %s failed: %s", sym, ak_period, e)
        # 1.7) adata 兜底（东财族源，风控解除后可用）
        try:
            ad_start = w_start or ("1990-01-01" if full_req else
                                   (now - timedelta(days=int(days * cal_factor) + 60)).strftime("%Y-%m-%d"))
            result = AStockDataService._adata_kline(code, ak_period, ad_start, w_end)
            if result:
                threading.Thread(
                    target=lambda: SupabaseStore.save_kline(code, p, result, "AData"),
                    daemon=True).start()
                _remember(result)
                return AStockDataService._tag(_window_out(result), "AData")
        except Exception as e:
            logger.warning("AData kline %s failed: %s", sym, e)
        # 2) 新浪日线（周/月重采样，日线直出）
        if ak is not None:
            try:
                today_s = now.strftime("%Y-%m-%d")
                df = AStockDataService._with_retry(
                    lambda: ak.stock_zh_a_daily(symbol=sym, start_date=sina_start, end_date=today_s, adjust="qfq"), 2, 0.8)
                if df is not None and len(df):
                    daily_rows = []
                    for _, r in df.iterrows():
                        daily_rows.append({
                            "date": str(r["date"])[:10], "open": float(r["open"]), "close": float(r["close"]),
                            "high": float(r["high"]), "low": float(r["low"]),
                            "volume": float(r.get("volume") or 0), "turnover": float(r.get("amount") or 0),
                            "amplitude": 0.0, "change_percent": 0.0,
                        })
                    period_rows = AStockDataService._resample_daily(daily_rows, ak_period)
                    if period_rows:
                        threading.Thread(
                            target=lambda: SupabaseStore.save_kline(code, p, period_rows, "新浪财经"),
                            daemon=True).start()
                    _remember(period_rows)
                    return AStockDataService._tag(period_rows if full_req else period_rows[-days:], "新浪财经")
            except Exception as e:
                logger.warning("SINA daily %s failed: %s", sym, e)
        # 3) 腾讯日线（周/月重采样）
        if ak is not None:
            try:
                df = AStockDataService._with_retry(
                    lambda: ak.stock_zh_a_hist_tx(symbol=sym, start_date=tx_start, end_date=today), 2, 0.8)
                if df is not None and len(df):
                    daily_rows = []
                    for _, r in df.iterrows():
                        daily_rows.append({
                            "date": str(r["date"])[:10], "open": float(r["open"]), "close": float(r["close"]),
                            "high": float(r["high"]), "low": float(r["low"]),
                            "volume": float(r.get("volume") or 0), "turnover": float(r.get("amount") or 0),
                            "amplitude": 0.0, "change_percent": 0.0,
                        })
                    period_rows = daily_rows if ak_period == "daily" else AStockDataService._resample_daily(daily_rows, ak_period)
                    if period_rows:
                        threading.Thread(
                            target=lambda: SupabaseStore.save_kline(code, p, period_rows, "腾讯"),
                            daemon=True).start()
                    _remember(period_rows)
                    return AStockDataService._tag(period_rows if full_req else period_rows[-days:], "腾讯")
            except Exception as e:
                logger.warning("TX hist %s failed: %s", sym, e)

        # 4) Vibe 拉取（周/月重采样，熔断失败后 60s 内跳过直达腾讯）
        if time.time() - AStockDataService._vibe_down_since > 60:
            try:
                vcode = AStockDataService._to_code(sym)
                url = f"{AStockDataService.VIBE_BASE}/api/kline"
                r = requests.get(url, params={"code": vcode, "category": 4, "offset": max(days, 1)}, timeout=6)
                r.raise_for_status()
                rows = r.json().get("data") or []
                if rows:
                    source = "Vibe"
                    result = []
                    for row in rows:
                        result.append({
                            "date": str(row.get("date"))[:10],
                            "open": float(row.get("open") or 0), "close": float(row.get("close") or 0),
                            "high": float(row.get("high") or 0), "low": float(row.get("low") or 0),
                            "volume": float(row.get("volume") or 0),
                            "turnover": float(row.get("turnover") or 0) if row.get("turnover") else 0,
                            "amplitude": 0.0, "change_percent": 0.0,
                        })
                    period_rows = result if ak_period == "daily" else AStockDataService._resample_daily(result, ak_period)
                    if period_rows:
                        threading.Thread(
                            target=lambda: SupabaseStore.save_kline(code, p, period_rows, source),
                            daemon=True).start()
                    _remember(period_rows)
                    return AStockDataService._tag(period_rows if full_req else period_rows[-days:], source)
                AStockDataService._vibe_down_since = 0.0
            except Exception as e:
                logger.warning("Vibe kline failed for %s: %s", symbol, e)
                AStockDataService._vibe_down_since = time.time()

        # 5) 腾讯 fqkline（直连 HTTP，不依赖 akshare，可拉多年日线）
        try:
            tx_sym = sym if sym[:2] in ("sh", "sz", "bj") else AStockDataService._to_symbol(code)
            end_d = now.strftime("%Y-%m-%d")
            url = "https://ifzq.gtimg.cn/appstock/app/fqkline/get"
            r = requests.get(url, params={"param": f"{tx_sym},day,{fq_beg},{end_d},{min(days, 700)},qfq"},
                             headers={"User-Agent": "Mozilla/5.0"}, timeout=12)
            r.raise_for_status()
            dd = r.json().get("data", {}).get(tx_sym, {})
            arr = dd.get("qfqday") or dd.get("day") or []
            if arr:
                daily_rows = []
                for f in arr:
                    daily_rows.append({
                        "date": str(f[0])[:10], "open": float(f[1] or 0), "close": float(f[2] or 0),
                        "high": float(f[3] or 0), "low": float(f[4] or 0),
                        "volume": float(f[5] or 0), "turnover": 0.0,
                        "amplitude": 0.0, "change_percent": 0.0,
                    })
                period_rows = daily_rows if ak_period == "daily" else AStockDataService._resample_daily(daily_rows, ak_period)
                if period_rows:
                    threading.Thread(
                        target=lambda: SupabaseStore.save_kline(code, p, period_rows, "腾讯"),
                        daemon=True).start()
                    _remember(period_rows)
                    return AStockDataService._tag(period_rows if full_req else period_rows[-days:], "腾讯")
        except Exception as e:
            logger.warning("TX fqkline %s failed: %s", sym, e)

        # 5.5) Tushare 门控兜底（仅当设置 TUSHARE_TOKEN 环境变量后启用）
        try:
            ts_start = w_start or (now - timedelta(days=int(days * cal_factor) + 60)).strftime("%Y-%m-%d")
            result = AStockDataService._tushare_kline(code, ak_period, ts_start, w_end)
            if result:
                threading.Thread(
                    target=lambda: SupabaseStore.save_kline(code, p, result, "Tushare"),
                    daemon=True).start()
                _remember(result)
                return AStockDataService._tag(_window_out(result), "Tushare")
        except Exception as e:
            logger.warning("Tushare kline %s failed: %s", sym, e)

        # 6) mock 兜底（仅极端异常；超长请求截断到 1500 根）
        seed = int(hashlib.md5(f"d_{sym}_{p}".encode()).hexdigest()[:8], 16)
        rng = np.random.RandomState(seed)
        base_price = rng.uniform(10, 500)
        result = []
        n = min(max(days, 30), 1500)
        for i in range(n):
            date = (now - timedelta(days=n - i)).strftime("%Y-%m-%d")
            change_pct = rng.uniform(-3, 3)
            change = base_price * change_pct / 100
            close = base_price + change
            high = max(base_price, close) + rng.uniform(0, base_price * 0.02)
            low = min(base_price, close) - rng.uniform(0, base_price * 0.02)
            volume = rng.uniform(5000, 200000)
            turnover = volume * close
            result.append({
                "date": date, "open": round(base_price, 2),
                "close": round(close, 2), "high": round(high, 2),
                "low": round(low, 2), "volume": round(volume, 0),
                "turnover": round(turnover, 2), "amplitude": 0.0, "change_percent": round(change_pct, 2),
            })
            base_price = close
        return AStockDataService._tag(result, "mock")

    @staticmethod
    def compute_indicators(kline: List[Dict]) -> Dict:
        """基于真实K线计算技术指标: MA5/10/20/60, RSI14, MACD(12,26,9)"""
        closes = [float(k["close"]) for k in kline]
        n = len(closes)
        out: Dict = {}

        def _ma(p: int):
            return round(sum(closes[-p:]) / p, 2) if n >= p else None

        for p in (5, 10, 20, 60):
            out[f"ma{p}"] = _ma(p)

        if n >= 15:
            gains, losses = [], []
            for i in range(1, n):
                diff = closes[i] - closes[i - 1]
                gains.append(max(diff, 0))
                losses.append(max(-diff, 0))
            avg_g = sum(gains[-14:]) / 14
            avg_l = sum(losses[-14:]) / 14
            rs = avg_g / avg_l if avg_l > 0 else 999
            out["rsi"] = round(100 - 100 / (1 + rs), 2)
        else:
            out["rsi"] = None

        if n >= 26:
            def _ema(arr, period):
                k = 2 / (period + 1)
                e = arr[0]
                for v in arr[1:]:
                    e = v * k + e * (1 - k)
                return e

            dif = round(_ema(closes, 12) - _ema(closes, 26), 4)
            dea = round(_ema([closes[i] for i in range(n)], 9), 4) if n >= 35 else None
            if dea is not None:
                hist = round((dif - dea) * 2, 4)
            else:
                hist = None
            out["macd"] = {"dif": dif, "dea": dea, "hist": hist}
        else:
            out["macd"] = {"dif": None, "dea": None, "hist": None}
        return out

    @staticmethod
    def get_stock_info(symbol: str) -> Dict:
        quote = AStockDataService.get_realtime_quote(symbol)
        kline = AStockDataService.get_kline(symbol, period="daily", days=80)
        return {
            "quote": asdict(quote),
            "kline": kline[-10:],
            "indicators": AStockDataService.compute_indicators(kline),
        }

    _recommend_cache: Dict[str, tuple] = {}
    _RECOMMEND_TTL = 900.0  # 15 分钟

    @staticmethod
    def _kline_positions(symbol: str) -> Dict:
        """基于近 250+ 日计算 52周相对位置/距高点等技面因子。"""
        kl = AStockDataService.get_kline(symbol, period="daily", days=260)
        if len(kl) < 60:
            return {}
        closes = [float(k["close"]) for k in kl]
        hi = max(closes)
        lo = min(closes)
        last = closes[-1]
        pos52 = (last - lo) / (hi - lo) * 100 if hi > lo else 100.0
        ma5 = sum(closes[-5:]) / 5
        ma20 = sum(closes[-20:]) / 20
        ma60 = sum(closes[-60:]) / 60
        up_trend = ma5 >= ma20 >= ma60
        down20 = (closes[-20] - closes[-1]) / closes[-20] * 100 if closes[-20] else 0
        return {
            "pos52": round(pos52, 1), "drawdown_max": round((last - hi) / hi * 100, 1),
            "up_trend": up_trend, "ma5": round(ma5, 2), "ma20": round(ma20, 2), "ma60": round(ma60, 2),
            "rsi": AStockDataService.compute_indicators(kl).get("rsi"),
            "vol20": round(down20, 2),
        }

    @staticmethod
    def get_recommendations(limit: int = 10, force: bool = False) -> Dict:
        """低位荐股：全市场快照（基本面 PE/PB/市值）+ 近2周低位 + 研报覆盖评分。"""
        key = f"recommend_{limit}"
        now = time.time()
        if not force and key in AStockDataService._recommend_cache:
            ts, val = AStockDataService._recommend_cache[key]
            if now - ts < AStockDataService._RECOMMEND_TTL:
                return val
        try:
            df, _src = AStockDataService._load_all_snapshot()
            rows = df.to_dict("records")[:3000] if df is not None and len(df) else []
            # 基本面初筛：PE 0~80、市值20亿~3000亿、价格2~300
            cands = []
            for r in rows:
                pe = AStockDataService._row_num(r, "市盈率", "市盈率-动态", "pe")
                mc = AStockDataService._row_num(r, "总市值", "mktcap")
                price = AStockDataService._row_num(r, "最新价", "trade", "price")
                pct = AStockDataService._row_num(r, "涨跌幅", "changepercent")
                code = str(r.get("代码") or r.get("code") or "")
                name = AStockDataService._row_name(r, "")
                if not code:
                    continue
                if not (code.startswith(("6", "0", "3"))):  # 仅沪深主板+创业板+科创板
                    continue
                if not (0 < pe <= 80):
                    continue
                if not (2 <= price <= 300):
                    continue
                if mc and not (2e9 <= mc <= 3e11):
                    continue
                if abs(pct) > 9.5:  # 剔除涨停/跌停
                    continue
                cands.append({"code": code, "name": name, "pe": pe, "pb": float(r.get("市净率") or 0),
                              "cap": mc, "price": price, "pct": pct})
            # 并发拉研报覆盖评分
            pool = cands[:30]  # 初筛后取 30 只有代表性
            scored = []

            def _score_one(c: Dict):
                symbol = AStockDataService._to_symbol(c["code"])
                pos = AStockDataService._kline_positions(symbol)
                if not pos or pos.get("pos52", 100) > 40:  # 仅低<40%位置
                    return None
                reports = ResearchReportService.get_reports(code=c["code"], limit=6)
                r_cnt = len(reports)
                total_target = sum(1 for x in reports if (x.get("target_price") or 0) > c["price"])
                # 评分：位置越低越好，研报越多/看多越多越好，RSI适中偏低，PE低加分
                score = 100.0 - pos["pos52"] + min(r_cnt * 4, 24)
                if total_target > 0:
                    score += 6
                if pos.get("rsi") is not None:
                    score += 5 if 40 <= pos["rsi"] <= 68 else (0 if pos["rsi"] < 75 else -15)
                score -= max(0, 30 - c["pe"]) * 0.3     # PE过低有陷阱
                score += 5 if c["pe"] < 20 else 0
                if c["pb"] and c["pb"] < 3:
                    score += 3
                return {
                    "code": c["code"], "name": c["name"], "price": c["price"], "pct": c["pct"],
                    "pe": c["pe"], "pb": c["pb"] or 0, "cap": c["cap"],
                    "pos52": pos["pos52"], "rsi": pos.get("rsi"),
                    "up_trend": bool(pos.get("up_trend")), "ma_bull": pos["ma5"] >= pos["ma20"],
                    "report_cnt": r_cnt, "total_target": total_target,
                    "score": round(score, 1),
                    "reasons": [
                        f"52周相对位置仅{pos['pos52']:.0f}%（低位）",
                        f"PE {c['pe']:.1f}" + ("估值低于20倍" if c["pe"] < 20 else ""),
                        f"{r_cnt} 篇研报覆盖" + ("，目标价高于现价" if total_target > 0 else ""),
                        "均线多头排列(MA5>MA20>MA60)" if pos["up_trend"] else "",
                    ],
                }

            with ThreadPoolExecutor(max_workers=6) as ex:
                for r_ in ex.map(_score_one, pool):
                    if r_:
                        scored.append(r_)
            scored.sort(key=lambda x: x["score"], reverse=True)
            val = {
                "items": scored[:limit],
                "pool_size": len(cands),
                "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                "source": "tech_fund_report",
            }
            AStockDataService._recommend_cache[key] = (time.time(), val)
            return val
        except Exception as e:
            logger.warning("recommend failed: %s", e)
            return {"items": [], "pool_size": 0, "error": str(e),
                    "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

    _spark_cache: Dict[str, List] = {}
    _spark_ts = 0.0
    _SPARK_TTL = 180.0

    @staticmethod
    def get_sparklines_batch(codes: List[str], limit: int = 40) -> Dict[str, List[float]]:
        """并发批量获取近期收盘价走势（自带缓存限时）。"""
        from concurrent.futures import ThreadPoolExecutor
        now = time.time()
        if now - AStockDataService._spark_ts > AStockDataService._SPARK_TTL:
            AStockDataService._spark_cache = {}
        out: Dict[str, List[float]] = dict(AStockDataService._spark_cache)
        need = [c for c in codes if c and c not in out][:limit]
        if not need:
            return out

        def _one(c: str):
            try:
                kl = AStockDataService.get_kline(AStockDataService._to_symbol(c), "daily", 12)
                if kl:
                    return c, [float(k["close"]) for k in kl]
            except Exception as e:
                logger.warning("sparkline %s failed: %s", c, e)
            return None

        with ThreadPoolExecutor(max_workers=4) as ex:
            for item in ex.map(_one, need):
                if item:
                    out[item[0]] = item[1]
                    AStockDataService._spark_cache[item[0]] = item[1]
        AStockDataService._spark_ts = now
        return out

    @staticmethod
    def search_stocks(q: str = "") -> List[Dict]:
        """全市场搜索（东财/新浪快照），仅用缓存快照，避免每次输入都全量拉取）。"""
        df, src = AStockDataService._load_all_snapshot()
        if df is None or not len(df):
            return [{"symbol": s, "name": n} for s, n in AStockDataService.STOCK_POOL.items()
                    if not q or q in s or q in n]
        results = []
        for row in df.to_dict("records"):
            code = str(row.get("代码") or row.get("code") or "")
            name = AStockDataService._row_name(row, "")
            if not code:
                continue
            if q and q not in code and q not in name:
                continue
            results.append({"symbol": AStockDataService._to_symbol(code), "name": name, "source": src})
            if len(results) >= 50:
                break
        return results

    # ------------------------------------------------------------------
    # MSN Finance 数据源（A股指数/历史K线/基本面/搜索）    # ------------------------------------------------------------------
    _MSN_KEY = "0QfOX3Vn51YCzitbLaRkTTBadtWpgTN8NZLW0C1SEM"
    _MSN_HEADERS = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126.0 Safari/537.36 Edg/126.0",
        "Referer": "https://www.msn.cn/zh-cn/finance",
        "Origin": "https://www.msn.cn",
    }
    # 申万行业级指数映射 MSN 指数（通过 "399xxx code" / "CSI SWS <英文> Index" 搜索定位 SecId）。
    _SWS_SECTORS = {
        "电子": "Electronics", "医药生物": "Pharmaceuticals and Biotechnology",
        "食品饮料": "Food, Beverages and Tobacco", "化工": "Chemicals",
        "电力设备": "Power Equipment", "计算机": "Computer", "医药医疗": "Medical Devices",
        "汽车": "Automobiles", "国防军工": "Aerospace and Defense", "银行": "Banks",
        "保险": "Insurance", "传媒": "Media", "通信设备": "Telecommunications Equipment",
        "煤炭": "Coal", "钢铁": "Steel", "房地产": "Real Estate", "服装": "Apparel and Luxury Goods",
        "农业": "Agricultural Products", "家电": "Home Appliances", "石油石化": "Oil, Gas and Fossil Fuels",
    }

    @staticmethod
    def _msn_find_index_secid(sector_name_cn: str) -> str:
        """搜索申万行业指数 SecId：先查 CSI SWS <英文> Index，再兜底返回缓存的电子指数。"""
        if not sector_name_cn:
            return ""
        en = AStockDataService._SWS_SECTORS.get(sector_name_cn, "")
        cands = []
        if en:
            cands.append(f"CSI SWS {en} Index")
            cands.append(f"SWS {en} Index")
        cands.append("CSI SWS Index")
        for q in cands:
            secid = AStockDataService._msn_search_secid(q)
            if secid:
                return secid
        return ""
    _sws_cache: Dict[str, dict] = {}
    _sws_ts = 0.0

    @staticmethod
    def _msn_search_secid(query: str) -> str:
        """MSN Finance 搜索 读取/定位 A股指数 SecId。"""
        try:
            r = requests.get("https://services.bingapis.com/contentservices-finance.csautosuggest/api/v1/Query",
                             params={"query": query, "market": "zh-cn", "count": 4},
                             headers=AStockDataService._MSN_HEADERS, timeout=10)
            stocks = ((r.json() or {}).get("data") or {}).get("stocks") or []
            for raw in stocks:
                d = json.loads(raw)
                secid = d.get("SecId") or ""
                full = d.get("FullInstrument") or ""
                mic = d.get("ExMicCode") or ""
                if "XSHG_CSI" in mic or ".10." in full or ".10." in full:
                    return secid
                if secid:
                    return secid
        except Exception:
            pass
        return ""

    @staticmethod
    def _msn_charts_10yh1(secid: str, days: int = 30) -> List[Dict]:
        """MSN Charts 10YH1 → 近 N 日日K"""
        try:
            r = requests.get("https://assets.msn.com/service/Finance/Charts",
                             params={"ids": secid, "type": "10YH1", "apikey": AStockDataService._MSN_KEY,
                                     "wrapodata": "false", "ocid": "finance-utils-peregrine"},
                             headers=AStockDataService._MSN_HEADERS, timeout=12)
            j = r.json()
            arr = j if isinstance(j, list) else [j]
            series = arr[0].get("series", {}) if arr else {}
            prices = series.get("prices") or []
            opens = series.get("openPrices") or []
            highs = series.get("pricesHigh") or []
            lows = series.get("pricesLow") or []
            dates = series.get("timeStamps") or []
            vols = series.get("volumes") or []
            n = min(len(dates), len(prices))
            out = []
            for i in range(max(0, n - days), n):
                c = prices[i] if i < len(prices) else 0
                o = opens[i] if i < len(opens) else c
                h = highs[i] if i < len(highs) else c
                lo = lows[i] if i < len(lows) else c
                v = vols[i] if i < len(vols) else 0
                out.append({"date": str(dates[i])[:10], "open": float(o), "close": float(c),
                            "high": float(h), "low": float(lo), "volume": float(v),
                            "turnover": 0.0, "amplitude": 0.0, "change_percent": 0.0})
            return out
        except Exception as e:
            logger.warning("MSN charts %s failed: %s", secid, e)
            return []

    @staticmethod
    def _msn_quote_summary(secid: str) -> Dict:
        """MSN QuoteSummary 拉取行情/财务概要。"""
        try:
            r = requests.get("https://assets.msn.com/service/Finance/QuoteSummary",
                             params={"ids": secid, "apikey": AStockDataService._MSN_KEY,
                                     "wrapodata": "false", "ocid": "finance-utils-peregrine",
                                     "cm": "zh-cn", "it": "web"},
                             headers=AStockDataService._MSN_HEADERS, timeout=10)
            data = r.json()
            if isinstance(data, list) and data:
                return data[0]
            return data
        except Exception as e:
            logger.warning("MSN quote %s failed: %s", secid, e)
            return {}

    @staticmethod
    def get_sector_heat_ths() -> Dict[str, float]:
        """同花顺行业板块涨跌。"""
        if ak is None:
            return {}
        try:
            df = AStockDataService._with_retry(lambda: ak.stock_board_industry_summary_ths(), 2, 0.8)
            if df is not None and len(df):
                out = {}
                for _, r in df.head(30).iterrows():
                    out[str(r["板块"])] = float(r["涨跌幅"])
                return out
        except Exception as e:
            logger.warning("THS board failed: %s", e)
        return {}

    # ------------------------------------------------------------------
    # 智能看板扩展数据：涨停/跌停/炸板池、板块涨跌曲线、主力资金流、连板晋级    # 全部走东方财富直连 HTTP（不依赖 akshare）    # ------------------------------------------------------------------

    _EM = "https://push2delay.eastmoney.com"
    _EMEX = "https://push2ex.eastmoney.com"
    _EMHIS = "https://push2his.eastmoney.com"
    _ZX_UT = "7eea3edcaed734bea9cbfc24409ed989"
    _KT_HEADERS = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                      "(KHTML, like Gecko) Chrome/126.0 Safari/537.36",
        "Referer": "https://quote.eastmoney.com/",
    }
    _pool_cache: Dict[str, tuple] = {}
    _trend_cache: Dict[str, tuple] = {}
    _fflow_cache: Dict[str, tuple] = {}

    @staticmethod
    def _mkymd(dt: datetime) -> str:
        return dt.strftime("%Y%m%d")

    @staticmethod
    def _recent_trade_dates(n: int = 5) -> List[str]:
        """取 n 个自然日候选交易日（不只按日历单判定）。"""
        out, d = [], datetime.now()
        while len(out) < n and d >= datetime.now() - timedelta(days=14):
            if d.weekday() < 5:
                out.append(AStockDataService._mkymd(d))
            d -= timedelta(days=1)
        return out

    @staticmethod
    def _em_pool(date: str, mode: str) -> list:
        """东财涨停(dpt=wz.ztzt)/跌停/炸板池原始列表; mode in ('zt','dt','zb')"""
        if ak is not None:
            try:
                if mode == "zt":
                    df = ak.stock_zt_pool_em(date=date)
                    if df is not None and len(df):
                        return df.to_dict("records")
                elif mode == "dt":
                    df = ak.stock_zt_pool_dtgc_em(date=date)
                    if df is not None and len(df):
                        return df.to_dict("records")
                elif mode == "zb":
                    df = ak.stock_zt_pool_zbgc_em(date=date)
                    if df is not None and len(df):
                        return df.to_dict("records")
            except Exception as e:
                logger.warning("em_pool akshare %s failed: %s", mode, e)
        paths = {"zt": "/getTopicZTPool", "dt": "/getTopicDTPool", "zb": "/getTopicZBPool"}
        sorts = {"zt": "fbt:asc", "dt": "fund:asc", "zb": "zbc:desc"}
        try:
            url = AStockDataService._EMEX + paths[mode]
            params = {"ut": AStockDataService._ZX_UT, "dpt": "wz.ztzt",
                      "Pageindex": 0, "pagesize": 500, "sort": sorts[mode], "date": date}
            r = requests.get(url, params=params, headers=AStockDataService._KT_HEADERS, timeout=12)
            r.raise_for_status()
            j = r.json()
            pool = ((j or {}).get("data") or {}).get("pool") or []
            if pool:
                return pool
        except Exception as e:
            logger.warning("em_pool %s %s failed: %s", mode, date, e)
        return []

    @staticmethod
    def _em_pool_items(date: str, mode: str) -> List[Dict]:
        """涨停/跌停/炸板池，统一结构列表。"""
        rows = AStockDataService._em_pool(date, mode)
        out = []
        for x in rows:
            if isinstance(x, dict) and "c" in x:
                code = AStockDataService._clean_code(x.get("c") or "")
                if not code:
                    continue
                out.append({
                    "code": code,
                    "name": x.get("n") or "",
                    "price": float(x.get("p") or 0),
                    "pct": float(x.get("zdp") or 0) if x.get("zdp") is not None else 0.0,
                    "amount": float(x.get("amount") or 0) if x.get("amount") is not None else 0.0,
                    "fund": float(x.get("fund") or 0) if x.get("fund") is not None else 0.0,
                    "seal": float(x.get("fund") or 0) if x.get("fund") is not None else 0.0,
                    "hs": float(x.get("hs") or 0) if x.get("hs") is not None else 0.0,
                    "fbt": str(x.get("fbt") or ""),
                    "lbt": str(x.get("lbt") or ""),
                    "zbc": int(x.get("zbc") or 0) if x.get("zbc") is not None else 0,
                    "lbc": int(x.get("lbc") or 1) if x.get("lbc") is not None else 1,
                    "zttj": str(x.get("zttj") or ""),
                    "hybk": str(x.get("hybk") or ""),
                })
            elif isinstance(x, dict):
                # DataFrame 形态（akshare 场景）
                code = AStockDataService._clean_code(x.get("代码") or x.get("code") or "")
                if not code:
                    continue
                out.append({
                    "code": code,
                    "name": x.get("名称") or x.get("name") or "",
                    "price": float(x.get("最新价") or x.get("price") or 0),
                    "pct": float(x.get("涨跌幅") or x.get("pct") or 0),
                    "amount": float(x.get("成交额") or 0),
                    "fund": float(x.get("封板资金") or x.get("fund") or 0),
                    "seal": float(x.get("封板资金") or x.get("fund") or 0),
                    "hs": float(x.get("换手率") or 0),
                    "fbt": str(x.get("首次封板时间") or ""),
                    "lbt": str(x.get("最后封板时间") or ""),
                    "zbc": int(x.get("炸板次数") or 0),
                    "lbc": int(x.get("连板数") or 1),
                    "zttj": str(x.get("涨停统计") or ""),
                    "hybk": str(x.get("所属行业") or ""),
                })
            if len(out) >= 200:
                break
        return out

    @staticmethod
    def get_limit_pools(force: bool = False) -> Dict:
        """涨停/跌停/炸板 + 最近交易日；带缓存 60s。"""
        key = "limit_pools"
        now = time.time()
        if not force and key in AStockDataService._pool_cache:
            ts, val = AStockDataService._pool_cache[key]
            if now - ts < 60:
                return val
        date = AStockDataService._recent_trade_dates(5)[0]
        zuotian = AStockDataService._recent_trade_dates(7)[1] if len(AStockDataService._recent_trade_dates(7)) > 1 else date
        zt, dt, zb = [], [], []
        zt_rows, okdates = [], []
        for d in AStockDataService._recent_trade_dates(5):
            s = AStockDataService._em_pool_items(d, "zt")
            if s:
                zt_rows.append((d, s))
                break
        if zt_rows:
            date, zt = zt_rows[0]
        for d in AStockDataService._recent_trade_dates(5):
            s = AStockDataService._em_pool_items(d, "dt")
            if s:
                dt = s
                break
        for d in AStockDataService._recent_trade_dates(5):
            s = AStockDataService._em_pool_items(d, "zb")
            if s:
                zb = s
                break
        val = {"date": date, "prev_date": zuotian,
               "limit_up": zt, "limit_down": dt, "zhaban": zb}
        # 异步落库当天涨停池快照
        if val.get("date"):
            threading.Thread(
                target=lambda: SupabaseStore.save_pool("limit_up", val["date"], {
                    "date": val["date"], "prev_date": val["prev_date"],
                    "limit_up": zt, "limit_down": dt, "zhaban": zb}),
                daemon=True).start()
        AStockDataService._pool_cache[key] = (time.time(), val)
        return val

    @staticmethod
    def get_board_progress(force: bool = False) -> List[Dict]:
        """连板晋级：1板 / 2板 / 3板；用最近两日涨停池比较。"""
        key = "board_progress"
        now = time.time()
        if not force and key in AStockDataService._pool_cache:
            ts, val = AStockDataService._pool_cache[key]
            if now - ts < 120:
                return val
        rows = []
        try:
            dates = AStockDataService._recent_trade_dates(7)
            pools = []
            for d in dates:
                items = AStockDataService._em_pool_items(d, "zt")
                if items:
                    pools.append((d, items))
                if len(pools) >= 2:
                    break
            if not pools:
                return []
            today_date, today = pools[0]
            # 昨日涨停池（连板：lbc >= 1）
            prev = None
            for d, items in pools[1:]:
                if d != today_date:
                    prev = (d, items)
                    break
            # 今日各板位数量与具体个股（仅统计今日涨停还在连板的）
            today_boards: Dict[int, int] = {}
            today_stocks: Dict[int, List[Dict]] = {}
            for it in today:
                lb = int(it.get("lbc") or 1)
                today_boards[lb] = today_boards.get(lb, 0) + 1
                today_stocks.setdefault(lb, []).append({
                    "code": it.get("code") or "",
                    "name": it.get("name") or "",
                    "pct": float(it.get("pct") or 0),
                    "hybk": it.get("hybk") or "",
                })
            # 昨日各板位数量
            prev_boards: Dict[int, int] = {}
            if prev:
                for it in prev[1]:
                    lb = int(it.get("lbc") or 1)
                    prev_boards[lb] = prev_boards.get(lb, 0) + 1
            for n in range(1, 8):
                jinri = today_boards.get(n + 1, 0)
                zuori = prev_boards.get(n, 0) if prev else today_boards.get(n, 0)
                base = zuori or 0
                rate = round((jinri / base * 100), 1) if base else 0.0
                rows.append({
                    "name": f"{n}进{n + 1}板",
                    "from_n": n,
                    "count": jinri,
                    "prev_count": base,
                    "rate": rate,
                    "stocks": today_stocks.get(n + 1, []),
                })
            rows = [r for r in rows if r["count"] or r["prev_count"]]
            # 全局连板梯队（lbc >= 2，按板位分组含股明细）
            ladders: List[Dict] = []
            for lb in sorted(today_boards, reverse=True):
                if lb < 2:
                    continue
                ladders.append({
                    "lb": lb,
                    "count": today_boards[lb],
                    "stocks": today_stocks.get(lb, []),
                })
            rows.append({"ladders": ladders})
            if today_date:
                threading.Thread(
                    target=lambda: SupabaseStore.save_pool("board_progress", today_date, {"rows": rows,
                                                                                          "pin_date": today_date}),
                    daemon=True).start()
            AStockDataService._pool_cache[key] = (time.time(), rows)
        except Exception as e:
            logger.warning("board_progress failed: %s", e)
        return rows

    @staticmethod
    def get_limit_pool_by_board() -> Dict:
        """涨停池按连板数分层（首板 / 1进2 / 2进3 / ... / 6进7），duanxianxia 风格。
        返回 {date, boards:[{label, lbc, count, stocks:[{code,name,pct,seal,amount,hybk,fbt}]}],
               total, seal_total}"""
        pools = AStockDataService.get_limit_pools()
        zt = pools.get("limit_up") or []
        date = pools.get("date") or ""
        groups: Dict[int, List[Dict]] = {}
        seal_total = 0.0
        amount_total = 0.0
        for it in zt:
            lb = int(it.get("lbc") or 1)
            groups.setdefault(lb, []).append({
                "code": it.get("code") or "",
                "name": it.get("name") or "",
                "pct": float(it.get("pct") or 0),
                "price": float(it.get("price") or 0),
                "seal": float(it.get("seal") or it.get("fund") or 0),
                "amount": float(it.get("amount") or 0),
                "hs": float(it.get("hs") or 0),
                "hybk": it.get("hybk") or "",
                "fbt": it.get("fbt") or "",
            })
            seal_total += float(it.get("seal") or it.get("fund") or 0)
            amount_total += float(it.get("amount") or 0)
        boards = []
        for n in range(1, 8):
            stocks = groups.get(n, [])
            label = "首板" if n == 1 else f"{n - 1}进{n}"
            boards.append({
                "label": label, "lbc": n, "count": len(stocks),
                "stocks": sorted(stocks, key=lambda x: x.get("seal", 0), reverse=True),
            })
        return {
            "date": date,
            "total": len(zt),
            "seal_total": round(seal_total, 2),
            "amount_total": round(amount_total, 2),
            "boards": boards,
        }

    @staticmethod
    def get_sector_hist_em(board_code: str, days: int = 30, beg: str = "") -> list:
        """东财板块指数日K（secid=90.BKxxxx）；返回 [{date,close,open,high,low,pct}]"""
        try:
            beg = beg or (datetime.now() - timedelta(days=days * 2)).strftime("%Y%m%d")
            end = datetime.now().strftime("%Y%m%d")
            url = AStockDataService._EMHIS + "/api/qt/stock/kline/get"
            params = {"secid": f"90.{board_code}", "ut": AStockDataService._ZX_UT,
                      "fields1": "f1,f2,f3,f4,f5,f6",
                      "fields2": "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61",
                      "klt": "101", "fqt": "1", "beg": beg, "end": end,
                      "lmt": str(days)}
            r = requests.get(url, params=params, headers=AStockDataService._KT_HEADERS, timeout=12)
            r.raise_for_status()
            data = ((r.json() or {}).get("data") or {})
            klines = data.get("klines") or []
            out = []
            for k in klines:
                f = k.split(",")
                if len(f) < 11:
                    continue
                out.append({
                    "date": f[0],
                    "open": float(f[1] or 0), "close": float(f[2] or 0),
                    "high": float(f[3] or 0), "low": float(f[4] or 0),
                    "volume": float(f[5] or 0), "amount": float(f[6] or 0),
                    "amp": float(f[7] or 0), "pct": float(f[8] or 0),
                    "change": float(f[9] or 0), "turnover": float(f[10] or 0),
                })
            return out[-days:] if days and days > 0 else out
        except Exception as e:
            logger.warning("sector hist %s failed: %s", board_code, e)
            return []

    @staticmethod
    def _sector_trends_ths(days: int = 30, top: int = 0) -> Dict:
        """同花顺行业板块近 days 日日K（全量约 90 行业，稳定不受东财限流影响）。"""
        if ak is None:
            return {}
        try:
            names = AStockDataService._with_retry(
                lambda: ak.stock_board_industry_name_ths(), 2, 0.8)
            if names is None or not len(names):
                return {}
            limit = int(top or len(names))
            selected = [str(r["name"]).strip() for _, r in names.head(limit).iterrows() if str(r.get("name") or "").strip()]
            end = datetime.now().strftime("%Y%m%d")
            beg = (datetime.now() - timedelta(days=days * 2)).strftime("%Y%m%d")
            results: Dict[str, List[Dict]] = {}
            with ThreadPoolExecutor(max_workers=5) as ex:
                futs = {ex.submit(AStockDataService._ths_board_hist, nm, beg, end, days): nm for nm in selected}
                for fut in as_completed(futs):
                    nm = futs[fut]
                    try:
                        hist = fut.result()
                        if hist:
                            results[nm] = hist
                    except Exception as e:
                        logger.warning("sector trend ths %s failed: %s", nm, e)
            if not results:
                return {}
            dates: List[str] = []
            series: List[Dict] = []
            for nm, hist in results.items():
                if not series:
                    dates = [h["date"] for h in hist]
                closes = [h["close"] for h in hist]
                pcts = [h["pct"] for h in hist]
                series.append({
                    "name": nm,
                    "closes": closes,
                    "pcts": pcts,
                    "today_pct": pcts[-1] if pcts else 0,
                })
            return {"dates": dates, "series": series, "source": "ths",
                    "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
        except Exception as e:
            logger.warning("ths sector trends failed: %s", e)
            return {}

    @staticmethod
    def _ths_board_hist(sector_name: str, beg: str, end: str, days: int = 30) -> List[Dict]:
        """同花顺单板块指数日K（列位置映射：日期/收盘；涨跌幅由收盘序列推算）。"""
        try:
            df = ak.stock_board_industry_index_ths(symbol=sector_name, start_date=beg, end_date=end)
            if df is None or not len(df):
                return []
            rows = [list(r) for _, r in df.iterrows()]
            out = []
            prev = None
            for row in rows:
                date = str(row[0]) if row else ""
                close = float(row[4] or 0) if len(row) > 4 else 0
                pct = 0.0
                if prev:
                    pct = round((close - prev) / prev * 100, 2) if prev else 0.0
                prev = close
                out.append({"date": date, "close": close, "pct": pct})
            return out[-days:]
        except Exception as e:
            logger.warning("ths board hist %s failed: %s", sector_name, e)
            return []

    @staticmethod
    def _save_sector_trends(val: Dict) -> None:
        """板块曲线结果（dates+series）按板块落库；用作定时同步（可靠），失败静默。"""
        try:
            series = val.get("series") or []
            dates = val.get("dates") or []
            if not series or not dates:
                return
            for s in series:
                rows = [{"date": dates[i], "close": s["closes"][i],
                         "pct": s["pcts"][i] if i < len(s["pcts"]) else 0}
                        for i in range(len(dates))
                        if i < len(s["closes"])]
                ok = False
                for _ in range(2):
                    if SupabaseStore.save_sector_daily(s["name"], rows, val.get("source") or "ths"):
                        ok = True
                        break
                if not ok:
                    logger.warning("sector save retry exhausted: %s", s.get("name"))
        except Exception as e:  # noqa: BLE001
            logger.warning("save_sector_trends fail: %s", e)

    @staticmethod
    def _sector_trends_from_db(days: int = 30, top: int = 0) -> Optional[Dict]:
        """从Supabase杣捧瞿捧瞿（可选）当日板块趋势。"""

        try:
            if not SupabaseStore.ENABLED:
                return None
            name_df = None
            if ak is not None:
                try:
                    name_df = AStockDataService._with_retry(
                        lambda: ak.stock_board_industry_name_ths(), 1, 0.5)
                except Exception:  # noqa: BLE001
                    name_df = None
            if name_df is None or not len(name_df):
                return None
            limit = int(top or len(name_df))
            names = [str(r["name"]).strip() for _, r in name_df.head(limit).iterrows()
                     if str(r.get("name") or "").strip()]
            # 分页批量读取（PostgREST 单页 limit 上限 1000），单次 HTTP 一次取全量
            beg_date = (datetime.now() - timedelta(days=days * 2 + 7)).strftime("%Y-%m-%d")
            try:
                rows = []
                offset = 0
                while True:
                    page = SupabaseStore._select(
                        "quant_sector_daily", "*",
                        {"trade_date": f"gte.{beg_date}", "limit": str(SupabaseStore._PAGE_SIZE),
                         "offset": str(offset)})
                    if not page:
                        break
                    rows.extend(page)
                    if len(page) < SupabaseStore._PAGE_SIZE:
                        break
                    offset += SupabaseStore._PAGE_SIZE
            except Exception as e:  # noqa: BLE001
                logger.warning("sector batch read fail: %s", e)
                return None
            if not rows:
                return None
            by_sector: Dict[str, list] = {}
            for r in rows:
                by_sector.setdefault(r.get("sector"), []).append(r)
            series = []
            dates = []
            for nm in names:
                hist = by_sector.get(nm)
                if not hist:
                    return None
                hist.sort(key=lambda x: x.get("trade_date") or "")
                hist = hist[-days:]
                if len(hist) < max(days * 0.8, 15):
                    # 数据不足：整批放弃，交给实时抓取兜底
                    return None
                if not dates:
                    dates = [h["trade_date"] for h in hist]
                series.append({
                    "name": nm,
                    "closes": [float(h.get("close") or 0) for h in hist],
                    "pcts": [float(h.get("pct") or 0) for h in hist],
                    "today_pct": float(hist[-1].get("pct") or 0) if hist else 0,
                })
            if not series:
                return None
            return {"dates": dates, "series": series, "source": "supabase",
                    "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
        except Exception as e:  # noqa: BLE001
            logger.warning("sector_trends_from_db fail: %s", e)
            return None

    @staticmethod
    def get_sector_trends(days: int = 60, top: int = 10, force: bool = False) -> Dict:
        """板块涨跌曲线（外层硬超时包装）：15 秒内未完成则回退缓存/DB/空结构。"""
        key = f"sector_trends_{days}_{top}"
        now = time.time()
        if not force and key in AStockDataService._trend_cache:
            ts, val = AStockDataService._trend_cache[key]
            if now - ts < 300:
                return val
        try:
            with ThreadPoolExecutor(max_workers=1) as ex:
                fut = ex.submit(AStockDataService._get_sector_trends_impl, days, top, force)
                return fut.result(timeout=15)
        except TimeoutError:
            logger.warning("sector_trends timeout (15s), fallback to cache/db/empty")
            ts, val = AStockDataService._trend_cache.get(key, (0, None))
            if val:
                return val
            db_val = AStockDataService._sector_trends_from_db(days, top)
            if db_val:
                return db_val
            return {"dates": [], "series": [], "source": "timeout",
                    "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
        except Exception as e:
            logger.warning("sector_trends outer wrapper failed: %s", e)
            ts, val = AStockDataService._trend_cache.get(key, (0, None))
            if val:
                return val
            db_val = AStockDataService._sector_trends_from_db(days, top)
            if db_val:
                return db_val
            return {"dates": [], "series": [], "source": "error",
                    "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

    @staticmethod
    def _get_sector_trends_impl(days: int = 60, top: int = 10, force: bool = False) -> Dict:
        """板块涨跌曲线：行业板块列表 + 每板块近 days 日收盘涨跌幅。"""
        # 优先走同花顺全量板块（akshare stock_board_industry_*_ths，稳定覆盖约 90 个行业）；        失败回退到东财 clist + BK 列表。        """
        key = f"sector_trends_{days}_{top}"
        now = time.time()
        if not force and key in AStockDataService._trend_cache:
            ts, val = AStockDataService._trend_cache[key]
            if now - ts < 300:
                return val
        # 读库优先：10+ 板块近期数据齐时，直接由 Supabase 组装（不再整批抓取）
        db_val = AStockDataService._sector_trends_from_db(days, top)
        if db_val:
            AStockDataService._trend_cache[key] = (time.time(), db_val)
            return db_val
        ths_val = AStockDataService._sector_trends_ths(days, top)
        if ths_val and ths_val.get("series"):
            # 抓取结果异步落库
            threading.Thread(target=lambda: AStockDataService._save_sector_trends(ths_val), daemon=True).start()
            AStockDataService._trend_cache[key] = (time.time(), ths_val)
            return ths_val
        logger.warning("sector_trends ths empty, fallback to eastmoney")
        try:
            url = AStockDataService._EM + "/api/qt/clist/get"
            params = {"pn": 1, "pz": max(int(top) * 2, 20), "po": 1, "np": 1,
                      "fltt": 2, "invt": 2, "fid": "f3",
                      "fs": "m:90+t:2+f:!50",  # 行业板块
                      "fields": "f12,f14,f3,f20,f8"}
            r = requests.get(url, params=params, headers=AStockDataService._KT_HEADERS, timeout=12)
            r.raise_for_status()
            diff = ((r.json() or {}).get("data") or {}).get("diff") or []
            boards = []
            for x in diff:
                code = (x.get("f12") or "").strip()
                nm = (x.get("f14") or "").strip()
                if code.startswith("BK") and nm and len(boards) < int(top):
                    boards.append({"code": code, "name": nm,
                                   "pct": float(x.get("f3") or 0),
                                   "amount": float(x.get("f20") or 0),
                                   "hs": float(x.get("f8") or 0)})
            if not boards:
                raise RuntimeError("empty sector list")
            dates: List[str] = []
            series: List[Dict] = []
            results = {}
            # 分批小并发抓取历史，batch=2 控制并发避免限流；外层 15s 硬超时兜底
            batch = 2
            for i in range(0, len(boards), batch):
                chunk = boards[i:i + batch]
                with ThreadPoolExecutor(max_workers=batch) as ex:
                    futs = {ex.submit(AStockDataService.get_sector_hist_em, b["code"], max(days, 15)): b
                            for b in chunk}
                    for fut in as_completed(futs):
                        b = futs[fut]
                        try:
                            hist = fut.result()
                            if hist:
                                results[b["name"]] = {"board": b, "hist": hist}
                        except Exception as e:
                            logger.warning("sector trend %s failed: %s", b["name"], e)
                time.sleep(0.25)
            names = list(results.keys())
            for i, nm in enumerate(names):
                hist = results[nm]["hist"]
                if not series:
                    dates = [h["date"] for h in hist]
                closes = [h["close"] for h in hist]
                pcts = [h["pct"] for h in hist]
                series.append({
                    "name": nm,
                    "closes": closes,
                    "pcts": pcts,
                    "today_pct": results[nm]["board"]["pct"],
                })
            val = {"dates": dates, "series": series, "source": "eastmoney",
                   "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
            if series:
                AStockDataService._trend_cache[key] = (time.time(), val)
                return val
            logger.warning("sector_trends eastmoney series empty, fallback to MSN")
        except Exception as e:
            logger.warning("sector_trends failed: %s", e)
        # ---- MSN SWS 行业指数兜底 ----
        try:
            now2 = time.time()
            if now2 - AStockDataService._sws_ts < 600 and AStockDataService._sws_cache:
                return AStockDataService._sws_cache
            msn_boards = []
            with ThreadPoolExecutor(max_workers=4) as ex:
                def _fetch_sws(name):
                    secid = AStockDataService._msn_find_index_secid(name)
                    return name, secid
                futs = {ex.submit(_fetch_sws, name): name
                        for name in list(AStockDataService._SWS_SECTORS)[: max(int(top) * 2, 10)]}
                for fut in as_completed(futs):
                    name, secid = fut.result()
                    if secid:
                        hist = AStockDataService._msn_charts_10yh1(secid, max(days, 15))
                        if hist and len(hist) > 5:
                            closes = [h["close"] for h in hist]
                            pcts = [0.0] + [round((closes[i] - closes[i-1]) / closes[i-1] * 100, 2)
                                            if closes[i-1] else 0 for i in range(1, len(closes))]
                            msn_boards.append({"name": name, "closes": closes, "pcts": pcts,
                                               "today_pct": pcts[-1] if pcts else 0.0,
                                               "dates": [h["date"] for h in hist]})
            if msn_boards:
                msn_boards.sort(key=lambda b: abs(b.get("today_pct", 0)), reverse=True)
                msn_boards = msn_boards[:top]
                dates = msn_boards[0]["dates"]
                series = [{"name": b["name"], "closes": b["closes"], "pcts": b["pcts"],
                           "today_pct": b["today_pct"]} for b in msn_boards]
                val = {"dates": dates, "series": series, "source": "msn_sws",
                       "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
                AStockDataService._trend_cache[key] = (time.time(), val)
                AStockDataService._sws_cache = val
                AStockDataService._sws_ts = time.time()
                return val
        except Exception as e2:
            logger.warning("MSN sector fallback failed: %s", e2)
        return {"dates": [], "series": [], "source": "eastmoney",
                "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

    @staticmethod
    def _em_fflow(fid_secids: str, extra_fs: str = "", po: int = 1) -> list:
        """东财资金流排名单；fid: f62（主力净流入排名）；po=1 取流入，po=0 取流出。"""
        out = []
        try:
            url = AStockDataService._EM + "/api/qt/clist/get"
            params = {"pn": 1, "pz": 20, "po": po, "np": 1, "fltt": 2, "invt": 2,
                      "fid": fid_secids or "f62",
                      "fs": extra_fs or "m:90+t:2+f:!50",
                      "fields": "f12,f14,f2,f3,f62,f184,f66,f69,f72,f75,f78,f81,f84,f87,f124"}
            r = requests.get(url, params=params, headers=AStockDataService._KT_HEADERS, timeout=12)
            r.raise_for_status()
            diff = ((r.json() or {}).get("data") or {}).get("diff") or []
            for x in diff:
                def F(k):
                    v = x.get(k)
                    return float(v) if v is not None else 0.0
                out.append({
                    "code": str(x.get("f12") or ""),
                    "name": str(x.get("f14") or ""),
                    "price": F("f2"),
                    "pct": F("f3"),
                    "main": F("f62"),
                    "main_pct": F("f184"),
                    "super": F("f66"),
                    "big": F("f72"),
                    "medium": F("f78"),
                    "small": F("f84"),
                    "lead": str(x.get("f124") or ""),
                })
        except Exception as e:
            logger.warning("em_fflow %s failed: %s", fid_secids, e)
        return out

    @staticmethod
    def get_market_fund_flow(force: bool = False) -> Dict:
        """主力净流入流出：板块资金流入/流出榜 + 个股榜 + 大盘指数资金流。"""
        key = "market_fund_flow"
        now = time.time()
        if not force and key in AStockDataService._fflow_cache:
            ts, val = AStockDataService._fflow_cache[key]
            if now - ts < 60:
                return val
        try:
            sectors_in = AStockDataService._em_fflow("f62", "m:90+t:2+f:!50", po=1)
            sectors_out = AStockDataService._em_fflow("f62", "m:90+t:2+f:!50", po=0)
            stocks_in = AStockDataService._em_fflow(
                "f62", "m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048", po=1)
            stocks_out = AStockDataService._em_fflow(
                "f62", "m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048", po=0)
            # 大盘指数资金流（上证 1.000001 + 深证 0.399001）
            indices = []
            for secid, secid2, idxname in (("1.000001", "0.399001", "上证指数"),
                                           ("0.399001", "1.000001", "深证成指")):
                try:
                    url = AStockDataService._EM + "/api/qt/stock/fflow/kline/get"
                    params = {"ut": AStockDataService._ZX_UT,
                              "lmt": "0", "klt": "1", "secid": secid, "secid2": secid2,
                              "fields1": "f1,f2,f3,f7",
                              "fields2": "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f62,f63,f64,f65"}
                    r = requests.get(url, params=params, headers=AStockDataService._KT_HEADERS, timeout=12)
                    r.raise_for_status()
                    kl = ((r.json() or {}).get("data") or {}).get("klines") or []
                    if not kl:
                        continue
                    f = kl[-1].split(",")
                    if len(f) >= 6:
                        indices.append({
                            "name": idxname,
                            "date": f[0],
                            "main": float(f[1] or 0),
                            "small": float(f[2] or 0),
                            "medium": float(f[3] or 0),
                            "big": float(f[4] or 0),
                            "super": float(f[5] or 0),
                        })
                except Exception as e:
                    logger.warning("index fflow %s failed: %s", secid, e)
            val = {
                "indices": indices,
                "sectors_in": sectors_in[:12],
                "sectors_out": sectors_out[:12],
                "stocks_in": stocks_in[:12],
                "stocks_out": stocks_out[:12],
                "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            }
            threading.Thread(
                target=lambda: SupabaseStore.save_pool(
                    "fund_flow", datetime.now().strftime("%Y-%m-%d"), val),
                daemon=True).start()
            AStockDataService._fflow_cache[key] = (time.time(), val)
            return val
        except Exception as e:
            logger.warning("market_fund_flow failed: %s", e)
            return {"indices": [], "sectors_in": [], "sectors_out": [],
                    "stocks_in": [], "stocks_out": [],
                    "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

