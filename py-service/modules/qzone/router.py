# -*- coding: utf-8 -*-
"""qzone 路由层：扫码会话 / 抓取任务 / 媒体代理。前缀 /api/qzone。"""
from __future__ import annotations

import os
import re
import secrets
import shutil
import threading
from datetime import datetime

import requests
from fastapi import APIRouter, BackgroundTasks, Header, HTTPException, Query
from fastapi.responses import FileResponse, JSONResponse, StreamingResponse
from pydantic import BaseModel

from . import config
from . import protocol
from . import store
from .formatting import blog_uid

router = APIRouter(prefix="/api/qzone", tags=["qzone"])


@router.get("/health")
def health():
    return {"ok": True, "service": "qzone", "running": store.running_count,
            "qr_sessions": len(store.qr_sessions),
            "max_concurrent": config.MAX_CONCURRENT,
            "max_qr_sessions": config.MAX_QR_SESSIONS,
            "max_jobs_per_user": config.MAX_JOBS_PER_USER}


def _require_owner_session(sid: str, uid: str) -> dict:
    s = store.qr_sessions.get(sid)
    if not s:
        raise HTTPException(404, "二维码会话不存在或已过期，请重新获取")
    if uid and s.get("blog_uid") and s["blog_uid"] != uid:
        raise HTTPException(403, "无权访问该扫码会话：仅本人可查看自己的二维码")
    return s


def _require_owner_job(jid: str, uid: str) -> dict:
    job = store.jobs.get(jid)
    if not job:
        raise HTTPException(404, "任务不存在或已过期清理")
    if uid and job.get("blog_uid") and job["blog_uid"] != uid:
        raise HTTPException(403, "无权访问该任务：仅本人可查看自己扫码的抓取结果")
    return job


@router.post("/qr/start")
def qr_start(x_blog_uid: str | None = Header(default=None, alias="X-Blog-Uid")):
    """新建扫码会话，返回腾讯登录二维码 PNG（base64）。"""
    uid = blog_uid(x_blog_uid)
    if len(store.qr_sessions) >= config.MAX_QR_SESSIONS:
        t = store.now()
        for k in [k for k, v in store.qr_sessions.items() if t - v["created_at"] > config.QR_TTL]:
            store.qr_sessions.pop(k, None)
    if len(store.qr_sessions) >= config.MAX_QR_SESSIONS:
        raise HTTPException(429, f"同时扫码的会话已达上限（{config.MAX_QR_SESSIONS}），请稍后再试")
    try:
        sess, qrsig, png = protocol.fetch_qr()
    except Exception as e:  # noqa: BLE001
        raise HTTPException(502, f"获取二维码失败：{e}")
    import base64

    sid = secrets.token_urlsafe(16)
    store.qr_sessions[sid] = {"session": sess, "qrsig": qrsig, "status": "waiting",
                              "created_at": store.now(), "blog_uid": uid}
    return {"sid": sid, "qr_png": "data:image/png;base64," + base64.b64encode(png).decode()}


@router.get("/qr/status")
def qr_status(sid: str = Query(..., min_length=8),
              x_blog_uid: str | None = Header(default=None, alias="X-Blog-Uid")):
    uid = blog_uid(x_blog_uid)
    s = _require_owner_session(sid, uid)
    if s["status"] in ("success", "expired", "canceled"):
        return {"status": s["status"], "uin": s.get("uin"), "name": s.get("name"),
                "avatar": s.get("avatar")}
    try:
        result = protocol.poll_qr(s["session"], s["qrsig"])
    except Exception as e:  # noqa: BLE001
        raise HTTPException(502, f"查询登录状态失败：{e}")
    if result["status"] == "success":
        s.update({"status": "success", "cookies": result["cookies"],
                  "uin": result["uin"], "name": result["name"],
                  "avatar": result["avatar"]})
        return {"status": "success", "uin": result["uin"],
                "name": result["name"], "avatar": result["avatar"]}
    s["status"] = result["status"]
    return {"status": result["status"]}


class JobStart(BaseModel):
    sid: str
    interval: float = config.DEFAULT_INTERVAL
    workers: int = 2


@router.post("/job/start")
def job_start(body: JobStart,
              x_blog_uid: str | None = Header(default=None, alias="X-Blog-Uid")):
    """开始后台抓取。同一博客用户最多并发 1 个抓取任务。"""
    uid = blog_uid(x_blog_uid)
    s = store.qr_sessions.get(body.sid)
    if not s or s.get("status") != "success":
        raise HTTPException(400, "登录态不存在或未完成扫码，请重新登录")
    if uid and s.get("blog_uid") and s["blog_uid"] != uid:
        raise HTTPException(403, "无权使用该扫码会话启动抓取")

    with store.store_lock:
        if store.running_count >= config.MAX_CONCURRENT:
            raise HTTPException(429, f"当前抓取任务较多（{store.running_count}/{config.MAX_CONCURRENT}），请稍后再试")
        if uid:
            user_running = sum(1 for j in store.jobs.values()
                               if j.get("blog_uid") == uid and j["status"] == "running")
            if user_running >= config.MAX_JOBS_PER_USER:
                raise HTTPException(429, "你已有抓取任务正在进行，请等待完成或取消后再试")
        store.running_count += 1

    interval = max(config.MIN_INTERVAL, min(float(body.interval or config.DEFAULT_INTERVAL), 10))
    workers = max(1, min(int(body.workers or 2), 3))
    jid = secrets.token_urlsafe(16)
    job = {
        "id": jid, "sid": body.sid, "blog_uid": uid,
        "uin": s["uin"], "name": s.get("name", ""),
        "status": "running", "total": None, "total_pages": None, "page": 0,
        "fetched": 0, "logs": [], "moments": [], "error": "",
        "pic_count": 0, "video_count": 0, "comment_count": 0,
        "range_from": "", "range_to": "", "files": [],
        "created_at": store.now(), "updated_at": store.now(),
        "finished_at": "", "workers": workers, "cancel": threading.Event(),
        "dir": os.path.join(config.DATA_DIR, jid),
    }
    store.jobs[jid] = job
    cookies, uin = s["cookies"], s["uin"]
    g_tk = protocol.bkn(cookies["p_skey"])
    threading.Thread(target=store.run_fetch,
                     args=(job, cookies, uin, g_tk, interval, workers), daemon=True).start()
    return {"jid": jid}


