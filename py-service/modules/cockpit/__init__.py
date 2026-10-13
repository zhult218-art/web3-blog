# -*- coding: utf-8 -*-
"""
cockpit 模块 —— 市场驾驶舱（tick-stock-panel 代码级移植）

将 tick-stock-panel-main/backend/app/ 整体复制为本模块的 app/ 子包，
通过 sys.path 注入使原 app.* import 链正常工作。
startup()/shutdown() 移植自 main.py 的 _application_lifespan。

功能：A股选股 + 监控 + 回测 + 因子挖掘 + AI 复盘 + 市场环境 + 异动监控
"""
from __future__ import annotations

import logging
import os
import sys
import threading
from pathlib import Path

logger = logging.getLogger("cockpit")

# ── 路径与配置注入 ──────────────────────────────────────────
_COCKPIT_DIR = Path(__file__).resolve().parent
_APP_DIR = _COCKPIT_DIR / "app"
_DATA_DIR = _COCKPIT_DIR.parent.parent / "data" / "cockpit"
_STATIC_DIR = _COCKPIT_DIR / "static"

# 将 cockpit 模块目录加入 sys.path，使 `from app.xxx import yyy` 正常工作
if str(_COCKPIT_DIR) not in sys.path:
    sys.path.insert(0, str(_COCKPIT_DIR))

# 注入环境变量，覆盖 tick-stock-panel config.py 的默认路径
# （_PROJECT_ROOT 推导为 modules/，需用实际路径覆盖 tiers_yaml/static_dir/data_dir）
os.environ.setdefault("DATA_DIR", str(_DATA_DIR))
os.environ.setdefault("STATIC_DIR", str(_STATIC_DIR))
os.environ.setdefault("TIERS_YAML", str(_COCKPIT_DIR / "tiers.yaml"))

from .router import router  # noqa: E402

MODULE_META = {
    "title": "市场驾驶舱",
    "description": "A股选股+监控+回测+因子挖掘+AI复盘（tick-stock-panel 移植）",
    "icon": "🚀",
    "version": "0.2.4",
}

# 运行时状态（startup 填充，shutdown 清理，health 读取）
_state: dict = {}


def health() -> dict:
    """模块健康检查。"""
    return {
        "ok": _state.get("ready", False),
        "service": "cockpit",
        "version": MODULE_META["version"],
        "capabilities": len(_state.get("capabilities", []) or []),
    }


def setup(app):
    """构建期：保存 app 引用 + 挂载 React 前端静态文件（如果 dist 已构建）。"""
    _state["_app"] = app
    if _STATIC_DIR.exists() and (_STATIC_DIR / "index.html").exists():
        from fastapi.staticfiles import StaticFiles
        app.mount("/cockpit/static", StaticFiles(directory=str(_STATIC_DIR)), name="cockpit_static")
        logger.info("cockpit 静态前端已挂载: %s", _STATIC_DIR)
    else:
        logger.warning("cockpit 前端未构建（%s 不存在），仅 API 可用", _STATIC_DIR)


