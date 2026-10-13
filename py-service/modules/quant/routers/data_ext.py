# -*- coding: utf-8 -*-
"""A 股全栈数据工具包（astock_ext）HTTP 端点聚合。

将 ``datasources/astock_ext.py`` 中的 40+ 个数据获取函数暴露为
``/api/quant/data-ext/*`` 路由，按十层数据源 + 备用源 + 估值公式组织。
所有端点统一返回 ``{"code": 0, "data": ...}``，异常返回 ``{"code": 1, "message": ...}``。
"""
from __future__ import annotations

import math
import logging
import datetime as _dt
from decimal import Decimal
from typing import Any

import pandas as pd
from fastapi import APIRouter, Query, HTTPException

from ..datasources import astock_ext as ax

logger = logging.getLogger("quant.data_ext")

router = APIRouter()


# ============================================================================
# 序列化 helper：DataFrame → records / inf → None / numpy/datetime → JSON 安全
# ============================================================================

def _serialize(obj: Any) -> Any:
    """递归将 DataFrame / inf / numpy / datetime / Decimal 转为 JSON 安全结构。"""
    # None / str / int / bool — 原样返回（注意 bool 必须在 int 之前判断）
    if obj is None or isinstance(obj, (str, bool)):
        return obj
    if isinstance(obj, pd.DataFrame):
        return _serialize(obj.to_dict(orient="records"))
    if isinstance(obj, pd.Series):
        return _serialize(obj.to_dict())
    # datetime 族（含 pd.Timestamp、np.datetime64.item() 的产物）
    if isinstance(obj, (pd.Timestamp, _dt.datetime, _dt.date)):
        return obj.isoformat()
    # float — inf/nan → None
    if isinstance(obj, float):
        return None if (math.isnan(obj) or math.isinf(obj)) else obj
    # Decimal → float
    if isinstance(obj, Decimal):
        return float(obj)
    # bytes → str
    if isinstance(obj, bytes):
        return obj.decode("utf-8", errors="replace")
    # numpy 标量（int64/float64/bool_ 等）— 先于 int 判断
    if hasattr(obj, "item") and not isinstance(obj, (int,)):
        try:
            return _serialize(obj.item())
        except Exception:  # noqa: BLE001
            pass
    if isinstance(obj, dict):
        return {str(k): _serialize(v) for k, v in obj.items()}
    if isinstance(obj, (list, tuple)):
        return [_serialize(v) for v in obj]
    # int 留到最后（numpy int 已被上面 .item() 转换）
    if isinstance(obj, int):
        return obj
    # 最终兜底：不可序列化的对象转字符串
    try:
        return str(obj)
    except Exception:  # noqa: BLE001
        return None


def _ok(data: Any, **extra) -> dict:
    out = {"code": 0, "data": _serialize(data)}
    if extra:
        out.update(extra)
    return out


def _err(msg: str, exc: Exception | None = None) -> dict:
    detail = f"{msg}: {exc}" if exc else msg
    logger.warning("data_ext endpoint error: %s", detail)
    return {"code": 1, "message": detail}


def _split_codes(raw: str) -> list[str]:
    """逗号分隔的代码字符串 → list。"""
    return [c.strip() for c in raw.split(",") if c.strip()]


# ============================================================================
# Layer 1: 行情层（quotes）—— mootdx / 腾讯 / 百度
# ============================================================================

@router.get("/api/quant/data-ext/tdx/klines")
def tdx_klines_ep(symbol: str = Query(..., description="6 位代码"), frequency: int = 9, offset: int = 100):
    """mootdx K 线（0=5分 4=日线 9=日线默认 11=年线）。"""
    try:
        return _ok(ax.tdx_klines(symbol, frequency=frequency, offset=offset))
    except RuntimeError as e:
        return _err("mootdx 未安装或不可用", e)
    except Exception as e:  # noqa: BLE001
        return _err("获取 K 线失败", e)


@router.get("/api/quant/data-ext/tdx/quotes")
def tdx_quotes_ep(symbols: str = Query(..., description="逗号分隔代码")):
    """mootdx 实时五档报价。"""
    try:
        return _ok(ax.tdx_quotes(_split_codes(symbols)))
    except RuntimeError as e:
        return _err("mootdx 未安装或不可用", e)
    except Exception as e:  # noqa: BLE001
        return _err("获取报价失败", e)


