# watchdog-services.ps1 - keep the web3 dev stack alive (pure ASCII, PS 5.1 safe)
# Runs from Task Scheduler every 5 minutes. If any critical port is down,
# re-runs start-all-services.ps1 (which skips already-running services).
# Why: services launched from an interactive/agent shell die with that session;
# Task Scheduler processes are detached, so this also fixes the "everything
# vanished again" problem.
$ErrorActionPreference = 'Continue'
$root = 'z:\project\myblog\web3'
$log = "$env:TEMP\web3-watchdog.log"

$critical = 3306, 6379, 8848, 8080, 8083, 5173, 9006
$down = @()
foreach ($p in $critical) {
  if (-not (Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue)) {
    $down += $p
  }
}
if ($down.Count -gt 0) {
  $stamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
  "$stamp DOWN: $($down -join ',') -> restarting stack" | Out-File $log -Append -Encoding utf8
  & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $root 'start-all-services.ps1') *>&1 |
    Out-File $log -Append -Encoding utf8
}