def startup():
    """应用启动：移植自 main.py _application_lifespan。

    初始化 DataStore / KlineRepository / 策略引擎 / 调度器等核心服务。
    失败的服务只记日志不阻断启动（与原项目一致）。
    """
    import app.__init__ as app_init  # noqa: F401  触发 UTF-8 stdout 重新配置
    from app.tickflow import client as tf_client
    from app.tickflow.repository import DataStore, KlineRepository
    from app.tickflow.policy import detect_capabilities

    # 获取 py-service app 的 state 引用，将 tick-stock-panel 的服务实例注入其中
    app_state = getattr(_state.get("_app"), "state", None)

    def _inject(**kwargs):
        """将键值对同时写入 _state 和 app.state，使 request.app.state.xxx 可用。"""
        for k, v in kwargs.items():
            _state[k] = v
            if app_state is not None:
                setattr(app_state, k, v)

    logger.info("Tick Stock Panel v%s starting (mode=%s)", MODULE_META["version"], tf_client.current_mode())

    # ── 数据层 ──
    store = DataStore()
    repo = KlineRepository(store)
    _inject(datastore=store, repo=repo)
    # indicators_ready 标志（enriched 缓存构建完成后置 True）
    if app_state is not None:
        app_state.indicators_ready = False

    # 自定义因子载入
    try:
        from app.factors.store import load_into_registry
        loaded = load_into_registry(store.data_dir)
        if loaded:
            logger.info("custom factors loaded: %s", len(loaded))
    except Exception as e:
        logger.warning("custom factors load failed: %s", e)

    # Mining 管理器
    try:
        from app.services.mining_manager import MiningJobManager
        mining_manager = MiningJobManager(store.data_dir)
        recovered = mining_manager.recover_interrupted()
        _inject(mining_manager=mining_manager)
        if recovered:
            logger.warning("recovered %d interrupted mining runs", recovered)
    except Exception as e:
        logger.warning("mining manager init failed: %s", e)

    # enriched 缓存预热
    try:
        # 预热完成回调（移植自 app/main.py lifespan）：后台 daemon 线程算完
        # enriched 切片后把 indicators_ready 置 True，前端 /data/status 据此
        # 解除"指标未就绪"降级态。此前遗漏注入导致该标志永久停留 False。
        def _on_warmup_done() -> None:
            _state["indicators_ready"] = True
            if app_state is not None:
                setattr(app_state, "indicators_ready", True)

        repo._on_warmup_done = _on_warmup_done
        repo.refresh_cache(background=True)
    except Exception as e:
        logger.warning("enriched cache prewarm failed: %s", e)

    # 自定义数据源
    try:
        from app.data_providers import custom as custom_sources
        custom_sources.load_all()
        logger.info("custom data sources loaded: %d", len(custom_sources.list_sources()))
    except Exception as e:
        logger.warning("custom data sources init failed: %s", e)

    # 能力探测
    try:
        capset = detect_capabilities()
        _inject(capabilities=capset)
        logger.info("ready; %d capabilities active", len(capset.all()))
    except Exception as e:
        logger.warning("capability detection failed: %s", e)
        _inject(capabilities=[])

    # 全局行情服务
    qs = None
    try:
        from app.services.quote_service import QuoteService
        qs = QuoteService()
        qs.set_repo(repo)
        qs.boot_check()
        _inject(quote_service=qs)
    except Exception as e:
        logger.warning("quote service init failed: %s", e)

    # 策略监控
    try:
        from app.strategy.monitor import StrategyMonitorService
        strategy_monitor = StrategyMonitorService()
        _inject(strategy_monitor=strategy_monitor)
        if qs and app_state is not None:
            qs.set_app_state(app_state)
    except Exception as e:
        logger.warning("strategy monitor init failed: %s", e)

    # 五档盘口 sealed 服务
    try:
        from app.services.depth_service import DepthService
        depth_service = DepthService()
        depth_service.set_repo(repo)
        if app_state is not None:
            depth_service.set_app_state(app_state)
        _inject(depth_service=depth_service)
        depth_service.boot_check()
        depth_service.start_polling()
    except Exception as e:
        logger.warning("depth_service init failed: %s", e)

    # 定时调度器
    try:
        from app.jobs import daily_pipeline
        if app_state is not None:
            daily_pipeline.set_app_state(app_state)
        scheduler = daily_pipeline.start_scheduler(repo, _state.get("capabilities"))
        _inject(scheduler=scheduler)
    except Exception as e:
        logger.warning("scheduler not started: %s", e)
        _inject(scheduler=None)

    # 盘中分钟增量刷新
    try:
        from app.services.minute_refresh import MinuteRefreshService
        minute_refresh = MinuteRefreshService(repo)
        if app_state is not None:
            minute_refresh.set_app_state(app_state)
        _inject(minute_refresh=minute_refresh)
        minute_refresh.start()
    except Exception as e:
        logger.warning("minute_refresh init failed: %s", e)

    # 数据完整性自检
    try:
        from app.services.data_integrity import boot_integrity_check
        if app_state is not None:
            timer = threading.Timer(30.0, boot_integrity_check, args=(app_state,))
            timer.daemon = True
            timer.start()
    except Exception as e:
        logger.warning("integrity boot check scheduling failed: %s", e)

    # 企业微信机器人（可选）
    try:
        from app.services.wecom_bot_service import WecomBotService
        wecom_bot = WecomBotService()
        if app_state is not None:
            wecom_bot.set_app_state(app_state)
        _inject(wecom_bot_service=wecom_bot)
        wecom_bot.boot_check()
    except Exception as e:
        logger.warning("wecom_bot_service init failed: %s", e)

    # 内置扩展表
    # startup() 运行在 uvicorn 事件循环线程内，不能在当前线程再
    # new_event_loop().run_until_complete()（报 "Cannot run the event loop
    # while another loop is running"）。放到独立守护线程用 asyncio.run 执行。
    try:
        import asyncio
        from app.services.ext_presets import ensure_builtin_presets

        def _ensure_presets() -> None:
            try:
                asyncio.run(ensure_builtin_presets(store.data_dir))
            except Exception as ex:  # noqa: BLE001
                logger.warning("内置扩展表初始化失败 (不影响启动): %s", ex)

        threading.Thread(target=_ensure_presets, name="cockpit-presets", daemon=True).start()
    except Exception as e:
        logger.warning("内置扩展表初始化调度失败 (不影响启动): %s", e)

    # 扩展数据定时拉取
    try:
        from app.services.ext_pull import pull_scheduler
        pull_scheduler.start(store.data_dir)
        pull_scheduler.refresh(store.data_dir)
        _inject(pull_scheduler=pull_scheduler)
    except Exception as e:
        logger.warning("ext_pull scheduler failed: %s", e)

    # 财务数据同步调度器
    try:
        from app.services.financial_sync import financial_scheduler
        financial_scheduler.start(store.data_dir, _state.get("capabilities"))
        _inject(financial_scheduler=financial_scheduler)
    except Exception as e:
        logger.warning("financial scheduler failed: %s", e)

    # 策略引擎
    try:
        from app.strategy.engine import StrategyEngine
        from app.strategy import config as strategy_config
        strategy_dirs = [
            _APP_DIR / "strategy" / "builtin",
            store.data_dir / "strategies" / "custom",
            store.data_dir / "strategies" / "ai",
            store.data_dir / "strategies" / "composite",
        ]
        strategy_engine = StrategyEngine(
            strategy_dirs=strategy_dirs,
            override_loader=lambda sid: strategy_config.load_override(store.data_dir, sid),
        )
        _inject(strategy_engine=strategy_engine)
        logger.info("strategy engine loaded: %d strategies", len(strategy_engine.list_strategies()))
    except Exception as e:
        logger.warning("strategy engine init failed: %s", e)

    # 监控规则引擎
    try:
        from app.strategy.monitor import MonitorRuleEngine
        from app.strategy import monitor_rules as mr_store
        monitor_engine = MonitorRuleEngine()
        if _state.get("strategy_engine"):
            monitor_engine.set_strategy_engine(_state["strategy_engine"])
        monitor_engine.set_data_dir(store.data_dir)
        rules = mr_store.load_all(store.data_dir)
        monitor_engine.set_rules(rules)
        _inject(monitor_engine=monitor_engine)
        logger.info("monitor engine loaded: %d rules", monitor_engine.rule_count)
    except Exception as e:
        logger.warning("monitor engine load failed: %s", e)

    _state["ready"] = True
    logger.info("cockpit 模块启动完成")


