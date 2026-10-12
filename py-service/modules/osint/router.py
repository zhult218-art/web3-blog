# -*- coding: utf-8 -*-
"""OSINT 路由：模块列表 / 扫描任务（创建/状态/结果/停止 + WebSocket 实时日志）。"""
from __future__ import annotations

import asyncio

from fastapi import APIRouter, HTTPException, Query, WebSocket, WebSocketDisconnect
from pydantic import BaseModel

from . import engine

router = APIRouter(prefix="/api/osint", tags=["osint"])


@router.get("/capabilities")
def get_capabilities():
    from . import capabilities
    return capabilities.summary()


@router.get("/modules")
def list_modules():
    return {"modules": engine.list_modules()}


class ScanRequest(BaseModel):
    target: str
    modules: list[str] = []   # 空列表 = 全部模块


@router.post("/scans")
def create_scan(body: ScanRequest):
    try:
        job = engine.create_scan(body.target, body.modules or None)
    except ValueError as exc:
        raise HTTPException(400, str(exc))
    return job.to_dict()


@router.get("/scans")
def list_scans():
    return {"scans": [j.to_dict() for j in engine.list_jobs()]}


@router.get("/scans/{jid}")
def scan_status(jid: str):
    job = engine.get_job(jid)
    if job is None:
        raise HTTPException(404, "扫描任务不存在")
    return job.to_dict()


@router.get("/scans/{jid}/summary")
def scan_summary(jid: str):
    job = engine.get_job(jid)
    if job is None:
        raise HTTPException(404, "扫描任务不存在")
    return job.summary()


@router.post("/scans/{jid}/stop")
def scan_stop(jid: str):
    job = engine.stop_job(jid)
    if job is None:
        raise HTTPException(404, "扫描任务不存在")
    return job.to_dict()


@router.get("/scans/{jid}/logs")
def scan_logs(jid: str, offset: int = Query(0, ge=0)):
    job = engine.get_job(jid)
    if job is None:
        raise HTTPException(404, "扫描任务不存在")
    logs = job.logs[offset:]
    return {"offset": offset, "next": len(job.logs), "status": job.status, "logs": logs}


@router.websocket("/scans/{jid}/ws")
async def scan_ws(websocket: WebSocket, jid: str):
    await websocket.accept()
    job = engine.get_job(jid)
    if job is None:
        await websocket.send_json({"error": "扫描任务不存在"})
        await websocket.close()
        return

    idx = 0
    result_idx = 0
    last_status = None
    try:
        while True:
            # 推送新日志
            if idx < len(job.logs):
                for line in job.logs[idx:]:
                    await websocket.send_json({"type": "log", "line": line})
                idx = len(job.logs)
            # 推送新结果
            if result_idx < len(job.results):
                for r in job.results[result_idx:]:
                    await websocket.send_json({"type": "result", "data": r})
                result_idx = len(job.results)
            # 推送状态变更
            if job.status != last_status:
                await websocket.send_json({"type": "status", "status": job.status,
                                          "total": len(job.results)})
                last_status = job.status
            if job.status in ("done", "failed", "stopped"):
                await asyncio.sleep(0.3)
                break
            await asyncio.sleep(0.5)
    except WebSocketDisconnect:
        return
