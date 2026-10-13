# -*- coding: utf-8 -*-
"""
cockpit 路由聚合：将 tick-stock-panel 的 27 个 API 子路由统一挂载到 /cockpit 前缀下。

最终路径：/cockpit/health, /cockpit/api/backtest/*, /cockpit/api/kline/*, ...
"""
from __future__ import annotations

import sys
from pathlib import Path

# 将 cockpit 模块目录加入 sys.path，使 app.* import 链正常工作
_COCKPIT_DIR = str(Path(__file__).resolve().parent)
if _COCKPIT_DIR not in sys.path:
    sys.path.insert(0, _COCKPIT_DIR)

from fastapi import APIRouter, HTTPException
from fastapi.responses import FileResponse, Response

from app.api import (
    abnormal,
    alerts,
    analysis,
    backtest,
    data,
    ext_data,
    factors,
    financials,
    indices,
    intraday,
    kline,
    lots,
    market_recap,
    mining,
    monitor_rules,
    overview,
    pipeline,
    regime,
    rps,
    screener,
    sector_rotation,
    signals,
    stock_analysis,
    strategy,
    watchlist,
)
from app.api import auth as auth_api
from app.api import settings as settings_api
from app.api.routes import router as core_router

router = APIRouter(prefix="/cockpit", tags=["cockpit"])

# 核心路由 (/health, /api/capabilities)
router.include_router(core_router)
# 27 个业务子路由（各自带 /api/xxx 前缀）
for _sub in (
    auth_api.router,
    kline.router,
    watchlist.router,
    screener.router,
    backtest.router,
    factors.router,
    mining.router,
    intraday.router,
    indices.router,
    overview.router,
    abnormal.router,
    regime.router,
    analysis.router,
    pipeline.router,
    data.router,
    ext_data.router,
    financials.router,
    stock_analysis.router,
    market_recap.router,
    settings_api.router,
    strategy.router,
    signals.router,
    monitor_rules.router,
    lots.router,
    alerts.router,
    rps.router,
    sector_rotation.router,
):
    router.include_router(_sub)


# ── SPA 前端兜底：静态资源直接返回文件，其余非 API 路径回退 index.html ──
_STATIC_DIR = Path(__file__).resolve().parent / "static"

# 安全前缀检查：只允许 static 目录内文件（防路径穿越）
_SAFE_STATIC = _STATIC_DIR.resolve()

# 静态文件 MIME 映射（补 Starlette 缺失的类型）
_MIME = {
    ".js": "application/javascript",
    ".mjs": "application/javascript",
    ".css": "text/css",
    ".svg": "image/svg+xml",
    ".html": "text/html; charset=utf-8",
    ".json": "application/json",
    ".woff": "font/woff",
    ".woff2": "font/woff2",
    ".png": "image/png",
    ".ico": "image/x-icon",
}


@router.get("/{path:path}", include_in_schema=False)
async def _spa_fallback(path: str):
    """React SPA 路由兜底：静态资源返回文件，API 路径 404，其余返回 index.html。"""
    if path.startswith("api/") or path == "health":
        raise HTTPException(status_code=404, detail="Not Found")

    # 1. 尝试返回静态文件（assets/*.js, favicon.svg 等）
    file_path = (_STATIC_DIR / path).resolve()
    if str(file_path).startswith(str(_SAFE_STATIC)) and file_path.is_file():
        media_type = _MIME.get(file_path.suffix.lower(), "application/octet-stream")
        with open(file_path, "rb") as f:
            content = f.read()
        return Response(content=content, media_type=media_type,
                        headers={"Cache-Control": "public, max-age=86400"})

    # 2. 兜底返回 index.html（React Router 接管客户端路由）
    index = _STATIC_DIR / "index.html"
    if index.exists():
        return FileResponse(str(index), headers={"Cache-Control": "no-store, must-revalidate"})
    raise HTTPException(status_code=404, detail="cockpit frontend not built")
