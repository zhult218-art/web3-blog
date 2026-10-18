# -*- coding: utf-8 -*-
"""AI 上游 provider 目录与轮询：多个免费模型互相兜底，任一被限流/宕机自动切换。

候选来源（按优先级合并，重复的 base_url+model 自动去重）：
1. 环境变量 AI_PROVIDERS（JSON 数组）/ secrets.json 的 ai_providers（显式多渠道路由）
2. 现有单条配置 AI_BASE_URL / AI_API_KEY / AI_MODEL（向后兼容，仍作为其中一个候选）
3. 内置免费预设：配了对应 KEY 才启用；Pollinations 无需 key，可多模型轮换

轮询与兜底：
- 模块级游标 round-robin，避免请求总砸向同一个免费额度
- 命中 402/429 → 长冷却；其它错误 → 短冷却；冷却期内优先跳过（仍作最后兜底）
- 只有全部候选都不可用时才真正报错 → 「不要断我的 ai」
"""
from __future__ import annotations

import json
import logging
import os
import threading
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Sequence

logger = logging.getLogger("ai")

_SECRETS_PATH = Path(__file__).resolve().parents[2] / "data" / "cockpit" / "user_data" / "secrets.json"

_LIMIT_STATUS = {402, 429}
_COOLDOWN_LIMIT = 120.0   # 限流冷却（秒）
_COOLDOWN_ERROR = 30.0    # 其它错误冷却（秒）


@dataclass(frozen=True)
class Provider:
    """单个 OpenAI 兼容上游。

    fast=True 表示正规 API 免费档（首字通常 <2s）；False 表示共享的免费聚合
    网关（如 Pollinations，实测首字 19~30s）。轮询时快渠道优先。
    """
    name: str
    base_url: str
    api_key: str
    model: str
    fast: bool = False


# 内置免费预设：(名称, base_url, 启用所需环境变量, 默认模型, 是否快渠道)
# env 变量为空 → keyless，始终可用；否则仅在配置了该 KEY 时启用
# 可用 <PROVIDER>_MODEL 覆盖默认模型（如 GROQ_MODEL=openai/gpt-oss-20b）
# 说明：Pollinations 是共享免费聚合网关，实测首字 19~30s，故 fast=False；
#       正规 API 免费档（Groq/OpenRouter/Gemini 等）首字通常在 2s 内。
_PRESETS = (
    ("groq", "https://api.groq.com/openai/v1", "GROQ_API_KEY", "llama-3.1-8b-instant", True),
    ("openrouter", "https://openrouter.ai/api/v1", "OPENROUTER_API_KEY", "deepseek/deepseek-chat-v3-0324:free", True),
    ("gemini", "https://generativelanguage.googleapis.com/v1beta/openai", "GEMINI_API_KEY", "gemini-2.5-flash", True),
    ("mistral", "https://api.mistral.ai/v1", "MISTRAL_API_KEY", "open-mistral-nemo", True),
    ("siliconflow", "https://api.siliconflow.cn/v1", "SILICONFLOW_API_KEY", "Qwen/Qwen2.5-7B-Instruct", True),
    ("pollinations", "https://text.pollinations.ai/openai", "", "openai", False),
)

_lock = threading.Lock()
_cursor = 0
_cooldown: dict = {}   # provider.name -> 冷却截止时间戳

# ── 本机 Our Free Model（OFM）独立服务 ──
# 开源项目 dsh-our-free-model 的独立本地服务：免注册、免 Key，内置匿名与 Kilo
# 免费模型池，OpenAI 兼容。默认监听 127.0.0.1:18900，API Key 为服务首次启动时
# 自动生成并写入数据目录 settings.json 的 forwardKey 字段（本机回环，不出宿主）。
# 地址/模型可用 OUR_FREE_MODEL_BASE_URL / OUR_FREE_MODEL_MODEL 覆盖，
# 密钥也可用 OUR_FREE_MODEL_KEY 直接注入（跳过读文件）。
_OFM_DEFAULT_BASE = "http://127.0.0.1:18900/v1"
_OFM_DEFAULT_MODEL = "deepseek-v4.1-flash-free"
_OFM_DEFAULT_HOME = Path.home() / ".our-free-model"


def _ofm_home() -> Path:
    """OFM 数据目录：OFM_HOME 优先，否则用其默认的 ~/.our-free-model。"""
    raw = (os.environ.get("OFM_HOME") or "").strip()
    return Path(raw) if raw else _OFM_DEFAULT_HOME


