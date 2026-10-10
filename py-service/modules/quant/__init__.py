# -*- coding: utf-8 -*-
"""量化模块：A股行情 / 回测 / AI投研 / 持仓 / 策略 / 监控大屏。

由 quant-py-service/main.py（单体 7358 行）机械迁移为分层模块，
全部接口路径、HTTP 方法、参数与返回结构保持原样。
"""
from __future__ import annotations

import logging
import threading

from . import config  # noqa: F401  初始化数据目录
from . import state   # noqa: F401  跨文件共享内存状态
from .routers import router

MODULE_META = {
    "title": "量化交易",
    "description": "A股行情、策略回测、多Agent AI投研、持仓管理与监控大屏",
    "icon": "📈",
    "version": "1.0.0",
}

logger = logging.getLogger("quant")


def health() -> dict:
    """模块健康检查（替代原单体的 GET /health）。"""
    return {
        "ok": True,
        "service": "quant-py",
        "version": "1.0.0",
        "time": __import__("datetime").datetime.now().isoformat(),
    }


def startup() -> None:
    """原 @app.on_event("startup") 内容：预热缓存 + 后台调度线程。"""
    from .datasources.astock import AStockDataService
    from .datasources.supabase import SupabaseStore
    from .services.bigscreen import BigScreenService
    from .services.data_sync import _schedule_daily_sync
    from .services.fund_history import (
        _FUND_HIST_STATE,
        _fund_history_worker,
        _schedule_fund_history_sync,
    )

    # 1) 预热全市场行情缓存，避免首页接口等待抓取
    threading.Thread(
        target=lambda: AStockDataService._load_all_snapshot(), daemon=True
    ).start()
    # 2) 预热监控大屏快照（净值历史/基准指数/持仓成本）
    threading.Thread(target=lambda: BigScreenService.snapshot(), daemon=True).start()
    # 3) 每日增量同步线程（Supabase 落库）
    threading.Thread(target=_schedule_daily_sync, daemon=True).start()
    # 4) 基本面历史定时刷新线程（交易日 16:30 后全池刷新）
    threading.Thread(target=_schedule_fund_history_sync, daemon=True).start()

    def _initial_fund_backfill():
        # 启动 20s 后：首次启动做一次全池基本面历史回填
        threading.Event().wait(20)
        if not SupabaseStore.ENABLED:
            return
        try:
            probe = SupabaseStore.load_stock_history(
                "fundflow", AStockDataService._to_code("sh600519")
            )
        except Exception:  # noqa: BLE001
            probe = None
        if probe is not None and not probe and not _FUND_HIST_STATE["running"]:
            logger.info("首次启动：开始全池基本面历史回填（财务/股东/两融/资金流/分红/大宗）")
            _fund_history_worker(list(AStockDataService.STOCK_POOL.keys()))

    # 5) 首次启动全量回填线程
    threading.Thread(target=_initial_fund_backfill, daemon=True).start()


__all__ = ["router", "MODULE_META", "health", "startup"]
