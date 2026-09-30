# -*- coding: utf-8 -*-
"""
main.py —— video-service 入口
启动：uvicorn main:app --host 0.0.0.0 --port 8300  （或 python main.py）
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))   # 允许 `python main.py` 直接运行

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

import config
import database
from routers.video import router as video_router
from services.transcode import manager

app = FastAPI(title="Video Service", version="1.0.0",
              description="视频上传/转码(HLS)/OSS 签名分发服务")

# 跨域：本地前端页面访问用（生产建议收紧为具体域名）
app.add_middleware(
    CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"],
)

app.include_router(video_router)

# 静态资源：管理/播放页面 + 未接 OSS 时的本地媒体 fallback
app.mount("/static", StaticFiles(directory=str(config.BASE_DIR / "static")), name="static")
app.mount("/media", StaticFiles(directory=str(config.MEDIA_DIR)), name="media")


@app.on_event("startup")
def startup():
    database.init_db()   # 建表
    manager.start()      # 启动转码 worker（2 并发 + 内存队列）


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host=config.HOST, port=config.PORT)
