# -*- coding: utf-8 -*-
"""
时光回溯 · QQ 空间说说备份服务（qzone-service, 端口 9012）

由 GetQzonehistory（https://github.com/LibraHp/GetQzonehistory）的登录与
emotion_cgi_msglist_v6 抓取协议重写而来。原项目的阻塞式模块导入、input()
交互、sys.exit、PyQt 终端二维码等在 Web 场景下全部剔除，改为：
  扫码会话（内存）→ 后台线程分页抓取 → JSON / HTML / CSV 导出 → TTL 自动清理。

仅监听 127.0.0.1，由前端 Vite /qzone 代理对外提供同源访问。
"""
import base64
import csv
import html
import io
import json
import math
import os
import random
import re
import shutil
import threading
import time
import secrets
from datetime import datetime, timedelta
from urllib.parse import quote as url_quote

import requests
import uvicorn
from fastapi import FastAPI, Header, HTTPException, Query, Request
from fastapi.responses import FileResponse, JSONResponse, StreamingResponse
from pydantic import BaseModel

# ============================================================
# 常量
# ============================================================
HOST = "127.0.0.1"
PORT = 9012
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, "data")
QR_TTL = timedelta(minutes=10)        # 扫码会话存活时间
JOB_TTL = timedelta(hours=24)         # 抓取结果临时保存时间
REAP_INTERVAL = 300                   # 清理线程扫描间隔（秒）
PAGE_SIZE = 30
MIN_INTERVAL = 0.8                    # 页间隔下限（秒），避免对腾讯服务器造成压力
DEFAULT_INTERVAL = 1.5
MAX_ATTEMPTS = 5
MAX_CONCURRENT = 4                    # 全站同时进行的抓取任务上限（多人并扫时容下 4 个）
MAX_JOBS_PER_USER = 1                 # 单用户同时进行的抓取任务上限（防止单人占满 slot）
MAX_QR_SESSIONS = 8                   # 同时存活的扫码会话上限（最多 8 人同时扫码）

# QQ 表情：content 中的 [em]eN[/em] → qzonestyle.gtimg.cn/qzone/em/eN.gif
EMOJI_RE = re.compile(r"\[em\]e(\d+)\[/em\]")
EMOJI_URL_TPL = "https://qzonestyle.gtimg.cn/qzone/em/e{}.gif"

# QQ 媒体防盗链白名单：只代理这些域名后缀的资源，防止 SSRF
QQ_MEDIA_HOST_SUFFIXES = (
    ".qq.com", ".gtimg.cn", ".qlogo.cn",
)
PROXY_FETCH_TIMEOUT = 20


def _is_qq_media(url: str) -> bool:
    """仅放行 QQ 域名下的图片/视频/表情，防止被滥用为开放代理。"""
    if not url:
        return False
    u = url.lower()
    if not (u.startswith("http://") or u.startswith("https://")):
        return False
    # 提取 host
    rest = u.split("://", 1)[1]
    host = rest.split("/", 1)[0].split("?", 1)[0]
    # 去掉端口
    host = host.split(":", 1)[0]
    return any(host == s[1:] or host.endswith(s) for s in QQ_MEDIA_HOST_SUFFIXES)

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

os.makedirs(DATA_DIR, exist_ok=True)

# ============================================================
# 协议层（纯函数，对应原项目 LoginUtil / GetAllMomentsUtil）
# ============================================================

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
        "User-Agent": UA,
        "Referer": "https://qzs.qq.com/",
    })
    url = QR_SHOW_URL.format(t=random.random())
    r = sess.get(url, timeout=15)
    r.raise_for_status()
    qrsig = sess.cookies.get("qrsig") or requests.utils.dict_from_cookiejar(r.cookies).get("qrsig")
    if not qrsig:
        raise RuntimeError("二维码响应中缺少 qrsig")
    if not r.content:
        raise RuntimeError("二维码图片为空")
    return sess, qrsig, r.content


