# -*- coding: utf-8 -*-
"""
core/config.py —— 统一 Python 服务（py-service）配置中心

设计目标：
1. 所有运行参数从环境变量读取，仓库根 .env / py-service/.env 兜底；
2. 单进程、单端口，所有功能模块（modules/*）共用本配置；
3. 后期新增服务只需新增 modules/<name>/ 目录，无需改动本文件。
"""
from __future__ import annotations

import os
import socket
from dataclasses import dataclass, field
from pathlib import Path
from typing import List

# py-service/ 与仓库根 web3/
BASE_DIR = Path(__file__).resolve().parent.parent
ROOT_DIR = BASE_DIR.parent
MODULES_DIR = BASE_DIR / "modules"


def _load_env_file(path: Path) -> None:
    """轻量 .env 加载（仅填充尚未设置的环境变量）。"""
    if not path.exists():
        return
    try:
        for raw in path.read_text(encoding="utf-8").splitlines():
            line = raw.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, _, v = line.partition("=")
            k, v = k.strip(), v.strip().strip('"').strip("'")
            if k and os.environ.get(k) is None:
                os.environ[k] = v
    except Exception:  # noqa: BLE001
        pass


def load_dotenv() -> None:
    """依次加载仓库根 .env 与 py-service/.env（进程已存在的环境变量优先）。"""
    _load_env_file(ROOT_DIR / ".env")
    _load_env_file(BASE_DIR / ".env")


def _csv(value: str) -> List[str]:
    return [p.strip() for p in value.split(",") if p.strip()]


def detect_lan_ip() -> str:
    """探测本机首选 LAN IP（UDP connect 不实际发包）；失败回退 127.0.0.1。"""
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        sock.connect(("8.8.8.8", 80))
        return sock.getsockname()[0]
    except Exception:  # noqa: BLE001
        return "127.0.0.1"
    finally:
        sock.close()


@dataclass
class Settings:
    # ── 服务基本信息（注册到 Nacos 的服务名） ──
    service_name: str = "py-service"
    host: str = "0.0.0.0"
    port: int = 9006

    # ── Nacos 注册中心 ──
    nacos_enabled: bool = True
    nacos_server: str = "127.0.0.1:8848"
    nacos_namespace: str = ""
    nacos_group: str = "DEFAULT_GROUP"
    nacos_cluster: str = "DEFAULT"
    register_ip: str = ""
    heartbeat_interval: float = 5.0

    # ── 模块开关/顺序（空 = 自动加载全部 modules/*） ──
    modules: List[str] = field(default_factory=list)

    # ── CORS 允许来源（空 = 仅允许本地开发来源，部署域名需显式配置） ──
    cors_origins: List[str] = field(default_factory=list)

    # ── 运行数据目录 ──
    data_dir: Path = BASE_DIR / "data"

    def ensure_dirs(self) -> None:
        self.data_dir.mkdir(parents=True, exist_ok=True)


def _truthy(value: str | None) -> bool:
    return (value or "").strip().lower() in ("1", "true", "yes", "on")


_cached: Settings | None = None


def get_settings() -> Settings:
    """读取并缓存配置（调用前应先 load_dotenv()）。"""
    global _cached
    if _cached is not None:
        return _cached

    register_ip = os.environ.get("PY_SERVICE_IP", "").strip() or detect_lan_ip()
    cors_raw = os.environ.get("CORS_ALLOWED_ORIGINS", "").strip()

    settings = Settings(
        service_name=os.environ.get("PY_SERVICE_NAME", "py-service").strip(),
        host=os.environ.get("PY_SERVICE_HOST", "0.0.0.0").strip(),
        port=int(os.environ.get("PY_SERVICE_PORT", "9006")),
        nacos_enabled=_truthy(os.environ.get("NACOS_ENABLED", "true")),
        nacos_server=os.environ.get("NACOS_SERVER", "127.0.0.1:8848").strip(),
        nacos_namespace=os.environ.get("NACOS_NAMESPACE", "").strip(),
        nacos_group=os.environ.get("NACOS_GROUP", "DEFAULT_GROUP").strip(),
        nacos_cluster=os.environ.get("NACOS_CLUSTER", "DEFAULT").strip(),
        register_ip=register_ip,
        heartbeat_interval=float(os.environ.get("NACOS_HEARTBEAT_INTERVAL", "5")),
        modules=_csv(os.environ.get("PY_SERVICE_MODULES", "")),
        cors_origins=_csv(cors_raw),
        data_dir=Path(os.environ.get("PY_SERVICE_DATA_DIR", str(BASE_DIR / "data"))),
    )
    settings.ensure_dirs()
    _cached = settings
    return settings
