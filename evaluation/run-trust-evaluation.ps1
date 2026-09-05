$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$results = Join-Path $PSScriptRoot 'results'
New-Item -ItemType Directory -Force $results | Out-Null
$out = Join-Path $results 'trust-results.csv'
$summary = Join-Path $results 'trust-summary.md'
Write-Warning "This run overwrites $out and $summary"
Push-Location (Join-Path $root 'backend')
try {
    $isWindows = [Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT
    $wrapper = Join-Path (Get-Location) $(if ($isWindows) { 'mvnw.cmd' } else { 'mvnw' })
    if (!(Test-Path $wrapper)) { throw "Maven wrapper was not found: $wrapper" }
    $previousPreference = $ErrorActionPreference
    try {
        # Windows PowerShell 5.1 represents native stderr as ErrorRecord objects when
        # streams are merged. Only the Maven process exit code determines success.
        $ErrorActionPreference = 'Continue'
        $nativeOutput = & $wrapper '-Dtest=M9TrustEvaluationTest' test 2>&1
        $mavenExitCode = $LASTEXITCODE
    }
    finally { $ErrorActionPreference = $previousPreference }
    $log = @($nativeOutput | ForEach-Object { $_.ToString() })
    if ($mavenExitCode -ne 0) { $log | Write-Host; throw "Trust evaluation failed with Maven exit code $mavenExitCode." }
}
finally { Pop-Location }
$rows = @($log | ForEach-Object { if ($_ -match 'M9_RESULT,([^,]+),([^,]+),([^,]+),(.+)$') { [pscustomobject]@{ experiment_id=$Matches[1]; variant=$Matches[2]; metric=$Matches[3]; value=$Matches[4].Trim() } } })
if (!$rows.Count) { throw 'No M9_RESULT records were emitted; no result files written.' }
$rows | Export-Csv -NoTypeInformation -Encoding utf8 $out
$experiments = ($rows.experiment_id | Sort-Object -Unique).Count
$e1 = ($rows | Where-Object {$_.experiment_id -eq 'E1' -and $_.metric -eq 'absolute_difference'}).value
$e4 = ($rows | Where-Object {$_.experiment_id -eq 'E4' -and $_.metric -eq 'extremeness_attenuation_pct'}).value
$reductions = @($rows | Where-Object {$_.metric -eq 'distortion_reduction_pct'} | ForEach-Object {[double]$_.value})
$strongest = if ($reductions.Count) { ($reductions | Measure-Object -Maximum).Maximum } else { 'n/a' }
$transitions = ($rows | Where-Object {$_.experiment_id -eq 'E6' -and $_.metric -eq 'confidence'} | ForEach-Object { "$($_.variant)=$($_.value)" }) -join ', '
@("# Executed M9 Trust summary","","Generated from the production aggregation primitive at $(Get-Date -Format o).","","- Experiments: $experiments (E1-E6)","- Normal-consensus absolute deviation: $e1","- Strongest measured distortion reduction (%): $strongest","- Sparse-evidence extremeness attenuation (%): $e4","- Confidence transitions: $transitions","","These measurements describe controlled synthetic scenarios; they do not establish general superiority or field accuracy.") | Set-Content -Encoding utf8 $summary
Write-Host "Wrote $out and $summary"
