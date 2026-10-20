# -*- coding: utf-8 -*-
"""
GhostTrack 路由（/api/ghosttrack）。

端点：
  POST /api/ghosttrack/ip        查任意 IP 或「你的公网 IP」的地理 / ISP / 时区
  POST /api/ghosttrack/phone     手机号归属地 / 运营商 / 类型 / 时区（纯离线）
  POST /api/ghosttrack/username  用户名跨站存在性检测（白名单站点并发）
  GET  /api/ghosttrack/sites     检测站点清单（前端展示用）
"""
from __future__ import annotations

from typing import Any, Dict

from fastapi import APIRouter, HTTPException, Request, status
from pydantic import BaseModel, Field

from . import ip_lookup, phone_lookup, username_enum

# 免鉴权：与 scrape / video 等在线工具一致，页面侧由前端 perm='tools' 网关。
router = APIRouter(prefix="/api/ghosttrack", tags=["ghosttrack"])


class IpBody(BaseModel):
    ip: str = Field(default="", description="留空则查询你自己的公网 IP")


class PhoneBody(BaseModel):
    number: str = Field(..., description="手机号，可带 + 国际前缀")
    region: str = Field(default="CN", description="无 + 前缀时的默认地区码（两位）")


class NameBody(BaseModel):
    username: str = Field(..., description="用户名（字母 / 数字 / . _ -）")


def _client_ip(request: Request) -> str:
    """取调用方 IP：优先 X-Forwarded-For 首个，其次 X-Real-IP，兜底 socket 地址。"""
    xff = request.headers.get("x-forwarded-for", "")
    if xff:
        return xff.split(",")[0].strip()
    return request.headers.get("x-real-ip", "") or (request.client.host if request.client else "")


@router.get("/sites")
def sites() -> Dict[str, Any]:
    items = username_enum.list_sites()
    return {"ok": True, "sites": items, "reliable": sum(1 for s in items if s["reliable"])}


@router.post("/ip")
def ip_track(body: IpBody, request: Request) -> Dict[str, Any]:
    try:
        return ip_lookup.lookup(body.ip, _client_ip(request))
    except ValueError as exc:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status.HTTP_502_BAD_GATEWAY, str(exc)) from exc


@router.post("/phone")
def phone_track(body: PhoneBody) -> Dict[str, Any]:
    try:
        return phone_lookup.analyze(body.number, body.region)
    except ValueError as exc:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, str(exc)) from exc


@router.post("/username")
async def username_track(body: NameBody) -> Dict[str, Any]:
    name = (body.username or "").strip()
    if not name:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "请输入用户名")
    if not username_enum.NAME_RE.match(name):
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "用户名仅支持字母 / 数字 / . _ -，最长 40 位")
    try:
        items = await username_enum.check(name)
    except ValueError as exc:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, str(exc)) from exc
    return {
        "ok": True,
        "username": name,
        "count": len(items),
        "found": sum(1 for i in items if i["found"] == "yes"),
        "items": items,
    }
