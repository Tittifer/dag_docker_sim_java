param(
    [string]$BaseUrl = "http://localhost:8080",
    [string[]]$TerminalIds = @("fusion1", "fusion2", "fusion3"),
    [int]$DeviceCount = 30,
    [int]$Concurrency = 10,
    [string]$DevicePrefix = "sim-device",
    [bool]$UseBootstrapIdentity = $false,
    [bool]$AutoConfirm = $true,
    [string]$OutputFile = ""
)

. "$PSScriptRoot\Common.ps1"

if ([string]::IsNullOrWhiteSpace($OutputFile)) {
    $OutputFile = Resolve-LabPath "tools/concurrency-lab/output/register-results.csv"
}

Ensure-ParentDirectory -Path $OutputFile

Invoke-ConcurrencyLabRunner -Arguments @(
    "--mode", "register",
    "--base-url", $BaseUrl,
    "--terminals", ($TerminalIds -join ","),
    "--device-count", $DeviceCount,
    "--register-concurrency", $Concurrency,
    "--device-prefix", $DevicePrefix,
    "--use-bootstrap-identity", $UseBootstrapIdentity.ToString().ToLowerInvariant(),
    "--auto-confirm", $AutoConfirm.ToString().ToLowerInvariant(),
    "--register-output", $OutputFile
)
