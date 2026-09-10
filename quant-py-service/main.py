"""
Web3 量化交易平台 - FastAPI 微服务
整合功能：
1. A股行情数据（多源：mootdx/腾讯/东方财富/新浪）
2. 回测引擎（事件驱动，支持多策略）
3. 多Agent AI投研分析（7分析师+多空辩论）
4. 持仓管理
5. 新闻舆情雷达
6. 策略管理（小市值/网格/CTA等）
7. 数据导出（Markdown/PDF/Excel）
"""

import os
import sys
import json
import time
import uuid
import logging
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
from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, StreamingResponse
from pydantic import BaseModel

# ============================================================
# 日志配置
# ============================================================
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(name)s: %(message)s'
)
logger = logging.getLogger("quant")

# ============================================================
# 配置
# ============================================================
QUANT_DATA_DIR = Path(os.environ.get("QUANT_DATA_DIR", "./data"))
QUANT_OUTPUT_DIR = Path(os.environ.get("QUANT_OUTPUT_DIR", "./output"))
QUANT_CACHE_DIR = Path(os.path.expanduser("~/.quant-platform"))
for d in [QUANT_DATA_DIR, QUANT_OUTPUT_DIR, QUANT_CACHE_DIR]:
    d.mkdir(parents=True, exist_ok=True)

# ============================================================
# 数据模型
# ============================================================
class TaskStatus(str, Enum):
    PENDING = "pending"
    RUNNING = "running"
    COMPLETED = "completed"
    FAILED = "failed"

class SignalType(str, Enum):
    BUY = "BUY"
    SELL = "SELL"
    HOLD = "HOLD"

@dataclass
class StockQuote:
    symbol: str
    name: str
    price: float
    change: float
    change_percent: float
    volume: float
    turnover: float
    high: float
    low: float
    open: float
    amplitude: float
    turnover_rate: float
    pe: Optional[float] = None
    pb: Optional[float] = None
    market_cap: Optional[float] = None
    updated_at: str = ""

@dataclass
class KLineData:
    date: str
    open: float
    close: float
    high: float
    low: float
    volume: float
    turnover: float
    change_percent: float

@dataclass
class TradeSignal:
    signal: str
    symbol: str
    name: str
    price: float
    reason: str
    confidence: float
    timestamp: str = ""
    analysts: List[str] = field(default_factory=list)

@dataclass
class BacktestResult:
    strategy_name: str
    initial_cash: float
    final_value: float
    total_return: float
    annual_return: float
    max_drawdown: float
    sharpe_ratio: float
    win_rate: float
    trade_count: int
    equity_curve: List[float] = field(default_factory=list)
    transactions: List[Dict] = field(default_factory=list)

@dataclass
class AnalysisTask:
    task_id: str
    symbol: str
    status: TaskStatus
    progress: int = 0
    current_stage: str = ""
    result: Optional[Dict] = None
    error: Optional[str] = None
    created_at: str = ""
    completed_at: Optional[str] = None

# 内存中的任务存储
_task_store: Dict[str, AnalysisTask] = {}
_backtest_store: Dict[str, BacktestResult] = {}
_portfolio_store: Dict[str, Dict] = {}


# ============================================================
# A股数据服务
# ============================================================
try:
    import akshare as ak
except Exception:
    ak = None


try:
    import akshare as ak
except Exception:
    ak = None


try:
    import akshare as ak
except Exception:
    ak = None


