import os
import subprocess
import sys
from pathlib import Path


CONFIG = Path(__file__).resolve().parents[1] / "modules" / "video" / "config.py"


def test_video_config_rejects_missing_admin_token():
    env = os.environ.copy()
    env.pop("VIDEO_ADMIN_TOKEN", None)
    result = subprocess.run(
        [sys.executable, "-c", f"import runpy; runpy.run_path({str(CONFIG)!r})"],
        env=env,
        capture_output=True,
        text=True,
    )
    assert result.returncode != 0
    assert "VIDEO_ADMIN_TOKEN must be set" in result.stderr


def test_video_config_accepts_configured_admin_token():
    env = os.environ.copy()
    env["VIDEO_ADMIN_TOKEN"] = "test-token-with-sufficient-randomness"
    result = subprocess.run(
        [sys.executable, "-c", f"import runpy; runpy.run_path({str(CONFIG)!r})"],
        env=env,
        capture_output=True,
        text=True,
    )
    assert result.returncode == 0, result.stderr
