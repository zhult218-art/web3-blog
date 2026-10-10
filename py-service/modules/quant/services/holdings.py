# -*- coding: utf-8 -*-
from __future__ import annotations

import os
import sys
import json
import random
import time
import uuid
import logging
import re
import asyncio
import hashlib
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timedelta
from pathlib import Path
from typing import Optional, List, Dict, Any
from dataclasses import dataclass, field, asdict
from enum import Enum

import numpy as np
import pandas as pd
import requests
from fastapi import HTTPException, Query, Request, Body
from fastapi.responses import JSONResponse, StreamingResponse
from pydantic import BaseModel

logger = logging.getLogger("quant")

# ============================================================
# 用户持仓管理（user_holdings 表，MySQL）
# JWT 鉴权：从 Authorization 头解析 userId / username / role
# 普通用户只能看自己的持仓，管理员可查看所有用户
# ============================================================

import jwt as _pyjwt
import pymysql as _pymysql

def _get_jwt_claims(request: Request) -> dict:
    """从 FastAPI Request 的 Authorization 头解析 JWT claims。失败抛 HTTPException(401)。"""
    auth = request.headers.get("Authorization", "")
    if not auth.startswith("Bearer "):
        raise HTTPException(401, "未登录")
    token = auth[7:]
    secret = os.environ.get("JWT_SECRET", "")
    if not secret:
        raise HTTPException(500, "JWT_SECRET 未配置")
    try:
        return _pyjwt.decode(token, secret, algorithms=["HS256"])
    except Exception:
        raise HTTPException(401, "令牌已过期或无效")

def _holdings_conn():
    """获取 MySQL 连接（web3_portal 库）"""
    return _pymysql.connect(
        host=os.environ.get("MYSQL_HOST", "127.0.0.1"),
        port=int(os.environ.get("MYSQL_PORT", "3306")),
        user=os.environ.get("MYSQL_USERNAME", "root"),
        password=os.environ.get("MYSQL_PASSWORD", "root"),
        database=os.environ.get("MYSQL_DATABASE", "web3_portal"),
        charset="utf8mb4",
        cursorclass=_pymysql.cursors.DictCursor,
    )

def _row_to_dict(row: dict) -> dict:
    """将 MySQL 行转为前端需要的格式"""
    return {
        "id": row["id"],
        "user_id": row["user_id"],
        "username": row.get("username", ""),
        "stock_code": row["stock_code"],
        "stock_name": row.get("stock_name", ""),
        "shares": row["shares"],
        "cost_price": float(row["cost_price"]) if row.get("cost_price") else 0,
        "buy_date": str(row["buy_date"]) if row.get("buy_date") else None,
        "notes": row.get("notes", ""),
        "created_at": str(row["created_at"]) if row.get("created_at") else "",
        "updated_at": str(row["updated_at"]) if row.get("updated_at") else "",
    }
