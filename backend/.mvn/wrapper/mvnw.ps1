$ErrorActionPreference = "Stop"

$wrapperDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDir = Split-Path -Parent (Split-Path -Parent $wrapperDir)
$propertiesPath = Join-Path $wrapperDir "maven-wrapper.properties"

if (-not (Test-Path $propertiesPath)) {
    throw "Missing Maven wrapper properties: $propertiesPath"
}

$properties = Get-Content -Raw $propertiesPath | ConvertFrom-StringData
$distributionUrl = $properties.distributionUrl
if ([string]::IsNullOrWhiteSpace($distributionUrl)) {
    throw "distributionUrl is missing from maven-wrapper.properties"
}

$archiveName = [System.IO.Path]::GetFileName($distributionUrl)
$distributionName = $archiveName -replace '-bin\.zip$', ''
$mavenUserHome = if ($env:MAVEN_USER_HOME) { $env:MAVEN_USER_HOME } else { Join-Path $HOME ".m2" }
$mavenHome = Join-Path $mavenUserHome "wrapper\dists\$distributionName"
$mavenCommand = Join-Path $mavenHome "bin\mvn.cmd"

if (-not (Test-Path $mavenCommand)) {
    $tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("mvnw-" + [guid]::NewGuid().ToString("N"))
    $archivePath = Join-Path $tempRoot $archiveName
    New-Item -ItemType Directory -Path $tempRoot -Force | Out-Null

    try {
        Write-Host "Downloading Maven from $distributionUrl"
        Invoke-WebRequest -Uri $distributionUrl -OutFile $archivePath
        Expand-Archive -Path $archivePath -DestinationPath $tempRoot -Force

        $extracted = Get-ChildItem -Path $tempRoot -Directory |
            Where-Object { Test-Path (Join-Path $_.FullName "bin\mvn.cmd") } |
            Select-Object -First 1

        if (-not $extracted) {
            throw "Downloaded archive does not contain a Maven distribution"
        }

        New-Item -ItemType Directory -Path (Split-Path -Parent $mavenHome) -Force | Out-Null
        if (Test-Path $mavenHome) {
            Remove-Item -Path $mavenHome -Recurse -Force
        }
        Move-Item -Path $extracted.FullName -Destination $mavenHome
    }
    finally {
        if (Test-Path $tempRoot) {
            Remove-Item -Path $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
}

Push-Location $projectDir
try {
    & $mavenCommand @args
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
