# -*- coding: utf-8 -*-
"""wifi 路由：能力 / 扫描 / 网卡 / 配置文件 / 攻击引擎会话（含 WebSocket 实时日志）。"""
from __future__ import annotations

import asyncio
import os

from fastapi import APIRouter, File, Form, HTTPException, Query, UploadFile, WebSocket, WebSocketDisconnect
from pydantic import BaseModel

from . import capabilities, engine, wincrack
from .capabilities import IS_LINUX, IS_WINDOWS

router = APIRouter(prefix="/api/wifi", tags=["wifi"])


@router.get("/capabilities")
def get_capabilities():
    return capabilities.summary()


@router.get("/scan")
def scan(iface: str | None = None):
    try:
        if IS_WINDOWS:
            from . import windows
            return {"platform": "windows", "networks": windows.scan_networks()}
        if IS_LINUX:
            from . import linuxscan
            return {"platform": "linux", "networks": linuxscan.scan_networks(iface)}
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(500, f"扫描失败：{exc}")
    raise HTTPException(501, "当前平台不支持扫描")


@router.get("/interfaces")
def get_interfaces():
    return {"adapters": capabilities.adapters()}


@router.get("/profiles")
def get_profiles():
    if IS_WINDOWS:
        from . import windows
        return {"profiles": windows.list_profiles()}
    raise HTTPException(501, "配置文件枚举仅在 Windows 提供")


@router.get("/profiles/{name}")
def get_profile(name: str):
    if IS_WINDOWS:
        from . import windows
        return windows.profile_detail(name)
    raise HTTPException(501, "配置文件详情仅在 Windows 提供")


@router.get("/engines")
def get_engines():
    return {"engines": capabilities.engines()}


class SessionRequest(BaseModel):
    engine: str
    args: list[str] = []


@router.post("/sessions")
def create_session(body: SessionRequest):
    try:
        session = engine.start_session(body.engine, body.args)
    except PermissionError as exc:
        raise HTTPException(403, str(exc))
    except ValueError as exc:
        raise HTTPException(400, str(exc))
    return session.to_dict()


@router.get("/sessions")
def list_sessions():
    return {"sessions": [s.to_dict() for s in engine.list_sessions()]}


@router.get("/sessions/{sid}")
def session_status(sid: str):
    s = engine.get_session(sid)
    if s is None:
        raise HTTPException(404, "会话不存在")
    return s.to_dict()


@router.get("/sessions/{sid}/logs")
def session_logs(sid: str, offset: int = Query(0, ge=0)):
    s = engine.get_session(sid)
    if s is None:
        raise HTTPException(404, "会话不存在")
    logs = s.logs[offset:]
    return {"offset": offset, "next": len(s.logs), "status": s.status, "logs": logs}


@router.post("/sessions/{sid}/stop")
def stop_session(sid: str):
    try:
        s = engine.stop_session(sid)
    except KeyError:
        raise HTTPException(404, "会话不存在")
    return s.to_dict()


@router.websocket("/sessions/{sid}/ws")
async def session_ws(websocket: WebSocket, sid: str):
    await websocket.accept()
    s = engine.get_session(sid)
    if s is None:
        await websocket.send_json({"error": "会话不存在"})
        await websocket.close()
        return

    idx = 0
    last_status = None
    try:
        while True:
            if idx < len(s.logs):
                for line in s.logs[idx:]:
                    await websocket.send_json({"type": "log", "line": line})
                idx = len(s.logs)
            if s.status != last_status:
                await websocket.send_json({"type": "status", "status": s.status,
                                          "returncode": s.returncode})
                last_status = s.status
            if s.status in ("done", "failed") and idx >= len(s.logs):
                await asyncio.sleep(0.3)
                break
            await asyncio.sleep(0.4)
    except WebSocketDisconnect:
        return


# ============================================================
# Windows 邪恶孪生热点（Rogue AP）
# ============================================================
class RogueStartRequest(BaseModel):
    ssid: str
    key: str
    template: str = "wifi_login"   # wifi_login / firmware / oauth


@router.get("/rogue/status")
def rogue_status():
    from . import winrogue

    return winrogue.manager.status()


