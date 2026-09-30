# ============================================================
# Web3 Portal — 一键启动本地开发环境全部服务（Windows PowerShell）
#
# 2026-09-20 修订：
#   1. 合并原先重复的 backend\start-all.ps1，成为全仓唯一启动入口；
#   2. 移除硬编码的第三方 app_key（ak_xxx）与本机绝对路径（F:\tools、F:\project\...），
#      密钥统一从环境变量或仓库根 .env 读取，缺失即报错退出；
#   3. 覆盖完整栈：13 个 Java 微服务 + Whisper STT(9011) + 网易云音乐代理(3000)
#      + 量化 Python 服务(9006) + Vibe-Research(8900) + QQ空间备份(9012)；
#   4. 未构建 / 未安装的组件只跳过并给出提示，不中断其余服务启动。
#
# 用法：powershell -ExecutionPolicy Bypass -File start-all-services.ps1
#       （或直接双击 start-all-services.bat）
# ============================================================

$ErrorActionPreference = "Stop"

# ── 目录定位（脚本位于仓库根，后端在 <root>\backend） ──
$root = $PSScriptRoot
$base = Join-Path $root "backend"
if (-not (Test-Path $base)) {
    Write-Host "[ERROR] 未找到 backend 目录：$base" -ForegroundColor Red
    exit 1
}

# ── 加载仓库根 .env，仅填充尚未设置的环境变量 ──
$envFile = Join-Path $root ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
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
}

# ── 定位 java（JAVA_HOME → PATH → 常见安装目录自动探测） ──
if ($env:JAVA_HOME) {
    $java = Join-Path $env:JAVA_HOME "bin\java.exe"
} else {
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) { $java = $cmd.Source }
}
if (-not $java -or -not (Test-Path $java)) {
    # 自动探测常见 JDK 目录（本机 F:\tools\jdk17 优先），探测到即回填 JAVA_HOME
    $jdkCandidates = @(
        "F:\tools\jdk17",
        "F:\tools\java",
        "C:\Program Files\Java\jdk-17",
        "C:\Program Files\Eclipse Adoptium\jdk-17",
        "C:\Program Files\Microsoft\jdk-17"
    ) | Where-Object { Test-Path (Join-Path $_ "bin\java.exe") } | Select-Object -First 1
    if ($jdkCandidates) {
        $java = Join-Path $jdkCandidates "bin\java.exe"
        $env:JAVA_HOME = $jdkCandidates
        [Environment]::SetEnvironmentVariable("JAVA_HOME", $jdkCandidates, "User")
        Write-Host "自动探测到 JDK：$jdkCandidates（已回填 JAVA_HOME 到用户环境变量）" -ForegroundColor Cyan
    }
}
if (-not $java -or -not (Test-Path $java)) {
    Write-Host "未找到 java：请设置 JAVA_HOME，或把 java 加入 PATH。" -ForegroundColor Red
    exit 1
}

# ── 加载仓库根目录 .env（密钥不入库不入脚本；已存在的进程环境变量优先） ──
$envFile = Join-Path $root ".env"
if (Test-Path $envFile) {
    Get-Content $envFile -Encoding UTF8 | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$') {
            $k = $Matches[1]; $v = $Matches[2].Trim('"').Trim("'")
            if (-not [Environment]::GetEnvironmentVariable($k)) {
                [Environment]::SetEnvironmentVariable($k, $v)
            }
        }
    }
}

# ── 校验网关第三方 app_key（gateway/application.yml 以 ${J8Y_APP_KEY:} 引用，缺省空串可启动） ──
foreach ($k in @("J8Y_APP_KEY", "SHOWAPI_APP_KEY")) {
    $v = [Environment]::GetEnvironmentVariable($k)
    if ([string]::IsNullOrWhiteSpace($v)) {
        Write-Host "提示：环境变量 $k 未配置（.env 中为空）—— 相关第三方功能不可用，但服务可正常启动。" -ForegroundColor Yellow
    }
}