@router.get("/api/quant/data-ext/tdx/transactions")
def tdx_transactions_ep(symbol: str = Query(...), date: str = ""):
    """mootdx 逐笔成交（date 格式 YYYYMMDD）。"""
    try:
        return _ok(ax.tdx_transactions(symbol, date=date))
    except RuntimeError as e:
        return _err("mootdx 未安装或不可用", e)
    except Exception as e:  # noqa: BLE001
        return _err("获取逐笔成交失败", e)


@router.get("/api/quant/data-ext/tencent/quote")
def tencent_quote_ep(codes: str = Query(..., description="逗号分隔代码")):
    """腾讯实时行情（PE/PB/市值/换手率/涨跌停）。"""
    try:
        return _ok(ax.tencent_quote(_split_codes(codes)))
    except Exception as e:  # noqa: BLE001
        return _err("获取腾讯行情失败", e)


@router.get("/api/quant/data-ext/baidu/kline")
def baidu_kline_ep(code: str = Query(...), start_time: str = ""):
    """百度日 K + 均线。"""
    try:
        return _ok(ax.baidu_kline_with_ma(code, start_time=start_time))
    except Exception as e:  # noqa: BLE001
        return _err("获取百度 K 线失败", e)


# ============================================================================
# Layer 2: 研报层（reports）—— 东财 / 同花顺 / iwencai
# ============================================================================

@router.get("/api/quant/data-ext/reports")
def eastmoney_reports_ep(code: str = Query(...), max_pages: int = 5):
    """东财个股研报列表。"""
    try:
        return _ok(ax.eastmoney_reports(code, max_pages=max_pages))
    except Exception as e:  # noqa: BLE001
        return _err("获取研报失败", e)


@router.get("/api/quant/data-ext/reports/industry")
def eastmoney_industry_reports_ep(industry_code: str = "*", max_pages: int = 5, max_items: int = 100):
    """东财行业研报。"""
    try:
        return _ok(ax.eastmoney_industry_reports(industry_code=industry_code, max_pages=max_pages, max_items=max_items))
    except Exception as e:  # noqa: BLE001
        return _err("获取行业研报失败", e)


@router.get("/api/quant/data-ext/ths/eps-forecast")
def ths_eps_forecast_ep(code: str = Query(...)):
    """同花顺一致预期 EPS（未来两年）。"""
    try:
        return _ok(ax.ths_eps_forecast(code))
    except Exception as e:  # noqa: BLE001
        return _err("获取 EPS 预期失败", e)


@router.get("/api/quant/data-ext/iwencai/search")
def iwencai_search_ep(query: str = Query(...), channel: str = "report", size: int = 50):
    """iwencai 搜索（需 IWENCAI_API_KEY）。"""
    try:
        return _ok(ax.iwencai_search(query, channel=channel, size=size))
    except Exception as e:  # noqa: BLE001
        return _err("iwencai 搜索失败", e)


@router.get("/api/quant/data-ext/iwencai/query")
def iwencai_query_ep(query: str = Query(...), page: int = 1, limit: int = 50):
    """iwencai 结构化查询。"""
    try:
        return _ok(ax.iwencai_query(query, page=page, limit=limit))
    except Exception as e:  # noqa: BLE001
        return _err("iwencai 查询失败", e)


# ============================================================================
# Layer 3: 信号层（signals）—— 热点 / 北向 / 板块 / 资金流 / 龙虎榜 / 解禁
# ============================================================================

@router.get("/api/quant/data-ext/ths/hot-reason")
def ths_hot_reason_ep(date: str | None = None):
    """同花顺热点题材与原因。"""
    try:
        return _ok(ax.ths_hot_reason(date=date))
    except Exception as e:  # noqa: BLE001
        return _err("获取热点题材失败", e)


@router.get("/api/quant/data-ext/hsgt/realtime")
def hsgt_realtime_ep():
    """沪港通/深港通北向资金实时净流入。"""
    try:
        return _ok(ax.hsgt_realtime())
    except Exception as e:  # noqa: BLE001
        return _err("获取北向资金失败", e)


