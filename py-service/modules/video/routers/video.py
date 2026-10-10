# -*- coding: utf-8 -*-
"""
video.py —— 路由层：视频管理 API
权限约定：上传 / 删除 / 任务查询需 X-Admin-Token；列表与播放信息公开（播放页用）。
"""
import shutil
import uuid

from fastapi import APIRouter, BackgroundTasks, Depends, File, Form, HTTPException, UploadFile
from pydantic import BaseModel

from .. import config
from .. import database
from ..security import verify_admin_token
from ..services.transcode import manager, resolve_urls
from ..services import ytdlp

router = APIRouter(prefix="/api/video")


def _check_mp4_header(head: bytes):
    """校验 MP4 文件头：offset 4~8 必须是 'ftyp'（拦截改后缀的非视频文件）"""
    if len(head) < 12 or head[4:8] != b"ftyp":
        raise HTTPException(status_code=400, detail="文件头校验失败：不是有效的 MP4 视频")


@router.post("/upload", dependencies=[Depends(verify_admin_token)])
async def upload_video(file: UploadFile = File(...), title: str = Form(...), desc: str = Form("")):
    """上传 mp4 → 临时目录 → 创建 pending 记录 → 入转码队列，立即返回 video_id"""
    ext = "." + (file.filename or "").rsplit(".", 1)[-1].lower()
    if ext not in config.ALLOWED_EXT:
        raise HTTPException(status_code=400, detail="只允许上传 mp4 格式")

    video_id = uuid.uuid4().hex[:16]
    dest = config.TMP_DIR / f"{video_id}.mp4"

    # 流式落盘：边写边限大小，防超大文件撑爆内存/磁盘
    size = 0
    header = b""
    with open(dest, "wb") as f:
        while chunk := await file.read(1024 * 1024):
            size += len(chunk)
            if size > config.MAX_UPLOAD_BYTES:
                f.close(); dest.unlink(missing_ok=True)
                raise HTTPException(status_code=400, detail="单文件最大 2GB")
            if len(header) < 12:
                header += chunk
            f.write(chunk)
    if size == 0:
        dest.unlink(missing_ok=True)
        raise HTTPException(status_code=400, detail="空文件")

    _check_mp4_header(header)                       # 文件头校验
    database.insert_video(video_id, title.strip() or "未命名", desc.strip())
    manager.submit(video_id)                        # 入队（并发 2，其余排队）
    return {"code": 0, "message": "ok", "data": {"video_id": video_id}}


# ────────────────────────────────────────────────────────────
# yt-dlp URL 下载（HomeTube 风格：粘贴 URL → 下载 → 自动转码入库）
# ────────────────────────────────────────────────────────────
class DownloadReq(BaseModel):
    url: str
    quality: str = "best"          # best / 1080p / 720p / 480p / audio
    subtitle: bool = False         # 是否下载字幕
    subtitle_lang: str = "zh-CN"   # 字幕语言


@router.post("/download")
async def download_from_url(req: DownloadReq, bg: BackgroundTasks):
    """通过 yt-dlp 下载网络视频（YouTube/B站/抖音等 1800+ 平台），下载后自动转码入库"""
    if not ytdlp._ytdlp_available():
        raise HTTPException(status_code=500, detail="yt-dlp 未安装，请执行 pip install yt-dlp")

    # 先探测标题
    probe = ytdlp.probe(req.url)
    title = probe["title"] if probe.get("ok") else "网络视频"
    desc = f"来源: {req.url}"

    video_id = uuid.uuid4().hex[:16]
    database.insert_video(video_id, title, desc)
    database.update_video(video_id, progress=1)

    bg.add_task(ytdlp.download_async, video_id, req.url, req.quality, req.subtitle, req.subtitle_lang)
    return {"code": 0, "message": "ok", "data": {"video_id": video_id, "title": title}}


@router.get("/task/{video_id}", dependencies=[Depends(verify_admin_token)])
def task_status(video_id: str):
    """查询转码任务状态与进度百分比"""
    row = database.get_video(video_id)
    if not row:
        raise HTTPException(status_code=404, detail="视频不存在")
    return {"code": 0, "message": "ok", "data": {
        "video_id": video_id, "status": row["status"],
        "progress": row["progress"], "error": row["error"],
    }}


@router.get("/list")
def list_videos(page: int = 1, size: int = 10):
    """分页获取视频列表（公开，播放页/后台共用）"""
    page, size = max(1, page), min(50, max(1, size))
    total, rows = database.list_videos(page, size)
    items = [{
        "video_id": r["video_id"], "title": r["title"], "desc": r["desc"],
        "duration": r["duration"], "status": r["status"],
        "cover_url": resolve_urls(r)["cover_url"],
        "create_time": r["create_time"], "enable": r["enable"],
    } for r in rows]
    return {"code": 0, "message": "ok", "data": {"total": total, "page": page, "size": size, "list": items}}


@router.get("/{video_id}")
def video_detail(video_id: str):
    """单个视频播放信息（地址已带 6h 签名）"""
    row = database.get_video(video_id)
    if not row or not row["enable"]:
        raise HTTPException(status_code=404, detail="视频不存在或未启用")
    if row["status"] != "success":
        return {"code": 0, "message": "ok", "data": {"video_id": video_id, "status": row["status"],
                                                     "progress": row["progress"], "error": row["error"]}}
    urls = resolve_urls(row)
    return {"code": 0, "message": "ok", "data": {
        "video_id": video_id, "title": row["title"], "desc": row["desc"],
        "duration": row["duration"], "status": row["status"], "create_time": row["create_time"], **urls,
    }}


@router.delete("/{video_id}", dependencies=[Depends(verify_admin_token)])
def delete_video(video_id: str):
    """删除视频：清理 OSS（或本地）全部资源 + 删库记录"""
    row = database.get_video(video_id)
    if not row:
        raise HTTPException(status_code=404, detail="视频不存在")
    try:
        if config.oss_enabled and row["oss_keys"]:
            from ..services import oss_client
            prefix = f"videos/{video_id}/"
            oss_client.delete_by_prefix(prefix)          # 按前缀批量删除 OSS 资源
        elif not config.oss_enabled:
            shutil.rmtree(config.MEDIA_DIR / video_id, ignore_errors=True)
    except Exception as e:                               # 资源清理失败也要返回可读信息
        raise HTTPException(status_code=500, detail=f"清理存储资源失败: {e}")
    database.delete_video(video_id)
    return {"code": 0, "message": "ok", "data": {"video_id": video_id, "deleted": True}}