class AStockDataService:
    """A股数据服务 - 多源聚合 (akshare: 东方财富/新浪财经/腾讯/同花顺), Vibe/mock 降级"""

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
    _all_df = None
    _all_src = ""
    _all_ts = 0.0
    _ALL_TTL = 120.0
    _snap_lock = threading.Lock()
    _batch_lock = threading.Lock()

    KLINE_PERIOD_MAP = {"1m": "1", "5m": "5", "15m": "15", "30m": "30", "60m": "60"}
    DAILY_PERIOD_MAP = {"1d": "daily", "d": "daily", "day": "daily", "daily": "daily",
                        "1w": "weekly", "w": "weekly", "week": "weekly", "weekly": "weekly",
                        "1mo": "monthly", "m": "monthly", "month": "monthly", "monthly": "monthly"}

    @staticmethod
    def _to_code(symbol: str) -> str:
        s = str(symbol or "")
        if s[:2] in ("sh", "sz", "bj"):
            s = s[2:]
        return AStockDataService._clean_code(s)

    @staticmethod
    def _clean_code(code) -> str:
        """把代码规整为 6 位数字：处理 float 形式的 '920992.0'、首部 sh/sz/bj、补零。"""
        s = str(code or "").strip()
        if s[:2] in ("sh", "sz", "bj"):
            s = s[2:]
        # float 转字符串会带 .0（如 920992.0），数字代码直接取整数部分
        if s.endswith(".0"):
            try:
                s = str(int(float(s)))
            except (ValueError, TypeError):
                pass
        # 保留纯数字，去掉残留非数字
        digits = "".join(ch for ch in s if ch.isdigit())
        if not digits:
            return ""
        # 若长度 >6 通常是 B 股/美股带后缀，截断到 6 位
        if len(digits) > 6:
            digits = digits[:6]
        return digits.zfill(6)

    @staticmethod
    def _to_symbol(code: str) -> str:
        code = AStockDataService._clean_code(code)
        if not code:
            return str(code or "")
        prefix = "sh" if code.startswith(("6", "9", "68")) else ("bj" if code.startswith(("4", "8")) else "sz")
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
        """守护线程执行 fn，超过 budget 秒即放弃（僵尸线程留在后台，不阻塞进程）"""
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
        """东方财富 clist 直连拉取全市场快照（主域名被 WAF 拦时自动切 push2delay 镜像）"""
        url_tpl = "https://{host}/api/qt/clist/get"
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                          "(KHTML, like Gecko) Chrome/126.0 Safari/537.36",
            "Referer": "https://quote.eastmoney.com/",
        }
        fields = "f2,f3,f4,f5,f6,f7,f8,f9,f12,f14,f15,f16,f17,f18,f20,f23"
        col_map = {"f12": "代码", "f14": "名称", "f2": "最新价", "f3": "涨跌幅", "f4": "涨跌额",
                   "f5": "成交量", "f6": "成交额", "f7": "振幅", "f8": "换手率",
                   "f9": "市盈率-动态", "f15": "最高", "f16": "最低", "f17": "今开",
                   "f18": "昨收", "f20": "总市值", "f23": "市净率"}
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
            """东财 clist 单页抓取（返回原始 data 节点）"""
            params = {"pn": pn, "pz": 100, "po": 1, "np": 1, "fltt": 2, "invt": 2,
                      "fid": "f12", "fs": "m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048",
                      "fields": fields}
            r = requests.get(url_tpl.format(host=host), params=params, headers=headers, timeout=20)
            r.raise_for_status()
            return (r.json() or {}).get("data") or {}

        def fetch_page_retry(host, pn, retries=3):
            """单页抓取，带退避重试（东财对高频请求易中断连接）"""
            last = None
            for attempt in range(retries):
                try:
                    return fetch_page(host, pn)
                except Exception as e:  # noqa: BLE001
                    last = e
                    time.sleep(0.6 * (attempt + 1))
            raise last

        # 选出可用主机（主域名可能对非浏览器 TLS 指纹断连，镜像一般可用）
        host, first = None, None
        for h in ("push2.eastmoney.com", "push2delay.eastmoney.com"):
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
    def _fetch_snapshot_once():
        """依次尝试：东财直连 -> akshare 东财；任一成功即返回 (df, src)，均失败返回 (None, '')"""
        attempts = [(lambda: AStockDataService._em_snapshot_direct(), "东财直连", 120.0)]
        if ak is not None:
            attempts.append((lambda: ak.stock_zh_a_spot_em(), "东方财富(akshare)", 10.0))
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
        # 新鲜缓存直接命中
        now = time.time()
        if not force and AStockDataService._all_df is not None \
                and now - AStockDataService._all_ts < AStockDataService._ALL_TTL:
            return AStockDataService._all_df, AStockDataService._all_src
        lock = AStockDataService._snap_lock
        if lock.acquire(blocking=False):
            try:
                # 拿到锁后二次确认（可能别的请求刚写完缓存）
                if not force and AStockDataService._all_df is not None \
                        and time.time() - AStockDataService._all_ts < AStockDataService._ALL_TTL:
                    return AStockDataService._all_df, AStockDataService._all_src
                df, src = AStockDataService._fetch_snapshot_once()
                if df is not None and len(df):
                    AStockDataService._all_df = df
                    AStockDataService._all_src = src
                    AStockDataService._all_ts = time.time()
                    return df, src
            finally:
                lock.release()
        else:
            # 已有请求在拉取：最多等 12s 共享其结果，超时走旧缓存/失败路径
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
            pe=AStockDataService._row_num(row, "市盈率-动态", "pe") or None,
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
        """腾讯 qt.gtimg.cn 批量实时行情（单次请求，快且稳）"""
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
        if not force and now - AStockDataService._cache_ts < AStockDataService._CACHE_TTL and AStockDataService._quote_cache:
            return AStockDataService._quote_cache
        lock = AStockDataService._batch_lock
        if not lock.acquire(blocking=False):
            # 别的请求正在刷新：最多等 10s 共享结果
            if lock.acquire(timeout=10) :
                try:
                    return AStockDataService._quote_cache
                finally:
                    lock.release()
            return AStockDataService._quote_cache
        try:
            if not force and time.time() - AStockDataService._cache_ts < AStockDataService._CACHE_TTL \
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
        """日K重采样为周K/月K"""
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
    def get_kline(symbol: str, period: str = "1d", days: int = 60) -> List[Dict]:
        """K线: 分时(1m/5m/15m/30m/60m 新浪) / 日周月(东财, 腾讯兜底) / Vibe / mock"""
        sym = AStockDataService._to_symbol(symbol)
        code = AStockDataService._to_code(sym)
        p = (period or "1d").lower()
        source = ""

        if p in AStockDataService.KLINE_PERIOD_MAP and ak is not None:
            try:
                df = AStockDataService._with_retry(
                    lambda: ak.stock_zh_a_minute(symbol=sym, period=AStockDataService.KLINE_PERIOD_MAP[p]), 2, 0.8)
                if df is not None and len(df):
                    source = "新浪财经"
                    result = []
                    tail = df.tail(min(len(df), max(days * 4, 240)))
                    for _, r in tail.iterrows():
                        result.append({
                            "date": str(r["day"])[:19], "open": float(r["open"]), "close": float(r["close"]),
                            "high": float(r["high"]), "low": float(r["low"]),
                            "volume": float(r.get("volume") or 0), "turnover": float(r.get("amount") or 0),
                            "amplitude": 0.0, "change_percent": 0.0,
                        })
                    if result:
                        return AStockDataService._tag(result, source)
            except Exception as e:
                logger.warning("SINA minute %s %s failed: %s", sym, p, e)

        if p in AStockDataService.DAILY_PERIOD_MAP:
            ak_period = AStockDataService.DAILY_PERIOD_MAP[p]
            # 1) 东财原生日/周/月（重试3次）
            if ak is not None:
                try:
                    start = (datetime.now() - timedelta(days=days * 4 + 120)).strftime("%Y%m%d")
                    today = datetime.now().strftime("%Y%m%d")
                    df = AStockDataService._with_retry(
                        lambda: ak.stock_zh_a_hist(symbol=code, period=ak_period, start_date=start,
                                                   end_date=today, adjust="qfq"), 3, 1.2)
                    if df is not None and len(df):
                        source = "东方财富"
                        result = []
                        tail = df.tail(days)
                        for _, r in tail.iterrows():
                            result.append({
                                "date": str(r["日期"])[:10], "open": float(r["开盘"]), "close": float(r["收盘"]),
                                "high": float(r["最高"]), "low": float(r["最低"]),
                                "volume": float(r.get("成交量") or 0), "turnover": float(r.get("成交额") or 0),
                                "amplitude": float(r.get("振幅") or 0),
                                "change_percent": float(r.get("涨跌幅") or 0),
                            })
                        if result:
                            return AStockDataService._tag(result, source)
                except Exception as e:
                    logger.warning("EM hist %s %s failed: %s", sym, ak_period, e)
            # 2) 新浪日线（周/月重采样，日线直出）
            if ak is not None:
                try:
                    today = datetime.now().strftime("%Y-%m-%d")
                    start = (datetime.now() - timedelta(days=days * 6 + 120)).strftime("%Y-%m-%d")
                    df = AStockDataService._with_retry(
                        lambda: ak.stock_zh_a_daily(symbol=sym, start_date=start, end_date=today, adjust="qfq"), 2, 0.8)
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
                            return AStockDataService._tag(period_rows[-days:], "新浪财经")
                except Exception as e:
                    logger.warning("SINA daily %s failed: %s", sym, e)
            # 3) 腾讯日线（周/月重采样）
            if ak is not None:
                try:
                    today = datetime.now().strftime("%Y%m%d")
                    start = (datetime.now() - timedelta(days=days * 6 + 120)).strftime("%Y%m%d")
                    df = AStockDataService._with_retry(
                        lambda: ak.stock_zh_a_hist_tx(symbol=sym, start_date=start, end_date=today), 2, 0.8)
                    if df is not None and len(df):
                        daily_rows = []
                        for _, r in df.iterrows():
                            daily_rows.append({
                                "date": str(r["date"])[:10], "open": float(r["open"]), "close": float(r["close"]),
                                "high": float(r["high"]), "low": float(r["low"]),
                                "volume": float(r.get("volume") or 0), "turnover": float(r.get("amount") or 0),
                                "amplitude": 0.0, "change_percent": 0.0,
                            })
                        if ak_period == "daily":
                            period_rows = daily_rows
                        else:
                            period_rows = AStockDataService._resample_daily(daily_rows, ak_period)
                        if period_rows:
                            return AStockDataService._tag(period_rows[-days:], "腾讯")
                except Exception as e:
                    logger.warning("TX hist %s failed: %s", sym, e)

        # 4) Vibe 日K（周/月重采样）
        try:
            vcode = AStockDataService._to_code(sym)
            url = f"{AStockDataService.VIBE_BASE}/api/kline"
            r = requests.get(url, params={"code": vcode, "category": 4, "offset": max(days, 1)}, timeout=10)
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
                if ak_period != "daily" and p in AStockDataService.DAILY_PERIOD_MAP:
                    period_rows = AStockDataService._resample_daily(result, ak_period)
                    if period_rows:
                        return AStockDataService._tag(period_rows[-days:], source)
                if result:
                    return AStockDataService._tag(result[-days:], source)
        except Exception as e:
            logger.warning("Vibe kline failed for %s: %s", symbol, e)

        seed = int(hashlib.md5(f"{sym}_{p}".encode()).hexdigest()[:8], 16)
        rng = np.random.RandomState(seed)
        base_price = rng.uniform(10, 500)
        now = datetime.now()
        result = []
        for i in range(days):
            date = (now - timedelta(days=days - i)).strftime("%Y-%m-%d")
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
        return AStockDataService._tag(result, source)

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

    _spark_cache: Dict[str, List] = {}
    _spark_ts = 0.0
    _SPARK_TTL = 180.0

    @staticmethod
    def get_sparklines_batch(codes: List[str], limit: int = 40) -> Dict[str, List[float]]:
        """并发批量获取近12日收盘价走势（自带缓存/限速）"""
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
        """全市场搜索（东财/新浪快照，仅用缓存快照，避免每次键入都全量拉取）"""
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

    @staticmethod
    def get_sector_heat_ths() -> Dict[str, float]:
        """同花顺行业板块涨跌"""
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
def rng_float(seed: str, lo: float, hi: float) -> float:
    s = int(hashlib.md5(seed.encode()).hexdigest()[:8], 16)
    rng = np.random.RandomState(s)
    return float(rng.uniform(lo, hi))


