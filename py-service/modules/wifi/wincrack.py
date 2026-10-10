# -*- coding: utf-8 -*-
"""
WpaCracker —— Windows 下 WPA/WPA2 离线破解引擎（wifite 破解环节的对位实现）。

输入：hashcat 22000 格式哈希（PMKID 或 EAPOL MIC），可粘贴文本或上传文件。
两种执行方式：
  · python：纯标准库 PBKDF2-HMAC-SHA1 推导 PMK，逐条口令验证（速度慢，适合小字典）；
  · hashcat：探测到 hashcat.exe 时调用（GPU 加速，-m 22000），实时回传日志。

抓握手本身仍需要监听/注入（Linux 网卡），本引擎负责「拿到握手之后」的破解环节，
这部分在 Windows 上完全可用；hashcat 也是 Windows 原生程序。
"""
from __future__ import annotations

import hashlib
import hmac
import os
import shutil
import struct
import threading
import time
import uuid

from .process import decode_output

DATA_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
    "data", "wifi", "crack",
)


# ============================================================
# 22000 目标解析与验证
# ============================================================
class WpaTarget:
    def __init__(self, kind: str, **kw):
        self.kind = kind           # "pmkid" / "mic"
        self.essid: bytes = b""
        self.mic: bytes = b""
        self.mac_ap: bytes = b""
        self.mac_sta: bytes = b""
        self.nonce_ap: bytes = b""
        self.nonce_sta: bytes = b""
        self.frame: bytes = b""
        self.mic_off = 0
        self.keyver = 2
        self.hash_line = ""
        for k, v in kw.items():
            setattr(self, k, v)

    @property
    def label(self) -> str:
        try:
            name = self.essid.decode("utf-8")
        except UnicodeDecodeError:
            name = self.essid.hex()
        return f"{name} ({self.mac_ap.hex()})"


def parse_22000(line: str) -> WpaTarget | None:
    line = line.strip()
    if not line.startswith("WPA*"):
        return None
    f = line.split("*")
    if len(f) < 6:
        return None
    try:
        # 字段[1]是签名版本：01=PMKID / 02=EAPOL-MIC
        if f[1] == "01":
            t = WpaTarget(
                "pmkid",
                mic=bytes.fromhex(f[2]),
                mac_ap=bytes.fromhex(f[3]),
                mac_sta=bytes.fromhex(f[4]),
                essid=bytes.fromhex(f[5]),
                hash_line=line,
            )
            return t if len(t.mic) == 16 and len(t.mac_ap) == 6 and len(t.mac_sta) == 6 else None

        if f[1] == "02":
            if len(f) < 12:
                return None
            frame = bytes.fromhex(f[8])
            nonce_ap = bytes.fromhex(f[6]) if f[6] else b""
            nonce_sta = bytes.fromhex(f[7]) if f[7] else b""
            keyver = int(f[11]) if f[11] else 2
            mic_off = _mic_offset(frame)
            if mic_off < 0:
                return None
            t = WpaTarget(
                "mic",
                mic=bytes.fromhex(f[2]),
                mac_ap=bytes.fromhex(f[3]),
                mac_sta=bytes.fromhex(f[4]),
                essid=bytes.fromhex(f[5]),
                nonce_ap=nonce_ap,
                nonce_sta=nonce_sta,
                frame=frame,
                mic_off=mic_off,
                keyver=keyver,
                hash_line=line,
            )
            return t if len(t.mic) == 16 and nonce_ap and nonce_sta else None
    except (ValueError, IndexError):
        return None
    return None


def _mic_offset(frame: bytes) -> int:
    """在 802.11 数据帧中定位 EAPOL MIC 的字节偏移。"""
    if len(frame) < 40:
        return -1
    qos = bool(frame[0] & 0x80)  # FrameControl 的 QoS 位
    header = 26 if qos else 24
    # LLC-SNAP: aa aa 03 00 00 00 88 8e
    if frame[header : header + 8] != b"\xaa\xaa\x03\x00\x00\x00\x88\x8e":
        # 容错：在 24~40 之间扫描 LLC 标记
        found = frame.find(b"\xaa\xaa\x03\x00\x00\x00\x88\x8e", 20, 44)
        if found < 0:
            return -1
        header = found
    return header + 8 + 81  # EAPOL 中 MIC 的固定偏移 81


def _prf_sha1(key: bytes, label: bytes, data: bytes, length: int) -> bytes:
    out = b""
    i = 0
    while len(out) < length:
        out += hmac.new(key, label + b"\x00" + data + bytes([i]), hashlib.sha1).digest()
        i += 1
    return out[:length]


