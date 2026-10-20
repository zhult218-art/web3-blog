# ============================================================
# Web3 Portal — 一键启动本地开发环境全部服务（Windows PowerShell）
#
# 2026-10-09 修订（一键启动全栈）：
#   1. 全仓唯一启动入口：一个脚本拉起全部服务——MySQL / Redis / Nacos / 13 个 Java 微服务
#      / 网易云音乐代理(node-service:3000) / 统一 py-service(9006) / Vibe-Research(8900) / 前端 Vite(5173)；
#   2. 密钥统一从仓库根 .env 读取，缺失即报错退出，不入库不入脚本；
#   3. 统一 py-service(9006) 单进程聚合 quant/qzone/video/whisper/wifi，作为一个服务注册 Nacos；
#   4. 未构建 / 未安装的组件只跳过并给出提示，不中断其余服务启动。
#
# 用法：powershell -ExecutionPolicy Bypass -File start-all-services.ps1
#       （或直接双击 start-all-services.bat）
#       -NoWait：跳过末尾固定 20s 等待与端口自检（供 watchdog / 滚动重启调用，
#                调用方自行轮询端口就绪）
# ============================================================
param([switch]$NoWait, [string]$Only)

$ErrorActionPreference = "Stop"

# 端口探测用裸 TCP socket：换页/内存压力下 Get-NetTCPConnection 可能挂起数十分钟
# （2026-10-16 watchdog 实例实测卡死 25 分钟），connect 300ms 超时稳定可靠。
function Test-PortFast {
    param([int]$Port)
    $c = New-Object Net.Sockets.TcpClient
    try {
        $iar = $c.BeginConnect('127.0.0.1', $Port, $null, $null)
        $ok  = $iar.AsyncWaitHandle.WaitOne(300, $false)
        return [bool]($ok -and $c.Connected)
    } catch { return $false } finally { $c.Close() }
}

# ── 目录定位（脚本位于仓库根，后端在 <root>\backend） ──
$root = $PSScriptRoot
$base = Join-Path $root "backend"
if (-not (Test-Path $base)) {
    Write-Host "[ERROR] 未找到 backend 目录：$base" -ForegroundColor Red
    exit 1
}

# ── 定位 java（JAVA_HOME → PATH → 常见安装目录自动探测） ──
if ($env:JAVA_HOME) {
    $java = Join-Path $env:JAVA_HOME "bin\java.exe"
} else {
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) { $java = $cmd.Source }
}
if (-not $java -or -not (Test-Path $java)) {
    # 自动探测常见 JDK 目录，探测到即回填 JAVA_HOME
    $jdkCandidates = @(
        $env:JAVA_HOME,
        "C:\Program Files\Java\jdk-17",
        "C:\Program Files\Eclipse Adoptium\jdk-17",
        "C:\Program Files\Microsoft\jdk-17"
    ) | Where-Object { $_ -and (Test-Path (Join-Path $_ "bin\java.exe")) } | Select-Object -First 1
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

# ── 定位 python（统一 py-service 各模块共用） ──
$py = $env:PYTHON_EXE
if (-not $py) {
    foreach ($p in @(
        (Join-Path $root "py-service\.venv\Scripts\python.exe"),
        "$env:USERPROFILE\miniconda3\python.exe",
        "$env:USERPROFILE\AppData\Local\Programs\Python\Python311\python.exe"
    )) {
        if (Test-Path $p) { $py = $p; break }
    }
    if (-not $py -and (Get-Command python -ErrorAction SilentlyContinue)) { $py = "python" }
}

# ── 定位 node（前端 Vite dev server + netease-music-api 共用） ──
$node = $env:NODE_EXE
if (-not $node) {
    $cmd = Get-Command node -ErrorAction SilentlyContinue
    if ($cmd) { $node = $cmd.Source }
    if (-not $node) {
        # 探测便携版 / 常见安装目录
        $nodeCandidates = @(
            "$env:LOCALAPPDATA\Temp\node-portable\node-v22.11.0-win-x64\node.exe",
            "$env:ProgramFiles\nodejs\node.exe",
            "$env:USERPROFILE\AppData\Roaming\nvm\v22\node.exe"
        ) | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
        if ($nodeCandidates) { $node = $nodeCandidates }
    }
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
        # PowerShell 5.1 中 ArgumentList 传空字符串会报错，需条件传参
        $params = @{
            FilePath               = $File
            WorkingDirectory       = $WorkDir
            RedirectStandardOutput = $outLog
            RedirectStandardError  = $errLog
            WindowStyle            = 'Hidden'
            PassThru               = $true
        }
        if ($Arguments) { $params.ArgumentList = $Arguments }
        $proc = Start-Process @params
        Write-Host "  $Name 已启动 (PID $($proc.Id))" -ForegroundColor Green
        Write-Host "    日志: $outLog" -ForegroundColor DarkGray
        return $proc
    } catch {
        Write-Host "  $Name 启动失败：$_" -ForegroundColor Red
        return $null
    }
}

