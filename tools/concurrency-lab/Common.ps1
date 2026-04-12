Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-LabPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RelativePath
    )

    $projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
    return Join-Path $projectRoot $RelativePath
}

function Ensure-ParentDirectory {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $parent = Split-Path -Parent $Path
    if (-not [string]::IsNullOrWhiteSpace($parent) -and -not (Test-Path $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }
}

function Resolve-JavaTool {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ToolName
    )

    $candidates = @()
    if ($env:JAVA_HOME) {
        $candidates += (Join-Path $env:JAVA_HOME ("bin\" + $ToolName + ".exe"))
        $candidates += (Join-Path $env:JAVA_HOME ("bin\" + $ToolName))
    }
    $candidates += "D:\JDK11\bin\$ToolName.exe"
    $candidates += "$ToolName.exe"
    $candidates += $ToolName

    foreach ($candidate in $candidates) {
        if (Get-Command $candidate -ErrorAction SilentlyContinue) {
            return $candidate
        }
    }

    throw "Unable to find Java tool: $ToolName"
}

function Build-ConcurrencyLabRunner {
    $sourceFile = Resolve-LabPath "tools/concurrency-lab/src/ConcurrencyLabRunner.java"
    $buildDir = Resolve-LabPath "tools/concurrency-lab/bin"
    $classFile = Join-Path $buildDir "ConcurrencyLabRunner.class"
    if (-not (Test-Path $buildDir)) {
        New-Item -ItemType Directory -Path $buildDir -Force | Out-Null
    }

    $shouldCompile = $true
    if (Test-Path $classFile) {
        $sourceTime = (Get-Item $sourceFile).LastWriteTimeUtc
        $classTime = (Get-Item $classFile).LastWriteTimeUtc
        $shouldCompile = $sourceTime -gt $classTime
    }

    if ($shouldCompile) {
        $javac = Resolve-JavaTool -ToolName "javac"
        & $javac -encoding UTF-8 -d $buildDir $sourceFile
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to compile ConcurrencyLabRunner.java"
        }
    }

    return $buildDir
}

function Invoke-ConcurrencyLabRunner {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $buildDir = Build-ConcurrencyLabRunner
    $java = Resolve-JavaTool -ToolName "java"
    & $java -cp $buildDir ConcurrencyLabRunner @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "ConcurrencyLabRunner exited with code $LASTEXITCODE"
    }
}