def poll_qr(sess: requests.Session, qrsig: str) -> dict:
    """
    轮询二维码状态。成功时完成 check_sig 换 cookie。
    返回 {status: waiting|scanned|expired|canceled|success, ...}
    """
    token = ptqr_token(qrsig)
    url = QR_LOGIN_URL.format(token=token, ts=int(time.time()))
    # 关键：Referer 必须用 xui.ptlogin2.qq.com（原项目）而非 qzs.qq.com
    # 否则腾讯可能不识别扫码状态
    poll_headers = {
        "Referer": "https://xui.ptlogin2.qq.com/cgi-bin/xlogin?appid=549000912&daid=5&pt_skey_valid=0&pt_uistyle=40",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "Accept-Language": "zh-CN,zh;q=0.9",
    }
    r = sess.get(url, timeout=15, headers=poll_headers)
    # 腾讯 ptqrlogin 返回 UTF-8 编码的 JSONP（ptuiCB('code',...)）
    # 但 HTTP 响应头常不带 charset，requests 默认按 ISO-8859-1 解码 → 中文变乱码
    # 直接用 r.content 按 UTF-8 解码最稳
    try:
        text = r.content.decode("utf-8", errors="replace")
    except Exception:
        text = r.text

    # ── 用字符串 split 解析 ptuiCB，比正则更稳健 ──
    # 格式：ptuiCB('code','status','redirect','flag','message','nickname')
    # 中文 message 即使乱码，只要不含 ','  字面量，split 就正确
    code = None
    redirect = ""
    login_nick = ""
    head = text.find("ptuiCB('")
    if head >= 0:
        body_start = head + len("ptuiCB('")
        # 找结尾 ')：从 body_start 往后找
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

    # ── 调试日志 ──
    try:
        with open(os.path.join(BASE_DIR, "_poll_debug.log"), "a",
                  encoding="utf-8") as f:
            f.write(f"\n=== {now().strftime('%H:%M:%S')} === code={code}\n")
            f.write(f"response: {text[:500]}\n")
    except Exception:
        pass

    # ── 状态判断：用数字 code（66/67/0/68/65） ──
    if code == "0":
        # 登录成功：从 redirect URL 中取 ptsigx，调 check_sig 换登录态 cookie
        ms = re.search(r"ptsigx=([^&]+)", redirect)
        if not ms:
            raise RuntimeError("登录成功但响应中未找到 ptsigx")
        sigx = ms.group(1)
        uin_raw = requests.utils.dict_from_cookiejar(r.cookies).get("uin", "")
        cr = sess.get(CHECK_SIG_URL.format(uin=uin_raw, sigx=sigx),
                      allow_redirects=False, timeout=15)
        target = requests.utils.dict_from_cookiejar(cr.cookies)
        if not target.get("p_skey") or not target.get("uin"):
            raise RuntimeError("check_sig 未返回有效登录态")
        uin = re.sub(r"o0*", "", target["uin"])
        # 没拿到 nickname 时尝试用 cookies 中的 ptnick_<uin>
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
    # code=66 或未知 → 等待扫码
    return {"status": "waiting"}


def fetch_page(cookies: dict, uin: str, g_tk: int, pos: int, num: int) -> dict:
    """抓取一页说说（emotion_cgi_msglist_v6），返回解析后的 JSON dict。"""
    headers = {
        "accept": "*/*",
        "accept-language": "zh-CN,zh;q=0.9",
        "cookie": (f'uin={cookies.get("p_uin", cookies.get("uin"))};'
                   f'skey={cookies.get("skey")};p_uin={cookies.get("p_uin")};'
                   f'pt4_token={cookies.get("pt4_token")};p_skey={cookies.get("p_skey")}'),
        "referer": f"https://user.qzone.qq.com/{uin}/main",
        "user-agent": UA,
    }
    params = {
        "uin": uin, "ftype": "0", "sort": "0", "pos": str(pos),
        "num": str(num), "replynum": "100", "g_tk": str(g_tk),
        "callback": "_preloadCallback", "code_version": "1",
        "format": "jsonp", "need_private_comment": "1",
    }
    r = requests.get(MSGLIST_URL, headers=headers, params=params, timeout=20)
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


# ============================================================
# 用户身份隔离 / 媒体代理 / 表情解析（纯函数）
# ============================================================

def _blog_uid(x_blog_uid: str | None) -> str:
    """从请求头 X-Blog-Uid 提取博客用户 ID（字符串）。

    用户在博客登录后，前端 axios 拦截器把 auth.user.id 注入此头。
    qzone-service 只监听 127.0.0.1，公网经 Vite/SakuraFrp 同源进入；
    此 header 与 sid/jid 绑定，用于隔离不同博客用户的扫码会话与抓取任务。
    """
    return (x_blog_uid or "").strip()


def _proxy_url(url: str, kind: str = "img") -> str:
    """把 QQ 媒体 URL 改写为后端代理 URL（前端走 /qzapi 同源代理）。

    作用：绕过 QQ 空间图片/视频的防盗链（Referer 校验）。后端代理时
    会带上正确的 Referer 抓取再流式返回，浏览器侧无感知。
    非 QQ 域名资源原样返回，避免被滥用为开放代理。
    """
    if not url or not _is_qq_media(url):
        return url or ""
    return f"/qzapi/proxy?url={url_quote(url, safe='')}&type={kind}"