# ── 定位 python（Whisper / 量化服务共用） ──
$py = $env:PYTHON_EXE
if (-not $py) {
    foreach ($p in @(
        (Join-Path $root "quant-py-service\.venv\Scripts\python.exe"),
        "$env:USERPROFILE\miniconda3\python.exe",
        "$env:USERPROFILE\AppData\Local\Programs\Python\Python311\python.exe"
    )) {
        if (Test-Path $p) { $py = $p; break }
    }
    if (-not $py -and (Get-Command python -ErrorAction SilentlyContinue)) { $py = "python" }
}

# 在独立隐藏窗口中拉起一个进程，stdout/stderr 直接落盘到日志文件
# （旧版用 ProcessStartInfo + RedirectStandardOutput 但从不读取，会导致缓冲区满挂起且日志丢失）
function Start-HiddenProcess {
    param([string]$Name, [string]$File, [string]$Arguments, [string]$WorkDir, [string]$LogDir)
    if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir -Force | Out-Null }
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $outLog = Join-Path $LogDir "$Name-$stamp.out.log"
    $errLog = Join-Path $LogDir "$Name-$stamp.err.log"
    try {
        $proc = Start-Process -FilePath $File -ArgumentList $Arguments -WorkingDirectory $WorkDir `
            -RedirectStandardOutput $outLog -RedirectStandardError $errLog `
            -WindowStyle Hidden -PassThru
        Write-Host "  $Name 已启动 (PID $($proc.Id))" -ForegroundColor Green
        Write-Host "    日志: $outLog" -ForegroundColor DarkGray
        return $proc
    } catch {
        Write-Host "  $Name 启动失败：$_" -ForegroundColor Red
        return $null
    }
}

# ── 0. Nacos 注册/配置中心（端口 8848，必须先于 Java 微服务启动） ──
# 自动探测常见安装目录；startup.cmd 是 cmd 脚本，需 JAVA_HOME（已由上方探测回填到进程环境）。
Write-Host "启动 Nacos 注册/配置中心（8848）..." -ForegroundColor Cyan
$nacosCmd = $null
if ($env:NACOS_HOME -and (Test-Path (Join-Path $env:NACOS_HOME "bin\startup.cmd"))) {
    $nacosCmd = Join-Path $env:NACOS_HOME "bin\startup.cmd"
} else {
    $nacosCmd = @("F:\tools\nacos\bin\startup.cmd", "C:\tools\nacos\bin\startup.cmd") |
        Where-Object { Test-Path $_ } | Select-Object -First 1
}
if ($nacosCmd) {
    $nacosBin = Split-Path $nacosCmd -Parent
    $nacosHome = Split-Path $nacosBin -Parent
    # standalone 模式；JAVA_HOME 已在进程环境，cmd 子进程可继承
    Start-HiddenProcess -Name "nacos" -File "cmd.exe" `
        -Arguments "/c `"$nacosCmd`" -m standalone" `
        -WorkDir $nacosBin -LogDir (Join-Path $nacosHome "logs") | Out-Null
    Write-Host "  等待 Nacos 就绪（最多 60s）..." -ForegroundColor DarkGray
    $ready = $false
    for ($i = 0; $i -lt 30; $i++) {
        try {
            $r = Invoke-WebRequest "http://127.0.0.1:8848/nacos/v1/console/health/readiness" -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
            if ($r.StatusCode -eq 200) { $ready = $true; break }
        } catch { }
        Start-Sleep -Seconds 2
    }
    if ($ready) { Write-Host "  Nacos 已就绪" -ForegroundColor Green }
    else { Write-Host "  警告：Nacos 60s 内未就绪，Java 服务可能注册失败" -ForegroundColor Yellow }
} else {
    Write-Host "  跳过 Nacos：未找到 startup.cmd（Java 服务仍可本地启动，但服务注册发现不可用）" -ForegroundColor Yellow
}

