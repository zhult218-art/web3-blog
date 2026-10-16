# -*- coding: utf-8 -*-
"""
core/auth.py —— 统一鉴权依赖（wifi / osint 等敏感模块共用）

与 Java 服务共用同一套 JWT（HS256，JWT_SECRET 环境变量），并兼容运维直连的
X-Admin-Token（与 video 模块的 VIDEO_ADMIN_TOKEN 同源）：

1. 请求头 `Authorization: Bearer <JWT>`（浏览器普通请求，由前端统一注入）；
2. 查询参数 `?token=<JWT>`（浏览器 WebSocket 无法自定义请求头，只能走 query）；
3. 请求头 `X-Admin-Token: <VIDEO_ADMIN_TOKEN>`（运维脚本 / 服务间直连）。

权限收口与 common-security/AuthUtils.requirePerm 语义一致：
- ROLE_ADMIN 恒通过；
- 令牌 perms 声明含 `-<perm>`（被管理员收回）→ 403；
- 含 `<perm>`（显式授权）→ 通过；
- 都没有 → 按 default_allow 判定（wifi/osint 为 False，须管理员显式授权）。

安全约束：JWT_SECRET 未配置时拒绝访问（fail-closed），避免敏感模块变成裸奔接口。
"""
from __future__ import annotations

import hmac
import os

import jwt
from fastapi import Header, HTTPException, Query

# 与模块权限码保持一致（前端 utils/permissions.js 的 perm 值）
WIFI_PERM = "wifi"
OSINT_PERM = "osint"


def _jwt_secret() -> str:
    return (os.environ.get("JWT_SECRET") or "").strip()


def _admin_token() -> str:
    return (os.environ.get("VIDEO_ADMIN_TOKEN") or "").strip()


def decode_jwt(token: str) -> dict:
    """HS256 验签 + 过期校验；失败抛 401。"""
    secret = _jwt_secret()
    if not secret:
        raise HTTPException(status_code=503, detail="服务端未配置 JWT_SECRET，已拒绝访问")
    try:
        return jwt.decode(token, secret, algorithms=["HS256"])
    except jwt.ExpiredSignatureError as exc:
        raise HTTPException(status_code=401, detail="登录已过期，请重新登录") from exc
    except jwt.PyJWTError as exc:
        raise HTTPException(status_code=401, detail="凭证无效，请重新登录") from exc


def check_perm(claims: dict, perm: str, default_allow: bool = False) -> None:
    """复核细粒度权限（与 Java AuthUtils.requirePerm 相同语义）。"""
    if claims.get("authorities") == "ROLE_ADMIN":
        return
    perms = (claims.get("perms") or "").strip()
    if perms:
        for code in (p.strip() for p in perms.split(",")):
            if code == f"-{perm}":
                raise HTTPException(status_code=403, detail="该操作已被管理员限制")
            if code == perm:
                return
    if not default_allow:
        raise HTTPException(status_code=403, detail="无权限：请联系管理员开通")


def _match_admin_token(header_value: str) -> bool:
    configured = _admin_token()
    if not configured or not header_value:
        return False
    return hmac.compare_digest(header_value, configured)


def require_auth(perm: str | None = None, default_allow: bool = False):
    """生成一个鉴权依赖；perm 非空时叠加细粒度权限校验。

    浏览器 WebSocket 场景无法携带自定义请求头，故同时接受 `?token=` 查询参数。
    """

    async def _dependency(
        authorization: str = Header(default=""),
        x_admin_token: str = Header(default=""),
        token: str = Query(default=""),
    ) -> dict:
        # 1) 运维/服务间直连：X-Admin-Token 与 VIDEO_ADMIN_TOKEN 一致即放行
        if _match_admin_token(x_admin_token):
            return {"via": "admin-token"}
        # 2) 普通请求：Authorization 头优先，其次 WebSocket 的 ?token=
        raw = ""
        auth = authorization or ""
        if auth.lower().startswith("bearer "):
            raw = auth[7:].strip()
        if not raw and token:
            raw = token.strip()
        if not raw:
            raise HTTPException(status_code=401, detail="未登录：请先登录后再使用该功能")
        claims = decode_jwt(raw)
        if perm:
            check_perm(claims, perm, default_allow)
        return claims

    return _dependency
