# ============================================================
# Web3 Blog — 后端全栈上线脚本（免费 Quick Tunnel 方案）
#
# 做什么：
#   1. 读取仓库根 .env，补齐环境变量
#   2. best-effort 启动本地 Nacos（非硬依赖，失败不阻塞）
#   3. 按端口自检，只启动"没在跑"的 Java 微服务（13 个）
#      + 网易云 Node 代理(3000) + 量化 Python(9006)
#   4. 等待网关 8080 就绪
#   5. 启动/复用 cloudflared Quick Tunnel，解析 https://xxx.trycloudflare.com
#   6. URL 有变化时：自动更新 GitHub Secret VITE_API_BASE
#      并触发 Cloudflare Pages 重新部署（前端无需人工干预）
#
# 幂等：可反复执行，已在运行的组件不会重启。
# 用法：powershell -ExecutionPolicy Bypass -File deploy-local\online.ps1
#       -NoSync 只启动服务，不更新 GitHub / 不触发部署
# ============================================================
[CmdletBinding()]
param(
    [switch]$NoSync
)
$ErrorActionPreference = "Continue"

# ── 路径 ──
$repo = Split-Path -Parent $PSScriptRoot
$runtimeDir = Join-Path $env:LOCALAPPDATA "web3-blog-online"
$logDir     = Join-Path $runtimeDir "logs"
$stateFile  = Join-Path $runtimeDir "last-url.txt"
$cfLog      = Join-Path $runtimeDir "cloudflared.log"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

Start-Transcript -Path (Join-Path $logDir "online-$(Get-Date -Format yyyyMMdd-HHmmss).log") | Out-Null

function Log($msg, $color = "Gray") { Write-Host "[$(Get-Date -Format HH:mm:ss)] $msg" -ForegroundColor $color }
function Test-Port($p) {
    return [bool](netstat -ano -p tcp | Select-String "LISTENING" | Select-String ":$p\s")
}
function Wait-Port($p, $timeoutSec = 120) {
    $t0 = Get-Date
    while (-not (Test-Port $p)) {
        if (((Get-Date) - $t0).TotalSeconds -gt $timeoutSec) { return $false }
        Start-Sleep -Milliseconds 800
    }
    return $true
}
function Wait-Http($url, $timeoutSec = 60) {
    $t0 = Get-Date
    while (((Get-Date) - $t0).TotalSeconds -lt $timeoutSec) {
        try {
            $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 8
            if ($r.StatusCode -eq 200) { return $true }
        } catch {}
        Start-Sleep -Seconds 2
    }
    return $false
}

