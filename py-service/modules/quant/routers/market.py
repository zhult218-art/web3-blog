# -*- coding: utf-8 -*-
from __future__ import annotations

import os
import time
import logging
import threading
from datetime import datetime
from typing import Dict

import requests
from fastapi import Query

logger = logging.getLogger("quant")

from ..datasources.astock import AStockDataService
from ..datasources.supabase import SupabaseStore
from ..services.data_sync import _backfill_sector_history
from ..services.fund_history import FundHistoryService
from ..services.news import NewsRadarService


from fastapi import APIRouter

router = APIRouter()

_EODHD_TOKEN = os.environ.get("EODHD_TOKEN", "")
_eodhd_news_cache: Dict[str, tuple] = {}  # key -> (ts, rows)
_EODHD_LOCK = threading.Lock()

@router.get("/api/quant/market/quotes")
def get_quotes(limit: int = 0, page: int = 0, page_size: int = 30):
    """获取 A 股行情列表。limit>0 取前 limit 支；page>0 时支持分页（返回 total/page/page_size 切片）。"""
    data = AStockDataService.get_stock_list(limit)
    if page and page > 0:
        start = (page - 1) * page_size
        total = len(data)
        return {"code": 0, "data": data[start:start + page_size], "total": total, "page": page, "page_size": page_size}
    return {"code": 0, "data": data}

@router.get("/api/quant/market/quote/{symbol}")
def get_quote(symbol: str):
    """单只股票实时行情"""
    name = AStockDataService.STOCK_POOL.get(symbol, symbol)
    quote = AStockDataService.get_realtime_quote(symbol, name)
    return {"code": 0, "data": asdict(quote)}

@router.get("/api/quant/market/finance/{symbol}")
def py_stock_finance(symbol: str):
    """季报财务摘要（同花顺按报告期 8 期，Supabase 持久化）。"""
    code = AStockDataService._to_code(symbol)
    rows, src = FundHistoryService.read("finance", code)
    return {"code": 0, "data": rows or [], "source": src}

@router.get("/api/quant/market/holders/{symbol}")
def py_stock_holders(symbol: str):
    """股东户数历史（东财全量历史，Supabase 持久化）。"""
    code = AStockDataService._to_code(symbol)
    rows, src = FundHistoryService.read("holders", code)
    return {"code": 0, "data": rows or [], "source": src}

@router.get("/api/quant/market/margin/{symbol}")
def py_stock_margin(symbol: str):
    """融资融券明细（东财日级，Supabase 持久化）。"""
    code = AStockDataService._to_code(symbol)
    rows, src = FundHistoryService.read("margin", code)
    # 库中为升序，前端表格期望最新在前
    return {"code": 0, "data": (rows or [])[::-1], "source": src}

@router.get("/api/quant/market/fund-flow/{symbol}")
def py_stock_fund_flow(symbol: str):
    """个股资金流向（东财 120 日主力净流入，Supabase 持久化）。"""
    code = AStockDataService._to_code(symbol)
    rows, src = FundHistoryService.read("fundflow", code)
    return {"code": 0, "data": rows or [], "source": src}

@router.get("/api/quant/market/dividend/{symbol}")
def py_stock_dividend(symbol: str):
    """分红送配历史（Supabase 持久化）。"""
    code = AStockDataService._to_code(symbol)
    rows, src = FundHistoryService.read("dividend", code)
    return {"code": 0, "data": rows or [], "source": src}

@router.get("/api/quant/market/block-trade/{symbol}")
def py_stock_block_trade(symbol: str):
    """大宗交易记录（Supabase 持久化）。"""
    code = AStockDataService._to_code(symbol)
    rows, src = FundHistoryService.read("blocktrade", code)
    return {"code": 0, "data": rows or [], "source": src}

