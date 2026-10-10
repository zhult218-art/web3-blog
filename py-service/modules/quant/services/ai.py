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

from ..config import QUANT_CACHE_DIR
from ..datasources.astock import AStockDataService
from ..services.news import NewsRadarService
from ..services.research import ResearchReportService


LLM_PROVIDERS = [
    {"key": "deepseek", "name": "DeepSeek", "base_url": "https://api.deepseek.com/v1",
     "models": ["deepseek-chat", "deepseek-reasoner"]},
    {"key": "openai", "name": "OpenAI", "base_url": "https://api.openai.com/v1",
     "models": ["gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-4.1", "gpt-4.1-mini"]},
    {"key": "qwen", "name": "通义千问", "base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
     "models": ["qwen3-max", "qwen3-plus", "qwen-plus", "qwen-turbo"]},
    {"key": "zhipu", "name": "智谱GLM", "base_url": "https://open.bigmodel.cn/api/paas/v4",
     "models": ["glm-4.7-flash", "glm-4-plus", "glm-4-air"]},
    {"key": "minimax", "name": "MiniMax", "base_url": "https://api.minimaxi.com/v1",
     "models": ["MiniMax-M2.7", "MiniMax-M2.7-highspeed", "MiniMax-Text-01"]},
    {"key": "siliconflow", "name": "硅基流动(免费)", "base_url": "https://api.siliconflow.cn/v1",
     "models": ["deepseek-chat", "Qwen/Qwen2.5-7B-Instruct", "Qwen/Qwen2.5-14B-Instruct"]},
    {"key": "kimi", "name": "Kimi(Moonshot)", "base_url": "https://api.moonshot.cn/v1",
     "models": ["moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k"]},
    {"key": "openrouter", "name": "OpenRouter", "base_url": "https://openrouter.ai/api/v1",
     "models": ["deepseek/deepseek-chat", "anthropic/claude-3.5-sonnet", "openai/gpt-4o"]},
    {"key": "ollama", "name": "Ollama(本地)", "base_url": "http://localhost:11434/v1",
     "models": ["qwen3:latest", "llama3.1:8b", "deepseek-r1:7b"]},
]

# 服务端启动时读取 LLM env 配置；AI 深研 tab 选择时自动使用。
LLM_DEFAULTS: Dict[str, str] = {
    "base_url": os.environ.get("LLM_BASE_URL", "").strip(),
    "api_key":  os.environ.get("LLM_API_KEY", "").strip(),
    "model":    os.environ.get("LLM_MODEL", "").strip(),
}


def _as_llm_endpoint(base_url: str) -> str:
    """把用户填写的 base url 规范成 /chat/completions 完整端点"""
    url = (base_url or "").strip().rstrip("/")
    if not url:
        return ""
    if url.endswith("/chat/completions"):
        return url
    return url + "/chat/completions"


def fetch_llm_models(base_url: str, api_key: str = "", timeout: float = 15.0) -> List[str]:
    """根据 base_url + api_key 拉取 OpenAI 兼容网关 /v1/models 的模型列表"""
    endpoint = _as_llm_endpoint(base_url)
    if not endpoint:
        return []
    models_url = endpoint.replace("/chat/completions", "").rstrip("/") + "/models"
    headers = {"Authorization": f"Bearer {api_key.strip()}"} if api_key and str(api_key).strip() else {}
    try:
        r = requests.get(models_url, headers=headers, timeout=timeout)
        r.raise_for_status()
        data = r.json()
        items = data.get("data") or []
        ids: List[str] = []
        for it in items:
            if isinstance(it, dict) and it.get("id"):
                ids.append(str(it["id"]))
            elif isinstance(it, str):
                ids.append(it)
        return sorted(set(ids))
    except Exception as e:  # noqa: BLE001
        logger.warning("LLM models fetch failed (%s): %s", models_url, e)
        return []


def _extract_json(text: str) -> Optional[Dict]:
    """从 LLM 输出中稳健提取 JSON 对象（兼容 markdown 围栏与前后杂文本）。"""
    if not text:
        return None
    s = text.strip()
    if s.startswith("```"):
        s = re.sub(r"^```[a-zA-Z]*\s*", "", s)
        s = re.sub(r"\s*```$", "", s)
    start, end = s.find("{"), s.rfind("}")
    if start < 0 or end <= start:
        return None
    try:
        obj = json.loads(s[start:end + 1])
        return obj if isinstance(obj, dict) else None
    except Exception:  # noqa: BLE001
        return None


def call_llm_json(base_url: str, api_key: str, model: str,
                  system_prompt: str, user_prompt: str,
                  timeout: float = 90.0) -> Optional[Dict]:
    """调用 OpenAI 兼容 /chat/completions，要求返回 JSON 对象。失败返回 None，上层兜底走 mock。"""
    endpoint = _as_llm_endpoint(base_url)
    if not endpoint or not (model or "").strip():
        return None
    headers = {"Content-Type": "application/json"}
    if api_key and str(api_key).strip():
        headers["Authorization"] = f"Bearer {api_key.strip()}"
    payload = {
        "model": model.strip(),
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
        "temperature": 0.3,
        "stream": False,
    }
    # response_format=json_object 部分兼容网关不支持：失败后去掉该参数重试。
    for payload0, retry in ((dict(payload, response_format={"type": "json_object"}), True),
                            (payload, False)):
        try:
            r = requests.post(endpoint, json=payload0, headers=headers, timeout=timeout)
            r.raise_for_status()
            data = r.json()
            content = (data.get("choices") or [{}])[0].get("message", {}).get("content") or ""
            obj = _extract_json(content)
            if obj is not None:
                return obj
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM call failed (model=%s endpoint=%s): %s", model, endpoint, e)
            if not retry:
                return None
    return None


