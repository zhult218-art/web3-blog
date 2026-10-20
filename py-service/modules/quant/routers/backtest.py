# -*- coding: utf-8 -*-
from __future__ import annotations

import uuid
import logging
import asyncio
from dataclasses import asdict
from datetime import datetime, timedelta

logger = logging.getLogger("quant")

from ..datasources.astock import AStockDataService
from ..services.ai import AIAnalysisService
from ..services.backtest import (BACKTEST_STRATEGY_META, BacktestEngine,
                                 _fetch_backtest_klines, run_backtest_task)
from ..state import _backtest_store, _task_store
from ..models.entities import AnalysisTask
from ..models.enums import TaskStatus
from ..models.schemas import BacktestRequest

from fastapi import APIRouter

router = APIRouter()

# 内存任务表 TTL：超过 30 分钟的已完成任务从表中剔除，避免进程长时间运行时无上限增长
_STORE_TTL = 1800.0


def _prune_stores():
    cutoff = datetime.now() - timedelta(seconds=_STORE_TTL)
    for store in (_task_store, _backtest_store):
        for k in list(store.keys()):
            ts = getattr(store.get(k), "completed_at", None)
            if not ts:
                continue
            try:
                if datetime.strptime(ts, "%Y-%m-%d %H:%M:%S") < cutoff:
                    store.pop(k, None)
            except (TypeError, ValueError):
                continue


def _on_task_done(t: asyncio.Task, task_id: str):
    """兜底：任务协程本身抛错时，把失败写进任务表并记日志，
    否则前端只会一直拿到 not_found（这正是此前回测「跑不出结果」的原因）。"""
    if t.cancelled():
        return
    exc = t.exception()
    if exc:
        logger.exception("backtest task %s crashed: %s", task_id, exc)
        task = _task_store.get(task_id) or AnalysisTask(
            task_id=task_id, symbol="", status=TaskStatus.FAILED, progress=0,
            current_stage="失败", created_at=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        )
        task.status = TaskStatus.FAILED
        task.error = f"{type(exc).__name__}: {exc}"
        task.current_stage = "失败"
        _task_store[task_id] = task


@router.post("/api/quant/backtest/run")
async def run_backtest(req: BacktestRequest):
    """提交回测任务"""
    task_id = f"bt_{uuid.uuid4().hex[:12]}"
    symbols = req.symbols if req.symbols else ["sh600519"]
    config = {
        "strategy_id": req.strategy_id,
        "initial_cash": req.initial_cash,
        "stop_loss": req.stop_loss,
        "max_position": req.max_position,
        "commission": 0.0003,
        "slippage": 0.001,
        "days": req.days,
        "code": req.code or "",
        "name": req.name or "",
        "include_ai": bool(req.include_ai),
    }
    _prune_stores()
    t = asyncio.create_task(run_backtest_task(task_id, req.strategy_id, symbols, config))
    t.add_done_callback(lambda fut: _on_task_done(fut, task_id))
    return {"code": 0, "data": {"task_id": task_id, "status": "pending", "message": "回测任务已提交",
                                "strategies": BACKTEST_STRATEGY_META}}

@router.get("/api/quant/backtest/status/{task_id}")
def get_backtest_status(task_id: str):
    """查询回测任务状态。"""
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

@router.get("/api/quant/backtest/result/{task_id}")
def get_backtest_result(task_id: str):
    """获取回测结果"""
    result = _backtest_store.get(task_id)
    if not result:
        return {"code": 404, "message": "回测结果不存在或任务已完成"}
    return {"code": 0, "data": asdict(result)}

@router.post("/api/quant/backtest/quick")
async def quick_backtest(req: BacktestRequest):
    """快速回测（同步等待完成）。"""
    symbols = req.symbols if req.symbols else ["sh600519"]
    config = {
        "strategy_id": req.strategy_id,
        "initial_cash": req.initial_cash,
        "stop_loss": req.stop_loss,
        "max_position": req.max_position,
        "commission": 0.0003,
        "slippage": 0.001,
        "days": req.days,
        "code": req.code or "",
        "name": req.name or "",
    }
    # 行情拉取与回测引擎都是阻塞调用，放线程里执行，避免堵死事件循环（其它请求也一起卡住）
    price_data = await asyncio.to_thread(_fetch_backtest_klines, symbols, config["days"])

    engine = BacktestEngine(config)
    metrics = await asyncio.to_thread(
        engine.run, req.strategy_id, symbols, price_data, config.get("code", ""))

    # 同时做AI分析
    ai_result = {}
    if req.include_ai:
        tid = f"ai_{uuid.uuid4().hex[:8]}"
        ai_result = await AIAnalysisService.analyze_stock(symbols[0], tid)

    return {"code": 0, "data": {"backtest": metrics, "ai_analysis": ai_result}}