# 直接经 Windows CredManager (CredRead) 读取 GCM 保存的 GitHub 令牌，
# 不依赖管道向原生命令传多行（spawned PS5.1 场景下不可靠）。
function Get-GitHubToken {
    $cs = @"
using System;
using System.Runtime.InteropServices;
public static class CredManRead {
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    public struct CREDENTIAL {
        public uint Flags; public uint Type; public string TargetName; public string Comment;
        public System.Runtime.InteropServices.ComTypes.FILETIME LastWritten;
        public uint CredentialBlobSize; public IntPtr CredentialBlob; public uint Persist;
        public uint AttributeCount; public IntPtr Attributes; public string TargetAlias; public string UserName;
    }
    [DllImport("advapi32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    public static extern bool CredRead(string target, uint type, uint reserved, out IntPtr cred);
    [DllImport("advapi32.dll")]
    public static extern void CredFree(IntPtr cred);
    public static string Read(string target) {
        IntPtr ptr;
        if (!CredRead(target, 1, 0, out ptr)) { return null; }   // 1 = CRED_TYPE_GENERIC
        try {
            CREDENTIAL c = (CREDENTIAL)Marshal.PtrToStructure(ptr, typeof(CREDENTIAL));
            if (c.CredentialBlobSize == 0 || c.CredentialBlob == IntPtr.Zero) { return null; }
            return Marshal.PtrToStringUni(c.CredentialBlob, (int)c.CredentialBlobSize / 2);
        } finally { CredFree(ptr); }
    }
}
"@
    if (-not ("CredManRead" -as [type])) { Add-Type -TypeDefinition $cs -Language CSharp }
    return [CredManRead]::Read("git:https://github.com")
}

# ── 1. 加载 .env.development（本地测试环境配置，只填充未设置项） ──
$envFile = Join-Path $repo ".env.development"
if (Test-Path $envFile) {
    Get-Content $envFile -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) { return }
        $key = $line.Substring(0, $idx).Trim()
        $val = $line.Substring($idx + 1).Trim().Trim('"').Trim("'")
        if (-not [Environment]::GetEnvironmentVariable($key)) {
            [Environment]::SetEnvironmentVariable($key, $val)
        }
    }
    Log ".env.development 已加载" "Cyan"
} else {
    # 回退到旧 .env
    $envFile = Join-Path $repo ".env"
    if (Test-Path $envFile) {
        Get-Content $envFile -Encoding UTF8 | ForEach-Object {
            $line = $_.Trim()
            if ($line -eq "" -or $line.StartsWith("#")) { return }
            $idx = $line.IndexOf("=")
            if ($idx -lt 1) { return }
            $key = $line.Substring(0, $idx).Trim()
            $val = $line.Substring($idx + 1).Trim().Trim('"').Trim("'")
            if (-not [Environment]::GetEnvironmentVariable($key)) {
                [Environment]::SetEnvironmentVariable($key, $val)
            }
        }
        Log ".env 已加载（建议迁移到 .env.development）" "Yellow"
    } else {
        Log "未找到 .env.development 或 .env，继续但服务可能因缺配置启动失败" "Red"
    }
}

# 非 Docker 本地部署：.env 里的 host.docker.internal（Docker 时代遗留）会被
# 本地 MySQL 以 "Host not allowed" 拒绝，统一强制回落到本机回环。
[Environment]::SetEnvironmentVariable("MYSQL_HOST", "127.0.0.1")
[Environment]::SetEnvironmentVariable("REDIS_HOST", "127.0.0.1")
[Environment]::SetEnvironmentVariable("RABBITMQ_HOST", "127.0.0.1")

# 本地 Redis 若未设 requirepass，必须清空 REDIS_PASSWORD，否则服务带 AUTH 连接
# 会报 "ERR Client sent AUTH, but no password is set"（验证码等依赖 Redis 的接口 500）
try {
    $redisClient = New-Object System.Net.Sockets.TcpClient("127.0.0.1", 6379)
    $redisStream = $redisClient.GetStream()
    $probe = [System.Text.Encoding]::ASCII.GetBytes("AUTH probe`r`n")
    $redisStream.Write($probe, 0, $probe.Length)
    Start-Sleep -Milliseconds 400
    $rbuf = New-Object byte[] 128
    $rn = $redisStream.Read($rbuf, 0, 128)
    $rresp = [System.Text.Encoding]::ASCII.GetString($rbuf, 0, $rn)
    $redisStream.Close(); $redisClient.Close()
    if ($rresp -match "no password is set") {
        [Environment]::SetEnvironmentVariable("REDIS_PASSWORD", "")
        Log "检测到本地 Redis 无密码，已清空 REDIS_PASSWORD" "Yellow"
    }
} catch { Log "Redis 探测失败（忽略）：$_" "Yellow" }

# ── 2. 定位 java ──
$javaHome = $env:JAVA_HOME
if (-not $javaHome -or -not (Test-Path (Join-Path $javaHome "bin\java.exe"))) {
    $sysJava = (Get-Command java -ErrorAction SilentlyContinue).Source
    if ($sysJava) { $javaHome = Split-Path -Parent (Split-Path -Parent $sysJava) }
    elseif (Test-Path "F:\tools\jdk17\bin\java.exe") { $javaHome = "F:\tools\jdk17" }
}
$java = Join-Path $javaHome "bin\java.exe"
if (-not (Test-Path $java)) { Log "找不到 java，退出" "Red"; Stop-Transcript | Out-Null; exit 1 }
Log "JAVA: $java" "Cyan"