# ============================================================
# 多Agent AI分析服务
# ============================================================
class AIAnalysisService:
    """多Agent AI深度投研：优先调用真实LLM（OpenAI兼容接口），失败/无Key时自动降级为本地mock，保证界面可用。"""

    ANALYSTS = [
        ("market_analyst", "市场分析", "K线形态、技术指标、量价关系分析"),
        ("social_analyst", "情绪分析", "社交媒体情绪、散户讨论热度分析"),
        ("news_analyst", "新闻事件分析", "行业新闻、公告、宏观事件影响"),
        ("fundamentals_analyst", "基本面分析", "财报分析、盈利能力、估值水平"),
        ("policy_analyst", "政策分析", "监管政策、产业政策、窗口指导"),
        ("hot_money_analyst", "游资追踪", "龙虎榜、大单流向、主力资金"),
        ("lockup_analyst", "解禁监控", "限售解禁、大股东减持、股权质押"),
    ]

    @staticmethod
    def _load_context(symbol: str) -> Dict:
        """聚合真实行情/指标/研报/新闻/资金流/板块上下文供 LLM 使用（全程容错降级）。"""
        ctx: Dict = {}
        try:
            ctx["info"] = AStockDataService.get_stock_info(symbol)
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context info failed: %s", e)
        try:
            ctx["news"] = NewsRadarService.get_latest_news(10)
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context news failed: %s", e)
        try:
            ctx["reports"] = ResearchReportService.get_reports(
                code=AStockDataService._to_code(symbol), limit=6)
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context reports failed: %s", e)
        try:
            ctx["industry"] = ResearchReportService.get_industry_reports(6)
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context industry failed: %s", e)
        try:
            ctx["fund_flow"] = AStockDataService.get_market_fund_flow()
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context fund_flow failed: %s", e)
        try:
            ctx["sector_heat"] = AStockDataService.get_sector_heat_ths()
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context sector_heat failed: %s", e)
        try:
            ctx["limit_pools"] = AStockDataService.get_limit_pools()
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context limit_pools failed: %s", e)
        try:
            ctx["sector_trends"] = AStockDataService.get_sector_trends(days=10, top=5)
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context sector_trends failed: %s", e)
        try:
            ctx["quote"] = asdict(AStockDataService.get_realtime_quote(symbol))
        except Exception as e:  # noqa: BLE001
            logger.warning("LLM context realtime failed: %s", e)
        return ctx

    @staticmethod
    def _format_context(ctx: Dict) -> str:
        """将真实数据上下文压缩为紧凑文本喂给 LLM"""
        parts = []
        info = ctx.get("info") or {}
        quote = info.get("quote") or ctx.get("quote") or {}
        if quote:
            parts.append(
                "【实时行情】%s %s 现价%.2f 涨跌%.2f(%.2f%%) 市盈率%s 市净率%s 市值%.0f亿"
                % (quote.get("symbol", ""), quote.get("name", ""),
                   float(quote.get("price") or 0), float(quote.get("change") or 0),
                   float(quote.get("change_percent") or 0),
                   quote.get("pe") or "-", quote.get("pb") or "-",
                   float(quote.get("market_cap") or 0)))
        ind = info.get("indicators") or {}
        if ind:
            macd = ind.get("macd") or {}
            parts.append(
                "【技术指标】MA5=%s MA10=%s MA20=%s MA60=%s RSI=%s MACD(DIF=%s DEA=%s HIST=%s)"
                % (ind.get("ma5"), ind.get("ma10"), ind.get("ma20"), ind.get("ma60"),
                   ind.get("rsi"), macd.get("dif"), macd.get("dea"), macd.get("hist")))
        kl = info.get("kline") or []
        if kl:
            recent = [f"{k.get('date')}:收{float(k.get('close') or 0)}" for k in kl[-20:]]
            parts.append("【近20日收盘】" + " | ".join(recent))
        news = ctx.get("news") or []
        if news:
            parts.append("【财经新闻】" + " ".join(
                f"[{n.get('source', '')}/{n.get('sentiment', '')}] {n.get('title', '')}"
                for n in news[:10]))
        reports = ctx.get("reports") or []
        if reports:
            parts.append("【个股研报】" + " ".join(
                f"{r.get('org_name', '')}《{r.get('title', '')}》评级:{r.get('rating', '-')}"
                f"目标价{r.get('target_price') or '-'}" for r in reports[:6]))
        industry = ctx.get("industry") or []
        if industry:
            parts.append("【行业研报】" + " ".join(
                f"{i.get('org_name', '')}《{i.get('title', '')}》评级:{i.get('rating', '-')}"
                for i in industry[:4]))
        ff = ctx.get("fund_flow") or {}
        if ff:
            parts.append("【市场资金流】" + json.dumps(ff, ensure_ascii=False)[:400])
        sh = ctx.get("sector_heat") or {}
        if sh:
            top = [f"{k}({v}%)" for k, v in sorted(sh.items(), key=lambda x: float(x[1] or 0), reverse=True)[:8]]
            parts.append("【板块热点TOP8】" + " | ".join(top))
        lp = ctx.get("limit_pools") or {}
        rows = lp.get("data") or lp.get("pools") or []
        if rows:
            body = " ".join(f"{r.get('name', '')}{r.get('code', '')}:{r.get('reason', '')}" for r in rows[:8])
            parts.append("【涨停池】" + body)
        st = ctx.get("sector_trends") or {}
        if st and isinstance(st.get("series"), list):
            trend_body = " ".join(
                f"{s.get('name', '')}近{len(s.get('closes', []) or [])}日由{s.get('closes', [None, None])[0]}→{s.get('closes', [None, None])[-1]}"
                for s in st["series"][:5])
            if trend_body:
                parts.append("【板块趋势】" + trend_body)
        return "\n".join(parts)

    # A股交易约束（TradingAgents-astock 的 trader/PM 必含）
    A_STOCK_RULES = (
        "- T+1 交易制度：当日买入次日才能卖出\n"
        "- 涨跌停制度：主板±10%，创业板/科创板±20%，ST股±5%\n"
        "- 最小交易单位：主板100股（1手），科创板/创业板200股\n"
        "- 交易时间：09:30-11:30 / 13:00-15:00（北京时间）\n"
        "- ST/*ST 表示存在退市风险，需谨慎控制仓位\n"
        "- 北向资金（沪深港通）流入流出及游资（龙虎榜席位）动向是需要关注的信号指标\n"
        "（以上参数仅供技术研究参考，不构成投资建议）"
    )

    _ANALYST_SYSTEM = {
        "market_analyst": (
            "你是A股资深技术分析师。基于给定行情数据输出JSON，字段："
            "{\"score\":0-10,\"verdict\":\"看多/看空/中性\",\"summary\":\"100-180字结论\","
            "\"key_points\":[\"3-5条要点\"],\"confidence\":0.0-1.0,"
            "\"trend\":\"上升趋势/下降趋势/震荡\",\"support\":数,\"resistance\":数,"
            "\"indicators\":{\"RSI\":数,\"MACD\":\"金叉/死叉/粘合\",\"KDJ\":\"超买/超卖/中性\"}}"
            "只输出JSON，不要其它文字"),
        "social_analyst": (
            "你是A股舆情情绪分析师。基于新闻与行业氛围判断散户情绪，输出JSON："
            "{\"score\":0-10,\"verdict\":\"看多/看空/中性\",\"summary\":\"100-180字\","
            "\"key_points\":[\"3-5条\"],\"confidence\":0.0-1.0,"
            "\"sentiment\":\"乐观/谨慎/悲观\",\"discussion_heat\":0-100}只输出JSON"),
        "news_analyst": (
            "你是A股新闻事件分析师，评估新闻对个股与板块的影响与风险，输出JSON："
            "{\"score\":0-10,\"verdict\":\"看多/看空/中性\",\"summary\":\"100-180字\","
            "\"key_points\":[\"3-5条\"],\"confidence\":0.0-1.0,\"event_impact\":\"利好/利空/中性\"}"
            "只输出JSON"),
        "fundamentals_analyst": (
            "你是A股基本面分析师，基于估值、市值与券商研报评级/EPS预测分析，输出JSON："
            "{\"score\":0-10,\"verdict\":\"看多/看空/中性\",\"summary\":\"100-180字\","
            "\"key_points\":[\"3-5条\"],\"confidence\":0.0-1.0,"
            "\"valuation\":\"低估/合理/高估\",\"eps_forecast\":\"一句话\"}只输出JSON"),
        "policy_analyst": (
            "你是A股政策分析师（A股是政策市），基于行业研报与财经新闻评估政策面影响，输出JSON："
            "{\"score\":0-10,\"verdict\":\"看多/看空/中性\",\"summary\":\"100-180字\","
            "\"key_points\":[\"3-5条\"],\"confidence\":0.0-1.0,"
            "\"policy_impact\":\"利好/利空/中性\",\"key_policies\":[\"相关具体政策\"]}只输出JSON"),
        "hot_money_analyst": (
            "你是A股游资资金流分析师，结合市场资金流向与题材热点评估短线资金动向，输出JSON："
            "{\"score\":0-10,\"verdict\":\"看多/看空/中性\",\"summary\":\"100-180字\","
            "\"key_points\":[\"3-5条\"],\"confidence\":0.0-1.0,"
            "\"capital_flow\":\"大幅流入/小幅流入/平衡/小幅流出/大幅流出\","
            "\"dragon_tiger\":\"机构买入/游资介入/无明显异动\"}只输出JSON"),
        "lockup_analyst": (
            "你是A股解禁减持监控分析师（数据有限时基于已有信息合理推断），输出JSON："
            "{\"score\":0-10,\"verdict\":\"看多/看空/中性\",\"summary\":\"100-180字\","
            "\"key_points\":[\"3-5条\"],\"confidence\":0.0-1.0,"
            "\"lockup_risk\":\"高/中/低\",\"major_shareholder_activity\":\"一句话\"}只输出JSON"),
    }

    # 分析师报告保留的字段白名单（防止模型夹带无关字段）
    _REPORT_KEYS = (
        "score", "verdict", "summary", "key_points", "confidence",
        "trend", "support", "resistance", "indicators",
        "sentiment", "discussion_heat", "event_impact",
        "valuation", "eps_forecast", "policy_impact", "key_policies",
        "capital_flow", "dragon_tiger", "lockup_risk", "major_shareholder_activity",
    )

    @staticmethod
    def _mock_report(stage_id: str, name: str, task_id: str) -> Dict:
        """mock 分析师报告（LLM 无配置或调用失败时兜底）"""
        seed = int(hashlib.md5(f"{task_id}_{stage_id}".encode()).hexdigest()[:8], 16)
        rng = np.random.RandomState(seed)
        hint = "模拟数据（未配置模型接口）"
        if stage_id == "market_analyst":
            return {
                "trend": rng.choice(["上升趋势", "下降趋势", "震荡"], p=[0.4, 0.2, 0.4]),
                "support": round(rng.uniform(80, 150), 2),
                "resistance": round(rng.uniform(160, 300), 2),
                "indicators": {"RSI": round(rng.uniform(30, 70), 1), "MACD": rng.choice(["金叉", "死叉", "粘合"]),
                               "KDJ": rng.choice(["超买", "超卖", "中性"])},
                "score": round(rng.uniform(5, 9), 1),
                "verdict": rng.choice(["看多", "看空", "中性"], p=[0.45, 0.2, 0.35]),
                "summary": "基于K线与量价形态的综合分析。" + hint,
                "key_points": [hint],
            }
        if stage_id == "policy_analyst":
            return {
                "policy_impact": rng.choice(["利好", "利空", "中性"], p=[0.35, 0.15, 0.5]),
                "key_policies": ["货币政策调整", "行业监管政策变化"],
                "score": round(rng.uniform(5, 9), 1),
                "verdict": rng.choice(["看多", "看空", "中性"], p=[0.4, 0.15, 0.45]),
                "summary": "基于政策与产业趋势的综合分析。" + hint,
                "key_points": [hint],
            }
        if stage_id == "hot_money_analyst":
            return {
                "capital_flow": rng.choice(["大幅流入", "小幅流入", "平衡", "小幅流出", "大幅流出"]),
                "dragon_tiger": rng.choice(["机构买入", "游资介入", "无明显异动"]),
                "score": round(rng.uniform(4, 9), 1),
                "verdict": rng.choice(["看多", "看空", "中性"], p=[0.35, 0.2, 0.45]),
                "summary": "基于资金流与龙虎榜的综合分析。" + hint,
                "key_points": [hint],
            }
        return {
            "score": round(rng.uniform(5, 9), 1),
            "verdict": rng.choice(["看多", "看空", "中性"], p=[0.4, 0.2, 0.4]),
            "summary": f"{name}完成分析，综合评分{round(rng.uniform(5, 9), 1)}。" + hint,
            "key_points": [f"关键要点{i + 1}" for i in range(3)],
        }

    @staticmethod
    async def _run_analyst(stage_id: str, name: str, desc: str, symbol: str,
                           ctx: Dict, llm_cfg: Optional[Dict], task_id: str) -> Dict:
        """运行单个分析师：优先真实 LLM（OpenAI 兼容），输出非法或无配置时 mock 兜底"""
        cfg = llm_cfg or {}
        if cfg.get("base_url") and cfg.get("model"):
            system = AIAnalysisService._ANALYST_SYSTEM.get(
                stage_id, AIAnalysisService._ANALYST_SYSTEM["news_analyst"])
            user = (f"股票代码：{symbol}\n请以「{name}」（{desc}）的视角，分析以下真实数据并给出专业判断：\n"
                    + AIAnalysisService._format_context(ctx))
            raw = await asyncio.to_thread(
                call_llm_json, cfg.get("base_url"), cfg.get("api_key"), cfg.get("model"),
                system, user)
            if raw:
                report = {k: raw[k] for k in AIAnalysisService._REPORT_KEYS if k in raw}
                if not report.get("summary"):
                    v = report.get("verdict", "中性")
                    report["summary"] = f"{name}完成分析：{v}"
                if "verdict" not in report:
                    report["verdict"] = "中性"
                if "score" not in report:
                    report["score"] = 5.0
                if not report.get("key_points"):
                    report["key_points"] = f"LLM完成{name}分析"
                return report
            logger.warning("analyst %s LLM 无有效JSON，降级mock", stage_id)
        return AIAnalysisService._mock_report(stage_id, name, task_id)

    @staticmethod
    def _report_line(r: Dict) -> str:
        rep = r["report"]
        return (f"- {r['name']}({rep.get('verdict', '中性')}) score={rep.get('score', 0)}: "
                f"{rep.get('summary', '')}")

    @staticmethod
    async def _quality_gate(analysts_reports: Dict, symbol: str,
                            ctx_text: str, call, task_id: str) -> Dict:
        """数据质量门控：TradingAgents Quality Gate，硬规则 + LLM 复核"""
        grades = {}
        notes = []
        fail_markers = ("无法获取", "I cannot retrieve", "I don't have access",
                        "unable to fetch", "工具调用失败", "模拟数据")
        for stage_id, r in analysts_reports.items():
            rep = r["report"]
            text = str(rep.get("summary") or "") + " " + str(rep.get("verdict") or "")
            text += " " + " ".join(str(k) for k in (rep.get("key_points") or []))
            length = len(text.strip())
            fails = sum(1 for m in fail_markers if m in str(rep.get("summary") or ""))
            if length < 20 and fails:
                g, note = "D", "报告过短且含失败信息"
            elif fails:
                g, note = "C", "关键信息缺失/失败提示，建议人工复核"
            elif "score" not in rep or "verdict" not in rep:
                g, note = "B", "缺少结构化评分子段"
            else:
                g, note = "A", f"完整({length}字)"
            grades[stage_id] = g
            notes.append(f"- {r['name']}: [{g}] {note}")
        hard_summary = "\n".join(notes)

        review = ""
        if call is not None:
            system = ("你是A股数据质量复核员。核对 7 位分析师报告的完整性与合理性，"
                      "输出JSON：{\"grade\":\"A/B/C/D\",\"confidence\":\"高/中/低\","
                      "\"suggestion\":\"40-100字建议\"}只输出JSON。")
            raw = await call(system,
                             f"股票代码{symbol}\n检测结果：\n{hard_summary}\n\n分析师摘要：\n"
                             + "\n".join(AIAnalysisService._report_line(r) for r in analysts_reports.values()))
            if raw:
                review = (f"[LLM复核] 等级{raw.get('grade', '-')} 置信度:{raw.get('confidence', '-')} "
                          f"建议:{raw.get('suggestion', '')}")
        gate_summary = ("## 数据质量门控结果\n\n### 硬规则\n" + hard_summary
                        + ("\n\n" + review if review else ""))
        return {"grades": grades, "summary": gate_summary}

    @staticmethod
    async def _debate(analysts_reports: Dict, symbol: str, ctx_text: str,
                      bull_score: float, bear_score: float, bull_pct: float,
                      call, task_id: str) -> Dict:
        """多空研究员辩论：TradingAgents Bull/Bear Researcher，各1轮辩论"""
        reports_text = "\n".join(AIAnalysisService._report_line(r) for r in analysts_reports.values())
        bull_arg = bear_arg = ""

        bull_system = (
            "你是A股看多研究员（Bull Analyst），为建仓该A股构建证据充分的多头观点。"
            "重点：政策加持（专精特新/国家战略行业）、北向资金持续流入、游资接力（连板+成交量确认题材归属）、"
            "估值与成长故事（前向PE/PEG/消化年限）、解禁利空出尽。"
            "必须用具体数值（现价/指标/评分）反击空头要点。只输出JSON："
            "{\"argument\":\"80-160字多头论点\",\"key_catalysts\":[\"3-5条催化\"],\"risk_mitigation\":\"40-80字\"}"
        )
        bull_user = (f"股票代码{symbol}\n{ctx_text}\n\n分析师观点：\n{reports_text}\n"
                     f"多头得分{bull_score:.1f} 空头得分{bear_score:.1f}（多方占比{round(bull_pct*100,1)}%）")
        if call is not None:
            raw = await call(bull_system, bull_user)
            if raw:
                bull_arg = str(raw.get("argument") or "").strip()
        if not bull_arg or not call:
            bulls = [f"{r['name']}:{r['report'].get('summary', '')}"
                     for r in analysts_reports.values() if r["report"].get("verdict") == "看多"]
            bull_arg = ("多头核心论点：" + (" | ".join(bulls) if bulls else "多方证据偏弱")
                        + f"（多方占比{round(bull_pct*100,1)}%）")

        bear_system = (
            "你是A股看空研究员（Bear Analyst），构建证据充分的空头核验观点。"
            "重点：估值消化年限不足、政策不及预期、北向资金撤离、游资撤退（连板/题材熄火）、解禁高峰将至。"
            "必须用具体数值反击多头要点。只输出JSON："
            "{\"argument\":\"80-160字空头论点\",\"key_risks\":[\"3-5条风险\"],\"counter_to_bull\":\"40-80字\"}"
        )
        bear_user = (f"股票代码{symbol}\n{ctx_text}\n\n分析师观点：\n{reports_text}\n"
                     f"多头得分{bull_score:.1f} 空头得分{bear_score:.1f}（空方占比{round((1-bull_pct)*100,1)}%）"
                     f"\n看多研究员最新观点：{bull_arg}")
        if call is not None:
            raw = await call(bear_system, bear_user)
            if raw:
                bear_arg = str(raw.get("argument") or "").strip()
        if not bear_arg or not call:
            bears = [f"{r['name']}:{r['report'].get('summary', '')}"
                     for r in analysts_reports.values() if r["report"].get("verdict") == "看空"]
            bear_arg = ("空头核心论点：" + (" | ".join(bears) if bears else "空方证据偏弱")
                        + f"（空方占比{round((1-bull_pct)*100,1)}%）")

        return {
            "bull_history": [bull_arg] if bull_arg else [],
            "bear_history": [bear_arg] if bear_arg else [],
            "history": (["Bull: " + bull_arg, "Bear: " + bear_arg] if bull_arg and bear_arg
                        else [bull_arg or bear_arg]),
            "bull_score": round(bull_score, 1), "bear_score": round(bear_score, 1),
            "bull_percentage": round(bull_pct * 100, 1),
        }

    @staticmethod
    async def _research_manager(debate: Dict, symbol: str, ctx_text: str,
                                call, task_id: str) -> Dict:
        """研究经理（Research Manager）：把辩论收敛为投资计划（Buy/Overweight/Hold/Underweight/Sell）"""
        history = "\n".join(debate.get("history", []))
        plan = {
            "recommendation": "",
            "rationale": "",
            "strategic_actions": "",
        }
        system = (
            "你是A股研究经理兼辩论主持人。综合多空辩论给出明确的投资计划和行动指引。"
            "评级只取其一：Buy(多头强势)/Overweight(偏多)/Hold(均衡)/Underweight(偏空)/Sell(空方强势)。"
            "注意A股：政策市属性、北向与游资动向、解禁减持。只输出JSON："
            "{\"recommendation\":\"Buy/Overweight/Hold/Underweight/Sell\",\"rationale\":\"80-150字\","
            "\"strategic_actions\":\"60-120字可执行步骤与仓位指引\"}"
        )
        user = (f"股票代码{symbol}\n{ctx_text}\n\nA股特殊约束\n{AIAnalysisService.A_STOCK_RULES}\n\n辩论记录：\n{history}")
        if call is not None:
            raw = await call(system, user)
            if raw:
                rating = str(raw.get("recommendation") or "")[:1].upper()
                full = str(raw.get("recommendation") or "")
                low = full.lower()
                if "buy" in low and "over" not in low:
                    rating = "Buy"
                elif "overweight" in low:
                    rating = "Overweight"
                elif "hold" in low:
                    rating = "Hold"
                elif "underweight" in low:
                    rating = "Underweight"
                elif "sell" in low:
                    rating = "Sell"
                if rating in ("Buy", "Overweight", "Hold", "Underweight", "Sell"):
                    plan["recommendation"] = rating
                plan["rationale"] = str(raw.get("rationale") or "")
                plan["strategic_actions"] = str(raw.get("strategic_actions") or "")

        if not plan["recommendation"]:
            bp = float(debate.get("bull_percentage") or 50)
            if bp >= 65:
                plan["recommendation"] = "Buy"
            elif bp >= 55:
                plan["recommendation"] = "Overweight"
            elif bp <= 35:
                plan["recommendation"] = "Sell"
            elif bp <= 45:
                plan["recommendation"] = "Underweight"
            else:
                plan["recommendation"] = "Hold"
            plan["rationale"] = f"多方占比{debate.get('bull_percentage', 50)}%，依据多空计票给出{plan['recommendation']}评级。"
            plan["strategic_actions"] = "结合多方占比决定仓位：评级越高仓位上限越高，严格控制单票风险。"
        return plan

    @staticmethod
    async def _trader(analysts_reports: Dict, plan: Dict, symbol: str, ctx_text: str,
                      call, task_id: str) -> Dict:
        """交易员（Trader）：把投资计划转成可执行的交易提案（Buy/Hold/Sell + 入场/止损/仓位）"""
        proposal = {"action": "Hold", "reasoning": "", "entry_price": None,
                    "stop_loss": None, "position_sizing": ""}
        reports_text = "\n".join(AIAnalysisService._report_line(r) for r in analysts_reports.values())
        system = (
            "你是专注A股的交易台执行交易员。把研究经理的投资计划转成具体交易提案。"
            "A股限制：T+1、涨跌停（主板±10%、创业科创±20%、ST±5%）、100股（科创/创业200股）。"
            "只输出JSON：{\"action\":\"Buy/Hold/Sell\",\"reasoning\":\"40-100字\","
            "\"entry_price\":\"数字或null\",\"stop_loss\":\"数字或null\",\"position_sizing\":\"百分比仓位\"}"
        )
        user = (f"股票代码{symbol}\n{ctx_text}\n\n{AIAnalysisService.A_STOCK_RULES}\n\n投资计划：\n"
                f"评级：{plan.get('recommendation')}\n理由：{plan.get('rationale')}\n行动：{plan.get('strategic_actions')}\n"
                f"\n分析师观点：\n{reports_text}")
        if call is not None:
            raw = await call(system, user)
            if raw:
                action = str(raw.get("action") or "").strip()
                low = action.lower()
                if "buy" in low:
                    proposal["action"] = "Buy"
                elif "sell" in low:
                    proposal["action"] = "Sell"
                else:
                    proposal["action"] = "Hold"
                proposal["reasoning"] = str(raw.get("reasoning") or "")
                for k in ("entry_price", "stop_loss"):
                    v = raw.get(k)
                    if v not in (None, "", "null"):
                        try:
                            proposal[k] = round(float(v), 2)
                        except (TypeError, ValueError):
                            proposal[k] = None
                proposal["position_sizing"] = str(raw.get("position_sizing") or "")
        if not proposal["reasoning"] and not call:
            plan_rating = plan.get("recommendation", "Hold")
            proposal["action"] = {"Buy": "Buy", "Overweight": "Buy"}.get(plan_rating, "Hold")
            proposal["reasoning"] = f"基于研究经理评级{plan_rating}给出的默认交易方向。"
        return proposal

    @staticmethod
    async def _risk_debate(trader_proposal: Dict, analysts_reports: Dict, symbol: str,
                           ctx_text: str, call, task_id: str) -> Dict:
        """风险位持仓辩论：Aggressive/Conservative/Neutral 三角色，TradingAgents Risk Debate 一轮"""
        reports_text = "\n".join(AIAnalysisService._report_line(r) for r in analysts_reports.values())
        trader_text = (f"交易方向：{trader_proposal.get('action')}；理由：{trader_proposal.get('reasoning')}"
                       f"；入场{trader_proposal.get('entry_price')} 止损{trader_proposal.get('stop_loss')}"
                       f" 仓位{trader_proposal.get('position_sizing')}")
        speakers = ["Aggressive", "Conservative", "Neutral"]
        frames = {
            "Aggressive": {
                "role": "激进风险分析师，主张把握高收益高弹性机会（连板效应、政策市、游资共识、北向双认可），用数据反驳保守与中性观点。",
                "system_key": "aggressive",
            },
            "Conservative": {
                "role": "保守风险分析师，强调回撤控制、解禁减持与估值消化风险，要求更厚安全边际，用数据反驳激进与中性观点。",
                "system_key": "conservative",
            },
            "Neutral": {
                "role": "中性风险分析师，平衡激进与保守，给出折中仓位与风控建议。",
                "system_key": "neutral",
            },
        }
        histories = {k: [] for k in ("aggressive", "conservative", "neutral")}
        last_others = {s: "" for s in speakers}

        for speaker in speakers:
            other_lines = [f"{s}: {last_others[s]}" for s in speakers if s != speaker and last_others[s]]
            system = (
                f"你是在风险辩论环节发言的{['激进', '保守', '中性'][speakers.index(speaker)]}风险分析师。{frames[speaker]['role']} "
                "结合交易提案与分析师报告发言。只输出JSON："
                "{\"stance\":\"看多/谨慎/中性\",\"argument\":\"60-140字\",\"suggested_position\":\"0-100数字\",\"suggested_stop_loss\":\"数字或null\"}"
            )
            user = (f"股票代码{symbol}\n{ctx_text}\n\n{speaker} 当前需回应：\n交易提案：{trader_text}\n"
                    + (("\n其他立场" + " | ".join(other_lines)) if other_lines else "")
                    + f"\n\n分析师观点：\n{reports_text}")
            arg_text = ""
            if call is not None:
                raw = await call(system, user)
                if raw:
                    histories[frames[speaker]["system_key"]].append(str(raw.get("argument") or ""))
                    arg_text = f"{speaker}: {raw.get('argument', '')}"
            if not arg_text:
                arg_text = f"{speaker}: 结合交易提案{trader_text[:60]}给出{'激进看多' if speaker == 'Aggressive' else ('谨慎控制风险' if speaker == 'Conservative' else '中性平衡')}观点。"
                histories[frames[speaker]["system_key"]].append(arg_text)
            last_others[speaker] = arg_text

        return {
            "history": [last_others[s] for s in speakers if last_others[s]],
            "aggressive_history": histories["aggressive"],
            "conservative_history": histories["conservative"],
            "neutral_history": histories["neutral"],
            "trader_reference": trader_text,
        }

    @staticmethod
    async def _portfolio_manager(risk_debate: Dict, plan: Dict, trader_proposal: Dict,
                                 symbol: str, ctx_text: str, past_context: str,
                                 call, task_id: str) -> Dict:
        """投资组合经理（Portfolio Manager）：综合风险辩论给出最终决策（Rating/执行摘要/逻辑/目标价/周期）"""
        decision = {"rating": "", "executive_summary": "", "investment_thesis": "",
                    "price_target": None, "time_horizon": ""}
        risk_history = "\n".join(risk_debate.get("history", []))
        lessons = (f"- 过往投资历史与复盘：\n{past_context}\n" if past_context else "")
        system = (
            "你是A股投资组合经理，综合全部信息给出最终决策。评级只取其一："
            "Buy(强烈建仓)/Overweight(偏多加仓)/Hold(持有)/Underweight(减仓)/Sell(清仓回避)。"
            "必须考虑A股约束与过往教训。只输出JSON："
            "{\"rating\":\"Buy/Overweight/Hold/Underweight/Sell\",\"executive_summary\":\"50-100字执行摘要(入场/仓位/关键风险/持有周期)\","
            "\"investment_thesis\":\"80-160字投资逻辑\",\"price_target\":\"数字或null\",\"time_horizon\":\"3-6个月\"}"
        )
        user = (
            f"股票代码{symbol}\n{ctx_text}\n\n{AIAnalysisService.A_STOCK_RULES}\n{lessons}"
            f"\n研究经理投资计划：{plan.get('recommendation')} {plan.get('rationale')}\n"
            f"\n交易员提案：{trader_proposal.get('action')} {trader_proposal.get('reasoning')} "
            f"仓{trader_proposal.get('position_sizing')}\n\n风险辩论：\n{risk_history}")
        if call is not None:
            raw = await call(system, user)
            if raw:
                low = str(raw.get("rating") or "").lower()
                rating = next((r for r in ("buy", "overweight", "hold", "underweight", "sell") if r in low), "")
                if rating:
                    decision["rating"] = {"buy": "Buy", "overweight": "Overweight", "hold": "Hold",
                                          "underweight": "Underweight", "sell": "Sell"}[rating]
                decision["executive_summary"] = str(raw.get("executive_summary") or "")
                decision["investment_thesis"] = str(raw.get("investment_thesis") or "")
                for k in ("price_target",):
                    v = raw.get(k)
                    if v not in (None, "", "null"):
                        try:
                            decision[k] = round(float(v), 2)
                        except (TypeError, ValueError):
                            decision[k] = None
                decision["time_horizon"] = str(raw.get("time_horizon") or "")
        if not decision["rating"]:
            decision["rating"] = plan.get("recommendation", "Hold")
            decision["executive_summary"] = f"维持研究经理评级{decision['rating']}；参考交易员提案{trader_proposal.get('action', 'Hold')}。"
            decision["investment_thesis"] = "综合多空辩论、交易提案与风险讨论后的审慎结论。"
        return decision

    @staticmethod
    def _parse_rating(text: str or Dict, default: str = "Hold") -> str:
        """从 PM 决策/文本解析 5 档评级"""
        rating = ""
        if isinstance(text, dict):
            rating = text.get("rating") or text.get("recommendation") or ""
        elif text:
            rating = text
        low = str(rating).lower()
        for key, mapped in (("overweight", "Overweight"), ("underweight", "Underweight"),
                            ("buy", "Buy"), ("sell", "Sell"), ("hold", "Hold")):
            if key in low:
                return mapped
        return default

    @staticmethod
    def _memory_load(symbol: str) -> str:
        """读取该标的历史决策记忆（近 5 条）作为 PM 的 past_context。"""
        try:
            with open(QUANT_CACHE_DIR / "ai_memory.json", encoding="utf-8") as f:
                mem = json.load(f)
            entries = mem.get(symbol, [])[-5:]
            if not entries:
                return ""
            return "\n".join(
                f"- {e.get('date', '')}: 评级{e.get('rating', '-')} -> {e.get('outcome', '待验证')} | {e.get('thesis', '')[:120]}" 
                for e in entries)
        except (FileNotFoundError, json.JSONDecodeError, KeyError):
            return ""

    @staticmethod
    def _memory_save(symbol: str, rating: str, thesis: str, signal: str, confidence: float):
        """记录本次决策到记忆文件（便于下次分析复盘）。"""
        try:
            path = QUANT_CACHE_DIR / "ai_memory.json"
            mem: Dict = {}
            try:
                with open(path, encoding="utf-8") as f:
                    mem = json.load(f)
            except (FileNotFoundError, json.JSONDecodeError):
                mem = {}
            entries = mem.setdefault(symbol, [])
            entries.append({
                "date": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                "rating": rating, "signal": signal, "confidence": confidence,
                "thesis": thesis, "outcome": None,
            })
            mem[symbol] = entries[-20:]
            with open(path, "w", encoding="utf-8") as f:
                json.dump(mem, f, ensure_ascii=False, indent=2)
        except Exception as e:  # noqa: BLE001
            logger.warning("ai memory save failed: %s", e)

    @staticmethod
    async def _run_full_pipeline(analysts_reports: Dict, symbol: str, ctx: Dict,
                                 llm_cfg: Optional[Dict], task_id: str) -> Dict:
        """TradingAgents-astock 完整流水线：质量校验 -> 多空辩论 -> 研究经理 -> 交易员        -> 三位风险辩手 -> 组合经理最终决策 -> 信号。各阶段 LLM 失败有降级。"""
        cfg = llm_cfg or {}
        has_llm = bool(cfg.get("base_url")) and bool(cfg.get("model"))

        async def call(system: str, user: str) -> Optional[Dict]:
            if not has_llm:
                return None
            return await asyncio.to_thread(
                call_llm_json, cfg.get("base_url"), cfg.get("api_key"), cfg.get("model"),
                system, user)

        bull_score = sum(r["report"]["score"] for r in analysts_reports.values()
                         if r["report"].get("verdict") == "看多")
        bear_score = sum(r["report"]["score"] for r in analysts_reports.values()
                         if r["report"].get("verdict") == "看空")
        total = bull_score + bear_score or 1
        bull_pct = bull_score / total
        ctx_text = AIAnalysisService._format_context(ctx)

        gate = await AIAnalysisService._quality_gate(
            analysts_reports, symbol, ctx_text, call if has_llm else None, task_id)
        debate = await AIAnalysisService._debate(
            analysts_reports, symbol, ctx_text, bull_score, bear_score, bull_pct,
            call if has_llm else None, task_id)
        plan = await AIAnalysisService._research_manager(
            debate, symbol, ctx_text, call if has_llm else None, task_id)
        trader_proposal = await AIAnalysisService._trader(
            analysts_reports, plan, symbol, ctx_text, call if has_llm else None, task_id)
        risk_debate = await AIAnalysisService._risk_debate(
            trader_proposal, analysts_reports, symbol, ctx_text,
            call if has_llm else None, task_id)
        past_context = AIAnalysisService._memory_load(symbol)
        pm = await AIAnalysisService._portfolio_manager(
            risk_debate, plan, trader_proposal, symbol, ctx_text, past_context,
            call if has_llm else None, task_id)

        return {
            "gate": gate,
            "debate": debate,
            "investment_plan": plan,
            "trader_proposal": trader_proposal,
            "risk_debate": risk_debate,
            "pm_decision": pm,
            "past_context": past_context,
        }

    @staticmethod
    async def analyze_stock(symbol: str, task_id: str,
                            llm_config: Optional[Dict] = None) -> Dict:
        """TradingAgents-astock 完整投研流水线入口。
        7分析师并发产出报告 -> 数据质量门 -> 多空研究员辩论 -> 研究经理投资计划
        -> 交易员交易提案 -> 三位风险辩手（激进/保守/中性） -> 组合经理 5 档评级
        -> 信号与记忆落盘。配置 llm_config（base_url + model）时各阶段用真实
        LLM；未配置或调用失败自动降级为规则/计票推断。"""
        cfg = llm_config or {}
        has_llm = bool(cfg.get("base_url")) and bool(cfg.get("model"))
        ctx: Dict = {}
        if has_llm:
            ctx = await asyncio.to_thread(AIAnalysisService._load_context, symbol)

        reports = await asyncio.gather(*[
            AIAnalysisService._run_analyst(stage_id, name, desc, symbol, ctx, llm_config, task_id)
            for stage_id, name, desc in AIAnalysisService.ANALYSTS
        ])

        analysts_reports = {}
        for (stage_id, name, desc), report in zip(AIAnalysisService.ANALYSTS, reports):
            analysts_reports[stage_id] = {
                "id": stage_id, "name": name, "description": desc,
                "status": "completed", "report": report,
            }

        pipeline = await AIAnalysisService._run_full_pipeline(
            analysts_reports, symbol, ctx, llm_config, task_id)

        gate = pipeline.get("gate") or {}
        debate = pipeline.get("debate") or {}
        plan = pipeline.get("investment_plan") or {}
        trader_proposal = pipeline.get("trader_proposal") or {}
        risk_debate = pipeline.get("risk_debate") or {}
        pm = pipeline.get("pm_decision") or {}
        past_context = pipeline.get("past_context") or ""
        debate_history = debate.get("history") or []

        rating = AIAnalysisService._parse_rating(pm, "Hold")
        if rating in ("Buy", "Overweight"):
            signal_name, direction, score = "BUY", "bull", 85
        elif rating in ("Sell", "Underweight"):
            signal_name, direction, score = "SELL", "bear", 25
        else:
            signal_name, direction, score = "HOLD", "neutral", 55

        bull_pct = debate.get("bull_percentage")
        base_conf = (bull_pct / 100.0) if isinstance(bull_pct, (int, float)) else 0.5
        if rating in ("Buy", "Overweight"):
            conf = 0.5 + base_conf / 2
        elif rating in ("Sell", "Underweight"):
            conf = 0.5 + (1 - base_conf) / 2
        else:
            conf = 0.5
        conf = min(0.95, max(0.3, conf))

        bull_args = debate.get("bull_history") or []
        bear_args = debate.get("bear_history") or []

        reason_parts = [p for p in (pm.get("executive_summary") or "",
                                    pm.get("investment_thesis") or "") if p]
        reason = " | ".join(reason_parts) or plan.get("rationale") or ""
        if not reason and debate_history:
            reason = debate_history[-1]

        final_signal = {
            "signal": signal_name,
            "direction": direction,
            "score": score,
            "confidence": round(conf, 3),
            "reason": reason,
            "bull_argument": bull_args[-1] if bull_args else "",
            "bear_argument": bear_args[-1] if bear_args else "",
            "rating": rating,
            "bull_score": debate.get("bull_score", 0),
            "bear_score": debate.get("bear_score", 0),
            "bull_percentage": debate.get("bull_percentage", 50.0),
        }

        AIAnalysisService._memory_save(
            symbol, rating,
            pm.get("investment_thesis") or plan.get("rationale") or "",
            signal_name, round(conf, 3))

        return {
            "symbol": symbol,
            "task_id": task_id,
            "status": "completed",
            "model": cfg.get("model") or "",
            "llm_used": has_llm,
            "analysts": analysts_reports,
            "debate": debate,
            "final_signal": final_signal,
            "quality_gate": gate,
            "investment_plan": plan,
            "trader_proposal": trader_proposal,
            "risk_debate": risk_debate,
            "pm_decision": pm,
            "signal_rating": rating,
            "confidence": round(conf, 3),
            "memory_context": past_context,
            "completed_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        }


# ============================================================
# 持仓管理
# ============================================================
