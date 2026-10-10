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

from ..services.holdings import _get_jwt_claims, _holdings_conn, _row_to_dict


from fastapi import APIRouter

router = APIRouter()

@router.get("/api/quant/my-holdings")
def get_my_holdings(request: Request):
    """获取当前用户的持仓列表"""
    claims = _get_jwt_claims(request)
    user_id = claims.get("userId")
    is_admin = "ROLE_ADMIN" in (claims.get("authorities", "") or "")
    username = claims.get("username", "")

    conn = _holdings_conn()
    try:
        cur = conn.cursor()
        if is_admin:
            # 管理员：查看所有用户持仓
            cur.execute("SELECT * FROM user_holdings ORDER BY user_id, stock_code")
        else:
            cur.execute("SELECT * FROM user_holdings WHERE user_id = %s ORDER BY stock_code", (user_id,))
        rows = cur.fetchall()
        items = [_row_to_dict(r) for r in rows]
        return {"code": 0, "data": {"holdings": items, "is_admin": is_admin, "username": username}}
    except Exception as e:
        logger.warning("get_my_holdings failed: %s", e)
        raise HTTPException(500, str(e))
    finally:
        conn.close()

@router.post("/api/quant/my-holdings")
def save_holding(request: Request, body: dict = Body(...)):
    """新增或更新持仓（同一 user_id + stock_code 则 upsert）"""
    claims = _get_jwt_claims(request)
    user_id = claims.get("userId")
    username = claims.get("username", "")

    code = str(body.get("stock_code", "")).strip()
    if not code:
        raise HTTPException(400, "股票代码不能为空")

    name = str(body.get("stock_name", "")).strip()
    shares = int(body.get("shares", 0) or 0)
    cost = float(body.get("cost_price", 0) or 0)
    buy_date = body.get("buy_date")
    notes = str(body.get("notes", "")).strip()

    conn = _holdings_conn()
    try:
        cur = conn.cursor()
        cur.execute("""
            INSERT INTO user_holdings (user_id, username, stock_code, stock_name, shares, cost_price, buy_date, notes)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE
                stock_name = VALUES(stock_name),
                shares = VALUES(shares),
                cost_price = VALUES(cost_price),
                buy_date = VALUES(buy_date),
                notes = VALUES(notes)
        """, (user_id, username, code, name, shares, cost, buy_date, notes))
        conn.commit()
        return {"code": 0, "data": {"ok": True}}
    except Exception as e:
        logger.warning("save_holding failed: %s", e)
        raise HTTPException(500, str(e))
    finally:
        conn.close()

@router.delete("/api/quant/my-holdings/{holding_id}")
def delete_holding(request: Request, holding_id: int):
    """删除一条持仓（只能删自己的）"""
    claims = _get_jwt_claims(request)
    user_id = claims.get("userId")
    is_admin = "ROLE_ADMIN" in (claims.get("authorities", "") or "")

    conn = _holdings_conn()
    try:
        cur = conn.cursor()
        if is_admin:
            cur.execute("DELETE FROM user_holdings WHERE id = %s", (holding_id,))
        else:
            cur.execute("DELETE FROM user_holdings WHERE id = %s AND user_id = %s", (holding_id, user_id))
        conn.commit()
        if cur.rowcount == 0:
            raise HTTPException(404, "持仓不存在或无权删除")
        return {"code": 0, "data": {"ok": True}}
    except HTTPException:
        raise
    except Exception as e:
        logger.warning("delete_holding failed: %s", e)
        raise HTTPException(500, str(e))
    finally:
        conn.close()
