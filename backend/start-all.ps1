# 启动所有后端微服务
$java = "F:\tools\jdk17\bin\java.exe"
$backend = "F:\project\my-blog\web3-blog\backend"

$services = @(
    @{ name = "gateway";         jar = "$backend\gateway\target\gateway-1.0.0.jar";             dir = "$backend\gateway" },
    @{ name = "user-service";    jar = "$backend\user-service\target\user-service-1.0.0.jar";    dir = "$backend\user-service" },
    @{ name = "blog-service";    jar = "$backend\blog-service\target\blog-service-1.0.0.jar";    dir = "$backend\blog-service" },
    @{ name = "forum-service";   jar = "$backend\forum-service\target\forum-service-1.0.0.jar";   dir = "$backend\forum-service" },
    @{ name = "shop-service";    jar = "$backend\shop-service\target\shop-service-1.0.0.jar";    dir = "$backend\shop-service" },
    @{ name = "media-service";   jar = "$backend\media-service\target\media-service-1.0.0.jar";   dir = "$backend\media-service" },
    @{ name = "quant-service";   jar = "$backend\quant-service\target\quant-service-1.0.0.jar";   dir = "$backend\quant-service" },
    @{ name = "tool-service";    jar = "$backend\tool-service\target\tool-service-1.0.0.jar";    dir = "$backend\tool-service" },
    @{ name = "software-service";jar = "$backend\software-service\target\software-service-1.0.0.jar"; dir = "$backend\software-service" },
    @{ name = "resource-service";jar = "$backend\resource-service\target\resource-service-1.0.0.jar"; dir = "$backend\resource-service" },
    @{ name = "ai-proxy-service";jar = "$backend\ai-proxy-service\target\ai-proxy-service-1.0.0.jar"; dir = "$backend\ai-proxy-service" },
    @{ name = "jarvis-service";  jar = "$backend\jarvis-service\target\jarvis-service-1.0.0.jar";  dir = "$backend\jarvis-service" },
    @{ name = "admin-service";   jar = "$backend\admin-service\target\admin-service-1.0.0.jar";   dir = "$backend\admin-service" }
)

foreach ($svc in $services) {
    $logDir = Join-Path $svc.dir "logs"
    if (!(Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }
    $logFile = Join-Path $logDir "stdout.log"

    Start-Process -FilePath $java -ArgumentList "-jar", $svc.jar `
        -WorkingDirectory $svc.dir `
        -RedirectStandardOutput $logFile `
        -RedirectStandardError "$logDir\stderr.log" `
        -WindowStyle Hidden

    Write-Host "Started: $($svc.name)"
    Start-Sleep -Milliseconds 500
}

Write-Host ""
Write-Host "All 13 services starting... waiting 20s for them to boot"
Start-Sleep -Seconds 20

Write-Host ""
Write-Host "=== Port Check ==="
$ports = @(8080,8081,8082,8083,8084,8085,8086,8087,8088,8089,8093,9001,9002)
foreach ($port in $ports) {
    $listening = netstat -ano | Select-String ":$port" | Select-String "LISTENING"
    if ($listening) { Write-Host "  :$port  OK" -ForegroundColor Green }
    else { Write-Host "  :$port  DOWN" -ForegroundColor Red }
}