def ofm_defaults() -> Optional[Dict[str, str]]:
    """本机 Our Free Model（OFM）默认连接信息：{base_url, api_key, model}。

    依赖轻量、永不抛异常：服务未启动 / 尚未生成密钥时返回 None。作为 OFM 解析
    的唯一来源，供本模块与 quant 等其它模块复用（量子模块据此拿到开箱即用的默认
    LLM 配置）。密钥优先取 OUR_FREE_MODEL_KEY 环境变量，否则读数据目录
    settings.json 的 forwardKey 字段。
    """
    try:
        key = (os.environ.get("OUR_FREE_MODEL_KEY") or "").strip()
        if not key:
            settings = _ofm_home() / "settings.json"
            if not settings.is_file():
                return None
            data = json.loads(settings.read_text(encoding="utf-8"))
            key = str((data or {}).get("forwardKey") or "").strip()
        if not key:
            return None
        base = (os.environ.get("OUR_FREE_MODEL_BASE_URL") or _OFM_DEFAULT_BASE).strip()
        model = (os.environ.get("OUR_FREE_MODEL_MODEL") or _OFM_DEFAULT_MODEL).strip()
        if not base:
            return None
        return {
            "base_url": base,
            "api_key": key,
            "model": model or _OFM_DEFAULT_MODEL,
        }
    except Exception as e:  # noqa: BLE001
        logger.debug("本机 Our Free Model 探测跳过: %s", e)
        return None


def _from_local_ofm() -> List[Provider]:
    """本机 Our Free Model 服务（免注册免费模型池）。

    服务在跑就自动加入轮询；未启动 / 尚未生成密钥时静默跳过，不影响其它上游
    （遵循本模块「配了就启用，没配就跳过」的既有约定，缺服务绝不报错）。
    """
    d = ofm_defaults()
    if not d:
        return []
    # 本机回环直连、实测首字约 3s，按快渠道处理（12s 超时快速淘汰）
    p = _mk("ourfreemodel", d["base_url"], d["api_key"], d["model"], True)
    return [p] if p else []


def _read_secrets() -> dict:
    try:
        if _SECRETS_PATH.is_file():
            data = json.loads(_SECRETS_PATH.read_text(encoding="utf-8"))
            return data if isinstance(data, dict) else {}
    except Exception as e:  # noqa: BLE001
        logger.debug("secrets.json 读取失败: %s", e)
    return {}


def _mk(name: str, base_url: object, api_key: object, model: object,
        fast: bool = False) -> Optional[Provider]:
    base = str(base_url or "").strip().rstrip("/")
    if not base:
        return None
    return Provider(
        name=str(name or base),
        base_url=base,
        api_key=str(api_key or "").strip(),
        model=str(model or "openai").strip() or "openai",
        fast=bool(fast),
    )


def _from_items(items: object, prefix: str) -> List[Provider]:
    out: List[Provider] = []
    if isinstance(items, list):
        for i, item in enumerate(items):
            if isinstance(item, dict):
                p = _mk(item.get("name") or f"{prefix}-{i}", item.get("base_url"),
                        item.get("api_key"), item.get("model"), item.get("fast", False))
                if p:
                    out.append(p)
    return out


def _from_env_json() -> List[Provider]:
    raw = (os.environ.get("AI_PROVIDERS") or "").strip()
    if not raw:
        return []
    try:
        return _from_items(json.loads(raw), "env")
    except Exception as e:  # noqa: BLE001
        logger.warning("AI_PROVIDERS 解析失败（应为 JSON 数组）: %s", e)
        return []


def _from_secrets_list() -> List[Provider]:
    try:
        return _from_items(_read_secrets().get("ai_providers"), "secret")
    except Exception as e:  # noqa: BLE001
        logger.debug("secrets ai_providers 解析失败: %s", e)
        return []


def _from_single_config() -> List[Provider]:
    """现有单条配置（保持向后兼容）。"""
    s = _read_secrets()
    base = s.get("ai_base_url") or os.environ.get("AI_BASE_URL") or os.environ.get("LLM_BASE_URL")
    key = s.get("ai_api_key") or os.environ.get("AI_API_KEY") or os.environ.get("LLM_API_KEY")
    model = s.get("ai_model") or os.environ.get("AI_MODEL") or os.environ.get("LLM_MODEL")
    p = _mk("primary", base, key, model)
    return [p] if p else []


def _from_presets() -> List[Provider]:
    out: List[Provider] = []
    for name, url, env_key, model, fast in _PRESETS:
        if env_key:
            if not (os.environ.get(env_key) or "").strip():
                continue   # 未配置该 KEY → 跳过，不报错
            key = os.environ.get(env_key, "")
            model = os.environ.get(env_key.replace("_API_KEY", "_MODEL"), "") or model
        else:
            key = "sk-pollinations-free"   # keyless 免费网关
        p = _mk(name, url, key, model, fast)
        if p:
            out.append(p)
    return out