# ============================================================
# 新闻舆情服务
# ============================================================
class NewsRadarService:
    """新闻舆情雷达 - 聚合财经新闻"""

    NEWS_SOURCES = [
        {"source": "财联社", "category": "宏观", "priority": "high"},
        {"source": "东方财富", "category": "市场", "priority": "high"},
        {"source": "新浪财经", "category": "综合", "priority": "medium"},
        {"source": "同花顺", "category": "个股", "priority": "medium"},
        {"source": "华尔街见闻", "category": "国际", "priority": "high"},
    ]

    @staticmethod
    def get_latest_news(limit: int = 20) -> List[Dict]:
        """获取最新财经新闻"""
        headlines = [
            "央行宣布降准0.5个百分点，释放长期资金约1万亿",
            "工信部发布人工智能产业发展三年行动计划",
            "北交所转板机制进一步完善，精选层企业迎利好",
            "新能源汽车销量再创新高，比亚迪月销突破30万辆",
            "美联储暗示年内可能降息，A股外资流入加速",
            "半导体国产替代加速，多家公司发布新品",
            "房地产政策持续优化，一线城市成交量回升",
            "中药板块异动拉升，多只个股涨停",
            "AI应用落地加速，算力需求爆发式增长",
            "光伏产业链价格企稳，行业回暖信号明显",
            "军工板块获资金青睐，订单饱满支撑业绩",
            "消费复苏态势延续，白酒龙头业绩超预期",
            "医药反腐进入常态化，行业集中度提升",
            "5G-A商用元年开启，基站建设加速推进",
            "量子计算取得突破，相关概念股持续走强",
        ]
        result = []
        for i, headline in enumerate(headlines[:limit]):
            seed = int(hashlib.md5(f"news_{i}_{datetime.now().strftime('%Y%m%d')}".encode()).hexdigest()[:8], 16)
            rng = np.random.RandomState(seed)
            source = NewsRadarService.NEWS_SOURCES[i % len(NewsRadarService.NEWS_SOURCES)]
            sentiment = rng.choice(["positive", "negative", "neutral"], p=[0.4, 0.2, 0.4])
            impact = rng.choice(["high", "medium", "low"], p=[0.3, 0.5, 0.2])

            result.append({
                "id": f"news_{i}",
                "title": headline,
                "source": source["source"],
                "category": source["category"],
                "sentiment": sentiment,
                "impact": impact,
                "timestamp": (datetime.now() - timedelta(minutes=i * 15 + rng.randint(0, 14))).strftime("%Y-%m-%d %H:%M"),
                "summary": headline[:30] + "...",
            })
        return result

    @staticmethod
    def get_sector_heat() -> Dict[str, float]:
        """获取板块热度"""
        sectors = ["人工智能", "新能源", "半导体", "医药生物", "军工", "消费", "金融", "房地产", "有色金属", "电力"]
        rng = np.random.RandomState(int(time.time()) % 2**32)
        return {s: round(float(rng.uniform(-3, 5)), 2) for s in sectors}