def _parse_emojis(text: str) -> str:
    """把说说 content 中的 [em]eN[/em] 表情标签替换为 <img>。

    先 HTML escape 防 XSS，再注入表情 img 标签。返回的字符串可直接 v-html。
    图片 src 走代理 URL（前端同源访问，绕过防盗链）。
    """
    if not text:
        return ""
    safe = html.escape(text)
    def repl(m):
        eid = m.group(1)
        src = _proxy_url(EMOJI_URL_TPL.format(eid), "img")
        return (f'<img class="qz-emo" src="{html.escape(src)}" '
                f'alt="[e{eid}]" loading="lazy" />')
    return EMOJI_RE.sub(repl, safe)


def _parse_emojis_export(text: str) -> str:
    """导出 HTML 用的表情解析：使用原始 QQ 表情 URL（不代理）。

    导出文件离线打开时，代理 URL 无意义，故保留原始 URL。
    防盗链可能使图片无法显示，但链接完整保留档案价值。
    同时把换行符转为 <br> 适配 HTML 段落。
    """
    if not text:
        return ""
    safe = html.escape(text).replace("\n", "<br>")
    def repl(m):
        eid = m.group(1)
        src = EMOJI_URL_TPL.format(eid)
        return (f'<img class="qz-emo" src="{html.escape(src)}" '
                f'alt="[e{eid}]" loading="lazy" />')
    return EMOJI_RE.sub(repl, safe)


def normalize_moment(item: dict) -> dict:
    """把腾讯原始结构压平为预览/导出用的干净结构。"""
    pics, videos = [], []
    for p in item.get("pic") or []:
        url = p.get("url1") or p.get("smallurl") or ""
        if url:
            pics.append({"url": _proxy_url(url, "img"),
                         "small": _proxy_url(p.get("smallurl") or url, "img"),
                         "origin": url,
                         "w": p.get("width"), "h": p.get("height")})
    for v in item.get("video") or []:
        url = v.get("url1") or v.get("url") or ""
        if url:
            videos.append({"url": _proxy_url(url, "video"), "origin": url})

    comments = []
    for c in item.get("commentlist") or []:
        replies = []
        for r3 in c.get("list_3") or []:
            raw_rc = r3.get("content", "")
            replies.append({"name": r3.get("name", ""),
                            "content": raw_rc,
                            "content_html": _parse_emojis(raw_rc),
                            "time": r3.get("createTime2", "")})
        raw_cc = c.get("content", "")
        comments.append({"name": c.get("name", ""), "uin": c.get("uin", ""),
                         "time": c.get("createTime2", ""),
                         "content": raw_cc,
                         "content_html": _parse_emojis(raw_cc),
                         "replies": replies})

    raw_content = item.get("content") or ""
    return {
        "tid": item.get("tid", ""),
        "time": fmt_ts(item.get("created_time")),
        "content": raw_content,
        "content_html": _parse_emojis(raw_content),
        "name": item.get("name", ""),
        "secret": item.get("secret", 0),
        "cmtnum": item.get("cmtnum", 0),
        "fwdnum": item.get("fwdnum", 0),
        "source": item.get("source_name", ""),
        "lbs": (item.get("lbs") or {}).get("name", ""),
        "pics": pics,
        "videos": videos,
        "comments": comments,
    }


# ============================================================
# 内存存储：扫码会话 + 抓取任务
# ============================================================
qr_sessions: dict[str, dict] = {}
jobs: dict[str, dict] = {}
store_lock = threading.Lock()
running_count = 0


def now() -> datetime:
    return datetime.now()


def reap_loop():
    """后台清理过期扫码会话与任务数据。"""
    while True:
        time.sleep(REAP_INTERVAL)
        t = now()
        with store_lock:
            for sid in [s for s, v in qr_sessions.items() if t - v["created_at"] > QR_TTL]:
                qr_sessions.pop(sid, None)
            for jid, job in list(jobs.items()):
                if job["status"] in ("done", "failed", "canceled") and t - job["updated_at"] > JOB_TTL:
                    shutil.rmtree(job["dir"], ignore_errors=True)
                    jobs.pop(jid, None)


# ============================================================
# 抓取线程
# ============================================================

def _fetch_one_page(job: dict, cookies: dict, uin: str, g_tk: int, page_no: int):
    """抓取指定页（page_no 从 0 计），带重试退避；取消或最终失败返回 None。"""
    pos = page_no * PAGE_SIZE
    for attempt in range(1, MAX_ATTEMPTS + 1):
        if job["cancel"].is_set():
            return None
        try:
            return fetch_page(cookies, uin, g_tk, pos, PAGE_SIZE)
        except Exception as e:  # 分页失败重试，带退避
            job["logs"].append(f"第 {page_no + 1} 页第 {attempt} 次请求失败：{e}")
            time.sleep(min(2 * attempt, 10))
    return None