@router.get("/api/quant/data-ext/concept-blocks")
def eastmoney_concept_blocks_ep(code: str = Query(...)):
    """东财个股所属概念板块。"""
    try:
        return _ok(ax.eastmoney_concept_blocks(code))
    except Exception as e:  # noqa: BLE001
        return _err("获取概念板块失败", e)


@router.get("/api/quant/data-ext/fund-flow/minute")
def eastmoney_fund_flow_minute_ep(code: str = Query(...)):
    """东财个股分钟资金流。"""
    try:
        return _ok(ax.eastmoney_fund_flow_minute(code))
    except Exception as e:  # noqa: BLE001
        return _err("获取分钟资金流失败", e)


@router.get("/api/quant/data-ext/dragon-tiger/board")
def dragon_tiger_board_ep(code: str = Query(...), trade_date: str = Query(..., description="YYYY-MM-DD"), look_back: int = 30):
    """龙虎榜数据聚合（记录+买卖席位+机构）。"""
    try:
        return _ok(ax.dragon_tiger_board(code, trade_date=trade_date, look_back=look_back))
    except Exception as e:  # noqa: BLE001
        return _err("获取龙虎榜失败", e)


@router.get("/api/quant/data-ext/lockup-expiry")
def lockup_expiry_ep(code: str = Query(...), trade_date: str = Query(..., description="YYYY-MM-DD"), forward_days: int = 90):
    """限售解禁数据。"""
    try:
        return _ok(ax.lockup_expiry(code, trade_date=trade_date, forward_days=forward_days))
    except Exception as e:  # noqa: BLE001
        return _err("获取解禁数据失败", e)


@router.get("/api/quant/data-ext/industry-comparison")
def industry_comparison_ep(top_n: int = 20):
    """行业对比（涨跌/资金/估值）。"""
    try:
        return _ok(ax.industry_comparison(top_n=top_n))
    except Exception as e:  # noqa: BLE001
        return _err("获取行业对比失败", e)


@router.get("/api/quant/data-ext/daily-dragon-tiger")
def daily_dragon_tiger_ep(trade_date: str | None = None, min_net_buy: float | None = None):
    """每日龙虎榜全量（按净买入过滤）。"""
    try:
        return _ok(ax.daily_dragon_tiger(trade_date=trade_date, min_net_buy=min_net_buy))
    except Exception as e:  # noqa: BLE001
        return _err("获取每日龙虎榜失败", e)


# ============================================================================
# Layer 4: 资金面层（capital）—— 融资融券 / 大宗 / 股东户数 / 分红
# ============================================================================

@router.get("/api/quant/data-ext/margin")
def margin_trading_ep(code: str = Query(...), page_size: int = 30):
    """融资融券明细。"""
    try:
        return _ok(ax.margin_trading(code, page_size=page_size))
    except Exception as e:  # noqa: BLE001
        return _err("获取融资融券失败", e)


@router.get("/api/quant/data-ext/block-trade")
def block_trade_ep(code: str = Query(...), page_size: int = 20):
    """大宗交易明细。"""
    try:
        return _ok(ax.block_trade(code, page_size=page_size))
    except Exception as e:  # noqa: BLE001
        return _err("获取大宗交易失败", e)


@router.get("/api/quant/data-ext/holder-num-change")
def holder_num_change_ep(code: str = Query(...), page_size: int = 10):
    """股东户数变化。"""
    try:
        return _ok(ax.holder_num_change(code, page_size=page_size))
    except Exception as e:  # noqa: BLE001
        return _err("获取股东户数失败", e)


@router.get("/api/quant/data-ext/dividend")
def dividend_history_ep(code: str = Query(...), page_size: int = 20):
    """分红历史。"""
    try:
        return _ok(ax.dividend_history(code, page_size=page_size))
    except Exception as e:  # noqa: BLE001
        return _err("获取分红历史失败", e)


# ============================================================================
# Layer 5: 新闻层（news）—— 东财个股 / 财联社 / 东财全球
# ============================================================================

@router.get("/api/quant/data-ext/news/stock")
def eastmoney_stock_news_ep(code: str = Query(...), page_size: int = 20):
    """东财个股新闻。"""
    try:
        return _ok(ax.eastmoney_stock_news(code, page_size=page_size))
    except Exception as e:  # noqa: BLE001
        return _err("获取个股新闻失败", e)