# ── 0a. MySQL（端口 3306，Java 微服务数据库依赖，最先启动） ──
Write-Host "启动 MySQL（3306）..." -ForegroundColor Cyan
$mysqld = $null
if ($env:MYSQL_HOME -and (Test-Path (Join-Path $env:MYSQL_HOME "bin\mysqld.exe"))) {
    $mysqld = Join-Path $env:MYSQL_HOME "bin\mysqld.exe"
} else {
    $mysqld = @("C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe") |
        Where-Object { Test-Path $_ } | Select-Object -First 1
}
if ($mysqld) {
    $mysqlHome = Split-Path (Split-Path $mysqld -Parent) -Parent
    $myini = Get-ChildItem $mysqlHome -Filter "my.ini" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $myini) { $myini = Get-ChildItem $mysqlHome -Filter "my.cnf" -ErrorAction SilentlyContinue | Select-Object -First 1 }
    # 已在监听则跳过
    $mysqlUp = netstat -ano | Select-String ":3306 " | Select-String "LISTENING"
    if ($mysqlUp) {
        Write-Host "  MySQL 已在运行，跳过" -ForegroundColor DarkGray
    } else {
        $mysqlArgs = if ($myini) { "--defaults-file=`"$($myini.FullName)`"" } else { "" }
        Start-HiddenProcess -Name "mysql" -File $mysqld -Arguments $mysqlArgs `
            -WorkDir $mysqlHome -LogDir (Join-Path $mysqlHome "logs") | Out-Null
        Write-Host "  等待 MySQL 就绪（最多 15s）..." -ForegroundColor DarkGray
        for ($i = 0; $i -lt 8; $i++) {
            if (netstat -ano | Select-String ":3306 " | Select-String "LISTENING") { break }
            Start-Sleep -Seconds 2
        }
    }
    $mysqlUp = netstat -ano | Select-String ":3306 " | Select-String "LISTENING"
    if ($mysqlUp) { Write-Host "  MySQL 已就绪" -ForegroundColor Green }
    else { Write-Host "  警告：MySQL 未就绪，Java 服务数据库依赖将失败" -ForegroundColor Yellow }
} else {
    Write-Host "  跳过 MySQL：未找到 mysqld.exe" -ForegroundColor Yellow
}

