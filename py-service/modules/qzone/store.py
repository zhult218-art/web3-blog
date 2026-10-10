# -*- coding: utf-8 -*-
"""qzone 内存存储：扫码会话 + 抓取任务 + 后台抓取线程。"""
from __future__ import annotations

import json
import math
import os
import shutil
import threading
import time
from datetime import datetime

from . import config, exporter
from .formatting import normalize_moment
from .protocol import fetch_page

# 内存存储：扫码会话 + 抓取任务
qr_sessions: dict[str, dict] = {}
jobs: dict[str, dict] = {}
store_lock = threading.Lock()
running_count = 0


def now() -> datetime:
    return datetime.now()


def reap_loop():
    """后台清理过期扫码会话与任务数据。"""
    while True:
        time.sleep(config.REAP_INTERVAL)
        t = now()
        with store_lock:
            for sid in [s for s, v in qr_sessions.items() if t - v["created_at"] > config.QR_TTL]:
                qr_sessions.pop(sid, None)
            for jid, job in list(jobs.items()):
                if job["status"] in ("done", "failed", "canceled") and t - job["updated_at"] > config.JOB_TTL:
                    shutil.rmtree(job["dir"], ignore_errors=True)
                    jobs.pop(jid, None)


def _fetch_one_page(job: dict, cookies: dict, uin: str, g_tk: int, page_no: int):
    """抓取指定页（page_no 从 0 计），带重试退避；取消或最终失败返回 None。"""
    pos = page_no * config.PAGE_SIZE
    for attempt in range(1, config.MAX_ATTEMPTS + 1):
        if job["cancel"].is_set():
            return None
        try:
            return fetch_page(cookies, uin, g_tk, pos, config.PAGE_SIZE)
        except Exception as e:  # noqa: BLE001
            job["logs"].append(f"第 {page_no + 1} 页第 {attempt} 次请求失败：{e}")
            time.sleep(min(2 * attempt, 10))
    return None


def run_fetch(job: dict, cookies: dict, uin: str, g_tk: int, interval: float, workers: int):
    """后台抓取：先串行取首页拿总数，再由 workers-1 个线程并发翻页。"""
    global running_count
    raw_list: list[dict] = []
    pages: dict[int, list | None] = {}
    try:
        first = _fetch_one_page(job, cookies, uin, g_tk, 0)
        if first is None:
            if job["cancel"].is_set():
                job["status"] = "canceled"
                job["logs"].append("用户取消了任务")
            raise RuntimeError("首页连续抓取失败，任务终止")
        total = int(first.get("total", 0))
        job["total"] = total
        job["total_pages"] = max(1, math.ceil(total / config.PAGE_SIZE))
        if not job.get("name"):
            job["name"] = (first.get("msglist") or [{}])[0].get("name", "") or uin
        job["logs"].append(f"账号 {uin} 共有 {total} 条说说，启动 {workers} 线程并发抓取")
        pages[0] = first.get("msglist") or []
        job["page"] = 1
        job["fetched"] = len(pages[0])
        job["updated_at"] = now()

        if total == 0:
            pass
        elif job["cancel"].is_set():
            job["status"] = "canceled"
            job["logs"].append("用户取消了任务")
        else:
            total_pages = job["total_pages"]
            cursor = 1
            cur_lock = threading.Lock()

            def worker():
                nonlocal cursor
                while not job["cancel"].is_set():
                    with cur_lock:
                        if cursor >= total_pages:
                            return
                        idx = cursor
                        cursor += 1
                    page = _fetch_one_page(job, cookies, uin, g_tk, idx)
                    with cur_lock:
                        pages[idx] = None if page is None else (page.get("msglist") or [])
                        job["page"] = sum(1 for _ in pages)
                        job["fetched"] = sum(len(v) for v in pages.values() if v)
                        job["updated_at"] = now()
                    time.sleep(interval)

            threads = [threading.Thread(target=worker, daemon=True)
                       for _ in range(max(0, workers - 1))]
            for t in threads:
                t.start()
            for t in threads:
                t.join()

        failed_pages = [i + 1 for i, v in sorted(pages.items()) if v is None]
        for i in sorted(pages):
            if pages[i]:
                raw_list.extend(pages[i])
        if failed_pages and not job["cancel"].is_set():
            job["logs"].append(
                f"有 {len(failed_pages)} 页重试后仍失败：第 "
                f"{', '.join(str(p) for p in failed_pages[:10])} 页（对应数据缺失）")
        if not raw_list and total and not job["cancel"].is_set():
            raise RuntimeError("所有分页均失败，未能获取到任何数据")

        normalized = [normalize_moment(x) for x in raw_list]
        job["moments"] = normalized
        os.makedirs(job["dir"], exist_ok=True)
        export_at = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        with open(os.path.join(job["dir"], "data.json"), "w", encoding="utf-8") as f:
            json.dump({"uin": uin, "name": job.get("name", ""), "exported_at": export_at,
                       "total": job.get("total", 0), "count": len(raw_list),
                       "msglist": raw_list}, f, ensure_ascii=False, indent=2)
        with open(os.path.join(job["dir"], "moments.json"), "w", encoding="utf-8") as f:
            json.dump({"uin": uin, "name": job.get("name", ""), "exported_at": export_at,
                       "total": job.get("total", 0), "count": len(normalized),
                       "moments": normalized}, f, ensure_ascii=False, indent=2)
        with open(os.path.join(job["dir"], "index.html"), "w", encoding="utf-8") as f:
            f.write(exporter.render_html(job.get("name", uin), uin, export_at, normalized))
        exporter.write_csv(os.path.join(job["dir"], "moments.csv"), normalized)

        times = [m["time"] for m in normalized if m.get("time")]
        job.update({
            "status": job["status"] if job["status"] == "canceled" else "done",
            "pic_count": sum(len(m["pics"]) for m in normalized),
            "video_count": sum(len(m["videos"]) for m in normalized),
            "comment_count": sum(len(m["comments"]) for m in normalized),
            "range_from": min(times) if times else "",
            "range_to": max(times) if times else "",
            "files": ["json", "html", "csv"],
            "finished_at": now().strftime("%H:%M:%S"),
        })
        job["logs"].append(f"导出完成，共 {len(normalized)} 条，已临时保存（24 小时后自动删除）")
    except Exception as e:  # noqa: BLE001
        job["status"] = "failed"
        job["error"] = str(e)
        job["logs"].append(f"任务失败：{e}")
    finally:
        job["updated_at"] = now()
        with store_lock:
            running_count = max(0, running_count - 1)