def _prf_md5(key: bytes, label: bytes, data: bytes, length: int) -> bytes:
    out = b""
    i = 0
    while len(out) < length:
        out += hmac.new(key, label + b"\x00" + data + bytes([i]), hashlib.md5).digest()
        i += 1
    return out[:length]


def _aes_cmac(key: bytes, data: bytes) -> bytes:
    """keyver 3 的 AES-128-CMAC（依赖可用的加密库；缺失则抛错）。"""
    try:
        from Crypto.Hash import CMAC
        from Crypto.Cipher import AES

        return CMAC.new(key, ciphermod=AES, msg=data).digest()
    except ImportError:
        try:
            from cryptography.hazmat.primitives import cmac
            from cryptography.hazmat.primitives.ciphers import Cipher, algorithms

            c = cmac.CMAC(Cipher(algorithms.AES(key)))
            c.update(data)
            return c.finalize()
        except ImportError as exc:
            raise RuntimeError("keyver 3 需要 pycryptodome 或 cryptography 库") from exc


def verify_passphrase(target: WpaTarget, passphrase: str) -> bool:
    if not 8 <= len(passphrase) <= 63:
        return False
    pmk = hashlib.pbkdf2_hmac("sha1", passphrase.encode(), target.essid, 4096, 32)

    if target.kind == "pmkid":
        calc = hmac.new(pmk, b"PMK Name" + target.mac_ap + target.mac_sta,
                        hashlib.sha1).digest()[:16]
        return hmac.compare_digest(calc, target.mic)

    # PTK：MAC 与 Nonce 均按字节排序后拼接
    mac_min, mac_max = sorted((target.mac_ap, target.mac_sta))
    n_min, n_max = sorted((target.nonce_ap, target.nonce_sta))
    data = mac_min + mac_max + n_min + n_max

    frame = bytearray(target.frame)
    frame[target.mic_off : target.mic_off + 16] = b"\x00" * 16

    if target.keyver == 1:
        ptk = _prf_md5(pmk, b"Pairwise key expansion", data, 48)
        calc = hmac.new(ptk[:16], bytes(frame), hashlib.md5).digest()[:16]
    elif target.keyver == 3:
        ptk = _prf_sha1(pmk, b"Pairwise key expansion", data, 48)
        calc = _aes_cmac(ptk[:16], bytes(frame))
    else:  # keyver 2（绝大多数 WPA2-CCMP 握手）
        ptk = _prf_sha1(pmk, b"Pairwise key expansion", data, 48)
        calc = hmac.new(ptk[:16], bytes(frame), hashlib.sha1).digest()[:16]
    return hmac.compare_digest(calc, target.mic)


# ============================================================
# hashcat 探测
# ============================================================
def find_hashcat() -> str:
    found = shutil.which("hashcat")
    if found:
        return found
    for p in (
        r"C:\tools\hashcat\hashcat.exe",
        r"F:\tools\hashcat\hashcat.exe",
        r"C:\Program Files\hashcat\hashcat.exe",
    ):
        if os.path.exists(p):
            return p
    return ""


# ============================================================
# 破解任务管理
# ============================================================
class CrackJob:
    def __init__(self, targets: list[WpaTarget], wordlist_path: str, engine: str):
        self.jid = uuid.uuid4().hex
        self.targets = targets
        self.wordlist = wordlist_path
        self.engine = engine                      # "python" / "hashcat"
        self.status = "running"
        self.logs: list[str] = []
        self.tried = 0
        self.found: list[dict] = []
        self.speed = 0
        self.created = time.time()
        self._stop = threading.Event()
        self._proc = None

    def _log(self, text: str) -> None:
        self.logs.append(text)
        if len(self.logs) > 800:
            del self.logs[: len(self.logs) - 800]

    def request_stop(self) -> None:
        self._stop.set()
        if self._proc:
            try:
                self._proc.terminate()
            except OSError:
                pass

    def to_dict(self) -> dict:
        return {
            "jid": self.jid,
            "engine": self.engine,
            "status": self.status,
            "targets": [t.label for t in self.targets],
            "wordlist": self.wordlist,
            "tried": self.tried,
            "speed": self.speed,
            "found": self.found,
            "logs": self.logs,
        }


_jobs: dict[str, CrackJob] = {}
_jobs_lock = threading.Lock()