def run_fetch(job: dict, cookies: dict, uin: str, g_tk: int, interval: float, workers: int):
    """后台抓取：先串行取首页拿总数，再由 workers-1 个线程并发翻页。

    线程共享一个递增页游标，每线程抓完一页睡 interval 秒 —— 总请求速率约为
    单线程的 workers 倍，同时保持与原工具相近的礼貌间隔，避免触发腾讯风控。
    """
    global running_count
    raw_list: list[dict] = []
    pages: dict[int, list | None] = {}
    try:
        # ── 阶段一：串行取首页（拿 total / 昵称，失败即早退） ──
        first = _fetch_one_page(job, cookies, uin, g_tk, 0)
        if first is None:
            if job["cancel"].is_set():
                job["status"] = "canceled"
                job["logs"].append("用户取消了任务")
            raise RuntimeError("首页连续抓取失败，任务终止")
        total = int(first.get("total", 0))
        job["total"] = total
        job["total_pages"] = max(1, math.ceil(total / PAGE_SIZE))
        if not job.get("name"):
            job["name"] = (first.get("msglist") or [{}])[0].get("name", "") or uin
        job["logs"].append(f"账号 {uin} 共有 {total} 条说说，启动 {workers} 线程并发抓取")
        pages[0] = first.get("msglist") or []
        job["page"] = 1
        job["fetched"] = len(pages[0])
        job["updated_at"] = now()

        if total == 0:
            pass
        elif job["cancel"].is_set():
            job["status"] = "canceled"
            job["logs"].append("用户取消了任务")
        else:
            # ── 阶段二：workers-1 个线程领页并发（首页已占 1 个串行位） ──
            total_pages = job["total_pages"]
            cursor = 1
            cur_lock = threading.Lock()

            def worker():
                nonlocal cursor
                while not job["cancel"].is_set():
                    with cur_lock:
                        if cursor >= total_pages:
                            return
                        idx = cursor
                        cursor += 1
                    page = _fetch_one_page(job, cookies, uin, g_tk, idx)
                    with cur_lock:
                        pages[idx] = None if page is None else (page.get("msglist") or [])
                        job["page"] = sum(1 for _ in pages)  # pages 含首页（索引 0）
                        job["fetched"] = sum(len(v) for v in pages.values() if v)
                        job["updated_at"] = now()
                    time.sleep(interval)

            threads = [threading.Thread(target=worker, daemon=True)
                       for _ in range(max(0, workers - 1))]
            for t in threads:
                t.start()
            for t in threads:
                t.join()

        # ── 按页序号重组，保证导出顺序稳定（接口本身按时间倒序返回） ──
        failed_pages = [i + 1 for i, v in sorted(pages.items()) if v is None]
        for i in sorted(pages):
            if pages[i]:
                raw_list.extend(pages[i])
        if failed_pages and not job["cancel"].is_set():
            job["logs"].append(
                f"有 {len(failed_pages)} 页重试后仍失败：第 "
                f"{', '.join(str(p) for p in failed_pages[:10])} 页（对应数据缺失）")
        if not raw_list and total and not job["cancel"].is_set():
            raise RuntimeError("所有分页均失败，未能获取到任何数据")

        # 规范化 + 落盘（canceled 时也导出已抓到的部分）
        normalized = [normalize_moment(x) for x in raw_list]
        job["moments"] = normalized
        os.makedirs(job["dir"], exist_ok=True)
        export_at = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        with open(os.path.join(job["dir"], "data.json"), "w", encoding="utf-8") as f:
            json.dump({"uin": uin, "name": job.get("name", ""), "exported_at": export_at,
                       "total": job.get("total", 0), "count": len(raw_list),
                       "msglist": raw_list}, f, ensure_ascii=False, indent=2)
        with open(os.path.join(job["dir"], "moments.json"), "w", encoding="utf-8") as f:
            json.dump({"uin": uin, "name": job.get("name", ""), "exported_at": export_at,
                       "total": job.get("total", 0), "count": len(normalized),
                       "moments": normalized}, f, ensure_ascii=False, indent=2)
        with open(os.path.join(job["dir"], "index.html"), "w", encoding="utf-8") as f:
            f.write(render_html(job.get("name", uin), uin, export_at, normalized))
        write_csv(os.path.join(job["dir"], "moments.csv"), normalized)

        times = [m["time"] for m in normalized if m.get("time")]
        job.update({
            "status": job["status"] if job["status"] == "canceled" else "done",
            "pic_count": sum(len(m["pics"]) for m in normalized),
            "video_count": sum(len(m["videos"]) for m in normalized),
            "comment_count": sum(len(m["comments"]) for m in normalized),
            "range_from": min(times) if times else "",
            "range_to": max(times) if times else "",
            "files": ["json", "html", "csv"],
            "finished_at": now().strftime("%H:%M:%S"),
        })
        job["logs"].append(f"导出完成，共 {len(normalized)} 条，已临时保存（24 小时后自动删除）")
    except Exception as e:
        job["status"] = "failed"
        job["error"] = str(e)
        job["logs"].append(f"任务失败：{e}")
    finally:
        job["updated_at"] = now()
        with store_lock:
            running_count = max(0, running_count - 1)


