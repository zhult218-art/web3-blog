# -*- coding: utf-8 -*-
"""
Web3 统一 Python 服务（py-service）入口

将原先分散的 quant(9006) / whisper(9011) / qzone(9012) / video(8300)
以及新增的 wifi 能力合并为「单进程 · 单端口 · 单 Nacos 注册」。

扩展方式：在 modules/ 下新建功能目录（提供 router 等，约定见 core/loader.py），
框架启动时自动发现并挂载，无需修改本文件。
"""
from __future__ import annotations

import asyncio
import sys
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Any, Dict, List

BASE_DIR = Path(__file__).resolve().parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from fastapi import FastAPI
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware

from core.config import get_settings, load_dotenv
from core.loader import FAILED_MODULES, LoadedModule, is_awaitable, load_modules
from core.logging import setup_logging
from core.nacos import NacosRegistrar

load_dotenv()
settings = get_settings()
setup_logging()

import logging  # noqa: E402

logger = logging.getLogger("py-service")
_startup_failures: List[str] = []
_setup_failures: List[str] = []

# ── 装配模块（先 setup 挂载静态资源，再 include 路由） ──
modules: List[LoadedModule] = load_modules(settings)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # 启动各模块；失败会反映在 /health 中。
    _startup_failures.clear()
    for mod in modules:
        if mod.startup is None:
            continue
        try:
            result = mod.startup()
            if is_awaitable(mod.startup) or asyncio.iscoroutine(result):
                await result
        except Exception as exc:  # noqa: BLE001
            _startup_failures.append(mod.name)
            logger.exception("模块 %s 启动失败：%s", mod.name, exc)

    # 注册到 Nacos（metadata 暴露模块清单）
    meta = {
        "modules": ",".join(m.name for m in modules),
        "versions": "1.0",
    }
    registrar = NacosRegistrar(settings, metadata=meta)
    registrar.start()

    yield

    registrar.stop()
    for mod in reversed(modules):
        if mod.shutdown is None:
            continue
        try:
            result = mod.shutdown()
            if is_awaitable(mod.shutdown) or asyncio.iscoroutine(result):
                await result
        except Exception as exc:  # noqa: BLE001
            logger.error("模块 %s 关闭失败：%s", mod.name, exc)


app = FastAPI(
    title="Web3 Py Service",
    version="1.0.0",
    description="统一 Python 能力服务：量化 / QQ空间备份 / 语音识别 / 视频 / WiFi 审计",
    lifespan=lifespan,
)

# 空配置时仅允许本机开发地址；部署域名通过 CORS_ALLOWED_ORIGINS 显式配置。
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins or ["http://localhost", "http://127.0.0.1", "http://localhost:5173", "http://127.0.0.1:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

_setup_failures.clear()
for mod in modules:
    if mod.setup is not None:
        try:
            mod.setup(app)
        except Exception as exc:  # noqa: BLE001
            _setup_failures.append(mod.name)
            logger.exception("模块 %s setup 失败：%s", mod.name, exc)
    if mod.router is not None:
        app.include_router(mod.router)


async def _module_health(mod: LoadedModule) -> Dict[str, Any]:
    info: Dict[str, Any] = {"name": mod.name, "title": mod.title}
    if mod.health is not None:
        try:
            result = mod.health()
            if asyncio.iscoroutine(result):
                result = await result
            info["status"] = result
        except Exception as exc:  # noqa: BLE001
            info["status"] = {"ok": False, "error": str(exc)}
    else:
        info["status"] = {"ok": True}
    return info


@app.get("/health")
async def health():
    """聚合健康检查；关键模块异常时返回 HTTP 503。"""
    module_statuses = [await _module_health(m) for m in modules]
    unhealthy = [
        item["name"] for item in module_statuses
        if isinstance(item.get("status"), dict) and item["status"].get("ok") is False
    ]
    unhealthy.extend(name for name in _startup_failures + _setup_failures if name not in unhealthy)
    unhealthy.extend(name for name in FAILED_MODULES if name not in unhealthy)
    ok = not unhealthy
    payload = {
        "ok": ok,
        "service": settings.service_name,
        "ip": settings.register_ip,
        "port": settings.port,
        "modules": module_statuses,
        "unhealthy_modules": unhealthy,
    }
    return payload if ok else JSONResponse(status_code=503, content=payload)


@app.get("/")
def root():
    return {
        "service": settings.service_name,
        "version": "1.0.0",
        "docs": "/docs",
        "modules": [m.name for m in modules],
    }


if __name__ == "__main__":
    import uvicorn

    # access_log 关闭：健康轮询/行情轮询会把控制台刷满重复请求行
    uvicorn.run("main:app", host=settings.host, port=settings.port,
                log_level="warning", access_log=False)
