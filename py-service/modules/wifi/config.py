# -*- coding: utf-8 -*-
"""wifi 模块路径配置（含内置的 wifiphisher / wifite 第三方工具）。"""
from __future__ import annotations

from pathlib import Path

MODULE_DIR = Path(__file__).resolve().parent
VENDOR_DIR = MODULE_DIR / "vendor"

# 由 Z:\project\wifi 拷贝而来
WIFIPHISHER_DIR = VENDOR_DIR / "wifiphisher"
WIFIPHISHER_BIN = WIFIPHISHER_DIR / "bin" / "wifiphisher"
WIFITE_DIR = VENDOR_DIR / "wifite"
WIFITE_SCRIPT = WIFITE_DIR / "wifite.py"