# ============================================================
# 导出渲染
# ============================================================

def write_csv(path: str, moments: list[dict]):
    with open(path, "w", encoding="utf-8-sig", newline="") as f:
        w = csv.writer(f)
        w.writerow(["时间", "内容", "图片链接", "视频链接", "评论数", "转发数", "位置", "来源", "评论"])
        for m in moments:
            comments = " | ".join(
                f'{c["time"]} {c["name"]}：{c["content"]}'
                + (" 【回复】" + "；".join(f'{r["name"]}:{r["content"]}' for r in c["replies"])
                   if c["replies"] else "")
                for c in m["comments"])
            w.writerow([m["time"], m["content"],
                        " ".join(p.get("origin", "") for p in m["pics"]),
                        " ".join(v.get("origin", "") for v in m["videos"]),
                        m["cmtnum"], m["fwdnum"],
                        m["lbs"], m["source"], comments])


def render_html(name: str, uin: str, exported_at: str, moments: list[dict]) -> str:
    cards = []
    for m in moments:
        esc = lambda s: html.escape(str(s or ""))
        # 导出 HTML 走原始 QQ URL（保留档案链接，离线打开时图片可能因防盗链失效但 URL 完整）
        pics = "".join(
            f'<a href="{esc(p.get("origin", ""))}" target="_blank" rel="noopener">'
            f'<img loading="lazy" src="{esc(p.get("origin", ""))}" alt=""></a>'
            for p in m["pics"])
        vids = "".join(
            f'<video controls preload="none" src="{esc(v.get("origin", ""))}"></video>'
            for v in m["videos"])
        # content 用原始文本，把 [em]eN[/em] 解析为原始 QQ 表情 URL（非代理）
        content_html = _parse_emojis_export(m.get("content") or "")
        comments = ""
        if m["comments"]:
            lis = []
            for c in m["comments"]:
                inner = "".join(
                    f'<div class="reply">{esc(r["name"])}：{_parse_emojis_export(r.get("content") or "")}</div>'
                    for r in c["replies"])
                lis.append(f'<li><span class="cname">{esc(c["name"])}</span>'
                           f'<span class="ctime">{esc(c["time"])}</span>'
                           f'<div class="ctext">{_parse_emojis_export(c.get("content") or "")}</div>{inner}</li>')
            comments = f'<ul class="comments">{"".join(lis)}</ul>'
        cards.append(f"""
    <article class="card">
      <div class="meta">{esc(m["time"])}
        <span class="src">{esc(m["source"])}</span>
        <span class="lbs">{esc(m["lbs"])}</span>
      </div>
      <div class="content">{content_html}</div>
      <div class="pics">{pics}</div>
      <div class="vids">{vids}</div>
      {comments}
    </article>""")
    return f"""<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{html.escape(name)} 的QQ空间说说存档</title>
<style>
:root{{--bg:#060a12;--card:#0c1422;--line:rgba(120,170,220,.16);--amber:#fcd34d;--txt:#e2e8f0;--sub:#94a3b8;--cyan:#22d3ee}}
*{{box-sizing:border-box}}
body{{margin:0;background:radial-gradient(1200px 600px at 80% -10%,rgba(34,211,238,.08),transparent),var(--bg);color:var(--txt);font-family:"PingFang SC","Microsoft YaHei",serif;line-height:1.7}}
header{{max-width:820px;margin:0 auto;padding:48px 20px 20px}}
h1{{color:var(--amber);font-size:26px;margin:0 0 8px}}
.sub{{color:var(--sub);font-size:13px}}
main{{max-width:820px;margin:0 auto;padding:0 20px 60px}}
.card{{background:linear-gradient(150deg,#0c1422,#0a111d);border:1px solid var(--line);border-radius:12px;padding:18px 20px;margin:16px 0;box-shadow:0 6px 24px rgba(0,0,0,.35)}}
.meta{{color:var(--cyan);font-size:12px;margin-bottom:8px;font-family:monospace}}
.src,.lbs{{color:var(--sub);margin-left:10px}}
.content{{white-space:pre-wrap;word-break:break-word;font-size:15px}}
.content img.qz-emo{{display:inline-block;width:22px;height:22px;vertical-align:middle}}
.pics{{display:flex;flex-wrap:wrap;gap:8px;margin-top:12px}}
.pics img{{width:150px;height:150px;object-fit:cover;border-radius:8px;border:1px solid var(--line)}}
.vids video{{max-width:100%;border-radius:8px;margin-top:10px}}
.comments{{list-style:none;padding:12px 0 0;margin:12px 0 0;border-top:1px dashed var(--line)}}
.comments li{{font-size:13px;margin:8px 0;color:#cbd5e1}}
.cname{{color:var(--amber)}} .ctime{{color:var(--sub);font-size:11px;margin-left:8px;font-family:monospace}}
.ctext{{margin:2px 0}} .reply{{color:var(--sub);padding-left:14px;font-size:12px}}
</style></head>
<body>
<header><h1>时光回溯 · {html.escape(name)} 的说说存档</h1>
<div class="sub">QQ：{html.escape(uin)} ｜ 共 {len(moments)} 条 ｜ 导出时间：{html.escape(exported_at)}</div></header>
<main>{''.join(cards)}</main></body></html>"""