@router.get("/api/quant/market/dragon-tiger/{symbol}")
def py_stock_dragon_tiger(symbol: str, days: int = 30):
    """龙虎榜：代理 Vibe 获取并落库 Supabase（PK trade_date+code）。"""
    code = AStockDataService._to_code(symbol)
    try:
        url = f"{AStockDataService.VIBE_BASE}/api/dragon-tiger"
        r = requests.get(url, params={"code": code, "days": days}, timeout=15)
        r.raise_for_status()
        data = (r.json() or {}).get("data") or {}
        records = data.get("records") or []
        if records:
            latest_date = records[0].get("date") or ""
            save_rows = [{
                "code": code,
                "name": AStockDataService.STOCK_POOL.get(AStockDataService._to_symbol(code), ""),
                "close": None,
                "change_percent": None,
                "buy_amount": None,
                "sell_amount": None,
                "net_amount": float(rec.get("net_buy") or 0),
                "seats": data.get("seats"),
                "reason": rec.get("reason") or "",
            } for rec in records[:1]]  # 仅存最近一条（含席位明细）
            if latest_date:
                threading.Thread(
                    target=lambda: SupabaseStore.save_dragon_tiger(latest_date, save_rows),
                    daemon=True).start()
        return {"code": 0, "data": data, "source": "vibe"}
    except Exception as e:
        logger.warning("dragon-tiger %s failed: %s", code, e)
        return {"code": 0, "data": {"records": [], "seats": {"buy": [], "sell": []},
                                      "institution": {"buy_amt": 0, "sell_amt": 0, "net_amt": 0}},
                "source": "empty"}

@router.get("/api/quant/market/kline/{symbol}")
def get_kline(symbol: str, period: str = "1d", days: int = 60):
    """K线数据：负缓存防穿透——全部真实源失败的代码短缓存，不再重复打上游"""
    nkey = f"kline:{str(symbol).lower()}:{(period or '1d').lower()}"
    if AStockDataService._neg_get(nkey):
        return {"code": 0, "data": [], "negative": True}
    data = AStockDataService.get_kline(symbol, period, days)
    src0 = data[0].get("source") if data and isinstance(data[0], dict) else ""
    if not data or src0 in ("mock", "分时兜底"):
        # 所有真实数据源都失败（如不存在的代码）：写负缓存并返回空，不向前台输出伪造行情
        AStockDataService._neg_set(nkey)
        return {"code": 0, "data": []}
    return {"code": 0, "data": data}

@router.get("/api/quant/market/sparklines")
def get_sparklines(codes: str = ""):
    """批量股票近12日收盘价走势 (codes=600519,000858,...)"""
    code_list = [c.strip() for c in codes.split(",") if c.strip()]
    data = AStockDataService.get_sparklines_batch(code_list) if code_list else {}
    return {"code": 0, "data": data}

@router.get("/api/quant/market/alerts")
def get_sync_alerts():
    """获取未解决的行情同步告警（供前台 NoticePanel 轮询展示）。"""
    return {"code": 0, "data": SupabaseStore.get_open_alerts(limit=10)}

@router.get("/api/quant/market/search")
def search_stocks(q: str = ""):
    """搜索股票（akshare 全市场快照）"""
    return {"code": 0, "data": AStockDataService.search_stocks(q)}

@router.get("/api/quant/market/info/{symbol}")
def get_stock_info(symbol: str):
    """股票详细信息 + 实时行情。"""
    info = AStockDataService.get_stock_info(symbol)
    return {"code": 0, "data": info}

@router.get("/api/quant/market/sectors")
def get_sectors():
    """板块热度（同花顺 source, 兜底 mock）"""
    ths = AStockDataService.get_sector_heat_ths()
    if ths:
        return {"code": 0, "data": ths, "source": "同花顺"}
    return {"code": 0, "data": NewsRadarService.get_sector_heat()}

@router.get("/api/quant/market/limit-pools")
def get_limit_pools():
    """涨停/跌停/炸板 + 交易日。"""
    return {"code": 0, "data": AStockDataService.get_limit_pools()}

@router.get("/api/quant/market/board-progress")
def get_board_progress():
    """连板晋级：1板 / 2板 / 3板。"""
    return {"code": 0, "data": AStockDataService.get_board_progress()}

@router.get("/api/quant/market/limit-pool-boards")
def get_limit_pool_boards():
    """涨停池按连板数分层（首板 / 1进2 / ... / 6进7），duanxianxia 风格。"""
    return {"code": 0, "data": AStockDataService.get_limit_pool_by_board()}

@router.get("/api/quant/market/sector-trends")
def get_sector_trends(days: int = 60, top: int = 10):
    """板块涨跌曲线（行业板块近 N 日收盘涨跌幅）。外层 15s 硬超时；失败快速返回空结构。"""
    try:
        data = AStockDataService.get_sector_trends(days=days, top=top)
        if not isinstance(data, dict):
            data = {"dates": [], "series": [], "source": "empty"}
        return {"code": 0, "data": data}
    except Exception as e:  # noqa: BLE001
        logger.warning("sector_trends endpoint failed: %s", e)
        return {"code": 0, "data": {"dates": [], "series": [], "source": "error",
                                    "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")}}

