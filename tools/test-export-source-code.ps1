<#
.SYNOPSIS
    Automated fixture test for tools/export-source-code.ps1.

.DESCRIPTION
    Creates an isolated synthetic fixture project in $env:TEMP, exercises
    the exporter across discovery, exclusions, redactions, deletions,
    error resilience, and phase reporting, and asserts all contract criteria.
#>

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$exporterScript = Join-Path $projectRoot 'tools/export-source-code.ps1'

$testRunId = [System.Guid]::NewGuid().ToString('N').Substring(0, 8)
$fixtureDir = Join-Path $env:TEMP "Keeppeer-Fixture-$testRunId"

Write-Host "Creating isolated synthetic fixture at: $fixtureDir" -ForegroundColor Cyan
New-Item -ItemType Directory -Path $fixtureDir -Force | Out-Null

try {
    # 1. Populate synthetic fixture directories
    $fixtureAppDir = Join-Path $fixtureDir 'app'
    $fixtureGradleDir = Join-Path $fixtureDir 'gradle'
    $fixtureWrapperDir = Join-Path $fixtureGradleDir 'wrapper'
    $fixtureSchemasDir = Join-Path $fixtureAppDir 'schemas/com.gem.keeppeer.data.local.AppDatabase'
    $fixtureSrcDir = Join-Path $fixtureAppDir 'src/main/java/com/gem/keeppeer'
    $fixtureDocsDir = Join-Path $fixtureDir 'docs/reports'
    $fixtureToolsDir = Join-Path $fixtureDir 'tools'

    foreach ($d in @($fixtureAppDir, $fixtureGradleDir, $fixtureWrapperDir, $fixtureSchemasDir, $fixtureSrcDir, $fixtureDocsDir, $fixtureToolsDir)) {
        New-Item -ItemType Directory -Path $d -Force | Out-Null
    }

    # Copy exporter script to fixture tools
    Copy-Item -LiteralPath $exporterScript -Destination (Join-Path $fixtureToolsDir 'export-source-code.ps1')

    # Extensionless gradlew & gradlew.bat
    [System.IO.File]::WriteAllText((Join-Path $fixtureDir 'gradlew'), "#!/usr/bin/env sh`necho gradlew fixture`n")
    [System.IO.File]::WriteAllText((Join-Path $fixtureDir 'gradlew.bat'), "@echo off`necho gradlew bat fixture`n")
    [System.IO.File]::WriteAllText((Join-Path $fixtureDir '.gitignore'), "*.iml`n.gradle/`nbuild/`nlocal.properties`n")

    # Wrapper & toolchain properties
    [System.IO.File]::WriteAllText(
        (Join-Path $fixtureWrapperDir 'gradle-wrapper.properties'),
        "distributionUrl=https\://services.gradle.org/distributions/gradle-9.8.2-bin.zip`n"
    )
    [System.IO.File]::WriteAllText(
        (Join-Path $fixtureGradleDir 'gradle-daemon-jvm.properties'),
        "toolchainVersion=25`n"
    )

    # Version catalog & build scripts
    [System.IO.File]::WriteAllText(
        (Join-Path $fixtureGradleDir 'libs.versions.toml'),
        "[versions]`nagp = `"9.5.0`"`nkotlin = `"2.2.10`"`nroom = `"2.8.5`"`n"
    )
    [System.IO.File]::WriteAllText(
        (Join-Path $fixtureDir 'build.gradle.kts'),
        "// Root build file fixture`n"
    )
    [System.IO.File]::WriteAllText(
        (Join-Path $fixtureAppDir 'build.gradle.kts'),
        "android {`n    namespace = `"com.fixture.test`"`n    defaultConfig {`n        minSdk = 24`n        targetSdk = 37`n    }`n}`n"
    )

    # Room schema JSON
    [System.IO.File]::WriteAllText(
        (Join-Path $fixtureSchemasDir '1.json'),
        "{`"formatVersion`": 1, `"database`": {`"version`": 1, `"entities`": []}}`n"
    )

    # Kotlin source
    $fooFile = Join-Path $fixtureSrcDir 'Foo.kt'
    [System.IO.File]::WriteAllText(
        $fooFile,
        "package com.gem.keeppeer`n`nclass Foo {`n    fun hello(): String = `"Hello Fixture`"`n}`n"
    )

    # Secret / credential files (MUST BE EXCLUDED)
    [System.IO.File]::WriteAllText((Join-Path $fixtureDir 'signing.properties'), "storePassword=fake_super_secret_store_pass`n")
    [System.IO.File]::WriteAllText((Join-Path $fixtureDir 'secrets.properties'), "apiKey=fake_super_secret_api_key_123`n")
    [System.IO.File]::WriteAllText((Join-Path $fixtureDir '.env'), "VAULT_SECRET=fake_env_secret_value`n")
    [System.IO.File]::WriteAllText((Join-Path $fixtureDir 'local.properties'), "sdk.dir=C:\\FakeSdkPath`n")

    # Structured Phase 1 blocked report
    $fixtureReportContent = @'
{
  "phase": 1,
  "outcome": "blocked",
  "summary": "Phase 1 Room implementation blocked on verification harness.",
  "checks": [
    {
      "name": "Database Migration",
      "result": "FAIL",
      "details": "Synthetic migration test encountered unexpected table state"
    },
    {
      "name": "Device Unlock Verification",
      "result": "NOT RUN",
      "details": "Device testing not reached due to migration blocker"
    }
  ],
  "addedPaths": [
    "app/schemas/com.gem.keeppeer.data.local.AppDatabase/1.json"
  ],
  "modifiedPaths": [],
  "deletedPaths": [],
  "unresolvedIssues": [
    "Investigate synthetic table migration error"
  ],
  "deviceChecks": "Not run (blocked by migration check)"
}
'@
    [System.IO.File]::WriteAllText((Join-Path $fixtureDocsDir 'phase-1-report.json'), $fixtureReportContent)

    # Initialize a git repo in fixture for git probing
    $prevEAP = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    git -C $fixtureDir init 2>&1 | Out-Null
    $ErrorActionPreference = $prevEAP

    # -------------------------------------------------------------
    # TEST 1: Run exporter from OUTSIDE fixture directory (e.g. from $env:TEMP)
    # -------------------------------------------------------------
    Write-Host "[TEST 1] Invoking exporter from outside fixture root..." -ForegroundColor Yellow
    Push-Location $env:TEMP
    try {
        & powershell.exe -ExecutionPolicy Bypass -File (Join-Path $fixtureToolsDir 'export-source-code.ps1') `
            -Phase 1 -Outcome blocked -ProjectRoot $fixtureDir
        if ($LASTEXITCODE -ne 0) { throw "Exporter exited with code $LASTEXITCODE" }
    } finally {
        Pop-Location
    }

    $snapshot1Path = Join-Path $fixtureDir 'source-code.txt'
    if (-not (Test-Path -LiteralPath $snapshot1Path)) {
        throw "ASSERTION FAILED: source-code.txt was not created in fixture root"
    }
    $snapshot1Content = [System.IO.File]::ReadAllText($snapshot1Path, [System.Text.Encoding]::UTF8)

    # Check 1: gradlew included
    if ($snapshot1Content -notmatch '===== BEGIN FILE: gradlew =====') {
        throw "ASSERTION FAILED: extensionless gradlew missing from snapshot"
    }
    # Check 2: Room schema JSON included
    if ($snapshot1Content -notmatch '===== BEGIN FILE: app/schemas/com\.gem\.keeppeer\.data\.local\.AppDatabase/1\.json =====') {
        throw "ASSERTION FAILED: Room schema 1.json missing from snapshot"
    }
    # Check 3: Foo.kt included
    if ($snapshot1Content -notmatch '===== BEGIN FILE: app/src/main/java/com/gem/keeppeer/Foo\.kt =====') {
        throw "ASSERTION FAILED: Foo.kt missing from snapshot"
    }
    # Check 4: Secret files strictly excluded from Inventory & Boundaries
    if ($snapshot1Content -match '===== BEGIN FILE: .*signing\.properties' -or $snapshot1Content -match 'fake_super_secret_store_pass') {
        throw "ASSERTION FAILED: signing.properties was leaked into snapshot"
    }
    if ($snapshot1Content -match '===== BEGIN FILE: .*secrets\.properties' -or $snapshot1Content -match 'fake_super_secret_api_key_123') {
        throw "ASSERTION FAILED: secrets.properties was leaked into snapshot"
    }
    if ($snapshot1Content -match '===== BEGIN FILE: .*\.env' -or $snapshot1Content -match 'fake_env_secret_value') {
        throw "ASSERTION FAILED: .env was leaked into snapshot"
    }
    if ($snapshot1Content -match '===== BEGIN FILE: .*local\.properties' -or $snapshot1Content -match 'C:\\FakeSdkPath') {
        throw "ASSERTION FAILED: local.properties was leaked into snapshot"
    }
    # Check 5: Header reflects Phase 1 and outcome blocked
    if ($snapshot1Content -notmatch 'PHASE: 1' -or $snapshot1Content -notmatch 'PHASE OUTCOME: blocked') {
        throw "ASSERTION FAILED: Header does not match Phase 1 blocked"
    }
    # Check 6: Truthful report with FAIL and NOT RUN, no hardcoded Phase 0 PASS claims
    if ($snapshot1Content -notmatch 'Database Migration: FAIL') {
        throw "ASSERTION FAILED: Truthful FAIL result missing from report"
    }
    if ($snapshot1Content -notmatch 'Device Unlock Verification: NOT RUN') {
        throw "ASSERTION FAILED: Truthful NOT RUN result missing from report"
    }
    if ($snapshot1Content -match 'Phase 0 foundation and hygiene complete') {
        throw "ASSERTION FAILED: Hardcoded Phase 0 completion summary leaked into Phase 1 snapshot"
    }
    Write-Host "  -> PASS: Discovery, exclusion, and truthful reporting verified." -ForegroundColor Green

    # -------------------------------------------------------------
    # TEST 2: File Deletion Reflection
    # -------------------------------------------------------------
    Write-Host "[TEST 2] Testing deletion reflection..." -ForegroundColor Yellow
    Remove-Item -LiteralPath $fooFile -Force

    Push-Location $env:TEMP
    try {
        & powershell.exe -ExecutionPolicy Bypass -File (Join-Path $fixtureToolsDir 'export-source-code.ps1') `
            -Phase 1 -Outcome blocked -ProjectRoot $fixtureDir
        if ($LASTEXITCODE -ne 0) { throw "Exporter failed on re-export after deletion" }
    } finally {
        Pop-Location
    }

    $snapshot2Content = [System.IO.File]::ReadAllText($snapshot1Path, [System.Text.Encoding]::UTF8)
    if ($snapshot2Content -match '===== BEGIN FILE: app/src/main/java/com/gem/keeppeer/Foo\.kt =====') {
        throw "ASSERTION FAILED: Deleted Foo.kt still present in snapshot boundaries"
    }
    if ($snapshot2Content -match '(?m)^\s+app/src/main/java/com/gem/keeppeer/Foo\.kt$') {
        throw "ASSERTION FAILED: Deleted Foo.kt still present in inventory"
    }
    Write-Host "  -> PASS: Deletion reflected accurately." -ForegroundColor Green

    # -------------------------------------------------------------
    # TEST 3: Redaction Engine Verification
    # -------------------------------------------------------------
    Write-Host "[TEST 3] Testing secret redaction engine..." -ForegroundColor Yellow
    $secretFile = Join-Path $fixtureSrcDir 'SecretConfig.kt'
    [System.IO.File]::WriteAllText(
        $secretFile,
        "package com.gem.keeppeer`n`nobject SecretConfig {`n    val apiKey = `"super_secret_production_key_xyz987`"`n}`n"
    )

    Push-Location $env:TEMP
    try {
        & powershell.exe -ExecutionPolicy Bypass -File (Join-Path $fixtureToolsDir 'export-source-code.ps1') `
            -Phase 1 -Outcome blocked -ProjectRoot $fixtureDir
        if ($LASTEXITCODE -ne 0) { throw "Exporter failed during redaction test" }
    } finally {
        Pop-Location
    }

    $snapshot3Content = [System.IO.File]::ReadAllText($snapshot1Path, [System.Text.Encoding]::UTF8)
    # The secret value MUST NOT be in the snapshot
    if ($snapshot3Content -match 'super_secret_production_key_xyz987') {
        throw "ASSERTION FAILED: Secret value was not redacted in snapshot!"
    }
    # But the redaction marker MUST be present
    if ($snapshot3Content -notmatch '\[REDACTED: potential secret at app/src/main/java/com/gem/keeppeer/SecretConfig\.kt') {
        throw "ASSERTION FAILED: Redaction marker missing in snapshot"
    }
    # Underlying source file on disk MUST NOT be altered
    $secretFileDisk = [System.IO.File]::ReadAllText($secretFile, [System.Text.Encoding]::UTF8)
    if ($secretFileDisk -notmatch 'super_secret_production_key_xyz987') {
        throw "ASSERTION FAILED: Underlying source file was modified by redaction engine!"
    }
    Write-Host "  -> PASS: Redaction engine successfully redacted snapshot without modifying source." -ForegroundColor Green

    # -------------------------------------------------------------
    # TEST 4: Error Handling & Previous Snapshot Preservation
    # -------------------------------------------------------------
    Write-Host "[TEST 4] Testing error handling and previous snapshot preservation..." -ForegroundColor Yellow
    $lastUsableContent = [System.IO.File]::ReadAllText($snapshot1Path, [System.Text.Encoding]::UTF8)

    # Intentionally trigger error: outcome mismatch
    Push-Location $env:TEMP
    $errorTriggered = $false
    $savedEAP = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $null = & powershell.exe -ExecutionPolicy Bypass -File (Join-Path $fixtureToolsDir 'export-source-code.ps1') `
            -Phase 1 -Outcome complete -ProjectRoot $fixtureDir 2>&1
        if ($LASTEXITCODE -ne 0) {
            $errorTriggered = $true
        }
    } finally {
        $ErrorActionPreference = $savedEAP
        Pop-Location
    }

    if (-not $errorTriggered) {
        throw "ASSERTION FAILED: Exporter should have failed on outcome mismatch"
    }

    # Verify previous snapshot was preserved
    $contentAfterFailedRun = [System.IO.File]::ReadAllText($snapshot1Path, [System.Text.Encoding]::UTF8)
    if ($contentAfterFailedRun -ne $lastUsableContent) {
        throw "ASSERTION FAILED: Previous snapshot was corrupted or modified on failed run!"
    }
    Write-Host "  -> PASS: Failure safely handled; previous snapshot preserved." -ForegroundColor Green

    Write-Host "`nAll fixture assertions passed successfully!" -ForegroundColor Green
} finally {
    # Clean up fixture
    if (Test-Path -LiteralPath $fixtureDir) {
        Remove-Item -LiteralPath $fixtureDir -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "Cleaned up synthetic fixture at: $fixtureDir" -ForegroundColor DarkGray
    }
}
