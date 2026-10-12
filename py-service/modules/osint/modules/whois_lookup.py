# -*- coding: utf-8 -*-
"""WHOIS 查询模块（移植自 SpiderFoot sfp_whois）。

优先使用 python-whois 库，回退到系统 whois 命令（subprocess）。
监听 DOMAIN_NAME 事件，产生 DOMAIN_WHOIS / DOMAIN_REGISTRAR 事件。
"""
from __future__ import annotations

import json
import os
import shutil
import subprocess

from ..engine import (
    DOMAIN_NAME, DOMAIN_WHOIS, DOMAIN_REGISTRAR,
    OsintEvent, OsintModule, register_module,
)


class WhoisModule(OsintModule):
    meta = {
        "name": "whois_lookup",
        "title": "WHOIS 查询",
        "summary": "查询域名注册信息（注册商、注册日期、到期日期、DNS 服务器）",
        "category": "Public Registries",
    }

    def watched_events(self) -> list[str]:
        return [DOMAIN_NAME]

    def produced_events(self) -> list[str]:
        return [DOMAIN_WHOIS, DOMAIN_REGISTRAR]

    def handle_event(self, event, target, emit) -> None:
        domain = event.data

        whois_data = self._query_whois(domain)
        if not whois_data:
            return

        # 原始 WHOIS 文本
        raw_text = whois_data.get("raw_text", "")
        if raw_text:
            emit(OsintEvent(DOMAIN_WHOIS, raw_text[:5000],
                            self.meta["name"], event))

        # 注册商
        registrar = whois_data.get("registrar", "")
        if registrar:
            emit(OsintEvent(DOMAIN_REGISTRAR, registrar,
                            self.meta["name"], event))

    def _query_whois(self, domain: str) -> dict:
        # 尝试 python-whois 库
        try:
            import whois as _whois
            w = _whois.whois(domain)
            raw = str(w.text) if hasattr(w, "text") and w.text else json.dumps(
                {k: str(v) for k, v in w.__dict__.items() if v}, ensure_ascii=False, indent=2
            )
            return {
                "raw_text": raw,
                "registrar": getattr(w, "registrar", "") or "",
                "creation_date": str(getattr(w, "creation_date", "")),
                "expiration_date": str(getattr(w, "expiration_date", "")),
            }
        except ImportError:
            pass
        except Exception:  # noqa: BLE001
            pass

        # 回退到系统 whois 命令
        whois_bin = shutil.which("whois")
        if whois_bin:
            try:
                result = subprocess.run(
                    [whois_bin, domain],
                    capture_output=True, timeout=15,
                    creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
                )
                raw = result.stdout.decode("utf-8", errors="ignore")
                registrar = ""
                for line in raw.splitlines():
                    if "Registrar:" in line or "Sponsoring Registrar:" in line:
                        registrar = line.split(":", 1)[1].strip()
                        break
                return {"raw_text": raw, "registrar": registrar}
            except (subprocess.TimeoutExpired, OSError):
                pass

        return {}


register_module(WhoisModule())