# 网关的第三方 app_key 占位：.env 留空时用 dummy（未对接真实服务商，
# 空值会导致网关 AddRequestParameter 过滤器启动崩溃）
foreach ($k in @("J8Y_APP_KEY", "SHOWAPI_APP_KEY")) {
    if (-not [Environment]::GetEnvironmentVariable($k)) {
        [Environment]::SetEnvironmentVariable($k, "dummy")
    }
}

# 关键变量加载校验（PS5.1 默认按 ANSI 读无 BOM 的 .env 会静默丢变量）
foreach ($k in @("JWT_SECRET", "J8Y_APP_KEY", "SHOWAPI_APP_KEY")) {
    if (-not [Environment]::GetEnvironmentVariable($k)) {
        Log "关键环境变量 $k 未加载成功，请检查 .env 编码（须 UTF-8）与内容" "Red"
        Stop-Transcript | Out-Null
        exit 1
    }
}

# ai-proxy 本地加解密密钥（≥32 字符）：.env 未配置时自动生成并持久化，
# 重启后保持不变，否则库里已加密的渠道密钥/令牌将无法解密。
function Ensure-LocalSecret($name, $file) {
    $existing = [Environment]::GetEnvironmentVariable($name)
    if ($existing -and $existing.Length -ge 32) { return }
    if (Test-Path $file) {
        $saved = (Get-Content $file -Raw -Encoding UTF8).Trim()
        if ($saved.Length -ge 32) { [Environment]::SetEnvironmentVariable($name, $saved); return }
    }
    $bytes = New-Object byte[] 48
    (New-Object System.Security.Cryptography.RNGCryptoServiceProvider).GetBytes($bytes)
    $hex = -join ($bytes | ForEach-Object { $_.ToString("x2") })
    Set-Content -Path $file -Value $hex -NoNewline -Encoding UTF8
    [Environment]::SetEnvironmentVariable($name, $hex)
    Log "已生成并持久化 $name"
}
Ensure-LocalSecret "AI_PROXY_TOKEN_SECRET" (Join-Path $runtimeDir "ai-proxy-token-secret.txt")
Ensure-LocalSecret "AI_PROXY_CHANNEL_KEY_SECRET" (Join-Path $runtimeDir "ai-proxy-channel-secret.txt")

# ── 3. best-effort Nacos（optional 依赖，不阻塞） ──
$nacosCmd = "F:\tools\nacos\bin\startup.cmd"
if (-not (Test-Port 8848) -and (Test-Path $nacosCmd)) {
    Log "启动 Nacos（best-effort，JAVA_HOME=$javaHome）..." "Cyan"
    try {
        Start-Process -FilePath "cmd.exe" `
            -ArgumentList "/c", "set JAVA_HOME=$javaHome&& `"$nacosCmd`" -m standalone" `
            -WorkingDirectory "F:\tools\nacos\bin" -WindowStyle Hidden
    } catch { Log "Nacos 启动失败（忽略）：$_" "Yellow" }
}

# ── 4. Java 微服务（按端口缺失才启动） ──
$backend = Join-Path $repo "backend"
$services = @(
    @{ n = "gateway";          p = 8080; j = "gateway\target\gateway-1.0.0.jar";          extra = "" },
    @{ n = "user-service";     p = 8081; j = "user-service\target\user-service-1.0.0.jar"; extra = "-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7892 -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7892" },
    @{ n = "blog-service";     p = 8082; j = "blog-service\target\blog-service-1.0.0.jar"; extra = "" },
    @{ n = "forum-service";    p = 8083; j = "forum-service\target\forum-service-1.0.0.jar"; extra = "" },
    @{ n = "shop-service";     p = 8084; j = "shop-service\target\shop-service-1.0.0.jar"; extra = "" },
    @{ n = "media-service";    p = 8085; j = "media-service\target\media-service-1.0.0.jar"; extra = "" },
    @{ n = "quant-service";    p = 8086; j = "quant-service\target\quant-service-1.0.0.jar"; extra = "" },
    @{ n = "tool-service";     p = 8087; j = "tool-service\target\tool-service-1.0.0.jar"; extra = "" },
    @{ n = "software-service"; p = 8088; j = "software-service\target\software-service-1.0.0.jar"; extra = "" },
    @{ n = "resource-service"; p = 8089; j = "resource-service\target\resource-service-1.0.0.jar"; extra = "" },
    @{ n = "ai-proxy-service"; p = 8093; j = "ai-proxy-service\target\ai-proxy-service-1.0.0.jar"; extra = "" },
    @{ n = "jarvis-service";   p = 9001; j = "jarvis-service\target\jarvis-service-1.0.0.jar"; extra = "" },
    @{ n = "admin-service";    p = 9002; j = "admin-service\target\admin-service-1.0.0.jar"; extra = "" }
)