# ── 1. Java 微服务 ──
$services = @(
    @{ name = "gateway";          jar = "gateway\target\gateway-1.0.0.jar" },
    @{ name = "user-service";     jar = "user-service\target\user-service-1.0.0.jar" },
    @{ name = "blog-service";     jar = "blog-service\target\blog-service-1.0.0.jar" },
    @{ name = "forum-service";    jar = "forum-service\target\forum-service-1.0.0.jar" },
    @{ name = "shop-service";     jar = "shop-service\target\shop-service-1.0.0.jar" },
    @{ name = "media-service";    jar = "media-service\target\media-service-1.0.0.jar" },
    @{ name = "quant-service";    jar = "quant-service\target\quant-service-1.0.0.jar" },
    @{ name = "tool-service";     jar = "tool-service\target\tool-service-1.0.0.jar" },
    @{ name = "software-service"; jar = "software-service\target\software-service-1.0.0.jar" },
    @{ name = "resource-service"; jar = "resource-service\target\resource-service-1.0.0.jar" },
    @{ name = "ai-proxy-service"; jar = "ai-proxy-service\target\ai-proxy-service-1.0.0.jar" },
    @{ name = "jarvis-service";   jar = "jarvis-service\target\jarvis-service-1.0.0.jar" },
    @{ name = "admin-service";    jar = "admin-service\target\admin-service-1.0.0.jar" }
)

Write-Host "[INFO] JAVA_HOME=$env:JAVA_HOME" -ForegroundColor Cyan
Write-Host "启动 Java 微服务..." -ForegroundColor Cyan
foreach ($svc in $services) {
    $jarPath = Join-Path $base $svc.jar
    if (-not (Test-Path $jarPath)) {
        Write-Host "  跳过 $($svc.name)：未找到 $($svc.jar)，请先执行 mvn package。" -ForegroundColor Yellow
        continue
    }
    $svcDir = Split-Path $jarPath -Parent
    # 2026-09-20：user-service 需要访问 Google OAuth API（token 交换），
    # 本地直连不通，必须走系统代理（Clash 默认 7892）。其他服务不受影响。
    if ($svc.name -eq "user-service") {
        Start-HiddenProcess -Name $svc.name -File $java `
            -Arguments "-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7892 -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7892 -jar `"$jarPath`"" `
            -WorkDir $svcDir -LogDir (Join-Path $svcDir "logs") | Out-Null
    } else {
        Start-HiddenProcess -Name $svc.name -File $java `
            -Arguments "-jar `"$jarPath`"" -WorkDir $svcDir -LogDir (Join-Path $svcDir "logs") | Out-Null
    }
    Start-Sleep -Milliseconds 400
}

# ── 2. Whisper 语音识别（Python / faster-whisper，端口 9011） ──
Write-Host "启动 Whisper 语音识别（9011）..." -ForegroundColor Cyan
$whisperDir = Join-Path $base "whisper-service"
$whisperApp = Join-Path $whisperDir "app.py"
if ($py -and (Test-Path $whisperApp)) {
    Start-HiddenProcess -Name "whisper-stt" -File $py -Arguments "`"$whisperApp`"" `
        -WorkDir $whisperDir -LogDir (Join-Path $whisperDir "logs") | Out-Null
} else {
    Write-Host "  跳过 whisper-stt：未找到 python 或 app.py" -ForegroundColor Yellow
}

# ── 3. 网易云音乐 API 代理（Node / NeteaseCloudMusicApi，端口 3000） ──
Write-Host "启动网易云音乐代理（3000）..." -ForegroundColor Cyan
$ncmDir = Join-Path $base "netease-music-api"
$ncmEntry = Join-Path $ncmDir "server.js"
if ((Get-Command node -ErrorAction SilentlyContinue) -and (Test-Path $ncmEntry)) {
    Start-HiddenProcess -Name "netease-music-api" -File "node" -Arguments "`"$ncmEntry`"" `
        -WorkDir $ncmDir -LogDir $ncmDir | Out-Null
} else {
    Write-Host "  跳过 netease-music-api：未找到 node 或 server.js（音乐馆仍可用 Audius / iTunes 免费源）" -ForegroundColor Yellow
}

