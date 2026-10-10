# Elevated helper for legacy hosted network (one UAC consent per action).
# Runs only netsh/IP/firewall setup; the portal itself binds low ports from
# the NON-elevated service process (Windows has no low-port restriction).
# ASCII only. Params (ssid/key) come from %TEMP%\pywifi_hosted_params.json.
param([Parameter(Mandatory = $true)][string]$Action)
$ErrorActionPreference = 'Continue'
$outPath = Join-Path $env:TEMP 'pywifi_hosted_result.json'
$paramsPath = Join-Path $env:TEMP 'pywifi_hosted_params.json'

function Report($h) { ConvertTo-Json $h -Compress | Set-Content -Path $outPath -Encoding ascii }
function Get-HostedAdapter {
  Get-NetAdapter | Where-Object { $_.InterfaceDescription -like '*Hosted Network*' } | Select-Object -First 1
}

switch ($Action) {
  'start' {
    $ssid = ''; $key = ''
    if (Test-Path $paramsPath) {
      $p = ConvertFrom-Json (Get-Content $paramsPath -Raw)
      $ssid = [string]$p.ssid
      $key = [string]$p.key
    }
    if (-not $ssid) { Report @{ ok = $false; error = 'no_params' }; return }

    $null = netsh wlan set hostednetwork mode=allow "ssid=$ssid" "key=$key" keyUsage=persistent 2>&1
    $r = netsh wlan start hostednetwork 2>&1
    if ($LASTEXITCODE -ne 0) { Report @{ ok = $false; step = 'start'; error = ($r -join ' ') }; return }

    $adapter = $null
    for ($i = 0; $i -lt 15; $i++) {
      $adapter = Get-HostedAdapter
      if ($adapter) { break }
      Start-Sleep -Seconds 1
    }
    $adapterName = ''
    if ($adapter) {
      $adapterName = [string]$adapter.Name
      $null = netsh interface ip set address name="$adapterName" static 192.168.137.1 255.255.255.0 2>&1
    }

    Get-NetFirewallRule -DisplayName '__pywifi_rogue__*' -ErrorAction SilentlyContinue | Remove-NetFirewallRule
    New-NetFirewallRule -DisplayName '__pywifi_rogue__ dhcp' -Direction Inbound -Protocol UDP -LocalPort 67 -Action Allow | Out-Null
    New-NetFirewallRule -DisplayName '__pywifi_rogue__ dns' -Direction Inbound -Protocol UDP -LocalPort 53 -Action Allow | Out-Null
    New-NetFirewallRule -DisplayName '__pywifi_rogue__ http' -Direction Inbound -Protocol TCP -LocalPort 80 -Action Allow | Out-Null

    Report @{ ok = $true; adapter = $adapterName }
  }
  'stop' {
    $null = netsh wlan stop hostednetwork 2>&1
    Get-NetFirewallRule -DisplayName '__pywifi_rogue__*' -ErrorAction SilentlyContinue | Remove-NetFirewallRule
    $adapter = Get-HostedAdapter
    if ($adapter) {
      $null = netsh interface ip set address name="$([string]$adapter.Name)" dhcp 2>&1
    }
    Report @{ ok = $true }
  }
  default { Report @{ ok = $false; error = 'unknown_action' } }
}
