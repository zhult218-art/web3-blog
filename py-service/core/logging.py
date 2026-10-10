# -*- coding: utf-8 -*-
"""core/logging.py —— 统一日志配置。"""
from __future__ import annotations

import logging
import time


class DedupeFilter(logging.Filter):
    """相同日志模板在窗口期内只输出一次（抑制轮询场景的重复告警刷屏）。

    按 record.msg（格式模板）去重：`daily kline refresh %s fail` 换个股票代码
    仍算同一条；窗口内重复的 record 直接丢弃，首次照常打印，真实故障可见，
    只是不再每轮滚动刷屏。
    """
    WINDOW = 600.0      # 秒：同一模板 10 分钟内只打一次
    MAX_KEYS = 2000     # 防止键表无限膨胀

    def __init__(self) -> None:
        super().__init__()
        self._seen: dict[str, float] = {}

    def filter(self, record: logging.LogRecord) -> bool:
        now = time.monotonic()
        key = f"{record.name}:{record.levelno}:{record.msg}"
        last = self._seen.get(key)
        if last is not None and now - last < self.WINDOW:
            return False
        if len(self._seen) > self.MAX_KEYS:
            self._seen.clear()
        self._seen[key] = now
        return True


def setup_logging(level: str = "INFO") -> None:
    """配置全局日志格式；各模块用 logging.getLogger(<name>) 取子 logger。"""
    root = logging.getLogger()
    if root.handlers:
        return
    logging.basicConfig(
        level=getattr(logging, level.upper(), logging.INFO),
        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    )
    for h in root.handlers:
        h.addFilter(DedupeFilter())