# ============================================================
# 回测引擎
# ============================================================
class BacktestEngine:
    """事件驱动回测引擎"""

    def __init__(self, config: Dict):
        self.config = config
        self.initial_cash = config.get("initial_cash", 1000000)
        self.cash = self.initial_cash
        self.positions: Dict[str, Dict] = {}
        self.transactions: List[Dict] = []
        self.daily_values: List[float] = []
        self.dates: List[str] = []
        self.stop_loss = config.get("stop_loss", -0.10)
        self.max_position = config.get("max_position", 0.05)
        self.commission = config.get("commission", 0.0003)
        self.slippage = config.get("slippage", 0.001)

    def run(self, symbols: List[str], price_data: Dict[str, List[Dict]]) -> Dict:
        """运行回测"""
        dates = sorted(set(d["date"] for s in price_data.values() for d in s))
        if not dates:
            return {"error": "无数据"}

        benchmark = [1.0]
        portfolio_val = [1.0]

        for date in dates[:120]:  # 最多120天
            self._rebalance(date, symbols, price_data)
            value = self.cash + sum(
                p.get("shares", 0) * self._get_price(s, date, price_data)
                for s, p in self.positions.items()
            )
            self.daily_values.append(value)
            self.dates.append(date)
            portfolio_val.append(value / self.initial_cash)
            if len(benchmark) < len(portfolio_val):
                benchmark.append(benchmark[-1] * (1 + rng_float(date, -0.005, 0.005)))

        return self._calculate_metrics(portfolio_val, benchmark)

    def _rebalance(self, date: str, symbols: List[str], price_data: Dict):
        """调仓逻辑 - 小市值策略模拟"""
        candidates = symbols[:5] if len(symbols) > 5 else symbols
        target_value_per = self.initial_cash * self.max_position

        for code in candidates:
            price = self._get_price(code, date, price_data)
            if price <= 0:
                continue
            shares = int(target_value_per / price / 100) * 100
            if shares <= 0:
                continue

            if code not in self.positions:
                self._buy(code, shares, price, date)
            else:
                pnl = (price - self.positions[code]["cost"]) / self.positions[code]["cost"]
                if pnl <= self.stop_loss:
                    self._sell(code, self.positions[code]["shares"], price, date, reason="止损")

    def _buy(self, code, shares, price, date):
        cost = shares * price * (1 + self.commission + self.slippage)
        if cost > self.cash:
            return
        self.cash -= cost
        self.positions[code] = {"shares": shares, "cost": price, "buy_date": date}
        self.transactions.append({"date": date, "code": code, "action": "BUY", "shares": shares, "price": price})

    def _sell(self, code, shares, price, date, reason=""):
        if code not in self.positions:
            return
        revenue = shares * price * (1 - self.commission - self.slippage)
        self.cash += revenue
        self.transactions.append({"date": date, "code": code, "action": "SELL", "shares": shares, "price": price, "reason": reason})
        del self.positions[code]

    def _get_price(self, code, date, price_data):
        for d in price_data.get(code, []):
            if d["date"] == date:
                return d["close"]
        return 0

    def _calculate_metrics(self, equity, benchmark) -> Dict:
        if len(equity) < 2:
            return {}
        total_return = (equity[-1] - 1) * 100
        days = len(equity)
        annual_return = ((equity[-1] / equity[0]) ** (252 / max(days, 1)) - 1) * 100
        peak = np.maximum.accumulate(equity)
        drawdown = (peak - equity) / peak
        max_dd = np.max(drawdown) * 100 if len(drawdown) > 0 else 0
        returns = np.diff(equity) / np.array(equity[:-1])
        sharpe = (np.mean(returns) / (np.std(returns) + 1e-8)) * np.sqrt(252) * 100 if len(returns) > 0 else 0
        wins = sum(1 for t in self.transactions if t.get("action") == "SELL" and t.get("reason") != "止损")
        total_sells = sum(1 for t in self.transactions if t.get("action") == "SELL")
        win_rate = (wins / total_sells * 100) if total_sells > 0 else 0

        return {
            "initial_cash": self.initial_cash,
            "final_value": round(self.initial_cash * equity[-1], 2),
            "total_return": round(total_return, 2),
            "annual_return": round(annual_return, 2),
            "max_drawdown": round(max_dd, 2),
            "sharpe_ratio": round(sharpe, 2),
            "win_rate": round(win_rate, 2),
            "trade_count": len(self.transactions),
            "equity_curve": [round(v, 4) for v in equity],
            "benchmark_curve": [round(v, 4) for v in benchmark],
        }