@router.get("/api/quant/data-ext/news/cls")
def cls_telegraph_ep(page_size: int = 50):
    """财联社电报。"""
    try:
        return _ok(ax.cls_telegraph(page_size=page_size))
    except Exception as e:  # noqa: BLE001
        return _err("获取财联社电报失败", e)


@router.get("/api/quant/data-ext/news/global")
def eastmoney_global_news_ep(page_size: int = 50):
    """东财全球资讯。"""
    try:
        return _ok(ax.eastmoney_global_news(page_size=page_size))
    except Exception as e:  # noqa: BLE001
        return _err("获取全球资讯失败", e)


# ============================================================================
# Layer 6: 财务层（financials）—— mootdx / 东财 / 新浪
# ============================================================================

@router.get("/api/quant/data-ext/tdx/finance")
def tdx_finance_ep(symbol: str = Query(...)):
    """mootdx 财务数据。"""
    try:
        return _ok(ax.tdx_finance(symbol))
    except RuntimeError as e:
        return _err("mootdx 未安装或不可用", e)
    except Exception as e:  # noqa: BLE001
        return _err("获取财务数据失败", e)


@router.get("/api/quant/data-ext/stock-info")
def eastmoney_stock_info_ep(code: str = Query(...)):
    """东财个股信息（总股本/流通/行业/上市日等）。"""
    try:
        return _ok(ax.eastmoney_stock_info(code))
    except Exception as e:  # noqa: BLE001
        return _err("获取个股信息失败", e)


@router.get("/api/quant/data-ext/sina/financial-report")
def sina_financial_report_ep(code: str = Query(...), report_type: str = "lrb", num: int = 8):
    """新浪财报三表（lrb 利润 / zcfz 资产负债 / xjll 现金流）。"""
    try:
        return _ok(ax.sina_financial_report(code, report_type=report_type, num=num))
    except Exception as e:  # noqa: BLE001
        return _err("获取财报失败", e)


# ============================================================================
# Layer 7: 公告层（announcements）—— 巨潮 cninfo
# ============================================================================

@router.get("/api/quant/data-ext/announcements")
def cninfo_announcements_ep(code: str = Query(...), page_size: int = 30):
    """巨潮公告列表。"""
    try:
        return _ok(ax.cninfo_announcements(code, page_size=page_size))
    except Exception as e:  # noqa: BLE001
        return _err("获取公告失败", e)


# ============================================================================
# Layer 8: 打板层（limit-up）—— 涨停/炸板/跌停/昨涨停 / 同花顺
# ============================================================================

@router.get("/api/quant/data-ext/zt-pool")
def em_zt_pool_ep(date: str = Query(..., description="YYYY-MM-DD")):
    """东财涨停池。"""
    try:
        return _ok(ax.em_zt_pool(date))
    except Exception as e:  # noqa: BLE001
        return _err("获取涨停池失败", e)


@router.get("/api/quant/data-ext/zb-pool")
def em_zb_pool_ep(date: str = Query(..., description="YYYY-MM-DD")):
    """东财炸板池。"""
    try:
        return _ok(ax.em_zb_pool(date))
    except Exception as e:  # noqa: BLE001
        return _err("获取炸板池失败", e)


@router.get("/api/quant/data-ext/dt-pool")
def em_dt_pool_ep(date: str = Query(..., description="YYYY-MM-DD")):
    """东财跌停池。"""
    try:
        return _ok(ax.em_dt_pool(date))
    except Exception as e:  # noqa: BLE001
        return _err("获取跌停池失败", e)


@router.get("/api/quant/data-ext/yzt-pool")
def em_yzt_pool_ep(date: str = Query(..., description="YYYY-MM-DD")):
    """东财昨涨停池。"""
    try:
        return _ok(ax.em_yzt_pool(date))
    except Exception as e:  # noqa: BLE001
        return _err("获取昨涨停池失败", e)


@router.get("/api/quant/data-ext/ths/limit-up-pool")
def ths_limit_up_pool_ep(date: str = Query(..., description="YYYY-MM-DD")):
    """同花顺涨停揭秘。"""
    try:
        return _ok(ax.ths_limit_up_pool(date))
    except Exception as e:  # noqa: BLE001
        return _err("获取涨停揭秘失败", e)


