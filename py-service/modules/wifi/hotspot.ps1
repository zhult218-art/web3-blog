# Control Windows Mobile Hotspot (WinRT NetworkOperatorTetheringManager).
# Runs WITHOUT admin rights. ASCII only (PowerShell 5.1 reads .ps1 as ANSI).
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File hotspot.ps1 -Action status|start|stop|configure [-Ssid x] [-Passphrase x]
param(
  [Parameter(Mandatory = $true)][string]$Action,
  [string]$Ssid,
  [string]$Passphrase
)
$ErrorActionPreference = 'Stop'

function Out-Json($obj) { Write-Output (ConvertTo-Json $obj -Compress) }

try {
  Add-Type -AssemblyName System.Runtime.WindowsRuntime | Out-Null
  $null = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType = WindowsRuntime]
  $null = [Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType = WindowsRuntime]

  $asTaskOp = ([System.WindowsRuntimeSystemExtensions].GetMethods() |
    Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
  $asTaskAction = ([System.WindowsRuntimeSystemExtensions].GetMethods() |
    Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and (-not $_.IsGenericMethod) -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncAction' })[0]

  function AwaitOp($op, $type) {
    $t = $asTaskOp.MakeGenericMethod($type).Invoke($null, @($op))
    $t.Wait(-1) | Out-Null
    $t.Result
  }
  function AwaitAction($a) {
    $t = $asTaskAction.Invoke($null, @($a))
    $t.Wait(-1) | Out-Null
  }

  $connProfile = [Windows.Networking.Connectivity.NetworkInformation]::GetInternetConnectionProfile()
  if (-not $connProfile) { Out-Json @{ ok = $false; error = 'no_internet_profile' }; return }
  $manager = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager]::CreateFromConnectionProfile($connProfile)

  switch ($Action) {
    'status' {
      $cfg = $manager.GetCurrentAccessPointConfiguration()
      Out-Json @{ ok = $true; state = [string]$manager.TetheringOperationalState; ssid = $cfg.Ssid; passphrase = $cfg.Passphrase; client_count = $manager.ClientCount }
    }
    'configure' {
      $cfg = $manager.GetCurrentAccessPointConfiguration()
      $cfg.Ssid = $Ssid
      $cfg.Passphrase = $Passphrase
      AwaitAction ($manager.ConfigureAccessPointAsync($cfg))
      Out-Json @{ ok = $true }
    }
    'start' {
      $state = [string]$manager.TetheringOperationalState
      if ($Ssid -and $Passphrase) {
        try {
          $cfg = $manager.GetCurrentAccessPointConfiguration()
          $cfg.Ssid = $Ssid
          $cfg.Passphrase = $Passphrase
          AwaitAction ($manager.ConfigureAccessPointAsync($cfg))
        } catch { }
      }
      if ($state -eq 'On') {
        Out-Json @{ ok = $true; status = 'Success'; already = $true }
      } else {
        $res = AwaitOp ($manager.StartTetheringAsync()) ([Windows.Networking.NetworkOperators.NetworkOperatorTetheringOperationResult])
        $extra = ''
        try { $extra = [string]$res.AdditionalInformation } catch { }
        Out-Json @{ ok = ([string]$res.Status -eq 'Success'); status = [string]$res.Status; extra = $extra }
      }
    }
    'stop' {
      $res = AwaitOp ($manager.StopTetheringAsync()) ([Windows.Networking.NetworkOperators.NetworkOperatorTetheringOperationResult])
      Out-Json @{ ok = ([string]$res.Status -eq 'Success'); status = [string]$res.Status }
    }
    default { Out-Json @{ ok = $false; error = 'unknown_action' } }
  }
} catch {
  Out-Json @{ ok = $false; error = $_.Exception.Message }
}
