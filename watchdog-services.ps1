# watchdog-services.ps1 - keep the web3 dev stack alive (pure ASCII, PS 5.1 safe)
# Runs from Task Scheduler every 5 minutes. If any critical port is down,
# re-runs start-all-services.ps1 (which skips already-running services).
# Why Task Scheduler: services launched from an interactive/agent shell die
# with that session (job-object kill on close); scheduled tasks are detached.
# Port probes use raw TCP sockets: Get-NetTCPConnection can hang under heavy
# pagefile/memory pressure (observed: a watchdog instance froze for 25+ min).
$ErrorActionPreference = 'Continue'
# Repo root derived from this script's own location (no hardcoded absolute path).
$root = $PSScriptRoot
$log  = "$env:TEMP\web3-watchdog.log"

function Test-PortFast($p) {
  $c = New-Object Net.Sockets.TcpClient
  try {
    $iar = $c.BeginConnect('127.0.0.1', $p, $null, $null)
    $ok  = $iar.AsyncWaitHandle.WaitOne(300, $false)
    return [bool]($ok -and $c.Connected)
  } catch { return $false } finally { $c.Close() }
}

# Full sweep: infra + 13 Java services (verified ports) + 2 Node + py + Vite.
# 8900 (vibe-research) is intentionally excluded (optional, often absent).
$critical = 3306,6379,8848,
  8080,8081,8082,8083,8084,8085,8086,8087,8089,8090,
  8093,9001,9002,
  3000,3010,5173,9006
$down = @()
foreach ($p in $critical) {
  if (-not (Test-PortFast $p)) { $down += $p }
}
if ($down.Count -gt 0) {
  $stamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
  Add-Content -Path $log -Value ("$stamp DOWN: $($down -join ',') -> restarting stack") -Encoding UTF8
  & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $root 'start-all-services.ps1') *>&1 |
    Out-File $log -Append -Encoding utf8
}
