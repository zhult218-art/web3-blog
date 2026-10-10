# -*- coding: utf-8 -*-
"""qzone 模块配置：常量 / 协议 URL / 运行数据目录。"""
from __future__ import annotations

import re
from datetime import timedelta
from pathlib import Path

# 运行数据统一放在 py-service/data/qzone
DATA_DIR = Path(__file__).resolve().parent.parent.parent / "data" / "qzone"

QR_TTL = timedelta(minutes=10)        # 扫码会话存活时间
JOB_TTL = timedelta(hours=24)         # 抓取结果临时保存时间
REAP_INTERVAL = 300                   # 清理线程扫描间隔（秒）
PAGE_SIZE = 30
MIN_INTERVAL = 0.8                    # 页间隔下限（秒）
DEFAULT_INTERVAL = 1.5
MAX_ATTEMPTS = 5
MAX_CONCURRENT = 4                    # 全站同时进行的抓取任务上限
MAX_JOBS_PER_USER = 1                 # 单用户同时进行的抓取任务上限
MAX_QR_SESSIONS = 8                   # 同时存活的扫码会话上限

# QQ 表情：[em]eN[/em] → qzonestyle.gtimg.cn/qzone/em/eN.gif
EMOJI_RE = re.compile(r"\[em\]e(\d+)\[/em\]")
EMOJI_URL_TPL = "https://qzonestyle.gtimg.cn/qzone/em/e{}.gif"

# QQ 媒体防盗链白名单：只代理这些域名后缀，防止 SSRF
QQ_MEDIA_HOST_SUFFIXES = (".qq.com", ".gtimg.cn", ".qlogo.cn")
PROXY_FETCH_TIMEOUT = 20

UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")

QR_SHOW_URL = ("https://ssl.ptlogin2.qq.com/ptqrshow?appid=549000912&e=2&l=M"
               "&s=3&d=72&v=4&t={t}&daid=5&pt_3rd_aid=0")
QR_LOGIN_URL = ("https://ssl.ptlogin2.qq.com/ptqrlogin?u1=https%3A%2F%2Fqzs.qq.com"
                "%2Fqzone%2Fv5%2Floginsucc.html%3Fpara%3Dizone&ptqrtoken={token}"
                "&ptredirect=0&h=1&t=1&g=1&from_ui=1&ptlang=2052&action=0-0-{ts}"
                "&js_ver=20032614&js_type=1&login_sig=&pt_uistyle=40"
                "&aid=549000912&daid=5&")
CHECK_SIG_URL = ("https://ptlogin2.qzone.qq.com/check_sig?pttype=1&uin={uin}"
                 "&service=ptqrlogin&nodirect=0&ptsigx={sigx}"
                 "&s_url=https%3A%2F%2Fqzs.qq.com%2Fqzone%2Fv5%2Floginsucc.html"
                 "%3Fpara%3Dizone&f_url=&ptlang=2052&ptredirect=100"
                 "&aid=549000912&daid=5&j_later=0&low_login_hour=0&regmaster=0"
                 "&pt_login_type=3&pt_aid=0&pt_aaid=16&pt_light=0&pt_3rd_aid=0")
MSGLIST_URL = ("https://user.qzone.qq.com/proxy/domain/taotao.qq.com"
               "/cgi-bin/emotion_cgi_msglist_v6")
