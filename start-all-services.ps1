$java = "F:\tools\jdk17\bin\java.exe"
$base = "F:\project\my-blog\web3-blog\backend"
$profile = ""

$services = @(
    @{ n="Gateway";    p=8080; j="$base\gateway\target\gateway-1.0.0.jar" },
    @{ n="User";       p=8081; j="$base\user-service\target\user-service-1.0.0.jar" },
    @{ n="Blog";       p=8082; j="$base\blog-service\target\blog-service-1.0.0.jar" },
    @{ n="Forum";      p=8083; j="$base\forum-service\target\forum-service-1.0.0.jar" },
    @{ n="Shop";       p=8084; j="$base\shop-service\target\shop-service-1.0.0.jar" },
    @{ n="Media";      p=8085; j="$base\media-service\target\media-service-1.0.0.jar" },
    @{ n="Quant";      p=8086; j="$base\quant-service\target\quant-service-1.0.0.jar" },
    @{ n="Tool";       p=8087; j="$base\tool-service\target\tool-service-1.0.0.jar" },
    @{ n="Software";   p=8088; j="$base\software-service\target\software-service-1.0.0.jar" },
    @{ n="Resource";   p=8089; j="$base\resource-service\target\resource-service-1.0.0.jar" },
    @{ n="AIP";        p=8093; j="$base\ai-proxy-service\target\ai-proxy-service-1.0.0.jar" },
    @{ n="Jarvis";     p=9001; j="$base\jarvis-service\target\jarvis-service-1.0.0.jar" },
    @{ n="Admin";      p=9002; j="$base\admin-service\target\admin-service-1.0.0.jar" }
)

$logDir = "$base\target\logs"
if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir | Out-Null }

Write-Host "Starting services..."
foreach ($svc in $services) {
    $logFile = "$logDir\svc_$($svc.p).log"
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $java
    $psi.Arguments = "-jar `"$($svc.j)`" $profile"
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.CreateNoWindow = $true
    $proc = [System.Diagnostics.Process]::Start($psi)
    Start-Sleep -Milliseconds 200
    Write-Host "  $($svc.n) -> port $($svc.p) (PID $($proc.Id))" -ForegroundColor Green
}

Write-Host "`nWaiting 25s for services to boot..."
Start-Sleep -Seconds 25

Write-Host "`nService Status:"
$allOk = $true
foreach ($svc in $services) {
    $conn = Get-NetTCPConnection -LocalPort $svc.p -State Listen -ErrorAction SilentlyContinue
    if ($conn) {
        $proc2 = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
        Write-Host "  $($svc.p) $($svc.n) - RUNNING (PID $($conn.OwningProcess))" -ForegroundColor Green
    } else {
        Write-Host "  $($svc.p) $($svc.n) - NOT LISTENING" -ForegroundColor Red
        $allOk = $false
    }
}

if ($allOk) {
    Write-Host "`nAll services started successfully!" -ForegroundColor Cyan
} else {
    Write-Host "`nSome services failed. Check logs in $logDir" -ForegroundColor Yellow
}

# ---- Whisper 语音识别服务（Python / faster-whisper）----
Write-Host "`nStarting Whisper STT (python faster-whisper) -> http://127.0.0.1:9011 ..."
$py = "F:\tools\python\miniconda\python.exe"
$psi2 = New-Object System.Diagnostics.ProcessStartInfo
$psi2.FileName = $py
$psi2.Arguments = "`"$base\whisper-service\app.py`""
$psi2.WorkingDirectory = "$base\whisper-service"
$psi2.UseShellExecute = $false
$psi2.RedirectStandardOutput = $true
$psi2.RedirectStandardError = $true
$psi2.CreateNoWindow = $true
$proc2 = [System.Diagnostics.Process]::Start($psi2)
Start-Sleep -Seconds 2
$conn = Get-NetTCPConnection -LocalPort 9011 -State Listen -ErrorAction SilentlyContinue
if ($conn) {
    Write-Host "  Whisper STT - RUNNING (PID $($conn.OwningProcess))" -ForegroundColor Green
} else {
    Write-Host "  Whisper STT - NOT LISTENING (first start downloads model, check logs)" -ForegroundColor Yellow
}

# ---- NeteaseCloudMusicApi（网易云歌单/搜索代理，端口 3000）----
Write-Host "Starting NeteaseCloudMusicApi -> http://localhost:3000 ..."
$psi3 = New-Object System.Diagnostics.ProcessStartInfo
$psi3.FileName = "node"
$psi3.Arguments = "`"$base\netease-music-api\server.js`""
$psi3.WorkingDirectory = "$base\netease-music-api"
$psi3.UseShellExecute = $false
$psi3.RedirectStandardOutput = $true
$psi3.RedirectStandardError = $true
$psi3.CreateNoWindow = $true
$proc3 = [System.Diagnostics.Process]::Start($psi3)
Start-Sleep -Seconds 3
$conn3 = Get-NetTCPConnection -LocalPort 3000 -State Listen -ErrorAction SilentlyContinue
if ($conn3) {
    Write-Host "  NeteaseCloudMusicApi - RUNNING (PID $($conn3.OwningProcess))" -ForegroundColor Green
} else {
    Write-Host "  NeteaseCloudMusicApi - NOT LISTENING" -ForegroundColor Yellow
}