@router.get("/api/quant/data-ext/limit-up/sentiment")
def limit_up_sentiment_ep(date: str = Query(..., description="YYYY-MM-DD")):
    """打板情绪（涨停/炸板/跌停对比）。"""
    try:
        return _ok(ax.limit_up_sentiment(date))
    except Exception as e:  # noqa: BLE001
        return _err("获取打板情绪失败", e)


# ============================================================================
# Layer 9: 期权层（options）—— 新浪合约 / T型报价 / 希腊字母 + IV
# ============================================================================

@router.get("/api/quant/data-ext/options/codes")
def sina_option_codes_ep(underlying: str = "510050", call: bool = True):
    """新浪期权合约清单。"""
    try:
        return _ok(ax.sina_option_codes(underlying=underlying, call=call))
    except Exception as e:  # noqa: BLE001
        return _err("获取期权合约失败", e)


@router.get("/api/quant/data-ext/options/tquote")
def sina_option_tquote_ep(code: str = Query(...)):
    """新浪期权 T 型报价。"""
    try:
        return _ok(ax.sina_option_tquote(code))
    except Exception as e:  # noqa: BLE001
        return _err("获取期权报价失败", e)


@router.get("/api/quant/data-ext/options/greeks")
def sina_option_greeks_ep(code: str = Query(...)):
    """新浪期权希腊字母 + IV。"""
    try:
        return _ok(ax.sina_option_greeks(code))
    except Exception as e:  # noqa: BLE001
        return _err("获取期权希腊字母失败", e)


# ============================================================================
# Layer 10: 舆情层（sentiment）—— 互动易 / 热榜 / 人气榜 / 概念命中
# ============================================================================

@router.get("/api/quant/data-ext/cninfo/irm")
def cninfo_irm_ep(code: str = Query(...), page_size: int = 30, page_num: int = 1):
    """互动易问答。"""
    try:
        return _ok(ax.cninfo_irm(code, page_size=page_size, page_num=page_num))
    except Exception as e:  # noqa: BLE001
        return _err("获取互动易失败", e)


@router.get("/api/quant/data-ext/ths/hot-list")
def ths_hot_list_ep(period: str = "hour"):
    """同花顺热榜（hour/day/week）。"""
    try:
        return _ok(ax.ths_hot_list(period=period))
    except Exception as e:  # noqa: BLE001
        return _err("获取热榜失败", e)


@router.get("/api/quant/data-ext/em/hot-rank")
def em_hot_rank_ep(top: int = 50):
    """东财人气榜。"""
    try:
        return _ok(ax.em_hot_rank(top=top))
    except Exception as e:  # noqa: BLE001
        return _err("获取人气榜失败", e)


@router.get("/api/quant/data-ext/em/hot-concept")
def em_hot_concept_ep(code: str = Query(...)):
    """东财概念命中。"""
    try:
        return _ok(ax.em_hot_concept(code))
    except Exception as e:  # noqa: BLE001
        return _err("获取概念命中失败", e)


# ============================================================================
# 备用源（backup）—— 龙虎榜 / 资金流 / 公告 三个官方备胎
# ============================================================================

@router.get("/api/quant/data-ext/backup/dragon-tiger")
def dragon_tiger_backup_ep(trade_date: str = Query(..., description="YYYY-MM-DD")):
    """龙虎榜备用源。"""
    try:
        return _ok(ax.dragon_tiger_backup(trade_date))
    except Exception as e:  # noqa: BLE001
        return _err("获取龙虎榜备用源失败", e)


@router.get("/api/quant/data-ext/backup/fund-flow")
def fund_flow_backup_ep(code: str = Query(...), days: int = 60):
    """资金流备用源。"""
    try:
        return _ok(ax.fund_flow_backup(code, days=days))
    except Exception as e:  # noqa: BLE001
        return _err("获取资金流备用源失败", e)


@router.get("/api/quant/data-ext/backup/announcements")
def announcements_backup_ep(code: str = Query(...), page_size: int = 20):
    """公告备用源。"""
    try:
        return _ok(ax.announcements_backup(code, page_size=page_size))
    except Exception as e:  # noqa: BLE001
        return _err("获取公告备用源失败", e)