# ── 0b. Redis（端口 6379，Java 微服务缓存依赖） ──
Write-Host "启动 Redis（6379）..." -ForegroundColor Cyan
$redisExe = $null
if ($env:REDIS_HOME -and (Test-Path (Join-Path $env:REDIS_HOME "redis-server.exe"))) {
    $redisExe = Join-Path $env:REDIS_HOME "redis-server.exe"
} else {
    $redisExe = @("C:\Program Files\Redis\redis-server.exe") |
        Where-Object { Test-Path $_ } | Select-Object -First 1
}
if ($redisExe) {
    $redisDir = Split-Path $redisExe -Parent
    $redisUp = netstat -ano | Select-String ":6379 " | Select-String "LISTENING"
    if ($redisUp) {
        Write-Host "  Redis 已在运行，跳过" -ForegroundColor DarkGray
    } else {
        # 内存加固（2026-10-16 Redis MISCONF 事故后）：maxmemory 防写满失控；
        # volatile-lru 写满时优先淘汰带 TTL 的会话/验证码键而非拒写；BGSAVE 失败不再拒写
        # （Redis 只存会话/缓存，可容忍持久化失败，避免再次出现全站登录 500）
        $redisArgs = "--maxmemory 512mb --maxmemory-policy volatile-lru --stop-writes-on-bgsave-error no"
        Start-HiddenProcess -Name "redis" -File $redisExe -Arguments $redisArgs `
            -WorkDir $redisDir -LogDir $redisDir | Out-Null
        Start-Sleep -Seconds 2
    }
    $redisUp = netstat -ano | Select-String ":6379 " | Select-String "LISTENING"
    if ($redisUp) { Write-Host "  Redis 已就绪" -ForegroundColor Green }
    else { Write-Host "  警告：Redis 未就绪，Java 服务缓存依赖将失败" -ForegroundColor Yellow }
} else {
    Write-Host "  跳过 Redis：未找到 redis-server.exe" -ForegroundColor Yellow
}

# ── 0c. Nacos 注册/配置中心（端口 8848，必须先于 Java 微服务启动） ──
# 自动探测常见安装目录；startup.cmd 是 cmd 脚本，需 JAVA_HOME（已由上方探测回填到进程环境）。
Write-Host "启动 Nacos 注册/配置中心（8848）..." -ForegroundColor Cyan
$nacosCmd = $null
if ($env:NACOS_HOME -and (Test-Path (Join-Path $env:NACOS_HOME "bin\startup.cmd"))) {
    $nacosCmd = Join-Path $env:NACOS_HOME "bin\startup.cmd"
} else {
    $nacosCmd = $null
}
if ($nacosCmd -and -not $Only) {
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
# port 为各服务「实际监听」端口（实证：ai-proxy=8093、jarvis=9001、admin=9002，
# 与 jar 名直觉顺序不同；修改前请以 Get-NetTCPConnection 实测为准）。
# 启动前按 port 幂等跳过：watchdog 每 5 分钟触发本脚本，已运行的服务绝不能重复拉起，
# 否则重复实例会白占内存并因端口占用/OOM 连环崩溃（2026-10-16 事故根因）。
$services = @(
    @{ name = "gateway";          port = 8080; jar = "gateway\target\gateway-1.0.0.jar" },
    @{ name = "user-service";     port = 8081; jar = "user-service\target\user-service-1.0.0.jar" },
    @{ name = "blog-service";     port = 8082; jar = "blog-service\target\blog-service-1.0.0.jar" },
    @{ name = "forum-service";    port = 8083; jar = "forum-service\target\forum-service-1.0.0.jar" },
    @{ name = "shop-service";     port = 8084; jar = "shop-service\target\shop-service-1.0.0.jar" },
    @{ name = "media-service";    port = 8085; jar = "media-service\target\media-service-1.0.0.jar" },
    @{ name = "quant-service";    port = 8086; jar = "quant-service\target\quant-service-1.0.0.jar" },
    @{ name = "tool-service";     port = 8087; jar = "tool-service\target\tool-service-1.0.0.jar" },
    @{ name = "software-service"; port = 8090; jar = "software-service\target\software-service-1.0.0.jar" },
    @{ name = "resource-service"; port = 8089; jar = "resource-service\target\resource-service-1.0.0.jar" },
    @{ name = "ai-proxy-service"; port = 8093; jar = "ai-proxy-service\target\ai-proxy-service-1.0.0.jar" },
    @{ name = "jarvis-service";   port = 9001; jar = "jarvis-service\target\jarvis-service-1.0.0.jar" },
    @{ name = "admin-service";    port = 9002; jar = "admin-service\target\admin-service-1.0.0.jar" }
)

# 低内存 JVM 参数（16GB 机器跑 13 个 JVM 的保命配置，2026-10-16 native OOM 后定版）：
#   -Xmx300m/-Xms96m       堆按需提交，不预触内存（Xms 由 128m 再降，13 服务 idle 共省约 400MB 提交）
#   -Xss512k               线程栈减半（默认 1MB，百级线程省几十 MB）
#   MaxMetaspaceSize=192m  封顶类元数据（默认无上限，长跑微服务会缓慢膨胀）
#   CompressedClassSpace=128m  缩小 compressed class 保留空间（默认 1GB reserve）
#   ReservedCodeCache=128m C2 JIT 代码缓存封顶（上次崩溃点 Chunk::new 即此处 arena 分配）
#   +UseSerialGC           单线程 GC：小堆本地开发足够，省掉 G1 region/remembered set
#                          结构与并行 GC 线程栈，单 JVM 提交内存可降约 200-300MB
#   MaxDirectMemorySize=128m 封顶堆外直接内存：Netty 默认跟随 -Xmx 增长（网关长连接/大报文逐页提交）
#   ActiveProcessorCount=4 虚拟 4 核：Netty worker/线程池/JIT 均按此收缩（物理机 6 核，本地开发足够）
#   CICompilerCount=2      封顶 JIT 编译线程（默认随核数扩容，每线程约 1MB 提交）
$JvmOpts = "-Xms96m -Xmx300m -Xss512k -XX:MaxMetaspaceSize=192m -XX:CompressedClassSpaceSize=128m -XX:ReservedCodeCacheSize=128m -XX:+UseSerialGC -XX:MaxDirectMemorySize=128m -XX:ActiveProcessorCount=4 -XX:CICompilerCount=2"

Write-Host "[INFO] JAVA_HOME=$env:JAVA_HOME" -ForegroundColor Cyan
Write-Host "启动 Java 微服务..." -ForegroundColor Cyan
foreach ($svc in $services) {
    if ($Only -and $svc.name -ne $Only) { continue }
    $jarPath = Join-Path $base $svc.jar
    if (-not (Test-Path $jarPath)) {
        Write-Host "  跳过 $($svc.name)：未找到 $($svc.jar)，请先执行 mvn package。" -ForegroundColor Yellow
        continue
    }
    if (Test-PortFast -Port $svc.port) {
        Write-Host "  $($svc.name) 已在运行（:$($svc.port)），跳过" -ForegroundColor DarkGray
        continue
    }
    $svcDir = Split-Path $jarPath -Parent
    # 2026-09-20：user-service 需要访问 Google OAuth API（token 交换），
    # 本地直连不通，必须走系统代理（Clash 默认 7892）。其他服务不受影响。
    # 2026-10-10：software-service 默认端口 8088 被腾讯更新代理占用，改用 8090。
    if ($svc.name -eq "user-service") {
        Start-HiddenProcess -Name $svc.name -File $java `
            -Arguments "$JvmOpts -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7892 -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7892 -jar `"$jarPath`"" `
            -WorkDir $svcDir -LogDir (Join-Path $svcDir "logs") | Out-Null
    } elseif ($svc.name -eq "software-service") {
        Start-HiddenProcess -Name $svc.name -File $java `
            -Arguments "$JvmOpts -jar `"$jarPath`" --server.port=8090" -WorkDir $svcDir -LogDir (Join-Path $svcDir "logs") | Out-Null
    } else {
        Start-HiddenProcess -Name $svc.name -File $java `
            -Arguments "$JvmOpts -jar `"$jarPath`"" -WorkDir $svcDir -LogDir (Join-Path $svcDir "logs") | Out-Null
    }
    Start-Sleep -Milliseconds 400
}

