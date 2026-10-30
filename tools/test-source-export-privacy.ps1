# Synthetic host-only check: private incident material never enters a source review.
$ErrorActionPreference = 'Stop'
$fixtureRoot = Join-Path ([System.IO.Path]::GetTempPath()) ('keeppeer-export-privacy-' + [Guid]::NewGuid().ToString('N'))
$exporterPath = Join-Path $PSScriptRoot 'export-source-code.ps1'
try {
    [void][System.IO.Directory]::CreateDirectory((Join-Path $fixtureRoot 'docs/reports'))
    [System.IO.File]::WriteAllText((Join-Path $fixtureRoot 'docs/reports/phase-2-report.json'),
        '{"phase":2,"outcome":"incomplete","summary":"Synthetic privacy fixture","checks":[],"addedPaths":[],"modifiedPaths":[],"unresolvedIssues":[],"deviceChecks":"Not run"}')
    [System.IO.File]::WriteAllText((Join-Path $fixtureRoot 'README.md'), 'REVIEWABLE_SOURCE_SENTINEL')
    foreach ($directory in @('keeppeer-preservation', 'incident-payloads', 'private-captures', 'no_backup', 'no-backup')) {
        $privateDirectory = Join-Path $fixtureRoot $directory
        [void][System.IO.Directory]::CreateDirectory($privateDirectory)
        [System.IO.File]::WriteAllText((Join-Path $privateDirectory 'private.md'), 'PRIVATE_SYNTHETIC_SENTINEL')
    }
    foreach ($filename in @('incident-payload.md', 'private-capture.xml', 'preservation-manifest.md',
            'keeppeer_vault.db.md', 'keeppeer_prototype.sqlite.xml', 'vault-envelope.bin.md', 'biometric-envelope.bin.xml')) {
        [System.IO.File]::WriteAllText((Join-Path $fixtureRoot $filename), 'PRIVATE_SYNTHETIC_SENTINEL')
    }
    & $exporterPath -Phase 2 -Outcome incomplete -ProjectRoot $fixtureRoot
    $reviewText = [System.IO.File]::ReadAllText((Join-Path $fixtureRoot 'source-code.txt'))
    if (-not $reviewText.Contains('REVIEWABLE_SOURCE_SENTINEL')) { throw 'Ordinary source was omitted.' }
    if ($reviewText.Contains('PRIVATE_SYNTHETIC_SENTINEL')) { throw 'Private fixture was included.' }
    Write-Host 'PASS: ordinary source retained; 5 private directories and 7 sensitive filename families excluded.'
} finally {
    if ([System.IO.Directory]::Exists($fixtureRoot)) { Remove-Item -LiteralPath $fixtureRoot -Recurse -Force }
}
