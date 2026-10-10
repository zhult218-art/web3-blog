# -*- coding: utf-8 -*-
"""
config.py —— 全局配置（部署时主要改这里 / 或用环境变量覆盖）
（由独立 video-service 迁移为 py-service 模块，数据目录指向 py-service/data/video）
"""
import os
from pathlib import Path

# 本包位置：py-service/modules/video/config.py
# parents[0]=video  parents[1]=modules  parents[2]=py-service
PACKAGE_DIR = Path(__file__).resolve().parent
BASE_DIR = PACKAGE_DIR.resolve().parents[1]                  # py-service 根目录

# ─── 管理员 Token（生产环境务必用环境变量覆盖！见 README） ───
ADMIN_TOKEN = os.getenv("VIDEO_ADMIN_TOKEN", "change-me-admin-token")

# ─── 存储目录 ───
DATA_DIR = BASE_DIR / "data" / "video"                       # SQLite 与本地媒体根目录
TMP_DIR = DATA_DIR / "tmp"                                   # 上传/转码临时目录（不长期存放）
MEDIA_DIR = DATA_DIR / "media"                               # 未接 OSS 时的本地媒体 fallback
STATIC_DIR = PACKAGE_DIR / "static"                          # 模块内管理/播放静态页面
for d in (DATA_DIR, TMP_DIR, MEDIA_DIR):
    d.mkdir(parents=True, exist_ok=True)

# ─── 阿里云 OSS（未配置时自动降级为本地存储，方便开发调试） ───
OSS_ACCESS_KEY_ID = os.getenv("OSS_ACCESS_KEY_ID", "")
OSS_ACCESS_KEY_SECRET = os.getenv("OSS_ACCESS_KEY_SECRET", "")
OSS_ENDPOINT = os.getenv("OSS_ENDPOINT", "")                # 如 https://oss-cn-hangzhou.aliyuncs.com
OSS_BUCKET = os.getenv("OSS_BUCKET", "")
OSS_URL_TTL = 6 * 3600                                      # 签名 URL 有效期：6 小时（基础防盗链）

oss_enabled = bool(OSS_ACCESS_KEY_ID and OSS_ACCESS_KEY_SECRET and OSS_ENDPOINT and OSS_BUCKET)

# ─── 上传限制 ───
MAX_UPLOAD_BYTES = 2 * 1024 * 1024 * 1024                   # 单文件最大 2GB
ALLOWED_EXT = {".mp4"}                                      # 只允许 mp4

# ─── 转码参数 ───
FFMPEG = os.getenv("FFMPEG_PATH", "ffmpeg")                 # ffmpeg 可执行文件
FFPROBE = os.getenv("FFPROBE_PATH", "ffprobe")
MAX_WORKERS = 2                                             # 同一时刻最多 2 个转码任务，其余排队
SEG_SECONDS = 4                                             # HLS 分片 4 秒一片
AUDIO_BITRATE = "128k"                                      # 音频统一 aac 128k
SHORT_VIDEO_SECONDS = 120                                   # ≤120s 视为短视频（额外产出优化 mp4）
# 三档清晰度：(名称, 宽, 高, 视频码率, bufsize)
RENDITIONS = [
    ("1080p", 1920, 1080, "3500k", "7000k"),
    ("720p",  1280, 720,  "2000k", "4000k"),
    ("480p",  854,  480,  "1000k", "2000k"),
]

# ─── 原独立服务端口（仅保留配置常量，模块不再自行监听端口） ───
HOST = os.getenv("VIDEO_HOST", "0.0.0.0")
PORT = int(os.getenv("VIDEO_PORT", "8300"))