@router.get("/api/quant/market/fund-flow")
def get_market_fund_flow():
    """主力净流入流出（板块榜/大盘榜）。"""
    return {"code": 0, "data": AStockDataService.get_market_fund_flow()}

@router.get("/api/quant/market/news")
def get_news(limit: int = 20):
    """财经新闻。"""
    return {"code": 0, "data": NewsRadarService.get_latest_news(limit)}

@router.get("/api/quant/market/news/global")
def get_global_news(symbol: str = "", limit: int = 30):
    """eodhd 全球新闻：symbol 空则返回全市场要闻；传美股代码如 AAPL（自动补 .US）或 AAPL.US。"""
    sym = (symbol or "").strip().upper()
    if sym and "." not in sym:
        sym = f"{sym}.US"
    key = f"{sym}:{min(max(limit, 1), 100)}"
    with _EODHD_LOCK:
        hit = _eodhd_news_cache.get(key)
        if hit and time.time() - hit[0] < 600:
            return {"code": 0, "data": hit[1], "source": "eodhd-cache"}
    try:
        params = {"api_token": _EODHD_TOKEN, "fmt": "json", "limit": min(max(limit, 1), 100)}
        if sym:  # 空符号不能传 s（eodhd 422），省略则返回全市场要闻
            params["s"] = sym
        r = requests.get("https://eodhd.com/api/news", params=params, timeout=12)
        r.raise_for_status()
        raw = r.json() or []
        rows = [{
            "date": (item.get("date") or "")[:19].replace("T", " "),
            "title": item.get("title") or "",
            "summary": (item.get("content") or "")[:300],
            "url": item.get("link") or "",
            "symbols": item.get("symbols") or [],
            "sentiment": (item.get("sentiment") or {}).get("polarity") if isinstance(item.get("sentiment"), dict) else None,
        } for item in raw if isinstance(item, dict)]
        with _EODHD_LOCK:
            _eodhd_news_cache[key] = (time.time(), rows)
        return {"code": 0, "data": rows, "source": "eodhd"}
    except Exception as e:
        logger.warning("eodhd news %s failed: %s", sym or "ALL", e)
        return {"code": 0, "data": [], "source": "eodhd-error"}

@router.post("/api/quant/market/sync")
def manual_incremental_sync(force: bool = Query(False), backfill: bool = Query(False),
                            backfill_days: int = Query(120)):
    """手动触发同步：当日全市场快照 + 板块走势落库；backfill=true 时回填 backfill_days 日板块历史"""
    if not SupabaseStore.ENABLED:
        return {"code": 0, "data": {"enabled": False, "message": "Supabase 未配置 SUPABASE_URL / SUPABASE_SERVICE_KEY"}}
    try:
        today = datetime.now().strftime("%Y-%m-%d")
        df, src = AStockDataService._fetch_snapshot_once()
        snap_ok = False
        if df is not None:
            snap_ok = SupabaseStore.save_snapshot(today, df, src)
        SupabaseStore.update_count(
            today, "snapshot", int(len(df)) if df is not None else 0,
            "ok" if snap_ok else "fail", f"src={src}")
        sect = AStockDataService.get_sector_trends(days=30, top=0, force=True)
        sect_saved = 0
        if sect and sect.get("series"):
            sect_saved = len(sect["series"])
            AStockDataService._save_sector_trends(sect)
            SupabaseStore.update_count(today, "sector", sect_saved, "ok", f"src={sect.get('source') or ''}")
        resp = {
            "enabled": True, "date": today,
            "snapshot_saved": bool(snap_ok),
            "snapshot_rows": int(len(df)) if df is not None else 0,
            "sector_series": sect_saved,
        }
        if backfill:
            n = _backfill_sector_history(max(backfill_days, 30))
            resp["backfill_days"] = max(backfill_days, 30)
            resp["backfill_series"] = n
        if snap_ok or sect_saved:
            SupabaseStore.resolve_alerts(today)
        return {"code": 0, "data": resp}
    except Exception as e:
        logger.warning("manual sync failed: %s", e)
        SupabaseStore.save_alert(datetime.now().strftime("%Y-%m-%d"), f"手动同步失败: {e}", "error")
        return {"code": 0, "data": {"enabled": True, "error": str(e)}}
