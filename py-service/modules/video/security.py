# -*- coding: utf-8 -*-
"""
security.py —— 管理员 Token 校验依赖
上传 / 删除 / 任务查询等管理接口统一使用；token 错误返回 401。
"""
from fastapi import Header, HTTPException
from . import config


def verify_admin_token(x_admin_token: str = Header(default="")):
    """校验请求头 X-Admin-Token；部署后请用环境变量 VIDEO_ADMIN_TOKEN 覆盖默认值"""
    if not x_admin_token or x_admin_token != config.ADMIN_TOKEN:
        raise HTTPException(status_code=401, detail="管理员token无效")