$startedAny = $false
foreach ($s in $services) {
    if (Test-Port $s.p) { Log "  $($s.n) :$($s.p) 已在运行，跳过" "DarkGray"; continue }
    $jarPath = Join-Path $backend $s.j
    if (-not (Test-Path $jarPath)) { Log "  $($s.n) jar 不存在，跳过：$($s.j)" "Yellow"; continue }
    $out = Join-Path $logDir "$($s.n).out.log"
    $err = Join-Path $logDir "$($s.n).err.log"
    $argLine = ($s.extra + " -jar `"$jarPath`"").Trim()
    try {
        Start-Process -FilePath $java -ArgumentList $argLine -WorkingDirectory (Split-Path $jarPath -Parent) `
            -WindowStyle Hidden -RedirectStandardOutput $out -RedirectStandardError $err
        Log "  $($s.n) 已启动" "Green"
        $startedAny = $true
    } catch { Log "  $($s.n) 启动失败：$_" "Red" }
    Start-Sleep -Milliseconds 500
}

# ── 5. 可选：网易云 Node 代理(3000) ──
if (-not (Test-Port 3000)) {
    $node = (Get-Command node -ErrorAction SilentlyContinue).Source
    $ncmEntry = Join-Path $repo "node-service\netease-music-api\server.js"
    if ($node -and (Test-Path $ncmEntry)) {
        try {
            Start-Process -FilePath $node -ArgumentList "`"$ncmEntry`"" `
                -WorkingDirectory (Split-Path $ncmEntry -Parent) -WindowStyle Hidden
            Log "  netease-music-api 已启动" "Green"
        } catch { Log "  netease-music-api 启动失败：$_" "Yellow" }
    }
}