def resolve_providers() -> List[Provider]:
    """按优先级合并所有候选 provider，并按 base_url+model 去重。"""
    seen = set()
    out: List[Provider] = []
    # 本机 Our Free Model 免费池排在最前：本地回环、免注册，作为首选快渠道
    for group in (_from_local_ofm(), _from_env_json(), _from_secrets_list(),
                  _from_single_config(), _from_presets()):
        for p in group:
            sig = (p.base_url, p.model)
            if sig in seen:
                continue
            seen.add(sig)
            out.append(p)
    return out


def ordered_candidates() -> List[Provider]:
    """返回本轮候选顺序：round-robin 轮换起点，未冷却的在前、冷却中的垫底兜底。"""
    global _cursor
    provs = resolve_providers()
    if not provs:
        return []
    with _lock:
        start = _cursor % len(provs)
        _cursor = (_cursor + 1) % len(provs)
    rotated = provs[start:] + provs[:start]
    now = time.time()
    ready_fast, ready_slow, cooling = [], [], []
    for p in rotated:
        if _cooldown.get(p.name, 0.0) > now:
            cooling.append(p)
        elif p.fast:
            ready_fast.append(p)
        else:
            ready_slow.append(p)
    # 快渠道优先：慢网关（如 19~30s 首字的 Pollinations）排在后面，不拖住快渠道
    return ready_fast + ready_slow + cooling


def mark_failed(p: Provider, status: Optional[int] = None) -> None:
    """记录一次失败：限流类命中长冷却，其它错误短冷却。"""
    cd = _COOLDOWN_LIMIT if status in _LIMIT_STATUS else _COOLDOWN_ERROR
    with _lock:
        _cooldown[p.name] = time.time() + cd
    logger.info("AI provider 冷却 %s（status=%s, %.0fs）", p.name, status, cd)


def mark_ok(p: Provider) -> None:
    with _lock:
        _cooldown.pop(p.name, None)


def read_timeout(p: Provider) -> float:
    """静默/首字等待上限（秒）：快渠道 12s 快速淘汰，慢网关 45s（宁可慢也不断）。"""
    return 12.0 if p.fast else 45.0


def proxy_for(p: Provider) -> Optional[str]:
    """该 provider 使用的 HTTP 代理（按 <NAME>_PROXY > AI_PROXY 优先级）。

    中国大陆直连部分境外 API（如 Groq）会被 WAF 拒绝（403），需经本地代理；
    未配置则返回 None（直连），因此国内直连可用的 provider 不受影响。
    """
    v = (os.environ.get(f"{p.name.upper()}_PROXY") or os.environ.get("AI_PROXY") or "").strip()
    return v or None


def extra_options(p: Provider) -> dict:
    """provider 专属附加请求参数（<NAME>_EXTRA_JSON，如 {"reasoning_effort":"low"}）。

    仅对该 provider 生效，避免把不支持的参数发给其它上游（如 Pollinations）而报错。
    """
    raw = (os.environ.get(f"{p.name.upper()}_EXTRA_JSON") or "").strip()
    if not raw:
        return {}
    try:
        obj = json.loads(raw)
        return obj if isinstance(obj, dict) else {}
    except Exception:  # noqa: BLE001
        logger.warning("provider %s 的 %s_EXTRA_JSON 解析失败，忽略", p.name, p.name.upper())
        return {}


def endpoint(p: Provider) -> str:
    if p.base_url.endswith("/chat/completions"):
        return p.base_url
    return p.base_url + "/chat/completions"


def headers(p: Provider) -> dict:
    h = {"Content-Type": "application/json", "Accept": "*/*"}
    if p.api_key:
        h["Authorization"] = "Bearer " + p.api_key
    return h


def build_payload(p: Provider, messages: Optional[Sequence[dict]], system: Optional[str],
                  temperature: Optional[float], max_tokens: Optional[int],
                  model: Optional[str]) -> dict:
    msgs = []
    if system:
        msgs.append({"role": "system", "content": str(system)})
    for m in messages or []:
        role = str((m or {}).get("role") or "user")
        content = str((m or {}).get("content") or "")
        if role in ("system", "user", "assistant") and content:
            msgs.append({"role": role, "content": content})
    payload = {
        "model": (model or p.model),
        "messages": msgs,
        "temperature": 0.7 if temperature is None else float(temperature),
    }
    if max_tokens:
        payload["max_tokens"] = int(max_tokens)
    payload.update(extra_options(p))   # provider 专属参数（如 groq 的 reasoning_effort）
    return payload