# ============================================================
# FastAPI
# ============================================================
app = FastAPI(title="qzone-service", version="1.0.0")


@app.on_event("startup")
def _startup():
    # 上次残留的任务目录全部清除（临时数据不跨进程保留）
    for name in os.listdir(DATA_DIR):
        p = os.path.join(DATA_DIR, name)
        if os.path.isdir(p):
            shutil.rmtree(p, ignore_errors=True)
    threading.Thread(target=reap_loop, daemon=True).start()


@app.get("/api/qzone/health")
def health():
    return {"ok": True, "service": "qzone-service", "running": running_count,
            "qr_sessions": len(qr_sessions), "max_concurrent": MAX_CONCURRENT,
            "max_qr_sessions": MAX_QR_SESSIONS, "max_jobs_per_user": MAX_JOBS_PER_USER}


def _require_owner_session(sid: str, blog_uid: str) -> dict:
    """校验 sid 属于 blog_uid，返回 session dict，否则 raise 403/404。

    每个 QQ 扫码会话在创建时绑定博客用户 ID，仅本人可查询状态。
    blog_uid 为空时（未登录访客）降级为只校验存在性，方便调试。
    """
    s = qr_sessions.get(sid)
    if not s:
        raise HTTPException(404, "二维码会话不存在或已过期，请重新获取")
    if blog_uid and s.get("blog_uid") and s["blog_uid"] != blog_uid:
        raise HTTPException(403, "无权访问该扫码会话：仅本人可查看自己的二维码")
    return s


def _require_owner_job(jid: str, blog_uid: str) -> dict:
    """校验 jid 属于 blog_uid，返回 job dict，否则 raise 403/404。"""
    job = jobs.get(jid)
    if not job:
        raise HTTPException(404, "任务不存在或已过期清理")
    if blog_uid and job.get("blog_uid") and job["blog_uid"] != blog_uid:
        raise HTTPException(403, "无权访问该任务：仅本人可查看自己扫码的抓取结果")
    return job


@app.post("/api/qzone/qr/start")
def qr_start(x_blog_uid: str | None = Header(default=None, alias="X-Blog-Uid")):
    """新建扫码会话，返回腾讯登录二维码 PNG（base64）。

    每个 sid 绑定 X-Blog-Uid（博客登录用户 ID），他人无法查询你的二维码状态。
    """
    uid = _blog_uid(x_blog_uid)
    # 扫码会话上限：避免恶意用户开海里 session 耗尽内存
    if len(qr_sessions) >= MAX_QR_SESSIONS:
        # 顺手清一波过期的（reap_loop 5 分钟才扫一次，这里即时清）
        t = now()
        for k in [k for k, v in qr_sessions.items() if t - v["created_at"] > QR_TTL]:
            qr_sessions.pop(k, None)
    if len(qr_sessions) >= MAX_QR_SESSIONS:
        raise HTTPException(429, f"同时扫码的会话已达上限（{MAX_QR_SESSIONS}），请稍后再试")
    try:
        sess, qrsig, png = fetch_qr()
    except Exception as e:
        raise HTTPException(502, f"获取二维码失败：{e}")
    sid = secrets.token_urlsafe(16)
    qr_sessions[sid] = {"session": sess, "qrsig": qrsig, "status": "waiting",
                        "created_at": now(), "blog_uid": uid}
    return {"sid": sid, "qr_png": "data:image/png;base64," + base64.b64encode(png).decode()}


