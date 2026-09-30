# -*- coding: utf-8 -*-
"""
transcode.py —— 转码任务队列管理（核心业务层）
- 同一时刻最多 2 个 ffmpeg 任务在跑（MAX_WORKERS），超出的任务在内存队列排队，防止 CPU 打满
- 全流程：探测时长 → HLS 三档转码（进度 5~78%）→ 短视频额外产出 mp4（78~86%）
          → 截封面 → 上传 OSS / 本地归档（86~99%）→ 清理临时文件 → success
- 任何异常（ffmpeg 崩溃/超时/OSS 失败）→ status=failed + 可读错误信息
"""
import asyncio
import logging
import queue
import shutil
import threading

import config
import database
from utils import ffmpeg
from services import oss_client

log = logging.getLogger("transcode")


class TranscodeManager:
    def __init__(self):
        self.q: "queue.Queue[str]" = queue.Queue()
        self.workers: list[threading.Thread] = []

    def start(self):
        for i in range(config.MAX_WORKERS):
            t = threading.Thread(target=self._worker, name=f"transcode-worker-{i}", daemon=True)
            t.start()
            self.workers.append(t)
        log.info("转码 worker 就绪 x%d", config.MAX_WORKERS)

    def submit(self, video_id: str):
        """任务入队（pending）"""
        self.q.put(video_id)

    # ─────────────────────────── worker ───────────────────────────
    def _worker(self):
        while True:
            vid = self.q.get()
            try:
                asyncio.run(self._process(vid))
            except Exception as e:                      # 兜底：绝不让 worker 线程死掉
                log.exception("转码任务异常 %s", vid)
                database.update_video(vid, status="failed", error=str(e)[:500])
            finally:
                self.q.task_done()

    async def _process(self, vid: str):
        row = database.get_video(vid)
        if not row:
            return
        src = config.TMP_DIR / f"{vid}.mp4"             # 上传落地的原始文件
        work = config.TMP_DIR / vid                     # 本任务的临时工作目录
        work.mkdir(parents=True, exist_ok=True)
        database.update_video(vid, status="transcoding", progress=2)

        def set_pct(p):                                 # 统一进度落库（0~100 整数）
            database.update_video(vid, progress=max(2, min(99, int(p))))

        try:
            # 1) 探测时长与分辨率
            info = await ffmpeg.probe(src)
            duration = info["duration"]
            if duration <= 0:
                raise RuntimeError("无法读取视频时长，文件可能损坏")
            set_pct(5)

            # 2) HLS 三档转码（进度 5~78%）
            hls_dir = work / "hls"
            await ffmpeg.transcode_hls(
                src, hls_dir, duration,
                on_progress=lambda r: set_pct(5 + r * 73))

            # 3) 短视频（≤120s）额外产出 720p faststart mp4（进度 78~86%）
            is_short = duration <= config.SHORT_VIDEO_SECONDS
            mp4_name = None
            if is_short:
                mp4_name = f"{vid}_720p.mp4"
                await ffmpeg.make_optimized_mp4(src, work / mp4_name, duration,
                                                on_progress=lambda r: set_pct(78 + r * 8))

            # 4) 封面：第 3 秒画面，≤720p
            cover_name = "cover.jpg"
            await ffmpeg.make_cover(src, work / cover_name)
            set_pct(88)

            # 4.5) Windows 下 ffmpeg 生成的 m3u8 内部子路径是反斜杠，浏览器/hls.js 无法解析 → 统一替换为 /
            for playlist in hls_dir.rglob("*.m3u8"):
                txt = playlist.read_text(encoding="utf-8", errors="ignore")
                fixed = txt.replace("\\", "/")
                if fixed != txt:
                    playlist.write_text(fixed, encoding="utf-8")

            # 5) 上传 OSS 或本地归档；记录全部资源 key 供删除时清理
            keys, m3u8_key, mp4_key, cover_key = self._store(vid, work, hls_dir, mp4_name, cover_name)
            database.update_video(
                vid, duration=round(duration, 2),
                m3u8_url=m3u8_key, mp4_url=mp4_key or None, cover_url=cover_key,
                oss_keys=",".join(keys), progress=99)
            set_pct(99)

            # 6) 成功：清理临时原始文件与分片（磁盘不占满）
            database.update_video(vid, status="success", progress=100)
            log.info("转码完成 %s (%.1fs)", vid, duration)
        except Exception as e:
            database.update_video(vid, status="failed", error=str(e)[:500])
            raise
        finally:
            shutil.rmtree(work, ignore_errors=True)     # 工作目录必清
            if src.exists():
                src.unlink(missing_ok=True)             # 原始上传文件必删

    # ─────────────────────────── 存储层 ───────────────────────────
    def _store(self, vid, work, hls_dir, mp4_name, cover_name):
        """
        返回 (全部keys, m3u8_key, mp4_key, cover_key)。
        OSS 模式：key 即数据库存值，访问 URL 由查询接口实时签名（6h 有效）。
        本地模式：文件移动到 data/media/{vid}/，数据库存 /media/{vid}/xxx（静态目录直出）。
        """
        keys = []
        if config.oss_enabled:
            prefix = f"videos/{vid}/"
            # m3u8 与全部分片
            for p in hls_dir.rglob("*"):
                if p.is_file():
                    key = prefix + str(p.relative_to(hls_dir)).replace("\\", "/")
                    oss_client.put_file(key, p)
                    keys.append(key)
            for name in filter(None, [mp4_name, cover_name]):
                key = prefix + name
                oss_client.put_file(key, work / name)
                keys.append(key)
            return keys, prefix + "master.m3u8", (prefix + mp4_name if mp4_name else None), prefix + cover_name
        else:
            dest = config.MEDIA_DIR / vid
            if dest.exists():
                shutil.rmtree(dest, ignore_errors=True)
            shutil.move(str(hls_dir), str(dest))
            for name in filter(None, [mp4_name, cover_name]):
                shutil.move(str(work / name), str(dest / name))
            for p in dest.rglob("*"):
                if p.is_file():
                    keys.append(f"/media/{vid}/" + str(p.relative_to(dest)).replace("\\", "/"))
            base = f"/media/{vid}/"
            return keys, base + "master.m3u8", (base + mp4_name if mp4_name else None), base + cover_name


manager = TranscodeManager()   # 模块级单例


def resolve_urls(row) -> dict:
    """
    把数据库里的地址转换为可访问 URL：
    - OSS：实时签发 6 小时签名 URL（防盗链核心）
    - 本地：直接返回静态路径
    """
    def to_url(v):
        if not v:
            return None
        return oss_client.sign_url(v) if config.oss_enabled and not v.startswith("/") else v
    return {
        "m3u8_url": to_url(row["m3u8_url"]),
        "mp4_url": to_url(row["mp4_url"]),
        "cover_url": to_url(row["cover_url"]),
    }
