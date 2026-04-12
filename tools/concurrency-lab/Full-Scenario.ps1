param(
    [string]$BaseUrl = "http://localhost:8080",
    [string[]]$TerminalIds = @("fusion1", "fusion2", "fusion3"),
    [int]$DeviceCount = 30,
    [int]$RegisterConcurrency = 10,
    [int]$MessagesPerDevice = 3,
    [int]$TelemetryConcurrency = 10,
    [string]$OutputDirectory = ""
)

. "$PSScriptRoot\Common.ps1"

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Resolve-LabPath "tools/concurrency-lab/output"
}
if (-not (Test-Path $OutputDirectory)) {
    New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
}

$registerOutput = Join-Path $OutputDirectory "register-results.csv"
$telemetryOutput = Join-Path $OutputDirectory "telemetry-results.csv"

Invoke-ConcurrencyLabRunner -Arguments @(
    "--mode", "full",
    "--base-url", $BaseUrl,
    "--terminals", ($TerminalIds -join ","),
    "--device-count", $DeviceCount,
    "--register-concurrency", $RegisterConcurrency,
    "--messages-per-device", $MessagesPerDevice,
    "--telemetry-concurrency", $TelemetryConcurrency,
    "--register-output", $registerOutput,
    "--telemetry-output", $telemetryOutput
)
