import io
import os

os.environ.setdefault("HF_ENDPOINT", "https://hf-mirror.com")
os.environ.setdefault("HF_HUB_DISABLE_XET", "1")

import tempfile

from fastapi import FastAPI, UploadFile, File
from faster_whisper import WhisperModel

app = FastAPI()

MODEL_SIZE = os.environ.get("WHISPER_MODEL", "small")
DEVICE = os.environ.get("WHISPER_DEVICE", "cpu")
COMPUTE = os.environ.get("WHISPER_COMPUTE", "int8")

_model = None


def get_model():
    global _model
    if _model is None:
        _model = WhisperModel(MODEL_SIZE, device=DEVICE, compute_type=COMPUTE)
    return _model


@app.get("/health")
def health():
    return {"ok": True, "model": MODEL_SIZE, "device": DEVICE}


@app.post("/transcribe")
async def transcribe(file: UploadFile = File(...), language: str = "zh"):
    data = await file.read()
    if not data:
        return {"text": ""}
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        tmp.write(data)
        tmp_path = tmp.name
    try:
        model = get_model()
        segments, _info = model.transcribe(
            tmp_path,
            language=language,
            beam_size=1,
            vad_filter=True,
            vad_parameters={"min_silence_duration_ms": 350},
        )
        text = "".join(s.text for s in segments).strip()
        return {"text": text}
    finally:
        try:
            os.unlink(tmp_path)
        except OSError:
            pass


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="127.0.0.1", port=9011)