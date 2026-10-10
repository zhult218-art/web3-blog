# -*- coding: utf-8 -*-
"""
视频服务模块（由独立 video-service 迁移）。
- 路由前缀 /api/video，6 个接口路径与方法保持不变
- /static 管理/播放页面、/media 本地媒体 fallback 在 setup() 中挂载
- startup() 完成建表与转码 worker 启动
模块自身不监听端口、不运行 uvicorn。
"""
from __future__ import annotations

from fastapi.staticfiles import StaticFiles

from . import config
from . import database
from .routers.video import router
from .services.transcode import manager

MODULE_META = {
    "title": "视频服务",
    "description": "视频上传/转码(HLS)/OSS 签名分发服务",
    "icon": "🎬",
    "version": "1.0.0",
}


def setup(app) -> None:
    """原 main.py 的静态资源挂载：/static → 模块内 static；/media → 数据目录 media。"""
    app.mount("/static", StaticFiles(directory=str(config.STATIC_DIR)), name="static")
    app.mount("/media", StaticFiles(directory=str(config.MEDIA_DIR)), name="media")


def startup() -> None:
    """原 main.py @app.on_event('startup')：建表 + 启动转码 worker（2 并发 + 内存队列）。"""
    database.init_db()
    manager.start()


def health() -> dict:
    return {"ok": True, "oss_enabled": config.oss_enabled}


__all__ = ["router", "MODULE_META", "setup", "startup", "health"]
