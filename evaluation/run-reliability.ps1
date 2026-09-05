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
if($LASTEXITCODE){throw 'Unable to query connected Android devices with adb.'}
$deviceSerials=@($devices|ForEach-Object{if($_ -match '^([^\s]+)\s+device(?:\s|$)'){$Matches[1]}})
if($deviceSerials.Count -eq 0){throw 'A connected, authorized Android emulator/device in state device is required for the M9 instrumentation evaluation.'}
if($deviceSerials.Count -ne 1){throw 'Multiple authorized Android devices are connected. Disconnect all but the single deterministic M9 evaluation target.'}
$deviceSerial=$deviceSerials[0]
function Invoke-SelectedAdb([string[]]$Arguments,[string]$Operation){
    $previousPreference=$ErrorActionPreference
    try{$ErrorActionPreference='Continue';$nativeOutput=& $adbPath -s $deviceSerial @Arguments 2>&1;$adbExitCode=$LASTEXITCODE}
    finally{$ErrorActionPreference=$previousPreference}
    $text=@($nativeOutput|ForEach-Object{$_.ToString()})
    if($adbExitCode -ne 0){throw "adb failed while attempting to $Operation (exit code $adbExitCode)."}
    return $text
}
$out=Join-Path $PSScriptRoot 'results/reliability-results.csv';Write-Warning "This run overwrites $out"
[void](Invoke-SelectedAdb -Arguments @('logcat','-c') -Operation 'clear Logcat before the targeted evaluation')
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
$logcat=Invoke-SelectedAdb -Arguments @('logcat','-d','-s','M9_EVAL:I','*:S') -Operation 'read the current-run M9_EVAL Logcat results'
# Source priority is dedicated current-run Logcat, then Gradle output, then
# current-run artifacts. All available copies are retained for conflict checks.
$resultLines=@($logcat|Where-Object{$_ -match 'M9_RESULT,'})
$resultLines+=@($log|Where-Object{$_ -match 'M9_RESULT,'})
$artifactResultLines=@()
$artifactRoots=@(
    (Join-Path $root 'app/build/outputs/androidTest-results/connected'),
    (Join-Path $root 'app/build/outputs/androidTest-results'),
    (Join-Path $root 'app/build/reports/androidTests')
)|Select-Object -Unique
$candidateFiles=@()
foreach($artifactRoot in $artifactRoots){
    if(Test-Path -LiteralPath $artifactRoot){
        $candidateFiles+=@(Get-ChildItem -LiteralPath $artifactRoot -Recurse -File -ErrorAction SilentlyContinue |
            Where-Object{$_.LastWriteTime -ge $runStarted -and $_.Extension -in @('.xml','.txt')})
    }
}
# Stable result XML is searched before optional text/logcat artifacts. Android
# tooling may remove a listed logcat file while Gradle finalizes its results.
$candidateFiles=@($candidateFiles|Sort-Object @{Expression={if($_.Extension -eq '.xml'){0}else{1}}},FullName -Unique)
$artifactResultLines=@(foreach($candidate in $candidateFiles){
    try{
        if(Test-Path -LiteralPath $candidate.FullName){
            Select-String -LiteralPath $candidate.FullName -Pattern 'M9_RESULT,' -ErrorAction Stop | ForEach-Object{$_.Line}
        }
    }catch{
        # Skip only this transient or unreadable artifact; other current-run
        # results may still contain the instrumentation output.
        continue
    }
})
$resultLines+=@($artifactResultLines)
$parsedRows=@($resultLines|ForEach-Object{if($_ -match 'M9_RESULT,([^,]+),(\d+),(\d+),(\d+),(PASS|FAIL)'){[pscustomobject]@{scenario=$Matches[1];attempts=[int]$Matches[2];successes=[int]$Matches[3];success_rate_pct=[math]::Round(100*[int]$Matches[3]/[int]$Matches[2],2);duplicates=[int]$Matches[4];result=$Matches[5]}}})
if(!$parsedRows.Count){throw 'The targeted test passed, but no M9_RESULT records were found in current-run M9_EVAL Logcat, Gradle output, or readable instrumentation result files; no CSV was written.'}
$expectedScenarios=@('R1_offline_create_then_reconnect','R2_transient_delete_then_reconnect','R3_repeated_synchronization_idempotency')
$rows=@()
foreach($scenario in $expectedScenarios){
    $matches=@($parsedRows|Where-Object{$_.scenario -eq $scenario})
    if(!$matches.Count){throw "Missing required M9 reliability result for $scenario; no CSV was written."}
    $distinct=@($matches|Sort-Object scenario,attempts,successes,duplicates,result -Unique)
    if($distinct.Count -ne 1){throw "Conflicting M9 reliability results were found for $scenario; no CSV was written."}
    $row=$distinct[0]
    if($row.attempts -ne 30 -or $row.successes -ne 30 -or $row.duplicates -ne 0 -or $row.result -ne 'PASS'){throw "M9 reliability result validation failed for $scenario; no CSV was written."}
    $rows+=$row
}
if(@($parsedRows|Where-Object{$_.scenario -notin $expectedScenarios}).Count){throw 'Unexpected M9 reliability scenario results were found; no CSV was written.'}
$rows|Export-Csv -NoTypeInformation -Encoding utf8 $out;Write-Host "Wrote $out"
