# -*- coding: utf-8 -*-
"""
core/loader.py —— 功能模块自动发现 / 装配

模块约定（每个功能 = modules/<name>/ 一个包）。包内可按需定义：
  • router        : fastapi.APIRouter，会被原样挂载（路径由模块自行决定）
  • MODULE_META   : dict，元信息 {title, description, icon, version}
  • setup(app)    : 构建期调用一次，用于 StaticFiles 等挂载（同步函数）
  • startup()     : 应用启动时调用，可为普通函数或协程
  • shutdown()    : 应用关闭时调用，可为普通函数或协程
  • health()      : 可选，返回 dict，供聚合 /health 展示模块运行状态

新增服务：在 modules/ 下新建目录并提供 router 即可，本加载器会自动发现，
无需改动框架其它文件；也可用环境变量 PY_SERVICE_MODULES 精确控制启用与顺序。
"""
from __future__ import annotations

import importlib
import inspect
import logging
from dataclasses import dataclass, field
from types import ModuleType
from typing import Any, Callable, List

from .config import MODULES_DIR, Settings

logger = logging.getLogger("py-service.loader")
FAILED_MODULES: List[str] = []


@dataclass
class LoadedModule:
    name: str
    pkg: ModuleType
    router: Any = None
    meta: dict = field(default_factory=dict)
    setup: Callable | None = None
    startup: Callable | None = None
    shutdown: Callable | None = None
    health: Callable | None = None

    @property
    def title(self) -> str:
        return self.meta.get("title") or self.name


def _discover_names(settings: Settings) -> List[str]:
    """返回待加载模块名列表：环境变量指定时按其顺序，否则扫描目录（字母序）。"""
    if settings.modules:
        return list(settings.modules)
    if not MODULES_DIR.exists():
        return []
    names = [
        p.name
        for p in MODULES_DIR.iterdir()
        if p.is_dir() and (p / "__init__.py").exists() and not p.name.startswith("_")
    ]
    return sorted(names)


def load_modules(settings: Settings) -> List[LoadedModule]:
    """导入并装配所有启用的模块；单个模块失败不影响其余模块启动。"""
    loaded: List[LoadedModule] = []
    FAILED_MODULES.clear()
    for name in _discover_names(settings):
        try:
            pkg = importlib.import_module(f"modules.{name}")
        except Exception as exc:  # noqa: BLE001
            FAILED_MODULES.append(name)
            logger.exception("模块 %s 导入失败，已跳过：%s", name, exc)
            continue

        loaded.append(
            LoadedModule(
                name=name,
                pkg=pkg,
                router=getattr(pkg, "router", None),
                meta=getattr(pkg, "MODULE_META", {}) or {},
                setup=getattr(pkg, "setup", None),
                startup=getattr(pkg, "startup", None),
                shutdown=getattr(pkg, "shutdown", None),
                health=getattr(pkg, "health", None),
            )
        )
        logger.info("已加载模块：%s", name)
    return loaded


def maybe_await(result: Any) -> Any:
    """模块生命周期函数可能返回协程，交由调用方在事件循环中 await。"""
    return result


def is_awaitable(func: Callable) -> bool:
    return inspect.iscoroutinefunction(func)
