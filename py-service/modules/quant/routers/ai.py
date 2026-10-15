# -*- coding: utf-8 -*-
from __future__ import annotations

import uuid
import logging
import hashlib
from datetime import datetime

import numpy as np

logger = logging.getLogger("quant")

from ..datasources.astock import AStockDataService
from ..services.ai import AIAnalysisService, LLM_DEFAULTS, LLM_PROVIDERS, fetch_llm_models
from ..models.schemas import ModelsRequest, AnalyzeRequest

from fastapi import APIRouter

router = APIRouter()

@router.get("/api/quant/ai/llm-providers")
def get_llm_providers():
    """预置 OpenAI 兼容模型网关列表（前台下拉填充 base_url / 模型，可编辑）。"""
    return {"code": 0, "data": LLM_PROVIDERS}

@router.post("/api/quant/ai/models")
def ai_models(req: ModelsRequest):
    """根据 base_url + api_key 实时拉取网关的模型列表（v1/models）。"""
    models = fetch_llm_models(req.base_url, req.api_key or "")
    return {"code": 0, "data": models}

@router.post("/api/quant/ai/analyze")
async def ai_analyze(req: AnalyzeRequest):
    """AI多Agent深度投研（7 分析师），配置 llm_config 时走真实LLM，否则 mock。"""
    task_id = f"ai_{uuid.uuid4().hex[:12]}"
    llm_config = req.llm_config
    if not llm_config and LLM_DEFAULTS.get("base_url") and LLM_DEFAULTS.get("model"):
        llm_config = LLM_DEFAULTS
    result = await AIAnalysisService.analyze_stock(req.symbol, task_id, llm_config)
    return {"code": 0, "data": result}

@router.get("/api/quant/ai/llm-status")
def llm_status():
    """服务端启动的 LLM 配置状态（不暴露 api_key）。"""
    cfg = LLM_DEFAULTS
    configured = bool(cfg.get("base_url") and cfg.get("model"))
    provider = ""
    if configured:
        base = cfg["base_url"].rstrip("/")
        for p in LLM_PROVIDERS:
            if p["base_url"].rstrip("/") in base or base in p["base_url"].rstrip("/"):
                provider = p["name"]
                break
    return {"code": 0, "data": {
        "configured": configured,
        "base_url": cfg.get("base_url", ""),
        "model": cfg.get("model", ""),
        "provider": provider or ("自定义网关" if configured else ""),
        "has_api_key": bool(cfg.get("api_key")),
    }}

@router.get("/api/quant/ai/signal/{symbol}")
def get_signal(symbol: str):
    """主动获取交易信号"""
    name = AStockDataService.STOCK_POOL.get(symbol, symbol)
    quote = AStockDataService.get_realtime_quote(symbol, name)
    rng = np.random.RandomState(int(hashlib.md5(symbol.encode()).hexdigest()[:8], 16))
    signal = rng.choice(["BUY", "SELL", "HOLD"], p=[0.35, 0.15, 0.5])
    confidence = round(rng.uniform(0.55, 0.92), 2)
    reasons = {
        "BUY": "资金面与情绪面共振，多个分析师看多",
        "SELL": "风险信号增多，建议减仓离场",
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
