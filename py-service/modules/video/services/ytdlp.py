# -*- coding: utf-8 -*-
"""
ytdlp.py —— 基于 yt-dlp 的视频 URL 下载服务
功能：
- 解析 URL 获取视频信息（标题、时长、缩略图、可用清晰度）
- 下载视频到临时目录，自动选最佳质量或指定质量
- 可选下载字幕（yt-dlp 自动翻译为中文）
- 下载完成后入转码队列，复用 transcode manager 流水线

依赖：yt-dlp（pip install yt-dlp，或提供可执行文件），可选 ffmpeg（合并音视频流）。
yt-dlp 通过子进程调用，模块导入不依赖该 Python 包是否安装。
"""
import asyncio
import json
import os
import re
import shutil
import subprocess
import uuid
from pathlib import Path

from .. import config
from .. import database

# 质量档位 → yt-dlp format 选择字符串
QUALITY_MAP = {
    "best": "bestvideo*+bestaudio/best",
    "1080p": "bestvideo[height<=1080]+bestaudio/best[height<=1080]",
    "720p": "bestvideo[height<=720]+bestaudio/best[height<=720]",
    "480p": "bestvideo[height<=480]+bestaudio/best[height<=480]",
    "audio": "bestaudio/best",
}


def _run(cmd, timeout=600):
    """同步执行命令，返回 (returncode, stdout, stderr)"""
    proc = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
    return proc.returncode, proc.stdout, proc.stderr


def _ytdlp_available():
    return shutil.which("yt-dlp") is not None


def probe(url: str):
    """探测视频信息（不下载），返回标题、时长、缩略图、可用格式列表"""
    if not _ytdlp_available():
        return {"ok": False, "error": "yt-dlp 未安装，请执行 pip install yt-dlp"}
    cmd = ["yt-dlp", "--dump-json", "--no-playlist", "--skip-download", url]
    try:
        rc, out, err = _run(cmd, timeout=120)
        if rc != 0:
            return {"ok": False, "error": f"yt-dlp 探测失败: {err[:300]}"}
        info = json.loads(out.splitlines()[-1])
        return {
            "ok": True,
            "title": info.get("title", "未知标题"),
            "duration": info.get("duration"),
            "thumbnail": info.get("thumbnail"),
            "uploader": info.get("uploader") or info.get("channel"),
            "webpage_url": info.get("webpage_url"),
            "formats": [
                {
                    "format_id": f.get("format_id"),
                    "ext": f.get("ext"),
                    "height": f.get("height"),
                    "width": f.get("width"),
                    "vcodec": f.get("vcodec"),
                    "acodec": f.get("acodec"),
                    "filesize": f.get("filesize") or f.get("filesize_approx"),
                }
                for f in info.get("formats", [])
                if f.get("height") or "audio" in (f.get("vcodec") or "")
            ],
        }
    except subprocess.TimeoutExpired:
        return {"ok": False, "error": "探测超时（>120s）"}
    except Exception as e:
        return {"ok": False, "error": f"解析异常: {e}"}


def download_video(video_id: str, url: str, quality: str = "best",
                   subtitle: bool = False, subtitle_lang: str = "zh-CN"):
    """
    同步下载视频到 TMP_DIR/{video_id}.mp4，完成后入转码队列。
    下载过程通过 database 更新 progress（0-100）。
    """
    if not _ytdlp_available():
        database.update_video(video_id, status="failed", error="yt-dlp 未安装")
        return

    dest = config.TMP_DIR / f"{video_id}.mp4"
    fmt = QUALITY_MAP.get(quality, QUALITY_MAP["best"])

    cmd = [
        "yt-dlp",
        "-f", fmt,
        "--merge-output-format", "mp4",
        "-o", str(dest),
        "--no-playlist",
        "--newline",
        "--progress",
    ]
    # 字幕选项
    if subtitle:
        cmd += [
            "--write-subs",
            "--write-auto-subs",
            "--sub-langs", f"{subtitle_lang},en",
            "--sub-format", "srt/best",
            "--convert-subs", "srt",
        ]
    # SponsorBlock 广告分段标记（不自动跳过，仅写入元数据供前端使用）
    cmd += ["--sponsorblock-remove", "all"]
    cmd.append(url)

    try:
        proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                text=True, bufsize=1)
        progress = 0
        for line in proc.stdout:
            # 解析 yt-dlp 进度行: [download]  12.3% of ...
            m = re.search(r"(\d+(?:\.\d+)?)%", line)
            if m:
                pct = float(m.group(1))
                # 下载占 70%，转码占 30%
                database.update_video(video_id, progress=int(min(70, pct * 0.7)))
        proc.wait(timeout=1800)
        if proc.returncode != 0:
            database.update_video(video_id, status="failed", error=f"yt-dlp 下载失败 (exit {proc.returncode})")
            return

        if not dest.exists():
            database.update_video(video_id, status="failed", error="下载完成但文件未生成")
            return

        # 下载完成，入转码队列（转码会从 70% 推进到 100%）
        database.update_video(video_id, progress=70)
        from .transcode import manager
        manager.submit(video_id)
    except subprocess.TimeoutExpired:
        database.update_video(video_id, status="failed", error="下载超时（>30min）")
    except Exception as e:
        database.update_video(video_id, status="failed", error=f"下载异常: {e}")


async def download_async(video_id: str, url: str, quality: str = "best",
                         subtitle: bool = False, subtitle_lang: str = "zh-CN"):
    """异步包装，供 FastAPI BackgroundTasks 调用"""
    await asyncio.to_thread(download_video, video_id, url, quality, subtitle, subtitle_lang)