def _public_job(job: dict) -> dict:
    return {k: v for k, v in job.items()
            if k not in ("cancel", "dir", "moments", "sid", "blog_uid") and k != ""}


@router.get("/job/{jid}")
def job_status(jid: str,
               x_blog_uid: str | None = Header(default=None, alias="X-Blog-Uid")):
    job = _require_owner_job(jid, blog_uid(x_blog_uid))
    info = _public_job(job)
    info["logs"] = job["logs"][-30:]
    return info


@router.get("/job/{jid}/moments")
def job_moments(jid: str, offset: int = 0, limit: int = 50,
                x_blog_uid: str | None = Header(default=None, alias="X-Blog-Uid")):
    job = _require_owner_job(jid, blog_uid(x_blog_uid))
    moments = job.get("moments") or []
    limit = max(1, min(limit, 100))
    return {"total": len(moments), "offset": offset,
            "items": moments[offset:offset + limit]}


@router.post("/job/{jid}/cancel")
def job_cancel(jid: str,
               x_blog_uid: str | None = Header(default=None, alias="X-Blog-Uid")):
    job = _require_owner_job(jid, blog_uid(x_blog_uid))
    if job["status"] == "running":
        job["cancel"].set()
        return {"ok": True}
    return JSONResponse({"ok": False, "msg": "任务已结束"}, status_code=400)


@router.delete("/job/{jid}")
def job_delete(jid: str,
               x_blog_uid: str | None = Header(default=None, alias="X-Blog-Uid")):
    uid = blog_uid(x_blog_uid)
    job = store.jobs.get(jid)
    if not job:
        raise HTTPException(404, "任务不存在或已过期清理")
    if uid and job.get("blog_uid") and job["blog_uid"] != uid:
        raise HTTPException(403, "无权删除该任务")
    if job["status"] == "running":
        job["cancel"].set()
    shutil.rmtree(job["dir"], ignore_errors=True)
    store.jobs.pop(jid, None)
    return {"ok": True}


@router.get("/job/{jid}/download")
def job_download(jid: str, format: str = Query("json", pattern="^(json|html|csv)$"),
                 x_blog_uid: str | None = Header(default=None, alias="X-Blog-Uid")):
    job = _require_owner_job(jid, blog_uid(x_blog_uid))
    if job["status"] not in ("done", "canceled", "failed") or not job.get("moments"):
        raise HTTPException(400, "任务尚未完成，暂无可下载的数据")
    file_map = {
        "json": ("data.json", "application/json; charset=utf-8", "json"),
        "html": ("index.html", "text/html; charset=utf-8", "html"),
        "csv": ("moments.csv", "text/csv; charset=utf-8", "csv"),
    }
    fname, ctype, ext = file_map[format]
    path = os.path.join(job["dir"], fname)
    if not os.path.exists(path):
        raise HTTPException(404, "导出文件不存在")
    safe_name = re.sub(r'[\\/:*?"<>|\s]+', "_", job.get("name") or job["uin"])
    stamp = datetime.now().strftime("%Y%m%d")
    return FileResponse(path, media_type=ctype,
                        filename=f"{safe_name}_{job['uin']}_qqspace_{stamp}.{ext}")


@router.get("/proxy")
def proxy_media(url: str = Query(..., min_length=10),
                type: str = Query("img", pattern="^(img|video|emoji)$")):
    """代理 QQ 域名下的媒体，带正确 Referer 流式返回（防 SSRF）。"""
    from .formatting import is_qq_media

    if not is_qq_media(url):
        raise HTTPException(403, "仅允许代理 QQ 域名下的媒体资源")
    headers = {
        "User-Agent": config.UA,
        "Referer": "https://qzs.qq.com/",
        "Accept": "*/*",
    }
    try:
        r = requests.get(url, headers=headers, timeout=config.PROXY_FETCH_TIMEOUT, stream=True)
    except requests.RequestException as e:
        raise HTTPException(502, f"代理请求失败：{e}")
    if r.status_code != 200:
        raise HTTPException(502, f"源站返回 {r.status_code}")

    ctype = r.headers.get("Content-Type", "application/octet-stream")
    if type == "img" and not (ctype.startswith("image/") or ctype.startswith("application/octet")):
        raise HTTPException(403, f"非图片资源：{ctype}")
    if type == "video" and not (ctype.startswith("video/") or ctype.startswith("application/octet")):
        raise HTTPException(403, f"非视频资源：{ctype}")

    def stream():
        try:
            for chunk in r.iter_content(chunk_size=8192):
                if chunk:
                    yield chunk
        finally:
            r.close()

    return StreamingResponse(stream(), media_type=ctype,
                             headers={"Cache-Control": "public, max-age=3600"})
