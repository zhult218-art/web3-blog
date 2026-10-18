# roll-restart.ps1 - serial replacement of JVMs with low-memory flags.
# Socket-based port probes only (Get-NetTCPConnection can hang under pagefile thrash).
# Safe to re-run: services already running with UseSerialGC are skipped.
$ErrorActionPreference = 'Continue'
$root = 'z:\project\myblog\web3'
$log  = Join-Path $env:TEMP 'web3-roll-restart.log'

function Log($m) {
  $line = '[{0}] {1}' -f (Get-Date -Format 'HH:mm:ss'), $m
  Add-Content -Path $log -Value $line -Encoding UTF8
}

function Test-PortFast($p) {
  $c = New-Object Net.Sockets.TcpClient
  try {
    $iar = $c.BeginConnect('127.0.0.1', $p, $null, $null)
    $ok  = $iar.AsyncWaitHandle.WaitOne(300, $false)
    return [bool]($ok -and $c.Connected)
  } catch { return $false } finally { $c.Close() }
}

function Get-JavaAtPort($p) {
  $rows = netstat -ano -p TCP
  $row  = $rows | Where-Object { $_ -match 'LISTENING' -and $_ -match (':' + $p + '\s') } | Select-Object -First 1
  if (-not $row) { return $null }
  $owning = ($row.ToString().Trim() -split '\s+')[-1]
  $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$owning" -ErrorAction SilentlyContinue
  if ($proc -and $proc.Name -eq 'java.exe') { return $proc }
  return $null
}

function Commit-GB {
  $perf = Get-CimInstance Win32_PerfFormattedData_PerfOS_Memory
  return '{0:N2}/{1:N2} GB' -f ($perf.CommittedBytes/1GB), ($perf.CommitLimit/1GB)
}

# non-gateway services first, gateway last (it is the front door)
$targets = @(
  @{ name='user-service';     port=8081 },
  @{ name='blog-service';     port=8082 },
  @{ name='forum-service';    port=8083 },
  @{ name='shop-service';     port=8084 },
  @{ name='media-service';    port=8085 },
  @{ name='quant-service';    port=8086 },
  @{ name='tool-service';     port=8087 },
  @{ name='resource-service'; port=8089 },
  @{ name='software-service'; port=8090 },
  @{ name='ai-proxy-service'; port=8093 },
  @{ name='jarvis';           port=9001 },
  @{ name='admin';            port=9002 },
  @{ name='gateway';          port=8080 }
)

Log "=== roll-restart v2 begin, commit $(Commit-GB) ==="
$failed = @()

# our-free-model (:18900) is a Node process, not a JVM, so it is not in $targets.
# Ensure the free-model pool is up before the Java loop: -Only keeps it to the
# ungated non-JVM blocks (never wakes other missing JVMs concurrently).
if (Test-PortFast 18900) {
  Log "SKIP our-free-model :18900 (already running)"
} else {
  Log "NOTE our-free-model :18900 not running, starting"
  & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $root 'start-all-services.ps1') -NoWait -Only 'our-free-model' *> $null
  for ($i = 0; $i -lt 20; $i++) { Start-Sleep -Seconds 2; if (Test-PortFast 18900) { break } }
  if (Test-PortFast 18900) { Log "OK   our-free-model :18900 up" }
  else { Log "WARN our-free-model :18900 did not come up (AI free pool disabled, other upstreams unaffected)" }
}

foreach ($t in $targets) {
  $p = $t.port
  $proc = Get-JavaAtPort $p
  if ($proc -and ($proc.CommandLine -match 'UseSerialGC')) {
    Log ("SKIP {0} :{1} pid {2} (already new flags)" -f $t.name, $p, $proc.ProcessId)
    continue
  }

  if ($proc) {
    Log ("KILL {0} :{1} pid {2} (old flags)" -f $t.name, $p, $proc.ProcessId)
    Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
    for ($i=0; $i -lt 15; $i++) {
      Start-Sleep -Seconds 2
      if (-not (Test-PortFast $p)) { break }
    }
    Log ("  :{0} released, commit {1}" -f $p, (Commit-GB))
  } else {
    Log ("NOTE {0} :{1} not running, starting directly" -f $t.name, $p)
  }

  # -Only: start exactly this one service; never wake other missing JVMs concurrently
  & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $root 'start-all-services.ps1') -NoWait -Only $t.name *> $null

  $up = $false
  for ($i=0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 3
    if (Test-PortFast $p) { $up = $true; break }
  }

  if (-not $up) {
    Log ("FAIL {0} :{1} did not come up within 180s, commit {2}" -f $t.name, $p, (Commit-GB))
    $failed += $t.name
    Start-Sleep -Seconds 10
    continue
  }

  Start-Sleep -Seconds 25   # let JIT/startup commit peak settle before freeing next slot
  $newProc = Get-JavaAtPort $p
  if ($newProc -and ($newProc.CommandLine -match 'UseSerialGC')) {
    Log ("OK   {0} :{1} pid {2} new flags, commit {3}" -f $t.name, $p, $newProc.ProcessId, (Commit-GB))
  } else {
    Log ("WARN {0} :{1} up but new flags not confirmed, commit {2}" -f $t.name, $p, (Commit-GB))
    $failed += ($t.name + '?flags')
  }
}

Log ("=== roll-restart v2 done. failed: {0}; final commit {1} ===" -f ($(if ($failed) { $failed -join ',' } else { 'none' })), (Commit-GB))