def shutdown():
    """应用关闭：移植自 main.py lifespan finally 块。"""
    # Mining manager
    mm = _state.get("mining_manager")
    if mm:
        try:
            mm.shutdown()
        except Exception as e:
            logger.warning("mining manager shutdown: %s", e)

    # Scheduler
    sched = _state.get("scheduler")
    if sched:
        try:
            sched.shutdown(wait=False)
        except Exception as e:
            logger.warning("scheduler shutdown: %s", e)

    # Pull scheduler
    ps = _state.get("pull_scheduler")
    if ps:
        try:
            ps.stop()
        except Exception as e:
            logger.warning("pull scheduler shutdown: %s", e)

    # Financial scheduler
    fsc = _state.get("financial_scheduler")
    if fsc:
        try:
            fsc.stop()
        except Exception as e:
            logger.warning("financial scheduler shutdown: %s", e)

    # Quote service
    qs = _state.get("quote_service")
    if qs:
        try:
            qs.stop()
        except Exception as e:
            logger.warning("quote service shutdown: %s", e)

    # Depth service
    dsvc = _state.get("depth_service")
    if dsvc:
        try:
            dsvc.stop_polling()
        except Exception as e:
            logger.warning("depth service shutdown: %s", e)

    # Wecom bot
    wbot = _state.get("wecom_bot_service")
    if wbot:
        try:
            wbot.stop()
        except Exception as e:
            logger.warning("wecom bot shutdown: %s", e)

    # Minute refresh
    mrs = _state.get("minute_refresh")
    if mrs:
        try:
            mrs.stop()
        except Exception as e:
            logger.warning("minute refresh shutdown: %s", e)

    _state.clear()
    logger.info("cockpit 模块已关闭")


__all__ = ["router", "MODULE_META", "health", "startup", "shutdown", "setup"]
