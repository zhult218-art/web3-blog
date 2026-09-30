# -*- coding: utf-8 -*-
"""
database.py —— SQLite 数据访问层（轻量封装，线程安全）
表 videos 字段与需求一致，另加 progress / error / oss_keys 辅助字段。
"""
import sqlite3
import threading
import time
from config import DATA_DIR

DB_PATH = DATA_DIR / "video.db"
_lock = threading.Lock()          # SQLite 写并发保护（转码 worker 线程会频繁更新进度）


def _conn():
    conn = sqlite3.connect(str(DB_PATH), check_same_thread=False)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    """建表（幂等）"""
    with _lock, _conn() as c:
        c.execute("""
        CREATE TABLE IF NOT EXISTS videos (
            video_id    TEXT PRIMARY KEY,          -- 唯一字符串主键
            title       TEXT NOT NULL,             -- 标题
            desc        TEXT DEFAULT '',           -- 简介
            duration    REAL DEFAULT 0,            -- 视频时长（秒）
            m3u8_url    TEXT,                      -- 主自适应 m3u8 地址（OSS key 或本地路径）
            mp4_url     TEXT,                      -- 优化后 mp4 地址（短视频才有，长视频 NULL）
            cover_url   TEXT,                      -- 封面地址
            status      TEXT DEFAULT 'pending',    -- pending/transcoding/success/failed
            progress    INTEGER DEFAULT 0,         -- 转码进度百分比 0~100
            error       TEXT,                      -- 失败原因（可读信息）
            oss_keys    TEXT DEFAULT '',           -- 该视频在 OSS/本地 的全部资源 key（逗号分隔，删除时用）
            create_time INTEGER NOT NULL,          -- 创建时间（unix 秒）
            enable      INTEGER DEFAULT 1          -- 是否启用
        )""")


def insert_video(video_id: str, title: str, desc: str):
    with _lock, _conn() as c:
        c.execute("INSERT INTO videos (video_id, title, desc, create_time) VALUES (?,?,?,?)",
                  (video_id, title, desc, int(time.time())))


def update_video(video_id: str, **fields):
    """任意字段更新（内部使用，字段白名单见调用处）"""
    keys = ",".join(f"{k}=?" for k in fields)
    with _lock, _conn() as c:
        c.execute(f"UPDATE videos SET {keys} WHERE video_id=?", (*fields.values(), video_id))


def get_video(video_id: str):
    with _lock, _conn() as c:
        return c.execute("SELECT * FROM videos WHERE video_id=?", (video_id,)).fetchone()


def list_videos(page: int, size: int):
    """分页取列表：返回 (total, rows)"""
    with _lock, _conn() as c:
        total = c.execute("SELECT COUNT(*) n FROM videos").fetchone()["n"]
        rows = c.execute(
            "SELECT * FROM videos ORDER BY create_time DESC LIMIT ? OFFSET ?", (size, (page - 1) * size)
        ).fetchall()
    return total, rows


def delete_video(video_id: str):
    with _lock, _conn() as c:
        c.execute("DELETE FROM videos WHERE video_id=?", (video_id,))
