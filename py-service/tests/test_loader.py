import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from core import loader
from core.config import Settings


def test_module_import_failures_are_reported(monkeypatch, tmp_path):
    broken = tmp_path / "broken"
    broken.mkdir()
    (broken / "__init__.py").write_text("", encoding="utf-8")
    monkeypatch.setattr(loader, "MODULES_DIR", tmp_path)

    def fail_import(name):
        raise ImportError("missing optional dependency")

    monkeypatch.setattr(loader.importlib, "import_module", fail_import)
    loaded = loader.load_modules(Settings(modules=[]))

    assert loaded == []
    assert loader.FAILED_MODULES == ["broken"]