def _run_python(job: CrackJob) -> None:
    job._log(f"加载字典：{job.wordlist}")
    try:
        total = 0
        with open(job.wordlist, "rb") as fh:
            for _ in fh:
                total += 1
    except OSError as exc:
        job._log(f"字典读取失败：{exc}")
        job.status = "failed"
        return
    job._log(f"字典共 {total} 条，开始破解 {len(job.targets)} 个目标…")

    started = time.time()
    try:
        fh = open(job.wordlist, "rb")
    except OSError as exc:
        job._log(f"字典打开失败：{exc}")
        job.status = "failed"
        return
    with fh:
        for raw in fh:
            if job._stop.is_set():
                job.status = "stopped"
                break
            word = raw.decode("utf-8", "ignore").strip()
            if not word:
                continue
            job.tried += 1
            for t in job.targets:
                try:
                    hit = verify_passphrase(t, word)
                except RuntimeError as exc:
                    job._log(f"目标 {t.label} 无法验证：{exc}")
                    hit = False
                if hit:
                    entry = {"target": t.label, "password": word}
                    job.found.append(entry)
                    job._log(f"★ 破解成功 [{t.label}] → {word}")
            if job.tried % 50 == 0:
                elapsed = max(time.time() - started, 0.001)
                job.speed = int(job.tried / elapsed)
            # 全部目标命中则提前结束
            if len(job.found) >= len(job.targets):
                job.status = "done"
                break
    if job.status == "running":
        job.status = "exhausted"
    job.speed = 0
    job._log(f"任务结束：{job.status}，已试 {job.tried}，命中 {len(job.found)}")


def _run_hashcat(job: CrackJob, hashcat_bin: str, hash_file: str) -> None:
    import subprocess
    import sys

    job._log(f"hashcat: {hashcat_bin}")
    argv = [
        hashcat_bin, "-m", "22000", hash_file, job.wordlist,
        "--quiet", "--status", "--status-timer", "5", "-O",
    ]
    creationflags = getattr(subprocess, "CREATE_NO_WINDOW", 0)
    try:
        job._proc = subprocess.Popen(
            argv, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            creationflags=creationflags,
        )
    except OSError as exc:
        job._log(f"hashcat 启动失败：{exc}")
        job.status = "failed"
        return

    assert job._proc.stdout is not None
    for raw in iter(job._proc.stdout.readline, b""):
        line = decode_output(raw).rstrip()
        if line:
            job._log(line[:300])
    rc = job._proc.wait()
    # hashcat 输出 cracked 结果：hash:password
    # 直接再跑一次 --show 取结果（写进 .potfile）
    try:
        show = subprocess.run(
            [hashcat_bin, "-m", "22000", hash_file, "--show"],
            capture_output=True, creationflags=creationflags, timeout=20,
        )
        for line in decode_output(show.stdout).splitlines():
            if ":" in line:
                h, _, pw = line.partition(":")
                job.found.append({"target": h[:24], "password": pw})
                job._log(f"★ hashcat 破解成功 → {pw}")
    except Exception:  # noqa: BLE001
        pass
    job.status = "done" if job.found else ("stopped" if job._stop.is_set() else "exhausted")
    job._log(f"hashcat 退出码 {rc}，状态 {job.status}")


def create_job(hash_text: str, wordlist_path: str, engine: str = "auto") -> CrackJob:
    targets = []
    for line in hash_text.splitlines():
        t = parse_22000(line)
        if t is not None and not any(x.hash_line == t.hash_line for x in targets):
            targets.append(t)
    if not targets:
        raise ValueError("未解析到有效的 22000 哈希（PMKID 或 EAPOL-MIC）")
    if not wordlist_path or not os.path.exists(wordlist_path):
        raise ValueError(f"字典文件不存在：{wordlist_path}")

    hashcat_bin = find_hashcat()
    if engine == "auto":
        engine = "hashcat" if hashcat_bin else "python"
    if engine == "hashcat" and not hashcat_bin:
        engine = "python"

    job = CrackJob(targets, wordlist_path, engine)
    with _jobs_lock:
        _jobs[job.jid] = job

    def runner():
        if job.engine == "hashcat":
            os.makedirs(DATA_DIR, exist_ok=True)
            hash_file = os.path.join(DATA_DIR, f"{job.jid}.22000")
            with open(hash_file, "w", encoding="utf-8") as fh:
                for t in job.targets:
                    fh.write(t.hash_line + "\n")
            _run_hashcat(job, hashcat_bin, hash_file)
        else:
            _run_python(job)

    threading.Thread(target=runner, daemon=True).start()
    return job


def list_jobs() -> list[CrackJob]:
    return list(_jobs.values())


def get_job(jid: str) -> CrackJob | None:
    return _jobs.get(jid)


def stop_job(jid: str) -> CrackJob | None:
    job = _jobs.get(jid)
    if job and job.status == "running":
        job.request_stop()
    return job
