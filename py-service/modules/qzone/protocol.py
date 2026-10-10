# -*- coding: utf-8 -*-
"""qzone 协议层（对应原 GetQzonehistory 的 LoginUtil / GetAllMomentsUtil）。"""
from __future__ import annotations

import json
import random
import re
import time
from datetime import datetime

import requests

from . import config


def bkn(p_skey: str) -> int:
    """由 p_skey 计算 g_tk。"""
    t = 5381
    for ch in p_skey:
        t += (t << 5) + ord(ch)
    return t & 2147483647


def ptqr_token(qrsig: str) -> int:
    """由 qrsig 计算 ptqrtoken。"""
    e = 0
    for ch in qrsig:
        e += (e << 5) + ord(ch)
    return 2147483647 & e


def fetch_qr() -> tuple[requests.Session, str, bytes]:
    """取登录二维码：返回 (session, qrsig, png 字节)。"""
    sess = requests.Session()
    sess.headers.update({
        "User-Agent": config.UA,
        "Referer": "https://qzs.qq.com/",
    })
    url = config.QR_SHOW_URL.format(t=random.random())
    r = sess.get(url, timeout=15)
    r.raise_for_status()
    qrsig = sess.cookies.get("qrsig") or requests.utils.dict_from_cookiejar(r.cookies).get("qrsig")
    if not qrsig:
        raise RuntimeError("二维码响应中缺少 qrsig")
    if not r.content:
        raise RuntimeError("二维码图片为空")
    return sess, qrsig, r.content


def poll_qr(sess: requests.Session, qrsig: str) -> dict:
    """轮询二维码状态；成功时完成 check_sig 换 cookie。"""
    token = ptqr_token(qrsig)
    url = config.QR_LOGIN_URL.format(token=token, ts=int(time.time()))
    poll_headers = {
        "Referer": "https://xui.ptlogin2.qq.com/cgi-bin/xlogin?appid=549000912&daid=5&pt_skey_valid=0&pt_uistyle=40",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "Accept-Language": "zh-CN,zh;q=0.9",
    }
    r = sess.get(url, timeout=15, headers=poll_headers)
    text = r.content.decode("utf-8", errors="replace")

    code = None
    redirect = ""
    login_nick = ""
    head = text.find("ptuiCB('")
    if head >= 0:
        body_start = head + len("ptuiCB('")
        tail = text.find("')", body_start)
        if tail > body_start:
            body = text[body_start:tail]
            parts = body.split("','")
            if len(parts) >= 1:
                code = parts[0]
            if len(parts) >= 3:
                redirect = parts[2]
            if len(parts) >= 6:
                login_nick = parts[5]

    if code == "0":
        ms = re.search(r"ptsigx=([^&]+)", redirect)
        if not ms:
            raise RuntimeError("登录成功但响应中未找到 ptsigx")
        sigx = ms.group(1)
        uin_raw = requests.utils.dict_from_cookiejar(r.cookies).get("uin", "")
        cr = sess.get(config.CHECK_SIG_URL.format(uin=uin_raw, sigx=sigx),
                      allow_redirects=False, timeout=15)
        target = requests.utils.dict_from_cookiejar(cr.cookies)
        if not target.get("p_skey") or not target.get("uin"):
            raise RuntimeError("check_sig 未返回有效登录态")
        uin = re.sub(r"o0*", "", target["uin"])
        if not login_nick:
            for ck in sess.cookies:
                if ck.name.startswith("ptnick_"):
                    login_nick = ck.value
                    break
        return {"status": "success", "cookies": target, "uin": uin,
                "name": login_nick,
                "avatar": f"https://q1.qlogo.cn/g?b=qq&nk={uin}&s=100"}
    if code == "67":
        return {"status": "scanned"}
    if code == "68":
        return {"status": "expired"}
    if code == "65":
        return {"status": "canceled"}
    return {"status": "waiting"}


def fetch_page(cookies: dict, uin: str, g_tk: int, pos: int, num: int) -> dict:
    """抓取一页说说，返回解析后的 JSON dict。"""
    headers = {
        "accept": "*/*",
        "accept-language": "zh-CN,zh;q=0.9",
        "cookie": (f'uin={cookies.get("p_uin", cookies.get("uin"))};'
                   f'skey={cookies.get("skey")};p_uin={cookies.get("p_uin")};'
                   f'pt4_token={cookies.get("pt4_token")};p_skey={cookies.get("p_skey")}'),
        "referer": f"https://user.qzone.qq.com/{uin}/main",
        "user-agent": config.UA,
    }
    params = {
        "uin": uin, "ftype": "0", "sort": "0", "pos": str(pos),
        "num": str(num), "replynum": "100", "g_tk": str(g_tk),
        "callback": "_preloadCallback", "code_version": "1",
        "format": "jsonp", "need_private_comment": "1",
    }
    r = requests.get(config.MSGLIST_URL, headers=headers, params=params, timeout=20)
    raw = re.sub(r"^_preloadCallback\((.*)\);?$", r"\1", r.text, flags=re.S)
    data = json.loads(raw)
    if data.get("code") != 0:
        raise RuntimeError(f'接口返回错误 code={data.get("code")} message={data.get("message")}')
    return data


def fmt_ts(ts) -> str:
    try:
        return datetime.fromtimestamp(int(ts)).strftime("%Y-%m-%d %H:%M:%S")
    except (ValueError, TypeError):
        return ""
