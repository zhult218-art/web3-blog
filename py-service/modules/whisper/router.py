# -*- coding: utf-8 -*-
"""whisper 路由：统一前缀 /api/whisper（前端经 /whisper 代理重写到此前缀）。"""
from __future__ import annotations

from fastapi import APIRouter, File, HTTPException, UploadFile

from . import service

router = APIRouter(prefix="/api/whisper", tags=["whisper"])


@router.post("/transcribe")
async def transcribe(file: UploadFile = File(...), language: str = "zh"):
    if not service.available():
        raise HTTPException(status_code=503, detail="faster-whisper 未安装")
    data = await file.read()
    return {"text": service.transcribe(data, language=language)}