@app.get("/api/qzone/qr/status")
def qr_status(sid: str = Query(..., min_length=8),
              x_blog_uid: str | None = Header(default=None, alias="X-Blog-Uid")):
    uid = _blog_uid(x_blog_uid)
    s = _require_owner_session(sid, uid)
    if s["status"] in ("success", "expired", "canceled"):
        return {"status": s["status"], "uin": s.get("uin"), "name": s.get("name"),
                "avatar": s.get("avatar")}
    try:
        result = poll_qr(s["session"], s["qrsig"])
    except Exception as e:
        raise HTTPException(502, f"查询登录状态失败：{e}")
    if result["status"] == "success":
        s.update({"status": "success", "cookies": result["cookies"],
                  "uin": result["uin"], "name": result["name"],
                  "avatar": result["avatar"]})
        return {"status": "success", "uin": result["uin"],
                "name": result["name"], "avatar": result["avatar"]}
    s["status"] = result["status"]
    return {"status": result["status"]}


class JobStart(BaseModel):
    sid: str
    interval: float = DEFAULT_INTERVAL
    workers: int = 2  # 并发线程数 1-3，越大越快但越容易触发腾讯风控


@app.post("/api/qzone/job/start")
def job_start(body: JobStart,
              x_blog_uid: str | None = Header(default=None, alias="X-Blog-Uid")):
    """开始后台抓取。同一博客用户最多并发 1 个抓取任务（防单人占满 slot）。

    全站并发上限 MAX_CONCURRENT=4，单用户上限 MAX_JOBS_PER_USER=1，
    即最多 4 个不同用户同时抓取各自的说说。
    """
    global running_count
    uid = _blog_uid(x_blog_uid)
    s = qr_sessions.get(body.sid)
    if not s or s.get("status") != "success":
        raise HTTPException(400, "登录态不存在或未完成扫码，请重新登录")
    # 校验扫码会话归属
    if uid and s.get("blog_uid") and s["blog_uid"] != uid:
        raise HTTPException(403, "无权使用该扫码会话启动抓取")

    with store_lock:
        # 全站抓取任务上限
        if running_count >= MAX_CONCURRENT:
            raise HTTPException(429, f"当前抓取任务较多（{running_count}/{MAX_CONCURRENT}），请稍后再试")
        # 单用户抓取任务上限：同一博客用户最多 1 个 running 任务
        if uid:
            user_running = sum(1 for j in jobs.values()
                               if j.get("blog_uid") == uid and j["status"] == "running")
            if user_running >= MAX_JOBS_PER_USER:
                raise HTTPException(429, "你已有抓取任务正在进行，请等待完成或取消后再试")
        running_count += 1

    interval = max(MIN_INTERVAL, min(float(body.interval or DEFAULT_INTERVAL), 10))
    workers = max(1, min(int(body.workers or 2), 3))
    jid = secrets.token_urlsafe(16)
    job = {
        "id": jid, "sid": body.sid, "blog_uid": uid,
        "uin": s["uin"], "name": s.get("name", ""),
        "status": "running", "total": None, "total_pages": None, "page": 0,
        "fetched": 0, "logs": [], "moments": [], "error": "",
        "pic_count": 0, "video_count": 0, "comment_count": 0,
        "range_from": "", "range_to": "", "files": [],
        "created_at": now(), "updated_at": now(),
        "finished_at": "", "workers": workers, "cancel": threading.Event(),
        "dir": os.path.join(DATA_DIR, jid),
    }
    jobs[jid] = job
    cookies, uin = s["cookies"], s["uin"]
    g_tk = bkn(cookies["p_skey"])
    threading.Thread(target=run_fetch,
                     args=(job, cookies, uin, g_tk, interval, workers), daemon=True).start()
    return {"jid": jid}


def _public_job(job: dict) -> dict:
    return {k: v for k, v in job.items()
            if k not in ("cancel", "dir", "moments", "sid", "blog_uid") and k != ""}


@app.get("/api/qzone/job/{jid}")
def job_status(jid: str,
              x_blog_uid: str | None = Header(default=None, alias="X-Blog-Uid")):
    job = _require_owner_job(jid, _blog_uid(x_blog_uid))
    info = _public_job(job)
    # 日志只回传最后 30 条，避免轮询响应膨胀
    info["logs"] = job["logs"][-30:]
    return info


@app.get("/api/qzone/job/{jid}/moments")
def job_moments(jid: str, offset: int = 0, limit: int = 50,
                x_blog_uid: str | None = Header(default=None, alias="X-Blog-Uid")):
    job = _require_owner_job(jid, _blog_uid(x_blog_uid))
    moments = job.get("moments") or []
    limit = max(1, min(limit, 100))
    return {"total": len(moments), "offset": offset,
            "items": moments[offset:offset + limit]}