# ── 2. 网易云音乐 API 代理（Node / NeteaseCloudMusicApi，端口 3000） ──
# 已从 backend 移至根目录 node-service（后端 backend 保持纯 Java）。
Write-Host "启动网易云音乐代理（3000）..." -ForegroundColor Cyan
$ncmDir = Join-Path $root "node-service\netease-music-api"
$ncmEntry = Join-Path $ncmDir "server.js"
if ($node -and (Test-Path $ncmEntry)) {
    if (Test-PortFast -Port 3000) {
        Write-Host "  netease-music-api 已在运行（:3000），跳过" -ForegroundColor DarkGray
    } else {
        Start-HiddenProcess -Name "netease-music-api" -File $node -Arguments "`"$ncmEntry`"" `
            -WorkDir $ncmDir -LogDir $ncmDir | Out-Null
    }
} else {
    Write-Host "  跳过 netease-music-api：未找到 node 或 server.js（音乐馆仍可用 Audius / iTunes 免费源）" -ForegroundColor Yellow
}

# ── 2.5 聊天网关（Node / Socket.IO v4，端口 3010） ──
# 群聊与私聊的实时通道；JWT 走 httpOnly Cookie 鉴权，消息落 MySQL web3_forum。
# 前端经 Vite 的 /socket.io 代理同源接入，不经过网关（8080）。
Write-Host "启动聊天网关（3010）..." -ForegroundColor Cyan
$chatGwDir = Join-Path $root "node-service\chat-gateway"
$chatGwEntry = Join-Path $chatGwDir "server.js"
if ($node -and (Test-Path $chatGwEntry)) {
    if (Test-PortFast -Port 3010) {
        Write-Host "  chat-gateway 已在运行（:3010），跳过" -ForegroundColor DarkGray
    } else {
        Start-HiddenProcess -Name "chat-gateway" -File $node -Arguments "`"$chatGwEntry`"" `
            -WorkDir $chatGwDir -LogDir $chatGwDir | Out-Null
    }
} else {
    Write-Host "  跳过 chat-gateway：未找到 node 或 server.js（聊天将不可用）" -ForegroundColor Yellow
}

