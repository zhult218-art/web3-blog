# -*- coding: utf-8 -*-
"""
ffmpeg.py —— ffmpeg / ffprobe 工具类（子进程封装）
"""
import asyncio
import json
import re
from .. import config


async def probe(path) -> dict:
    """ffprobe 读取时长/分辨率（JSON 输出）"""
    cmd = [config.FFPROBE, "-v", "quiet", "-print_format", "json",
           "-show_format", "-show_streams", str(path)]
    proc = await asyncio.create_subprocess_exec(*cmd, stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE)
    out, _ = await proc.communicate()
    if proc.returncode != 0:
        raise RuntimeError("ffprobe 解析失败（文件可能不是有效视频）")
    info = json.loads(out.decode("utf-8", "ignore"))
    v = next((s for s in info.get("streams", []) if s.get("codec_type") == "video"), {})
    return {
        "duration": float(info.get("format", {}).get("duration", 0) or 0),
        "width": int(v.get("width", 0) or 0),
        "height": int(v.get("height", 0) or 0),
    }


async def run_ffmpeg(args: list, total_seconds: float = 0, on_progress=None):
    """
    执行 ffmpeg 命令；通过解析 stderr 的 time= 输出回调进度（0~1 之间的增量系数由调用方换算）。
    抛出 RuntimeError 表示转码失败（含超时/崩溃）。
    """
    cmd = [config.FFMPEG, "-y", "-hide_banner", "-loglevel", "error", "-progress", "pipe:1", "-nostats"] + args
    proc = await asyncio.create_subprocess_exec(*cmd, stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE)
    time_re = re.compile(r"time=(\d+):(\d+):(\d+\.\d+)")

    async def drain_out():
        """读 stdout 的 -progress 输出，换算进度"""
        async for raw in proc.stdout:
            line = raw.decode("utf-8", "ignore")
            m = time_re.search(line)
            if m and total_seconds > 0 and on_progress:
                h, mi, s = m.groups()
                on_progress(min(1.0, (int(h) * 3600 + int(mi) * 60 + float(s)) / total_seconds))

    async def drain_err():
        """并行读 stderr，避免管道缓冲填满导致死锁"""
        data = await proc.stderr.read()
        return data.decode("utf-8", "ignore")

    _, err = await asyncio.gather(drain_out(), drain_err())
    rc = await proc.wait()          # 必须等待进程退出，否则 returncode 为 None
    if rc != 0:
        raise RuntimeError(f"ffmpeg 转码失败: {err.strip()[-400:] or 'returncode ' + str(rc)}")


async def transcode_hls(input_path, out_dir, duration: float, on_progress=None):
    """
    生成 HLS 自适应码率流（ABR）：三档清晰度 + master.m3u8，4 秒一片。
    out_dir 结构：stream_0/playlist.m3u8 + ts 分片（0/1/2 对应 1080p/720p/480p）
    """
    out_dir.mkdir(parents=True, exist_ok=True)
    for i, (_, _, _, _, _) in enumerate(config.RENDITIONS):
        (out_dir / f"stream_{i}").mkdir(exist_ok=True)

    n = len(config.RENDITIONS)
    # 视频分三路缩放，统一 aac 128k 音频
    fc = "[0:v]split=%d%s" % (n, "".join(f"[v{i}]" for i in range(n)))
    maps = []
    for i, (name, w, h, br, buf) in enumerate(config.RENDITIONS):
        fc += f";[v{i}]scale=w={w}:h={h}[v{i}out]"
        maps += [f"-map", f"[v{i}out]", f"-c:v:{i}", "libx264",
                 f"-b:v:{i}", br, f"-maxrate:v:{i}", br, f"-bufsize:v:{i}", buf,
                 "-preset", "veryfast", "-g", "48", "-keyint_min", "48"]
    args = ["-i", str(input_path), "-filter_complex", fc]
    for i in range(n):
        args += ["-map", f"a:{0}"]  # 每档清晰度复用同一音轨
    args += maps + [
        "-c:a", "aac", "-b:a", config.AUDIO_BITRATE, "-ac", "2",
        "-f", "hls", "-hls_time", str(config.SEG_SECONDS),
        "-hls_playlist_type", "vod", "-hls_flags", "independent_segments",
        "-hls_segment_type", "mpegts",
        "-master_pl_name", "master.m3u8",
        f"-hls_segment_filename", str(out_dir / "stream_%v" / "seg_%04d.ts"),
        f"-var_stream_map", " ".join(f"v:{i},a:{i}" for i in range(n)),
        str(out_dir / "stream_%v" / "playlist.m3u8"),
    ]
    await run_ffmpeg(args, duration, on_progress)


async def make_optimized_mp4(input_path, out_path, duration: float, on_progress=None):
    """短视频专用：转 720p mp4，faststart 优化首屏（moov 前置），拖动延迟更低"""
    out_path.parent.mkdir(parents=True, exist_ok=True)
    args = ["-i", str(input_path), "-vf", "scale=-2:720",
            "-c:v", "libx264", "-preset", "veryfast", "-maxrate", "2000k", "-bufsize", "4000k",
            "-c:a", "aac", "-b:a", config.AUDIO_BITRATE,
            "-movflags", "+faststart", str(out_path)]
    await run_ffmpeg(args, duration, on_progress)


async def make_cover(input_path, out_path, at_sec: float = 3.0):
    """截取第 3 秒画面做封面，压缩到 720p 以内（短视频不足 3 秒时 ffmpeg 自动取末帧附近）"""
    out_path.parent.mkdir(parents=True, exist_ok=True)
    args = ["-ss", str(at_sec), "-i", str(input_path), "-frames:v", "1",
            "-vf", "scale=-2:min(720\\,ih)", "-q:v", "4", str(out_path)]
    await run_ffmpeg(args, 0, None)