# ── 6. 可选：统一 Python 服务 py-service(9006) ──
if (-not (Test-Port 9006)) {
    $py = Join-Path $repo "py-service\.venv\Scripts\python.exe"
    $qDir = Join-Path $repo "py-service"
    if (Test-Path $py) {
        try {
            Start-Process -FilePath $py -ArgumentList "-m", "uvicorn", "main:app", "--host", "127.0.0.1", "--port", "9006" `
                -WorkingDirectory $qDir -WindowStyle Hidden
            Log "  py-service 已启动" "Green"
        } catch { Log "  py-service 启动失败：$_" "Yellow" }
    }
}

# ── 7. 等待网关就绪（本地直连验证一条真实路由） ──
Log "等待网关 :8080 ..." "Cyan"
if (-not (Wait-Port 8080 120)) { Log "网关 120s 内未监听，放弃隧道同步" "Red"; Stop-Transcript | Out-Null; exit 1 }
if (-not (Wait-Http "http://127.0.0.1:8080/article/categories" 90)) {
    Log "网关路由无响应（blog-service 可能未就绪），仍尝试隧道" "Yellow"
} else {
    Log "网关就绪" "Green"
}

# ── 8. 隧道：-NoSync 时完全跳过（纯本地调试）；否则复用已有（本脚本启动且 URL 仍可用）或新建 ──
if ($NoSync) {
    Log "-NoSync：跳过隧道，后端仅在本地可用" "Yellow"
    Log "本地调试就绪。前端 cd frontend; npm run dev → http://localhost:5173" "Green"
    Stop-Transcript | Out-Null
    exit 0
}

$cfExe = "C:\Program Files (x86)\cloudflared\cloudflared.exe"
if (-not (Test-Path $cfExe)) { $cfExe = (Get-Command cloudflared -ErrorAction SilentlyContinue).Source }
$knownUrl = $null
if (Test-Path $stateFile) { $knownUrl = (Get-Content $stateFile -Raw).Trim() }

$tunnelUrl = $null
$cfRunning = [bool](Get-Process cloudflared -ErrorAction SilentlyContinue)
if ($cfRunning -and $knownUrl -and $knownUrl -match "^https://") {
    if (Wait-Http "$knownUrl/article/categories" 15) {
        $tunnelUrl = $knownUrl
        Log "复用现有隧道：$tunnelUrl" "Cyan"
    }
}

$changed = $false
if (-not $tunnelUrl) {
    # 现有 cloudflared 不是本脚本托管的（拿不到 URL），停掉重建
    if ($cfRunning) {
        Log "现有 cloudflared 无法确认 URL，停止后重建..." "Yellow"
        Stop-Process -Name cloudflared -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 3
    }
    if (Test-Path $cfLog) { Remove-Item $cfLog -Force }
    Log "启动 Quick Tunnel -> http://localhost:8080 ..." "Cyan"
    Start-Process -FilePath $cfExe -ArgumentList "tunnel", "--url", "http://localhost:8080", "--protocol", "http2" `
        -WindowStyle Hidden -RedirectStandardError $cfLog

    $t0 = Get-Date
    while (((Get-Date) - $t0).TotalSeconds -lt 60) {
        if (Test-Path $cfLog) {
            $m = Select-String -Path $cfLog -Pattern "https://[a-z0-9]+-[a-z0-9-]+\.trycloudflare\.com" | Select-Object -First 1
            if ($m) { $tunnelUrl = $m.Matches[0].Value; break }
        }
        Start-Sleep -Seconds 2
    }
    if (-not $tunnelUrl) { Log "60s 内未获取到隧道 URL，放弃" "Red"; Stop-Transcript | Out-Null; exit 1 }
    Log "新隧道地址：$tunnelUrl" "Green"
    $changed = $true
}

# 边缘节点生效后再验一次
if (-not (Wait-Http "$tunnelUrl/article/categories" 40)) {
    Log "警告：经隧道访问后端未成功（节点可能还在生效）" "Yellow"
} else {
    Log "公网链路验证通过 ✅" "Green"
}

# ── 9. URL 变化 → 更新 GitHub Secret + 触发 Pages 部署 ──
if ($NoSync) {
    Log "-NoSync 指定，跳过 GitHub 更新" "Yellow"
} elseif ($changed -or $knownUrl -ne $tunnelUrl) {
    Log "隧道 URL 有变化，同步到 GitHub..." "Cyan"
    $env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [Environment]::GetEnvironmentVariable("Path", "User")

    # 从 Windows 凭据管理器读取 GCM 已保存的 GitHub 令牌（不弹窗、无需 gh auth login）
    $token = Get-GitHubToken
    if (-not $token) { Log "未获取到 GitHub 凭据，跳过自动同步；请稍后手动执行本脚本" "Red"; Stop-Transcript | Out-Null; exit 1 }
    $env:GH_TOKEN = $token

    # owner/repo 由 remote 解析，避免硬编码
    $remote = (& git -C $repo remote get-url origin)
    $rm = [regex]::Match($remote, "github\.com[:/](?<slug>[^/]+/[^/.]+)")
    $repoSlug = $rm.Groups["slug"].Value
    if (-not $repoSlug) { Log "无法解析 GitHub remote：$remote" "Red"; Stop-Transcript | Out-Null; exit 1 }

    Log "更新 Secret VITE_API_BASE ..."
    gh secret set VITE_API_BASE --repo $repoSlug --body $tunnelUrl | Out-Null
    Set-Content -Path $stateFile -Value $tunnelUrl -NoNewline
    Log "触发 Pages 部署工作流 ..."
    gh workflow run "deploy-pages.yml" --repo $repoSlug | Out-Null
    Log "GitHub 已更新，前端 1~2 分钟后自动指向新隧道 ✅" "Green"
} else {
    Log "隧道 URL 未变化，无需同步 GitHub" "DarkGray"
}

Log "全部完成。后端：$tunnelUrl" "Green"
Stop-Transcript | Out-Null