@app.post("/api/qzone/job/{jid}/cancel")
def job_cancel(jid: str,
               x_blog_uid: str | None = Header(default=None, alias="X-Blog-Uid")):
    job = _require_owner_job(jid, _blog_uid(x_blog_uid))
    if job["status"] == "running":
        job["cancel"].set()
        return {"ok": True}
    return JSONResponse({"ok": False, "msg": "任务已结束"}, status_code=400)


@app.delete("/api/qzone/job/{jid}")
def job_delete(jid: str,
               x_blog_uid: str | None = Header(default=None, alias="X-Blog-Uid")):
    uid = _blog_uid(x_blog_uid)
    job = jobs.get(jid)
    if not job:
        raise HTTPException(404, "任务不存在或已过期清理")
    if uid and job.get("blog_uid") and job["blog_uid"] != uid:
        raise HTTPException(403, "无权删除该任务")
    if job["status"] == "running":
        job["cancel"].set()
    shutil.rmtree(job["dir"], ignore_errors=True)
    jobs.pop(jid, None)
    return {"ok": True}


@app.get("/api/qzone/job/{jid}/download")
def job_download(jid: str, format: str = Query("json", pattern="^(json|html|csv)$"),
                 x_blog_uid: str | None = Header(default=None, alias="X-Blog-Uid")):
    job = _require_owner_job(jid, _blog_uid(x_blog_uid))
    if job["status"] not in ("done", "canceled", "failed") or not job.get("moments"):
        raise HTTPException(400, "任务尚未完成，暂无可下载的数据")
    file_map = {
        "json": ("data.json", "application/json; charset=utf-8", "json"),
        "html": ("index.html", "text/html; charset=utf-8", "html"),
        "csv": ("moments.csv", "text/csv; charset=utf-8", "csv"),
    }
    fname, ctype, ext = file_map[format]
    path = os.path.join(job["dir"], fname)
    if not os.path.exists(path):
        raise HTTPException(404, "导出文件不存在")
    safe_name = re.sub(r'[\\/:*?"<>|\s]+', "_", job.get("name") or job["uin"])
    stamp = datetime.now().strftime("%Y%m%d")
    return FileResponse(path, media_type=ctype,
                        filename=f"{safe_name}_{job['uin']}_qqspace_{stamp}.{ext}")


# ============================================================
# 媒体代理：绕过 QQ 图片/视频/表情的防盗链（Referer 校验）
# ============================================================
@app.get("/api/qzone/proxy")
def proxy_media(url: str = Query(..., min_length=10),
                type: str = Query("img", pattern="^(img|video|emoji)$")):
    """代理 QQ 域名下的图片/视频/表情，带上正确 Referer 后流式返回。

    作用：浏览器 <img>/<video> 直接加载 QQ 域资源会因防盗链 403，
    经后端代理（带 Referer: https://qzs.qq.com/）抓取后流式吐回，前端无感知。
    仅放行 .qq.com / .gtimg.cn / .qlogo.cn 域名，防止被滥用为开放代理（SSRF）。
    """
    if not _is_qq_media(url):
        raise HTTPException(403, "仅允许代理 QQ 域名下的媒体资源")
    headers = {
        "User-Agent": UA,
        "Referer": "https://qzs.qq.com/",
        "Accept": "*/*",
    }
    try:
        r = requests.get(url, headers=headers, timeout=PROXY_FETCH_TIMEOUT, stream=True)
    except requests.RequestException as e:
        raise HTTPException(502, f"代理请求失败：{e}")
    if r.status_code != 200:
        raise HTTPException(502, f"源站返回 {r.status_code}")

    ctype = r.headers.get("Content-Type", "application/octet-stream")
    # 简单校验：仅放行图片/视频/二进制流类型，避免被用来代取 HTML
    if type == "img" and not (ctype.startswith("image/") or ctype.startswith("application/octet")):
        raise HTTPException(403, f"非图片资源：{ctype}")
    if type == "video" and not (ctype.startswith("video/") or ctype.startswith("application/octet")):
        raise HTTPException(403, f"非视频资源：{ctype}")

    def stream():
        try:
            for chunk in r.iter_content(chunk_size=8192):
                if chunk:
                    yield chunk
        finally:
            r.close()

    return StreamingResponse(stream(), media_type=ctype,
                             headers={"Cache-Control": "public, max-age=3600"})


if __name__ == "__main__":
    uvicorn.run("app:app", host=HOST, port=PORT, log_level="info")