# ============================================================================
# 估值公式（valuation）—— 前向 PE / PE 消化 / PEG / 单票全景
# ============================================================================

@router.get("/api/quant/data-ext/valuation/forward-pe")
def forward_pe_ep(price: float = Query(...), eps_forecast: float = Query(...)):
    """前向 PE = 股价 / 一致预期 EPS。"""
    try:
        return _ok(ax.forward_pe(price, eps_forecast=eps_forecast))
    except Exception as e:  # noqa: BLE001
        return _err("计算前向 PE 失败", e)


@router.get("/api/quant/data-ext/valuation/pe-digestion")
def pe_digestion_ep(current_pe: float = Query(...), cagr: float = Query(...), target_pe: float = 30):
    """PE 消化到目标值所需年数。"""
    try:
        return _ok(ax.pe_digestion(current_pe, cagr=cagr, target_pe=target_pe))
    except Exception as e:  # noqa: BLE001
        return _err("计算 PE 消化时间失败", e)


@router.get("/api/quant/data-ext/valuation/peg")
def calc_peg_ep(pe: float = Query(...), cagr: float = Query(...)):
    """PEG = 前向 PE / (CAGR × 100)。"""
    try:
        return _ok(ax.calc_peg(pe, cagr=cagr))
    except Exception as e:  # noqa: BLE001
        return _err("计算 PEG 失败", e)


@router.get("/api/quant/data-ext/valuation/full")
def full_valuation_ep(code: str = Query(...)):
    """单票完整估值分析（行情+EPS+PE+PEG+消化时间）。"""
    try:
        return _ok(ax.full_valuation(code))
    except Exception as e:  # noqa: BLE001
        return _err("获取完整估值失败", e)


# ============================================================================
# 元数据：列出全部可用端点
# ============================================================================

