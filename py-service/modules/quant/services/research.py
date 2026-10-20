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
import html
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

from ..datasources.supabase import SupabaseStore


class ResearchReportService:
    """券商研报元数据：标题/机构/分析师/评级/目标价/EPS预测/原文链接。

    多源聚合（任一源可用即有数据，单源失败不影响其它源）：
      1. 东财研报中心 reportapi（个股 / 行业 / 策略，含评级、目标价、EPS 预测）
      2. 新浪财经研报列表（标题 / 机构 / 研究员 / 日期 / 原文链接，无评级）
    合规边界：仅采集列表页元数据，不下载、不缓存研报 PDF 全文；字段随源站点调整，全程容错。
    """

    _cache: Dict[str, tuple] = {}
    _REPORT_TTL = 300  # 5 分钟
    _SRC_DOWN: Dict[str, float] = {}  # 源不可达熔断到期时间戳
    SRC_COOLDOWN = 180.0  # 熔断冷却：3 分钟内不再打该源

    _HEADERS = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                      "(KHTML, like Gecko) Chrome/126.0 Safari/537.36",
        "Referer": "https://data.eastmoney.com/report/",
    }

    @staticmethod
    def _src_ok(name: str) -> bool:
        """该源是否不在熔断冷却期内。"""
        return time.time() >= ResearchReportService._SRC_DOWN.get(name, 0.0)

    @staticmethod
    def _src_trip(name: str, err: object) -> None:
        """上游不可达 / 结构变化：短暂熔断，避免每个请求都空等超时。"""
        ResearchReportService._SRC_DOWN[name] = time.time() + ResearchReportService.SRC_COOLDOWN
        logger.warning("research source %s unavailable, skip %ss: %s",
                       name, int(ResearchReportService.SRC_COOLDOWN), err)

    @staticmethod
    def _norm(x) -> Optional[float]:
        if x in (None, "", "-"):
            return None
        try:
            return float(str(x).replace(",", "").replace("万", "").strip())
        except (TypeError, ValueError):
            return None

    @staticmethod
    def _authors(row: Dict) -> str:
        a = row.get("author")
        if isinstance(a, list):
            parts = []
            for it in a:
                if isinstance(it, str):
                    nm = it.split(".")[-1] if "." in it else it
                elif isinstance(it, dict):
                    nm = it.get("name") or ""
                else:
                    nm = ""
                if nm:
                    parts.append(nm)
            return "、".join(parts)
        s = row.get("authorName") or ""
        if isinstance(s, str):
            return s.replace("|", "、")
        return ""

    # ---------------- 源 1：东财研报中心（个股 / 行业 / 策略） ----------------
    _EM_QTYPE = {"0": "个股", "1": "行业", "2": "策略"}

    @staticmethod
    def _em_page(code: str = "", page_no: int = 1, q_type: str = "0",
                 page_size: int = 30, begin: str = "", end: str = "") -> List[Dict]:
        """东财研报中心列表接口直连（qType: 0个股/1行业/2策略），含评级/目标价/EPS。"""
        if not ResearchReportService._src_ok("eastmoney"):
            return []
        try:
            url = "https://reportapi.eastmoney.com/report/list"
            params = {
                "cb": "", "pageSize": str(page_size), "pageNo": str(page_no),
                "qType": q_type, "code": code, "industryCode": "*", "rating": "*",
                "ratingChange": "*",
                "beginTime": begin or (datetime.now() - timedelta(days=90)).strftime("%Y-%m-%d"),
                "endTime": end or datetime.now().strftime("%Y-%m-%d"),
                "_": str(int(time.time() * 1000)),
            }
            r = requests.get(url, params=params, headers=ResearchReportService._HEADERS, timeout=12)
            r.raise_for_status()
            payload = r.json()
        except Exception as e:  # noqa: BLE001
            ResearchReportService._src_trip("eastmoney", e)
            return []
        rows = (payload or {}).get("data") or []
        if isinstance(rows, dict):
            rows = rows.get("list") or []
        if not isinstance(rows, list):
            rows = []
        out = []
        for x in rows[:page_size]:
            if not isinstance(x, dict):
                continue
            stock_code = str(x.get("stockCode") or "")
            title = str(x.get("title") or "").strip()
            if not title:
                continue
            rating = x.get("emRatingName") or x.get("sRatingName") or x.get("newRatingName") or ""
            info_code = str(x.get("infoCode") or "")
            out.append({
                "report_id": info_code,
                "title": title,
                "stock_code": stock_code,
                "stock_name": str(x.get("stockName") or "").strip(),
                "org_name": str(x.get("orgSName") or x.get("orgName") or "").strip(),
                "author": ResearchReportService._authors(x),
                "publish_date": str(x.get("publishDate") or "")[:10],
                "rating": str(rating),
                "rating_change": str(x.get("emRatingChange") or ""),
                "target_price": ResearchReportService._norm(x.get("indvAimPrice") or x.get("aimPrice")),
                "eps_y1": ResearchReportService._norm(x.get("predictThisYearEps")),
                "eps_y2": ResearchReportService._norm(x.get("predictNextYearEps")),
                "eps_y3": ResearchReportService._norm(x.get("predictThreeYearEps")),
                "summary": str(x.get("summary") or x.get("jiYin") or "")[:200],
                "report_type": ResearchReportService._EM_QTYPE.get(str(q_type), "个股"),
                "source": "eastmoney",
                "url": "https://data.eastmoney.com/report/info/" + info_code + ".html",
            })
        return out

    # ---------------- 源 2：新浪财经研报列表 ----------------
    _SINA_URL = "https://vip.stock.finance.sina.com.cn/q/go.php/vReport_List/kind/{kind}/index.phtml"
    _SINA_ROW = re.compile(
        r"<tr>\s*<td>\d+</td>\s*<td class=\"tal f14\">\s*<a[^>]*?title=\"([^\"]*)\"[^>]*?"
        r"href=\"([^\"]*?/rptid/(\d+)/[^\"]*)\"[^>]*>.*?</a>\s*</td>\s*"
        r"<td>([^<]*)</td>\s*<td>([^<]*)</td>\s*<td>(.*?)</td>\s*<td>(.*?)</td>\s*</tr>", re.S)
    _sina_lock = threading.Lock()
    _sina_last = [0.0]
    SINA_MIN_INTERVAL = 6.0  # 实测：连续请求间隔 <5s 时新浪会返回假的「没有找到」空页

    @staticmethod
    def _sina_symbol(code: str) -> str:
        """6 位代码 → 新浪写法 sh600519 / sz000001 / bj920982。"""
        c = str(code or "").strip()
        if not c:
            return ""
        if c[0] == "6":
            return "sh" + c
        if c[0] in ("0", "3"):
            return "sz" + c
        if c[0] in ("4", "8", "9"):
            return "bj" + c
        return c

    @staticmethod
    def _sina_text(fragment: str) -> str:
        return html.unescape(re.sub(r"<[^>]+>", "", fragment or "")).strip()

    @staticmethod
    def _sina_page(code: str = "", page: int = 1, enforce_interval: bool = True) -> List[Dict]:
        """新浪研报列表（code 空=全市场最新；给代码=该股）。标题/类型/日期/机构/研究员，无评级与目标价。"""
        if not ResearchReportService._src_ok("sina"):
            return []
        if code:
            url = ResearchReportService._SINA_URL.format(kind="search")
            params = {"symbol": ResearchReportService._sina_symbol(code),
                      "t1": "all", "p": int(max(page, 1))}
        else:
            url = ResearchReportService._SINA_URL.format(kind="lastest")
            params = {"p": int(max(page, 1))}
        text = ""
        for _ in range(2):
            # 全市场最新抓取才需要强制最小间隔（个股搜索为交互路径，不额外等待）
            with ResearchReportService._sina_lock:
                if enforce_interval:
                    wait = ResearchReportService.SINA_MIN_INTERVAL - (time.time() - ResearchReportService._sina_last[0])
                    if wait > 0:
                        time.sleep(wait)
                try:
                    resp = requests.get(url, params=params, timeout=12,
                                        headers={"User-Agent": ResearchReportService._HEADERS["User-Agent"],
                                                 "Referer": "https://finance.sina.com.cn/"})
                    ResearchReportService._sina_last[0] = time.time()
                    text = resp.content.decode("gbk", "replace")
                except Exception as e:  # noqa: BLE001
                    ResearchReportService._sina_last[0] = time.time()
                    ResearchReportService._src_trip("sina", e)
                    return []
            if "没有找到相关内容" not in text:
                break
        if "tb_01" not in text or "研究员" not in text:
            ResearchReportService._src_trip("sina", "研报表格结构变化")
            return []
        out = []
        for title, href, rptid, rtype, day, org, author in ResearchReportService._SINA_ROW.findall(text):
            title = html.unescape(title or "").strip()
            if not title:
                continue
            out.append({
                "report_id": "sina_" + str(rptid),
                "title": title,
                "stock_code": str(code or ""),
                "stock_name": "",
                "org_name": ResearchReportService._sina_text(org),
                "author": ResearchReportService._sina_text(author).replace("|", "、"),
                "publish_date": re.sub(r"[()\s]", "", str(day or ""))[:10],
                "rating": "",
                "rating_change": "",
                "target_price": None,
                "eps_y1": None, "eps_y2": None, "eps_y3": None,
                "summary": "",
                "report_type": str(rtype or "").strip() or "个股",
                "source": "sina",
                "url": ("https:" + href) if str(href).startswith("//") else str(href),
            })
        return out

    # ---------------- 聚合 ----------------
    @staticmethod
    def _rid(r: Dict) -> str:
        return str(r.get("report_id") or (str(r.get("title") or "") + str(r.get("publish_date") or "")))

    @staticmethod
    def _merge(groups: List[List[Dict]], limit: int = 20, round_robin: bool = False) -> List[Dict]:
        """多源合并：组内按 report_id 去重，组内先按发布日期倒序。

        - round_robin=True（全市场榜单）：跨源轮转取。必须轮转，否则东财一次上百条会把新浪条目全部挤掉，
          接再多源也等于白接。
        - round_robin=False（单只个股）：全局按日期倒序，保证「最新在前」。
        """
        prepped: List[List[Dict]] = []
        for g in groups or []:
            seen, items = set(), []
            for r in g or []:
                rid = ResearchReportService._rid(r)
                if not rid or rid in seen:
                    continue
                seen.add(rid)
                items.append(r)
            items.sort(key=lambda x: str(x.get("publish_date") or ""), reverse=True)
            prepped.append(items)
        if not round_robin:
            seen_all, flat = set(), []
            for items in prepped:
                for r in items:
                    rid = ResearchReportService._rid(r)
                    if rid in seen_all:
                        continue
                    seen_all.add(rid)
                    flat.append(r)
            flat.sort(key=lambda x: str(x.get("publish_date") or ""), reverse=True)
            return flat[:limit]
        out, seen_all, idx = [], set(), 0
        while len(out) < limit:
            progressed = False
            for items in prepped:
                if idx >= len(items):
                    continue
                progressed = True
                r = items[idx]
                rid = ResearchReportService._rid(r)
                if rid not in seen_all:
                    seen_all.add(rid)
                    out.append(r)
                    if len(out) >= limit:
                        break
            if not progressed:
                break
            idx += 1
        return out

    @staticmethod
    def _store(rows: List[Dict]) -> None:
        """异步落库（所有获取到的研报元数据均入库，便于回测/复盘）。"""
        if rows:
            threading.Thread(target=lambda: SupabaseStore.save_reports(rows), daemon=True).start()

    @staticmethod
    def get_reports(code: str = "", page_no: int = 1, limit: int = 20,
                    q_type: str = "0", force: bool = False) -> List[Dict]:
        """按股票代码获取研报：东财（评级/目标价/EPS）+ 新浪（标题/机构/研究员）聚合，5 分钟缓存。"""
        key = f"reports_{code}_{page_no}_{q_type}"
        now = time.time()
        if not force and key in ResearchReportService._cache:
            ts, val = ResearchReportService._cache[key]
            if now - ts < ResearchReportService._REPORT_TTL:
                return val[:limit]
        want = max(limit, 20)
        em = ResearchReportService._em_page(code=code, page_no=page_no, q_type=q_type, page_size=want)
        sina = ResearchReportService._sina_page(code=code, page=page_no, enforce_interval=False) if code else []
        rows = ResearchReportService._merge([em, sina], limit=limit)
        ResearchReportService._cache[key] = (now, rows)
        ResearchReportService._store(rows)
        return rows

    @staticmethod
    def get_latest_reports(limit: int = 20, force: bool = False) -> List[Dict]:
        """最新研报速递：个股 + 行业 + 策略 三路东财研报 与 新浪最新 并行聚合，按发布日期倒序。"""
        key = "reports_latest_all"
        now = time.time()
        if not force and key in ResearchReportService._cache:
            ts, val = ResearchReportService._cache[key]
            if now - ts < ResearchReportService._REPORT_TTL:
                return val[:limit]
        want = max(limit, 20)
        groups: List[List[Dict]] = []
        jobs = [(ResearchReportService._em_page, "", 1, q, want) for q in ("0", "1", "2")]
        with ThreadPoolExecutor(max_workers=4) as ex:
            futs = [ex.submit(fn, c, pg, q, ps) for (fn, c, pg, q, ps) in jobs]
            futs.append(ex.submit(ResearchReportService._sina_page, "", 1))
            for f in as_completed(futs):
                try:
                    groups.append(f.result() or [])
                except Exception as e:  # noqa: BLE001
                    logger.warning("research latest source failed: %s", e)
        rows = ResearchReportService._merge(groups, limit=limit, round_robin=True)
        ResearchReportService._cache[key] = (now, rows)
        ResearchReportService._store(rows)
        return rows

    @staticmethod
    def get_industry_reports(limit: int = 20, force: bool = False) -> List[Dict]:
        """行业研报速递（东财 qType=1 + 新浪混合，过滤出行业类）。"""
        key = "reports_industry_all"
        now = time.time()
        if not force and key in ResearchReportService._cache:
            ts, val = ResearchReportService._cache[key]
            if now - ts < ResearchReportService._REPORT_TTL:
                return val[:limit]
        want = max(limit, 20)
        em = ResearchReportService._em_page(code="", page_no=1, q_type="1", page_size=want)
        rows = ResearchReportService._merge([em], limit=limit)
        ResearchReportService._cache[key] = (now, rows)
        ResearchReportService._store(rows)
        return rows


# ============================================================
# 回测引擎
# ============================================================
# 内置回测策略清单（含中文说明，供前端展示；id 与 BacktestEngine.STRATEGIES 一一对应）
