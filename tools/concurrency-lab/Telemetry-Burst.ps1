param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$SessionFile = "",
    [int]$MessagesPerDevice = 3,
    [int]$Concurrency = 10,
    [string]$OutputFile = ""
)

. "$PSScriptRoot\Common.ps1"

if ([string]::IsNullOrWhiteSpace($SessionFile)) {
    $SessionFile = Resolve-LabPath "tools/concurrency-lab/output/register-results.csv"
}
if ([string]::IsNullOrWhiteSpace($OutputFile)) {
    $OutputFile = Resolve-LabPath "tools/concurrency-lab/output/telemetry-results.csv"
}

Ensure-ParentDirectory -Path $OutputFile

Invoke-ConcurrencyLabRunner -Arguments @(
    "--mode", "telemetry",
    "--base-url", $BaseUrl,
    "--session-file", $SessionFile,
    "--messages-per-device", $MessagesPerDevice,
    "--telemetry-concurrency", $Concurrency,
    "--telemetry-output", $OutputFile
)