@router.get("/api/quant/data-ext/capabilities")
def data_ext_capabilities():
    """列出 data-ext 全部可用端点（供前端动态渲染）。"""
    layers = [
        {"layer": "行情", "prefix": "/api/quant/data-ext/tdx", "endpoints": [
            {"path": "/tdx/klines", "params": ["symbol", "frequency=9", "offset=100"], "desc": "mootdx K 线"},
            {"path": "/tdx/quotes", "params": ["symbols(逗号)"], "desc": "mootdx 实时报价"},
            {"path": "/tdx/transactions", "params": ["symbol", "date"], "desc": "逐笔成交"},
            {"path": "/tencent/quote", "params": ["codes(逗号)"], "desc": "腾讯实时行情"},
            {"path": "/baidu/kline", "params": ["code", "start_time"], "desc": "百度日 K+均线"},
        ]},
        {"layer": "研报", "prefix": "/api/quant/data-ext", "endpoints": [
            {"path": "/reports", "params": ["code", "max_pages=5"], "desc": "东财个股研报"},
            {"path": "/reports/industry", "params": ["industry_code", "max_pages", "max_items"], "desc": "东财行业研报"},
            {"path": "/ths/eps-forecast", "params": ["code"], "desc": "同花顺 EPS 预期"},
            {"path": "/iwencai/search", "params": ["query", "channel", "size"], "desc": "iwencai 搜索"},
            {"path": "/iwencai/query", "params": ["query", "page", "limit"], "desc": "iwencai 查询"},
        ]},
        {"layer": "信号", "prefix": "/api/quant/data-ext", "endpoints": [
            {"path": "/ths/hot-reason", "params": ["date"], "desc": "热点题材"},
            {"path": "/hsgt/realtime", "params": [], "desc": "北向资金"},
            {"path": "/concept-blocks", "params": ["code"], "desc": "概念板块"},
            {"path": "/fund-flow/minute", "params": ["code"], "desc": "分钟资金流"},
            {"path": "/dragon-tiger/board", "params": ["code", "trade_date", "look_back"], "desc": "龙虎榜聚合"},
            {"path": "/lockup-expiry", "params": ["code", "trade_date", "forward_days"], "desc": "解禁数据"},
            {"path": "/industry-comparison", "params": ["top_n"], "desc": "行业对比"},
            {"path": "/daily-dragon-tiger", "params": ["trade_date", "min_net_buy"], "desc": "每日龙虎榜"},
        ]},
        {"layer": "资金面", "prefix": "/api/quant/data-ext", "endpoints": [
            {"path": "/margin", "params": ["code", "page_size"], "desc": "融资融券"},
            {"path": "/block-trade", "params": ["code", "page_size"], "desc": "大宗交易"},
            {"path": "/holder-num-change", "params": ["code", "page_size"], "desc": "股东户数"},
            {"path": "/dividend", "params": ["code", "page_size"], "desc": "分红历史"},
        ]},
        {"layer": "新闻", "prefix": "/api/quant/data-ext/news", "endpoints": [
            {"path": "/news/stock", "params": ["code", "page_size"], "desc": "东财个股新闻"},
            {"path": "/news/cls", "params": ["page_size"], "desc": "财联社电报"},
            {"path": "/news/global", "params": ["page_size"], "desc": "东财全球资讯"},
        ]},
        {"layer": "财务", "prefix": "/api/quant/data-ext", "endpoints": [
            {"path": "/tdx/finance", "params": ["symbol"], "desc": "mootdx 财务"},
            {"path": "/stock-info", "params": ["code"], "desc": "东财个股信息"},
            {"path": "/sina/financial-report", "params": ["code", "report_type", "num"], "desc": "新浪财报三表"},
        ]},
        {"layer": "公告", "prefix": "/api/quant/data-ext", "endpoints": [
            {"path": "/announcements", "params": ["code", "page_size"], "desc": "巨潮公告"},
        ]},
        {"layer": "打板", "prefix": "/api/quant/data-ext", "endpoints": [
            {"path": "/zt-pool", "params": ["date"], "desc": "涨停池"},
            {"path": "/zb-pool", "params": ["date"], "desc": "炸板池"},
            {"path": "/dt-pool", "params": ["date"], "desc": "跌停池"},
            {"path": "/yzt-pool", "params": ["date"], "desc": "昨涨停池"},
            {"path": "/ths/limit-up-pool", "params": ["date"], "desc": "同花顺涨停揭秘"},
            {"path": "/limit-up/sentiment", "params": ["date"], "desc": "打板情绪"},
        ]},
        {"layer": "期权", "prefix": "/api/quant/data-ext/options", "endpoints": [
            {"path": "/options/codes", "params": ["underlying", "call"], "desc": "合约清单"},
            {"path": "/options/tquote", "params": ["code"], "desc": "T 型报价"},
            {"path": "/options/greeks", "params": ["code"], "desc": "希腊字母+IV"},
        ]},
        {"layer": "舆情", "prefix": "/api/quant/data-ext", "endpoints": [
            {"path": "/cninfo/irm", "params": ["code", "page_size", "page_num"], "desc": "互动易"},
            {"path": "/ths/hot-list", "params": ["period"], "desc": "同花顺热榜"},
            {"path": "/em/hot-rank", "params": ["top"], "desc": "东财人气榜"},
            {"path": "/em/hot-concept", "params": ["code"], "desc": "概念命中"},
        ]},
        {"layer": "备用源", "prefix": "/api/quant/data-ext/backup", "endpoints": [
            {"path": "/backup/dragon-tiger", "params": ["trade_date"], "desc": "龙虎榜备用"},
            {"path": "/backup/fund-flow", "params": ["code", "days"], "desc": "资金流备用"},
            {"path": "/backup/announcements", "params": ["code", "page_size"], "desc": "公告备用"},
        ]},
        {"layer": "估值", "prefix": "/api/quant/data-ext/valuation", "endpoints": [
            {"path": "/valuation/forward-pe", "params": ["price", "eps_forecast"], "desc": "前向 PE"},
            {"path": "/valuation/pe-digestion", "params": ["current_pe", "cagr", "target_pe"], "desc": "PE 消化"},
            {"path": "/valuation/peg", "params": ["pe", "cagr"], "desc": "PEG"},
            {"path": "/valuation/full", "params": ["code"], "desc": "单票完整估值"},
        ]},
    ]
    return {"code": 0, "data": {"layers": layers, "total_endpoints": sum(len(l["endpoints"]) for l in layers)}}
