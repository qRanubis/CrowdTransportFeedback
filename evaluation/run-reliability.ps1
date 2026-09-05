$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$isWindows=[Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT
$adbCommand=Get-Command $(if($isWindows){'adb.exe'}else{'adb'}) -ErrorAction SilentlyContinue
$adbPath=if($adbCommand){$adbCommand.Source}else{$null}
if(!$adbPath){
    $candidates=@()
    foreach($sdk in @($env:ANDROID_SDK_ROOT,$env:ANDROID_HOME)){if($sdk){$candidates+=Join-Path $sdk $(if($isWindows){'platform-tools/adb.exe'}else{'platform-tools/adb'})}}
    if($isWindows -and $env:LOCALAPPDATA){$candidates+=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'}
    $adbPath=$candidates|Where-Object{Test-Path $_}|Select-Object -First 1
}
if(!$adbPath){throw 'Android adb was not found. Install Android SDK platform-tools and add adb to PATH, ANDROID_SDK_ROOT, or ANDROID_HOME.'}
$devices=& $adbPath devices 2>$null
if($LASTEXITCODE -or !($devices|Select-String "\tdevice$")){throw 'A connected, authorized Android emulator/device in state device is required for the M9 instrumentation evaluation.'}
$out=Join-Path $PSScriptRoot 'results/reliability-results.csv';Write-Warning "This run overwrites $out"
$runStarted=Get-Date
$wrapper=Join-Path $root $(if($isWindows){'gradlew.bat'}else{'gradlew'})
if(!(Test-Path $wrapper)){throw "Gradle wrapper was not found: $wrapper"}
Push-Location $root
try{
    $previousPreference=$ErrorActionPreference
    try{
        # Gradle warnings on native stderr are data, not PowerShell failures. The
        # process exit code remains authoritative on Windows PowerShell 5.1+.
        $ErrorActionPreference='Continue'
        $nativeOutput=& $wrapper connectedDebugAndroidTest --info '-Pandroid.testInstrumentationRunnerArguments.class=com.example.crowdtransportfeedback.data.repository.M9ReliabilityEvaluationTest' 2>&1
        $gradleExitCode=$LASTEXITCODE
    }finally{$ErrorActionPreference=$previousPreference}
    $log=@($nativeOutput|ForEach-Object{$_.ToString()})
    if($gradleExitCode -ne 0){$log|Write-Host;throw "M9 reliability instrumentation evaluation failed with Gradle exit code $gradleExitCode."}
}finally{Pop-Location}
$resultLines=@($log|Where-Object{$_ -match 'M9_RESULT,'})
if(!$resultLines.Count){
    $resultLines=@(Get-ChildItem (Join-Path $root 'app/build') -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object{$_.LastWriteTime -ge $runStarted -and $_.Extension -in @('.xml','.txt')} |
        Select-String -Pattern 'M9_RESULT,' | ForEach-Object{$_.Line})
}
$rows=@($resultLines|ForEach-Object{if($_ -match 'M9_RESULT,([^,]+),(\d+),(\d+),(\d+),(PASS|FAIL)'){[pscustomobject]@{scenario=$Matches[1];attempts=[int]$Matches[2];successes=[int]$Matches[3];success_rate_pct=[math]::Round(100*[int]$Matches[3]/[int]$Matches[2],2);duplicates=[int]$Matches[4];result=$Matches[5]}}})
if(!$rows.Count){throw 'The targeted test passed, but no M9_RESULT records were found in current Gradle output or current-run instrumentation result files; no CSV was written.'}
$rows|Export-Csv -NoTypeInformation -Encoding utf8 $out;Write-Host "Wrote $out"