# ── 2.6 免费模型服务 Our Free Model（Node，端口 18900） ──
# 开源项目 dsh-our-free-model 的独立本地服务：免注册、免 Key 即可用匿名与 Kilo
# 免费模型池，对外提供 OpenAI 兼容接口。py-service 的免费 AI 通道与 ai-proxy 的
# Agent 直连通道都以本地回环地址调用它；服务未在跑时两侧均自动跳过，不影响其它上游。
# 仓库位于博客工程之外，可用环境变量 OFM_REPO 覆盖路径；未安装时整块跳过。
Write-Host "启动免费模型服务（18900）..." -ForegroundColor Cyan
$ofmRepo = @(
    $env:OFM_REPO
) | Where-Object { $_ -and (Test-Path (Join-Path $_ 'packages\standalone\cli.mjs')) } | Select-Object -First 1
if ($node -and $ofmRepo) {
    if (Test-PortFast -Port 18900) {
        Write-Host "  our-free-model 已在运行（:18900），跳过" -ForegroundColor DarkGray
    } else {
        # 不传 --data-dir：沿用其默认 ~/.our-free-model（py-service 按同一路径读密钥）
        Start-HiddenProcess -Name "our-free-model" -File $node `
            -Arguments "`"$(Join-Path $ofmRepo 'packages\standalone\cli.mjs')`" --port 18900" `
            -WorkDir $ofmRepo -LogDir (Join-Path $root 'logs') | Out-Null
    }
} else {
    Write-Host "  跳过 our-free-model：未找到 node 或工程目录（可设 OFM_REPO 指定路径）" -ForegroundColor Yellow
}

# ── 3. 统一 Python 服务 py-service（FastAPI，单进程单端口 9006） ──
# 一个进程聚合 quant / qzone / video / whisper / wifi 五个模块，作为「一个服务」
# 注册到 Nacos；新增功能只需在 py-service\modules 下新建模块目录，无需改动启动脚本。
Write-Host "启动统一 py-service（9006）..." -ForegroundColor Cyan
$pySvcDir = Join-Path $root "py-service"
if ($py -and (Test-Path (Join-Path $pySvcDir "main.py"))) {
    if (Test-PortFast -Port 9006) {
        Write-Host "  py-service 已在运行（:9006），跳过" -ForegroundColor DarkGray
    } else {
        Start-HiddenProcess -Name "py-service" -File $py `
            -Arguments "-m uvicorn main:app --host 0.0.0.0 --port 9006" `
            -WorkDir $pySvcDir -LogDir $pySvcDir | Out-Null
    }
} else {
    Write-Host "  跳过 py-service：未找到 python 或 main.py" -ForegroundColor Yellow
}

# ── 4. Vibe-Research A股扩展数据服务（Python FastAPI，端口 8900，仓库外可选） ──
# 该项目位于本仓库之外（同级 ..\..\Quant\Vibe-Research），为量化页提供
# /vr 代理背后的指数/情绪/雷达/全球指数等扩展数据；缺失则仅对应面板不可用，不影响其它服务。
Write-Host "启动 Vibe-Research 服务（8900）..." -ForegroundColor Cyan
$vibeDir = Join-Path (Split-Path (Split-Path $root -Parent) -Parent) 'Quant\Vibe-Research\backend'
$vibePy = Join-Path $vibeDir ".venv\Scripts\python.exe"
if ((-not $Only) -and (Test-Path $vibePy) -and (Test-Path (Join-Path $vibeDir "app.py"))) {
    Start-HiddenProcess -Name "vibe-research" -File $vibePy `
        -Arguments "-m uvicorn app:app --host 127.0.0.1 --port 8900" `
        -WorkDir $vibeDir -LogDir $vibeDir | Out-Null
} else {
    Write-Host "  跳过 vibe-research：未找到 $vibeDir（其 .venv 或 app.py 缺失），量化页 /vr 面板将不可用" -ForegroundColor Yellow
}

