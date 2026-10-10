# -*- coding: utf-8 -*-
"""
core/nacos.py —— Nacos 服务注册与心跳（基于 Nacos OpenAPI v1，零额外 SDK 依赖）

单进程注册为单个逻辑服务（默认 py-service），各功能模块通过 metadata 暴露。
Nacos 不可用时仅打印告警并重试，不影响 FastAPI 服务本身运行。
"""
from __future__ import annotations

import json
import logging
import threading
from typing import Dict

import requests

from .config import Settings

logger = logging.getLogger("py-service.nacos")


class NacosRegistrar:
    def __init__(self, settings: Settings, metadata: Dict | None = None):
        self.s = settings
        self.metadata = metadata or {}
        self._stop = threading.Event()
        self._thread: threading.Thread | None = None
        self.registered = False

    # ────────────────────────────────────────────────
    def start(self) -> None:
        if not self.s.nacos_enabled:
            logger.info("Nacos 注册已禁用（NACOS_ENABLED=false）")
            return
        self._thread = threading.Thread(target=self._run, name="nacos-register", daemon=True)
        self._thread.start()

    def stop(self) -> None:
        self._stop.set()
        if self.registered:
            try:
                self._deregister()
            except Exception as exc:  # noqa: BLE001
                logger.warning("注销 Nacos 实例失败：%s", exc)

    # ────────────────────────────────────────────────
    def _base(self) -> str:
        server = self.s.nacos_server
        return server if server.startswith("http") else f"http://{server}"

    def _common_params(self) -> Dict[str, str]:
        return {
            "serviceName": self.s.service_name,
            "groupName": self.s.nacos_group,
            "clusterName": self.s.nacos_cluster,
            "namespaceId": self.s.nacos_namespace,
        }

    def _register(self) -> bool:
        params = {
            **self._common_params(),
            "ip": self.s.register_ip,
            "port": str(self.s.port),
            "weight": "1",
            "ephemeral": "true",
            "enabled": "true",
            "metadata": json.dumps(self.metadata, ensure_ascii=False),
        }
        url = f"{self._base()}/nacos/v1/ns/instance"
        r = requests.post(url, params=params, timeout=3)
        ok = r.text.strip() == "ok" or r.status_code == 200
        if ok:
            logger.info(
                "已注册到 Nacos：%s (%s:%s)",
                self.s.service_name, self.s.register_ip, self.s.port,
            )
        return ok

    def _beat(self) -> None:
        beat = {
            "serviceName": self.s.service_name,
            "ip": self.s.register_ip,
            "port": self.s.port,
            "cluster": self.s.nacos_cluster,
            "weight": 1,
            "period": int(self.s.heartbeat_interval),
            "scheduled": True,
            "metadata": self.metadata,
        }
        params = {
            **self._common_params(),
            "beat": json.dumps(beat, ensure_ascii=False),
        }
        url = f"{self._base()}/nacos/v1/ns/instance/beat"
        requests.put(url, params=params, timeout=3)

    def _deregister(self) -> None:
        params = {
            **self._common_params(),
            "ip": self.s.register_ip,
            "port": str(self.s.port),
            "ephemeral": "true",
        }
        url = f"{self._base()}/nacos/v1/ns/instance"
        requests.delete(url, params=params, timeout=3)
        logger.info("已从 Nacos 注销：%s", self.s.service_name)

    # ────────────────────────────────────────────────
    def _run(self) -> None:
        interval = self.s.heartbeat_interval
        warn_cadence = 0
        while not self._stop.is_set():
            try:
                if not self.registered:
                    self.registered = self._register()
                if self.registered:
                    self._beat()
                warn_cadence = 0
            except Exception as exc:  # noqa: BLE001
                # Nacos 宕机时避免刷屏：每约 12 个周期提示一次
                warn_cadence += 1
                if warn_cadence == 1 or warn_cadence % 12 == 0:
                    logger.warning("Nacos 注册/心跳失败（稍后重试）：%s", exc)
                self.registered = False
            self._stop.wait(interval)
