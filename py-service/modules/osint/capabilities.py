# -*- coding: utf-8 -*-
"""OSINT 模块能力检测：可用模块 / 外部依赖 / API Key。"""
from __future__ import annotations

import os

from . import engine


def summary() -> dict:
    modules = engine.list_modules()
    for m in modules:
        # Shodan 需要检测 API Key
        if m["name"] == "shodan_lookup":
            m["available"] = bool(os.environ.get("SHODAN_API_KEY"))
        else:
            m["available"] = True

    return {
        "module_count": len(modules),
        "modules": modules,
        "env_keys": {
            "shodan": bool(os.environ.get("SHODAN_API_KEY")),
        },
    }