@router.post("/rogue/start")
def rogue_start(body: RogueStartRequest):
    from . import winrogue

    try:
        return winrogue.manager.start(body.ssid, body.key, body.template)
    except PermissionError as exc:
        raise HTTPException(403, str(exc))
    except (RuntimeError, ValueError) as exc:
        raise HTTPException(400, str(exc))


@router.post("/rogue/stop")
def rogue_stop():
    from . import winrogue

    return winrogue.manager.stop()


# ============================================================
# WPA/WPA2 离线破解
# ============================================================
@router.get("/crack/hashcat")
def crack_hashcat():
    return {"hashcat": wincrack.find_hashcat()}


class CrackJobRequest(BaseModel):
    hash_text: str = ""
    hash_path: str = ""
    wordlist_path: str = ""
    engine: str = "auto"
    crack_mode: str = "dictionary"    # dictionary / rule / brute
    rule_path: str = ""              # 规则文件路径（rule 模式）
    mask: str = ""                   # hashcat 掩码（brute 模式）


@router.post("/crack/jobs")
def crack_create(body: CrackJobRequest):
    hash_text = body.hash_text or ""
    if not hash_text.strip() and body.hash_path.strip():
        try:
            with open(body.hash_path, "r", encoding="utf-8", errors="ignore") as fh:
                hash_text = fh.read()
        except OSError as exc:
            raise HTTPException(400, f"哈希文件读取失败：{exc}")
    try:
        job = wincrack.create_job(
            hash_text, body.wordlist_path, body.engine,
            body.crack_mode, body.rule_path, body.mask,
        )
    except ValueError as exc:
        raise HTTPException(400, str(exc))
    return job.to_dict()


@router.post("/crack/upload")
async def crack_upload(
    hash_file: UploadFile = File(...),
    wordlist: UploadFile = File(...),
    engine: str = Form("auto"),
):
    """上传 22000 哈希文件与字典文件并立即开始破解。"""
    os.makedirs(wincrack.DATA_DIR, exist_ok=True)
    stamp = asyncio.get_event_loop().time()
    base = f"{int(stamp * 1000)}"

    hash_path = os.path.join(wincrack.DATA_DIR, f"{base}_hashes.22000")
    with open(hash_path, "wb") as fh:
        fh.write(await hash_file.read())

    wl_path = os.path.join(wincrack.DATA_DIR, f"{base}_wordlist.txt")
    with open(wl_path, "wb") as fh:
        fh.write(await wordlist.read())

    try:
        with open(hash_path, "r", encoding="utf-8", errors="ignore") as fh:
            hash_text = fh.read()
        job = wincrack.create_job(hash_text, wl_path, engine)
    except ValueError as exc:
        raise HTTPException(400, str(exc))
    return job.to_dict()


@router.get("/crack/jobs")
def crack_list():
    return {"jobs": [j.to_dict() for j in wincrack.list_jobs()]}


@router.get("/crack/jobs/{jid}")
def crack_get(jid: str):
    job = wincrack.get_job(jid)
    if job is None:
        raise HTTPException(404, "任务不存在")
    return job.to_dict()


@router.post("/crack/jobs/{jid}/stop")
def crack_stop(jid: str):
    job = wincrack.stop_job(jid)
    if job is None:
        raise HTTPException(404, "任务不存在")
    return job.to_dict()


@router.websocket("/crack/jobs/{jid}/ws")
async def crack_ws(websocket: WebSocket, jid: str):
    await websocket.accept()
    job = wincrack.get_job(jid)
    if job is None:
        await websocket.send_json({"error": "任务不存在"})
        await websocket.close()
        return

    idx = 0
    last_status = None
    try:
        while True:
            if idx < len(job.logs):
                for line in job.logs[idx:]:
                    await websocket.send_json({"type": "log", "line": line})
                idx = len(job.logs)
            if job.status != last_status:
                await websocket.send_json({"type": "status", "status": job.status,
                                          "tried": job.tried, "found": job.found})
                last_status = job.status
            if job.status in ("done", "exhausted", "failed", "stopped"):
                await asyncio.sleep(0.3)
                break
            await asyncio.sleep(0.6)
    except WebSocketDisconnect:
        return