# ── 4. 量化数据服务（Python FastAPI，端口 9006） ──
Write-Host "启动量化 Python 服务（9006）..." -ForegroundColor Cyan
$quantDir = Join-Path $root "quant-py-service"
if ($py -and (Test-Path (Join-Path $quantDir "main.py"))) {
    Start-HiddenProcess -Name "quant-py-service" -File $py `
        -Arguments "-m uvicorn main:app --host 127.0.0.1 --port 9006" `
        -WorkDir $quantDir -LogDir $quantDir | Out-Null
} else {
    Write-Host "  跳过 quant-py-service：未找到 python 或 main.py" -ForegroundColor Yellow
}

# ── 5. Vibe-Research A股扩展数据服务（Python FastAPI，端口 8900） ──
# 该项目位于本仓库之外（同级 ..\..\Quant\Vibe-Research），为量化页提供
# /vr 代理背后的指数/情绪/雷达/全球指数等扩展数据；缺失则仅对应面板不可用，不影响其它服务。
Write-Host "启动 Vibe-Research 服务（8900）..." -ForegroundColor Cyan
$vibeDir = Join-Path (Split-Path (Split-Path $root -Parent) -Parent) 'Quant\Vibe-Research\backend'
$vibePy = Join-Path $vibeDir ".venv\Scripts\python.exe"
if ((Test-Path $vibePy) -and (Test-Path (Join-Path $vibeDir "app.py"))) {
    Start-HiddenProcess -Name "vibe-research" -File $vibePy `
        -Arguments "-m uvicorn app:app --host 127.0.0.1 --port 8900" `
        -WorkDir $vibeDir -LogDir $vibeDir | Out-Null
} else {
    Write-Host "  跳过 vibe-research：未找到 $vibeDir（其 .venv 或 app.py 缺失），量化页 /vr 面板将不可用" -ForegroundColor Yellow
}

# ── 6. 时光回溯 QQ 空间备份服务（Python FastAPI，端口 9012） ──
# 由 GetQzonehistory 扫码登录 + emotion_cgi_msglist_v6 协议重写而来，
# 为网站 /qzone 页面提供扫码会话、后台分页抓取与 JSON/HTML/CSV 临时导出。
Write-Host "启动时光回溯 QQ 空间备份服务（9012）..." -ForegroundColor Cyan
$qzoneDir = Join-Path $root "qzone-service"
$qzonePy = Join-Path $qzoneDir ".venv\Scripts\python.exe"
if ((Test-Path $qzonePy) -and (Test-Path (Join-Path $qzoneDir "app.py"))) {
    Start-HiddenProcess -Name "qzone-service" -File $qzonePy `
        -Arguments "-m uvicorn app:app --host 127.0.0.1 --port 9012" `
        -WorkDir $qzoneDir -LogDir $qzoneDir | Out-Null
} else {
    Write-Host "  跳过 qzone-service：未找到其 .venv 或 app.py（/qzone 页面将不可用）" -ForegroundColor Yellow
}

# ── 端口自检 ──
Write-Host "`n等待 20s 让服务完成启动..." -ForegroundColor Cyan
Start-Sleep -Seconds 20
Write-Host "`n=== 端口检查 ===" -ForegroundColor Cyan
foreach ($p in @(8848,8080,8081,8082,8083,8084,8085,8086,8087,8088,8089,8093,9001,9002,3000,9006,8900,9011,9012)) {
    $listening = netstat -ano | Select-String ":$p" | Select-String "LISTENING"
    if ($listening) { Write-Host "  :$p  OK" -ForegroundColor Green }
    else { Write-Host "  :$p  DOWN" -ForegroundColor Red }
}
Write-Host "`n前端：cd frontend; npm run dev  → http://localhost:5173" -ForegroundColor Yellow