# ── 5. 前端 Vite dev server（端口 5173） ──
Write-Host "启动前端 Vite dev server（5173）..." -ForegroundColor Cyan
$feDir = Join-Path $root "frontend"
if ($node -and (Test-Path (Join-Path $feDir "node_modules"))) {
    # node_modules/.bin/npx 不可靠，直接用 node 调 vite
    $viteBin = Join-Path $feDir "node_modules\vite\bin\vite.js"
    if (Test-Path $viteBin) {
        if (Test-PortFast -Port 5173) {
            Write-Host "  vite-frontend 已在运行（:5173），跳过" -ForegroundColor DarkGray
        } else {
            Start-HiddenProcess -Name "vite-frontend" -File $node `
                -Arguments "`"$viteBin`" --host" `
                -WorkDir $feDir -LogDir (Join-Path $feDir "logs") | Out-Null
        }
    } else {
        # 回退：npx vite
        Start-HiddenProcess -Name "vite-frontend" -File $node `
            -Arguments "node_modules\npx\bin\npx.js vite --host" `
            -WorkDir $feDir -LogDir (Join-Path $feDir "logs") | Out-Null
    }
} else {
    Write-Host "  跳过前端：未找到 node 或 frontend/node_modules（请先 cd frontend; npm install）" -ForegroundColor Yellow
}

# ── 6. SakuraFrp 内网穿透（TCP 隧道 frp-six.com:44598 → 本机 5173） ──
# SakuraFrpService.exe 是用户态守护进程（非 Windows 服务），读取
# C:\ProgramData\SakuraFrpService\config.json 自动启动隧道列表中的隧道。
# 隧道为 TCP 类型，节点会为 HTTPS 访问自动生成自签证书（frp-six.com.crt），
# 已导入 CurrentUser\Root 信任存储区，Chrome/Edge 不再弹 SSL 警告页。
Write-Host "启动 SakuraFrp 内网穿透..." -ForegroundColor Cyan
$sakuraSvc = @(
    $env:SAKURAFRP_EXE,
    "C:\Program Files\SakuraFrpLauncher\SakuraFrpService.exe"
) | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
$sakuraRunning = Get-Process -Name "SakuraFrpService" -ErrorAction SilentlyContinue
if ($sakuraRunning) {
    Write-Host "  SakuraFrp 服务已在运行（PID $($sakuraRunning.Id)），跳过" -ForegroundColor DarkGray
} elseif ($sakuraSvc) {
    $sakuraDir = Split-Path $sakuraSvc -Parent
    Start-HiddenProcess -Name "sakurafrp" -File $sakuraSvc -Arguments "" `
        -WorkDir $sakuraDir -LogDir "C:\ProgramData\SakuraFrpService\Logs" | Out-Null
    Write-Host "  等待 frpc 隧道建立（最多 15s）..." -ForegroundColor DarkGray
    $frpcUp = $false
    for ($i = 0; $i -lt 8; $i++) {
        $frpc = Get-Process -Name "frpc" -ErrorAction SilentlyContinue
        if ($frpc) { $frpcUp = $true; break }
        Start-Sleep -Seconds 2
    }
    if ($frpcUp) { Write-Host "  SakuraFrp 隧道已建立 → https://frp-six.com:44598" -ForegroundColor Green }
    else { Write-Host "  警告：frpc 未启动，隧道可能未建立（检查 config.json auto_start_tunnels）" -ForegroundColor Yellow }
} else {
    Write-Host "  跳过 SakuraFrp：未找到 SakuraFrpService.exe" -ForegroundColor Yellow
}

# ── 端口自检 ──
if ($NoWait) {
    Write-Host "[INFO] -NoWait 已指定，跳过末尾等待与端口自检" -ForegroundColor DarkGray
} else {
    Write-Host "`n等待 20s 让服务完成启动..." -ForegroundColor Cyan
    Start-Sleep -Seconds 20
    Write-Host "`n=== 端口检查 ===" -ForegroundColor Cyan
    foreach ($p in @(3306,6379,8848,8080,8081,8082,8083,8084,8085,8086,8087,8089,8090,8093,9001,9002,3000,5173,9006,8900,3010)) {
        $listening = netstat -ano | Select-String ":$p" | Select-String "LISTENING"
        if ($listening) { Write-Host "  :$p  OK" -ForegroundColor Green }
        else { Write-Host "  :$p  DOWN" -ForegroundColor Red }
    }
    Write-Host "`n前端地址：http://localhost:5173" -ForegroundColor Cyan
    Write-Host "隧道地址：https://frp-six.com:44598" -ForegroundColor Cyan
}