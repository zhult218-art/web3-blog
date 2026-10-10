# -*- coding: utf-8 -*-
"""量化模块配置：数据/输出/缓存目录。框架已加载根 .env，这里不重复 dotenv。"""
from __future__ import annotations

import os
from pathlib import Path

QUANT_DATA_DIR = Path(os.environ.get("QUANT_DATA_DIR", "./data"))
QUANT_OUTPUT_DIR = Path(os.environ.get("QUANT_OUTPUT_DIR", "./output"))
QUANT_CACHE_DIR = Path(os.path.expanduser("~/.quant-platform"))

for _d in [QUANT_DATA_DIR, QUANT_OUTPUT_DIR, QUANT_CACHE_DIR]:
    _d.mkdir(parents=True, exist_ok=True)
