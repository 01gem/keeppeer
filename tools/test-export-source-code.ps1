<#
.SYNOPSIS
    Automated fixture test for tools/export-source-code.ps1.

.DESCRIPTION
    Creates an isolated synthetic fixture project in $env:TEMP, exercises
    the exporter across discovery, exclusions, typed/untyped redactions, deletions,
    report validation, unreadable source failure, replacement failure, and
    first-write/repeated atomic replacements.
#>

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$exporterScript = Join-Path $projectRoot 'tools/export-source-code.ps1'

$testRunId = [System.Guid]::NewGuid().ToString('N').Substring(0, 8)
$fixtureDir = Join-Path $env:TEMP "Keeppeer-Fixture-$testRunId"

# Guardrail: Ensure fixture directory is strictly within TEMP
$tempRoot = [System.IO.Path]::GetFullPath($env:TEMP).TrimEnd('\', '/')
$resolvedFixture = [System.IO.Path]::GetFullPath($fixtureDir)
if (-not $resolvedFixture.StartsWith($tempRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Safety guardrail triggered: fixture directory '$resolvedFixture' is not inside '$tempRoot'."
}

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
    # TEST 1: First creation (no existing snapshot) from OUTSIDE fixture root
    # -------------------------------------------------------------
    Write-Host "[TEST 1] First creation: Invoking exporter from outside fixture root..." -ForegroundColor Yellow
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
        throw "ASSERTION FAILED: source-code.txt was not created on first write"
    }
    $snapshot1Content = [System.IO.File]::ReadAllText($snapshot1Path, [System.Text.Encoding]::UTF8)

    # Verify discovery & exclusions
    if ($snapshot1Content -notmatch '===== BEGIN FILE: gradlew =====') {
        throw "ASSERTION FAILED: extensionless gradlew missing from snapshot"
    }
    if ($snapshot1Content -notmatch '===== BEGIN FILE: app/schemas/com\.gem\.keeppeer\.data\.local\.AppDatabase/1\.json =====') {
        throw "ASSERTION FAILED: Room schema 1.json missing from snapshot"
    }
    if ($snapshot1Content -notmatch '===== BEGIN FILE: app/src/main/java/com/gem/keeppeer/Foo\.kt =====') {
        throw "ASSERTION FAILED: Foo.kt missing from snapshot"
    }
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
    if ($snapshot1Content -notmatch 'PHASE: 1' -or $snapshot1Content -notmatch 'PHASE OUTCOME: blocked') {
        throw "ASSERTION FAILED: Header does not match Phase 1 blocked"
    }
    if ($snapshot1Content -notmatch 'Database Migration: FAIL' -or $snapshot1Content -notmatch 'Device Unlock Verification: NOT RUN') {
        throw "ASSERTION FAILED: Truthful check results missing from report"
    }
    if ($snapshot1Content -match 'Phase 0 foundation and hygiene complete') {
        throw "ASSERTION FAILED: Hardcoded Phase 0 completion summary leaked into Phase 1 snapshot"
    }
    Write-Host "  -> PASS: First write, discovery, exclusion, and truthful reporting verified." -ForegroundColor Green

    # -------------------------------------------------------------
    # TEST 2: Repeated atomic replacement & File Deletion Reflection
    # -------------------------------------------------------------
    Write-Host "[TEST 2] Testing repeated replacement with file deletion reflection..." -ForegroundColor Yellow
    Remove-Item -LiteralPath $fooFile -Force

    Push-Location $env:TEMP
    try {
        & powershell.exe -ExecutionPolicy Bypass -File (Join-Path $fixtureToolsDir 'export-source-code.ps1') `
            -Phase 1 -Outcome blocked -ProjectRoot $fixtureDir
        if ($LASTEXITCODE -ne 0) { throw "Exporter failed on repeated replacement" }
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
    Write-Host "  -> PASS: Repeated atomic replacement succeeded and deletion reflected accurately." -ForegroundColor Green

    # -------------------------------------------------------------
    # TEST 3: Typed & Untyped Kotlin Secret Redaction + Safe Typed Property
    # -------------------------------------------------------------
    Write-Host "[TEST 3] Testing typed and untyped Kotlin secret redaction..." -ForegroundColor Yellow
    $secretFile = Join-Path $fixtureSrcDir 'SecretConfig.kt'
    $secretSourceCode = @"
package com.gem.keeppeer

object SecretConfig {
    // 1. Untyped secret property
    val apiSecret = "super_secret_untyped_value_456"

    // 2. Ordinary typed secret property
    val apiKey: String = "super_secret_typed_value_123"

    // 3. Private const typed secret property
    private const val MASTER_PASSWORD_KEY: String = "super_master_key_secret_789"

    // 4. Normal nonsensitive typed property (MUST NOT be redacted)
    val appTitle: String = "Completely Safe Application Title"
}
"@
    [System.IO.File]::WriteAllText($secretFile, $secretSourceCode)

    Push-Location $env:TEMP
    try {
        & powershell.exe -ExecutionPolicy Bypass -File (Join-Path $fixtureToolsDir 'export-source-code.ps1') `
            -Phase 1 -Outcome blocked -ProjectRoot $fixtureDir
        if ($LASTEXITCODE -ne 0) { throw "Exporter failed during redaction test" }
    } finally {
        Pop-Location
    }

    $snapshot3Content = [System.IO.File]::ReadAllText($snapshot1Path, [System.Text.Encoding]::UTF8)

    # 1. Untyped secret must be redacted
    if ($snapshot3Content -match 'super_secret_untyped_value_456') {
        throw "ASSERTION FAILED: Untyped secret was not redacted in snapshot!"
    }
    # 2. Typed secret must be redacted
    if ($snapshot3Content -match 'super_secret_typed_value_123') {
        throw "ASSERTION FAILED: Typed secret was not redacted in snapshot!"
    }
    # 3. Private const typed secret must be redacted
    if ($snapshot3Content -match 'super_master_key_secret_789') {
        throw "ASSERTION FAILED: Private const typed secret was not redacted in snapshot!"
    }
    # 4. Normal nonsensitive typed property MUST remain reviewable
    if ($snapshot3Content -notmatch 'Completely Safe Application Title') {
        throw "ASSERTION FAILED: Normal nonsensitive typed property was erroneously redacted or omitted!"
    }
    # 5. Redaction markers present with line context
    if ($snapshot3Content -notmatch '\[REDACTED: potential secret at app/src/main/java/com/gem/keeppeer/SecretConfig\.kt') {
        throw "ASSERTION FAILED: Redaction marker missing in snapshot"
    }
    # 6. Underlying source file on disk MUST NOT be altered
    $secretFileDisk = [System.IO.File]::ReadAllText($secretFile, [System.Text.Encoding]::UTF8)
    if ($secretFileDisk -ne $secretSourceCode) {
        throw "ASSERTION FAILED: Underlying source file was modified by redaction engine!"
    }
    Write-Host "  -> PASS: Typed and untyped secrets redacted; nonsensitive property preserved; source untouched." -ForegroundColor Green

    # -------------------------------------------------------------
    # TEST 4: Report Mismatch Error Handling & Snapshot Preservation
    # -------------------------------------------------------------
    Write-Host "[TEST 4] Testing report mismatch failure and snapshot preservation..." -ForegroundColor Yellow
    $bytesBeforeMismatch = [System.IO.File]::ReadAllBytes($snapshot1Path)

    Push-Location $env:TEMP
    $mismatchFailed = $false
    $savedEAP = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $null = & powershell.exe -ExecutionPolicy Bypass -File (Join-Path $fixtureToolsDir 'export-source-code.ps1') `
            -Phase 1 -Outcome complete -ProjectRoot $fixtureDir 2>&1
        if ($LASTEXITCODE -ne 0) {
            $mismatchFailed = $true
        }
    } finally {
        $ErrorActionPreference = $savedEAP
        Pop-Location
    }

    if (-not $mismatchFailed) {
        throw "ASSERTION FAILED: Exporter should have failed on report outcome mismatch"
    }

    $bytesAfterMismatch = [System.IO.File]::ReadAllBytes($snapshot1Path)
    if (-not [System.Linq.Enumerable]::SequenceEqual([byte[]]$bytesBeforeMismatch, [byte[]]$bytesAfterMismatch)) {
        throw "ASSERTION FAILED: Snapshot was modified despite report mismatch failure!"
    }
    Write-Host "  -> PASS: Report mismatch safely failed; previous snapshot byte-for-byte preserved." -ForegroundColor Green

    # -------------------------------------------------------------
    # TEST 5: Real Unreadable Source Failure & Snapshot Preservation
    # -------------------------------------------------------------
    Write-Host "[TEST 5] Testing real unreadable-source I/O failure and snapshot preservation..." -ForegroundColor Yellow
    $bytesBeforeReadFail = [System.IO.File]::ReadAllBytes($snapshot1Path)

    # Create an unreadable file with an exclusive lock
    $unreadableFile = Join-Path $fixtureSrcDir 'UnreadableSource.kt'
    [System.IO.File]::WriteAllText($unreadableFile, "package com.gem.keeppeer`nclass UnreadableSource`n")
    $exclusiveLockStream = [System.IO.File]::Open(
        $unreadableFile,
        [System.IO.FileMode]::Open,
        [System.IO.FileAccess]::ReadWrite,
        [System.IO.FileShare]::None
    )

    $readFailTriggered = $false
    try {
        Push-Location $env:TEMP
        $savedEAP = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            $null = & powershell.exe -ExecutionPolicy Bypass -File (Join-Path $fixtureToolsDir 'export-source-code.ps1') `
                -Phase 1 -Outcome blocked -ProjectRoot $fixtureDir 2>&1
            if ($LASTEXITCODE -ne 0) {
                $readFailTriggered = $true
            }
        } finally {
            $ErrorActionPreference = $savedEAP
            Pop-Location
        }
    } finally {
        # Release the lock and remove the test file
        $exclusiveLockStream.Close()
        $exclusiveLockStream.Dispose()
        Remove-Item -LiteralPath $unreadableFile -Force -ErrorAction SilentlyContinue
    }

    if (-not $readFailTriggered) {
        throw "ASSERTION FAILED: Exporter should have failed when encountering an unreadable source file"
    }

    $bytesAfterReadFail = [System.IO.File]::ReadAllBytes($snapshot1Path)
    if (-not [System.Linq.Enumerable]::SequenceEqual([byte[]]$bytesBeforeReadFail, [byte[]]$bytesAfterReadFail)) {
        throw "ASSERTION FAILED: Snapshot was modified despite unreadable source failure!"
    }
    Write-Host "  -> PASS: Unreadable source triggered nonzero failure; previous snapshot byte-for-byte preserved." -ForegroundColor Green

    # -------------------------------------------------------------
    # TEST 6: Real Atomic Replacement Failure & Target Preservation
    # -------------------------------------------------------------
    Write-Host "[TEST 6] Testing real atomic replacement failure (locked destination)..." -ForegroundColor Yellow
    $bytesBeforeReplaceFail = [System.IO.File]::ReadAllBytes($snapshot1Path)

    # Exclusively lock the destination snapshot to simulate replacement failure
    $destLockStream = [System.IO.File]::Open(
        $snapshot1Path,
        [System.IO.FileMode]::Open,
        [System.IO.FileAccess]::Read,
        [System.IO.FileShare]::None
    )

    $replaceFailTriggered = $false
    try {
        Push-Location $env:TEMP
        $savedEAP = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            $null = & powershell.exe -ExecutionPolicy Bypass -File (Join-Path $fixtureToolsDir 'export-source-code.ps1') `
                -Phase 1 -Outcome blocked -ProjectRoot $fixtureDir 2>&1
            if ($LASTEXITCODE -ne 0) {
                $replaceFailTriggered = $true
            }
        } finally {
            $ErrorActionPreference = $savedEAP
            Pop-Location
        }
    } finally {
        # Release destination lock
        $destLockStream.Close()
        $destLockStream.Dispose()
    }

    if (-not $replaceFailTriggered) {
        throw "ASSERTION FAILED: Exporter should have failed when destination file was locked"
    }

    # Verify destination still exists and is byte-for-byte intact
    if (-not (Test-Path -LiteralPath $snapshot1Path)) {
        throw "ASSERTION FAILED: Target snapshot was deleted during replacement failure!"
    }
    $bytesAfterReplaceFail = [System.IO.File]::ReadAllBytes($snapshot1Path)
    if (-not [System.Linq.Enumerable]::SequenceEqual([byte[]]$bytesBeforeReplaceFail, [byte[]]$bytesAfterReplaceFail)) {
        throw "ASSERTION FAILED: Target snapshot was modified or corrupted during replacement failure!"
    }
    # Verify temporary file was cleaned up and not left lying around
    $tempSnapshotPath = Join-Path $fixtureDir 'source-code.txt.tmp'
    if (Test-Path -LiteralPath $tempSnapshotPath) {
        throw "ASSERTION FAILED: Temporary snapshot file was not cleaned up after replacement failure!"
    }
    Write-Host "  -> PASS: Replacement failure triggered nonzero exit; previous target byte-for-byte preserved; temp file cleaned." -ForegroundColor Green

    # TEST 7: Preserve mapping expressions and synthetic code embedded in scripts,
    # while still redacting sensitive property-file values.
    Write-Host "[TEST 7] Testing file-type-aware redaction..." -ForegroundColor Yellow
    $mappingSource = @'
fun map(password: String) = Entry(
    password = password,
    notes = "synthetic notes",
)
'@
    [System.IO.File]::WriteAllText((Join-Path $fixtureSrcDir 'Mapping.kt'), $mappingSource)
    $syntheticScript = @'
# Synthetic Kotlin source used by a test harness; this is not a live credential.
val apiSecret = "explicit-fixture-only"
'@
    [System.IO.File]::WriteAllText((Join-Path $fixtureToolsDir 'synthetic-fixture.ps1'), $syntheticScript)
    [System.IO.File]::WriteAllText((Join-Path $fixtureDir 'gradle.properties'), "vault_password=synthetic-property-value")
    & powershell.exe -ExecutionPolicy Bypass -File (Join-Path $fixtureToolsDir 'export-source-code.ps1') `
        -Phase 1 -Outcome blocked -ProjectRoot $fixtureDir
    if ($LASTEXITCODE -ne 0) { throw "Exporter failed during file-type redaction test" }
    $snapshot7Content = [System.IO.File]::ReadAllText($snapshot1Path, [System.Text.Encoding]::UTF8)
    if (-not $snapshot7Content.Contains('    password = password,')) {
        throw "ASSERTION FAILED: Ordinary Kotlin named argument was redacted"
    }
    if (-not $snapshot7Content.Contains('val apiSecret = "explicit-fixture-only"')) {
        throw "ASSERTION FAILED: Synthetic source embedded in PowerShell was redacted"
    }
    if ($snapshot7Content.Contains('synthetic-property-value')) {
        throw "ASSERTION FAILED: Sensitive property-file value was not redacted"
    }
    if (-not $snapshot7Content.Contains('vault_password = [REDACTED: potential secret at gradle.properties:1]')) {
        throw "ASSERTION FAILED: Included build property did not receive labeled redaction"
    }
    Write-Host "  -> PASS: Mapping and synthetic script preserved; sensitive config value redacted." -ForegroundColor Green

    Write-Host "`nAll 7 fixture assertions passed successfully!" -ForegroundColor Green
} finally {
    # Clean up fixture
    if (Test-Path -LiteralPath $fixtureDir) {
        # Check resolved path is inside TEMP before recursive deletion
        $resolved = [System.IO.Path]::GetFullPath($fixtureDir)
        if ($resolved.StartsWith($tempRoot, [System.StringComparison]::OrdinalIgnoreCase) -and $resolved -ne $tempRoot) {
            Remove-Item -LiteralPath $resolved -Recurse -Force -ErrorAction SilentlyContinue
            Write-Host "Cleaned up synthetic fixture at: $resolved" -ForegroundColor DarkGray
        }
    }
}