# ============================================================
# 多Agent AI分析服务
# ============================================================
class AIAnalysisService:
    """多Agent AI投研分析（简化版，生产环境接入LLM API）"""

    @staticmethod
    async def analyze_stock(symbol: str, task_id: str) -> Dict:
        """异步分析股票 - 7分析师流程"""
        stages = [
            ("market_analyst", "市场技术分析", "K线形态、技术指标、量价关系分析"),
            ("social_analyst", "舆情情绪分析", "社交媒体情绪、散户讨论热度"),
            ("news_analyst", "新闻事件分析", "行业新闻、公告、宏观事件影响"),
            ("fundamentals_analyst", "基本面分析", "财报分析、盈利能力、估值水平"),
            ("policy_analyst", "政策分析", "监管政策、产业政策、窗口指导"),
            ("hot_money_analyst", "游资追踪", "龙虎榜、大单流向、主力资金"),
            ("lockup_analyst", "解禁监控", "限售解禁、大股东减持、股权质押"),
        ]

        analysts_reports = {}
        for stage_id, name, desc in stages:
            await asyncio.sleep(0.3 + np.random.random() * 0.5)
            seed = int(hashlib.md5(f"{task_id}_{stage_id}".encode()).hexdigest()[:8], 16)
            rng = np.random.RandomState(seed)

            if stage_id == "market_analyst":
                report = {
                    "trend": rng.choice(["上升趋势", "下降趋势", "震荡整理"], p=[0.4, 0.2, 0.4]),
                    "support": round(rng.uniform(80, 150), 2),
                    "resistance": round(rng.uniform(160, 300), 2),
                    "indicators": {"RSI": round(rng.uniform(30, 70), 1), "MACD": rng.choice(["金叉", "死叉", "粘合"]),
                                   "KDJ": rng.choice(["超买", "超卖", "中性"])},
                    "score": round(rng.uniform(5, 9), 1),
                    "verdict": rng.choice(["看多", "看空", "中性"], p=[0.45, 0.2, 0.35]),
                }
            elif stage_id == "policy_analyst":
                report = {
                    "policy_impact": rng.choice(["利好", "利空", "中性"], p=[0.35, 0.15, 0.5]),
                    "key_policies": ["央行货币政策调整", "行业监管政策变化"],
                    "score": round(rng.uniform(5, 9), 1),
                    "verdict": rng.choice(["看多", "看空", "中性"], p=[0.4, 0.15, 0.45]),
                }
            elif stage_id == "hot_money_analyst":
                report = {
                    "capital_flow": rng.choice(["大幅流入", "小幅流入", "平衡", "小幅流出", "大幅流出"]),
                    "dragon_tiger": rng.choice(["机构买入", "游资介入", "无明显异动"]),
                    "score": round(rng.uniform(4, 9), 1),
                    "verdict": rng.choice(["看多", "看空", "中性"], p=[0.35, 0.2, 0.45]),
                }
            else:
                report = {
                    "score": round(rng.uniform(5, 9), 1),
                    "verdict": rng.choice(["看多", "看空", "中性"], p=[0.4, 0.2, 0.4]),
                    "summary": f"{name}完成分析，综合评分{round(rng.uniform(5,9),1)}分",
                    "key_points": [f"关键发现{i + 1}" for i in range(3)],
                }

            analysts_reports[stage_id] = {
                "id": stage_id, "name": name, "description": desc,
                "status": "completed", "report": report,
            }

        # Bull vs Bear 辩论
        bull_score = sum(r["report"]["score"] for r in analysts_reports.values() if r["report"].get("verdict") == "看多")
        bear_score = sum(r["report"]["score"] for r in analysts_reports.values() if r["report"].get("verdict") == "看空")
        total = bull_score + bear_score or 1
        bull_pct = bull_score / total

        if bull_pct > 0.6:
            final_signal, confidence, reason = "BUY", round(0.6 + bull_pct * 0.35, 2), "多方力量占优，建议关注买入机会"
        elif bull_pct < 0.4:
            final_signal, confidence, reason = "SELL", round(0.6 + (1 - bull_pct) * 0.35, 2), "空方力量占优，建议控制风险"
        else:
            final_signal, confidence, reason = "HOLD", 0.55, "多空力量均衡，建议观望等待明确信号"

        return {
            "symbol": symbol,
            "task_id": task_id,
            "status": "completed",
            "analysts": analysts_reports,
            "debate": {
                "bull_score": round(bull_score, 1), "bear_score": round(bear_score, 1),
                "bull_percentage": round(bull_pct * 100, 1),
            },
            "final_signal": {
                "signal": final_signal, "confidence": confidence,
                "reason": reason,
                "analysts": [a["name"] for a in analysts_reports.values()],
            },
            "completed_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        }


# ============================================================
# 持仓管理
# ============================================================
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
class StrategyService:
    """量化策略管理"""

    BUILTIN_STRATEGIES = [
        {"id": "bse_smallcap", "name": "北交所小市值", "description": "选择北交所市值最小的N只股票，等权配置，定期调仓",
         "category": "多因子", "risk_level": "MEDIUM", "tags": "小市值,北交所,等权"},
        {"id": "grid_okx", "name": "OKX网格交易", "description": "在价格区间内自动低买高卖，赚取波动收益",
         "category": "高频", "risk_level": "LOW", "tags": "网格,BTC,ETH,OKX"},
        {"id": "moving_avg", "name": "双均线策略", "description": "MA5上穿MA20金叉买入，死叉卖出",
         "category": "技术分析", "risk_level": "MEDIUM", "tags": "均线,趋势"},
        {"id": "macd_signal", "name": "MACD信号策略", "description": "MACD金叉买入，死叉卖出，配合成交量确认",
         "category": "技术分析", "risk_level": "MEDIUM", "tags": "MACD,趋势"},
        {"id": "bollinger", "name": "布林带策略", "description": "价格触及下轨买入，触及上轨卖出",
         "category": "技术分析", "risk_level": "LOW", "tags": "布林带,均值回归"},
        {"id": "rsi_meanrev", "name": "RSI均值回归", "description": "RSI超卖时买入，超买时卖出",
         "category": "技术分析", "risk_level": "LOW", "tags": "RSI,均值回归"},
        {"id": "qmt_auto", "name": "QMT自动交易", "description": "基于迅投QMT平台的自动化策略接口",
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


# ============================================================
# 回测执行器
# ============================================================
async def run_backtest_task(task_id: str, strategy_id: str, symbols: List[str], config: Dict):
    """异步执行回测任务"""
    task = AnalysisTask(
        task_id=task_id, symbol=",".join(symbols),
        status=TaskStatus.RUNNING, progress=0,
        current_stage="准备数据", created_at=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    )
    _task_store[task_id] = task

    try:
        # 阶段1: 获取数据
        task.current_stage = "获取行情数据"
        task.progress = 10
        price_data = {}
        for sym in symbols:
            price_data[sym] = AStockDataService.get_kline(sym, days=config.get("days", 120))
            await asyncio.sleep(0.1)

        # 阶段2: 运行回测
        task.current_stage = "运行回测引擎"
        task.progress = 40
        engine = BacktestEngine(config)
        metrics = engine.run(symbols, price_data)
        _backtest_store[task_id] = BacktestResult(**{k: metrics.get(k, 0) for k in BacktestResult.__dataclass_fields__})

        # 阶段3: 生成报告
        task.current_stage = "生成分析报告"
        task.progress = 70
        await asyncio.sleep(0.5)

        # 阶段4: AI分析
        task.current_stage = "AI智能分析"
        task.progress = 85
        ai_result = await AIAnalysisService.analyze_stock(symbols[0] if symbols else "", task_id)

        task.progress = 100
        task.status = TaskStatus.COMPLETED
        task.current_stage = "完成"
        task.result = {
            "backtest": metrics,
            "ai_analysis": ai_result,
            "strategy": StrategyService.get_strategy(strategy_id),
        }
        task.completed_at = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    except Exception as e:
        task.status = TaskStatus.FAILED
        task.error = str(e)
        task.current_stage = "失败"


# ============================================================
# FastAPI 应用
# ============================================================
app = FastAPI(
    title="Web3 Quant Platform API",
    description="量化交易平台 - 整合TradingAgents多Agent分析、回测引擎、行情数据、持仓管理",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ---- 请求模型 ----
class BacktestRequest(BaseModel):
    strategy_id: str = "bse_smallcap"
    symbols: List[str] = ["sh600519", "sz000858"]
    initial_cash: float = 1000000
    stop_loss: float = -0.10
    max_position: float = 0.05
    days: int = 120
    include_ai: bool = False

class AnalyzeRequest(BaseModel):
    symbol: str
    include_ai: bool = True

class HoldingRequest(BaseModel):
    symbol: str
    name: str
    shares: int
    cost_price: float


# ---- 健康检查 ----
@app.get("/health")
def health():
    return {"ok": True, "service": "quant-py", "version": "1.0.0", "time": datetime.now().isoformat()}


# ---- 行情数据 ----
@app.get("/api/quant/market/quotes")
def get_quotes(limit: int = 0):
    """获取全市场行情概览（akshare 实时快照），limit<=0 返回全部"""
    return {"code": 0, "data": AStockDataService.get_stock_list(limit)}


@app.get("/api/quant/market/quote/{symbol}")
def get_quote(symbol: str):
    """单只股票实时行情"""
    name = AStockDataService.STOCK_POOL.get(symbol, symbol)
    quote = AStockDataService.get_realtime_quote(symbol, name)
    return {"code": 0, "data": asdict(quote)}


@app.get("/api/quant/market/kline/{symbol}")
def get_kline(symbol: str, period: str = "1d", days: int = 60):
    """K线数据"""
    data = AStockDataService.get_kline(symbol, period, days)
    return {"code": 0, "data": data}


@app.get("/api/quant/market/sparklines")
def get_sparklines(codes: str = ""):
    """批量股票近12日收盘价走势 (codes=600519,000858,...)"""
    code_list = [c.strip() for c in codes.split(",") if c.strip()]
    data = AStockDataService.get_sparklines_batch(code_list) if code_list else {}
    return {"code": 0, "data": data}


@app.get("/api/quant/market/search")
def search_stocks(q: str = ""):
    """搜索股票（akshare 全市场快照）"""
    return {"code": 0, "data": AStockDataService.search_stocks(q)}


@app.get("/api/quant/market/info/{symbol}")
def get_stock_info(symbol: str):
    """股票详细信息 + 技术指标"""
    info = AStockDataService.get_stock_info(symbol)
    return {"code": 0, "data": info}


# ---- 板块数据 ----
@app.get("/api/quant/market/sectors")
def get_sectors():
    """板块热度（同花顺 source, 兜底 mock）"""
    ths = AStockDataService.get_sector_heat_ths()
    if ths:
        return {"code": 0, "data": ths, "source": "同花顺"}
    return {"code": 0, "data": NewsRadarService.get_sector_heat()}


@app.get("/api/quant/market/news")
def get_news(limit: int = 20):
    """财经新闻"""
    return {"code": 0, "data": NewsRadarService.get_latest_news(limit)}


# ---- 策略列表 ----
@app.get("/api/quant/strategies")
def list_strategies():
    """策略列表"""
    return {"code": 0, "data": StrategyService.list_strategies()}


@app.get("/api/quant/strategies/{strategy_id}")
def get_strategy(strategy_id: str):
    """策略详情"""
    s = StrategyService.get_strategy(strategy_id)
    if not s:
        raise HTTPException(404, "策略不存在")
    return {"code": 0, "data": s}


# ---- 回测 ----
@app.post("/api/quant/backtest/run")
async def run_backtest(req: BacktestRequest):
    """提交回测任务"""
    task_id = f"bt_{uuid.uuid4().hex[:12]}"
    symbols = req.symbols if req.symbols else ["sh600519"]
    config = {
        "initial_cash": req.initial_cash,
        "stop_loss": req.stop_loss,
        "max_position": req.max_position,
        "commission": 0.0003,
        "slippage": 0.001,
        "days": req.days,
    }
    asyncio.create_task(run_backtest_task(task_id, req.strategy_id, symbols, config))
    return {"code": 0, "data": {"task_id": task_id, "status": "pending", "message": "回测任务已提交"}}


@app.get("/api/quant/backtest/status/{task_id}")
def get_backtest_status(task_id: str):
    """查询回测任务状态"""
    task = _task_store.get(task_id)
    if not task:
        return {"code": 0, "data": {"task_id": task_id, "status": "not_found"}}
    return {
        "code": 0, "data": {
            "task_id": task.task_id, "symbol": task.symbol,
            "status": task.status.value, "progress": task.progress,
            "current_stage": task.current_stage,
            "result": task.result, "error": task.error,
            "created_at": task.created_at, "completed_at": task.completed_at,
        }
    }


@app.get("/api/quant/backtest/result/{task_id}")
def get_backtest_result(task_id: str):
    """获取回测结果"""
    result = _backtest_store.get(task_id)
    if not result:
        return {"code": 404, "message": "回测结果不存在或任务未完成"}
    return {"code": 0, "data": asdict(result)}


@app.post("/api/quant/backtest/quick")
async def quick_backtest(req: BacktestRequest):
    """快速回测（同步，等待完成）"""
    symbols = req.symbols if req.symbols else ["sh600519"]
    config = {
        "initial_cash": req.initial_cash,
        "stop_loss": req.stop_loss,
        "max_position": req.max_position,
        "commission": 0.0003,
        "slippage": 0.001,
        "days": req.days,
    }
    price_data = {}
    for sym in symbols:
        price_data[sym] = AStockDataService.get_kline(sym, days=config["days"])

    engine = BacktestEngine(config)
    metrics = engine.run(symbols, price_data)

    # 同时做AI分析
    ai_result = {}
    if req.include_ai:
        tid = f"ai_{uuid.uuid4().hex[:8]}"
        ai_result = await AIAnalysisService.analyze_stock(symbols[0], tid)

    return {"code": 0, "data": {"backtest": metrics, "ai_analysis": ai_result}}


# ---- AI分析 ----
@app.post("/api/quant/ai/analyze")
async def ai_analyze(req: AnalyzeRequest):
    """AI多Agent分析"""
    task_id = f"ai_{uuid.uuid4().hex[:12]}"
    result = await AIAnalysisService.analyze_stock(req.symbol, task_id)
    return {"code": 0, "data": result}


@app.get("/api/quant/ai/signal/{symbol}")
def get_signal(symbol: str):
    """快速获取交易信号"""
    name = AStockDataService.STOCK_POOL.get(symbol, symbol)
    quote = AStockDataService.get_realtime_quote(symbol, name)
    rng = np.random.RandomState(int(hashlib.md5(symbol.encode()).hexdigest()[:8], 16))
    signal = rng.choice(["BUY", "SELL", "HOLD"], p=[0.35, 0.15, 0.5])
    confidence = round(rng.uniform(0.55, 0.92), 2)
    reasons = {
        "BUY": "技术面+资金面共振，多个分析师看多",
        "SELL": "风险信号累积，建议减仓规避",
        "HOLD": "多空分歧，等待更明确信号",
    }
    return {
        "code": 0, "data": {
            "symbol": symbol, "name": name, "price": quote.price,
            "signal": signal, "confidence": confidence,
            "reason": reasons[signal],
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        }
    }


# ---- 持仓管理 ----
@app.get("/api/quant/portfolio")
def get_portfolio():
    """获取持仓及盈亏"""
    perf = PortfolioService.get_performance()
    return {"code": 0, "data": perf}


@app.post("/api/quant/portfolio/holding")
def add_holding(req: HoldingRequest):
    """添加持仓"""
    h = PortfolioService.add_holding(asdict(req))
    return {"code": 0, "data": h}


@app.delete("/api/quant/portfolio/holding/{holding_id}")
def delete_holding(holding_id: str):
    """删除持仓"""
    PortfolioService.remove_holding(holding_id)
    return {"code": 0, "message": "已删除"}


@app.post("/api/quant/portfolio/refresh")
def refresh_portfolio():
    """刷新持仓盈亏"""
    perf = PortfolioService.get_performance()
    return {"code": 0, "data": perf}


# ---- 数据看板 ----
@app.get("/api/quant/dashboard")
def get_dashboard():
    """量化数据看板"""
    quotes = AStockDataService.get_stock_list()
    news = NewsRadarService.get_latest_news(5)
    sectors = NewsRadarService.get_sector_heat()
    strategies = StrategyService.list_strategies()

    up = sum(1 for q in quotes if q["change_percent"] > 0)
    down = sum(1 for q in quotes if q["change_percent"] < 0)

    return {
        "code": 0, "data": {
            "market": {"total": len(quotes), "up": up, "down": down, "updated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")},
            "hot_stocks": sorted(quotes, key=lambda x: abs(x["change_percent"]), reverse=True)[:8],
            "sectors": sectors,
            "latest_news": news,
            "strategies": strategies,
        }
    }


# ---- QMT接口 ----
@app.get("/api/quant/qmt/status")
def qmt_status():
    """QMT连接状态（模拟）"""
    return {"code": 0, "data": {"connected": False, "account": "", "available": True, "message": "QMT未连接，请先启动QMT客户端"}}


@app.post("/api/quant/qmt/connect")
def qmt_connect(req: Dict = {}):
    """连接QMT（模拟）"""
    return {"code": 0, "data": {"connected": True, "account": "sim_001", "balance": 1000000, "message": "模拟连接成功"}}


# ============================================================
# 启动
# ============================================================
@app.on_event("startup")
def _warm_snapshot_cache():
    """后台预热全市场快照缓存，避免首个请求等待拉取"""
    threading.Thread(target=lambda: AStockDataService._load_all_snapshot(), daemon=True).start()


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=9006)
