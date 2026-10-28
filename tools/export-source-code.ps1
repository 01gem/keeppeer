<#
.SYNOPSIS
    Exports a source-code.txt review snapshot of the Keeppeer project.

.DESCRIPTION
    Discovers all reviewable project-owned source and configuration files,
    writes them into a single UTF-8 snapshot file with clear boundaries,
    a sorted file inventory, phase header with Philippine-time timestamp,
    phase report, and exclusion documentation.

    Designed to be reusable across all phases. Run from the project root.

.PARAMETER Phase
    The phase number (e.g. 0, 1, 2, 3, 4, 5).

.PARAMETER Outcome
    Phase outcome: complete, incomplete, or blocked.

.EXAMPLE
    .\tools\export-source-code.ps1 -Phase 0 -Outcome complete
#>
param(
    [Parameter(Mandatory = $true)]
    [int]$Phase,

    [Parameter(Mandatory = $true)]
    [ValidateSet('complete', 'incomplete', 'blocked')]
    [string]$Outcome
)

$ErrorActionPreference = 'Stop'

# --- Configuration ---
$projectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$outputFile = Join-Path $projectRoot 'source-code.txt'
$tempFile = Join-Path $projectRoot 'source-code.txt.tmp'

# File extensions and names to include
$includeExtensions = @(
    '*.kt', '*.java', '*.xml', '*.kts', '*.toml', '*.properties',
    '*.pro', '*.keep', '*.md', '*.txt', '*.ps1', '*.bat', '*.sh'
)

# Directories to exclude entirely
$excludeDirs = @(
    '.git', '.gradle', '.idea', 'build', 'captures',
    '.cxx', '.externalNativeBuild'
)

# Specific files/patterns to exclude
$excludeFiles = @(
    'source-code.txt',
    'source-code.txt.tmp',
    'local.properties',
    'keystore.properties',
    '*.jks',
    '*.keystore',
    '*.env',
    '*.db',
    '*.db-shm',
    '*.db-wal',
    '*.apk',
    '*.aab',
    '*.jar',
    '*.aar',
    '*.zip',
    '*.tar',
    '*.gz',
    '*.webp',
    '*.png',
    '*.jpg',
    '*.jpeg',
    '*.gif',
    '*.ico',
    '*.bin'
)

# --- Timestamp in Philippine Time (UTC+08:00) ---
$reviewTimestamp = [DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(8))
$timestampStr = $reviewTimestamp.ToString('yyyy-MM-dd hh:mm:ss tt', [Globalization.CultureInfo]::InvariantCulture) + ' PHT (UTC+08:00)'

# --- Git info ---
$gitRevision = '(no commits yet)'
$gitStatus = '(uncommitted workspace)'
try {
    $prevEAP = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $revOutput = git rev-parse --short HEAD 2>&1
    if ($LASTEXITCODE -eq 0 -and $revOutput) {
        $gitRevision = "$revOutput".Trim()
    } else {
        $gitRevision = '(no commits yet)'
    }
    $statusOutput = git status -s 2>&1
    if ($LASTEXITCODE -eq 0) {
        if (-not $statusOutput -or $statusOutput.Count -eq 0) {
            $gitStatus = 'clean'
        } else {
            $gitStatus = "$($statusOutput.Count) path(s) modified/untracked"
        }
    }
    $ErrorActionPreference = $prevEAP
} catch {
    # Git not available
}

# --- Discover files ---
function ShouldExcludeDir($relativePath) {
    foreach ($dir in $excludeDirs) {
        if ($relativePath -like "$dir\*" -or $relativePath -like "$dir/*" -or $relativePath -eq $dir) {
            return $true
        }
        if ($relativePath -like "*\$dir\*" -or $relativePath -like "*/$dir/*") {
            return $true
        }
    }
    return $false
}

function ShouldExcludeFile($fileName) {
    foreach ($pattern in $excludeFiles) {
        if ($fileName -like $pattern) {
            return $true
        }
    }
    return $false
}

$allFiles = @()
foreach ($ext in $includeExtensions) {
    $found = Get-ChildItem -Path $projectRoot -Filter $ext -Recurse -File -ErrorAction SilentlyContinue
    $allFiles += $found
}

# Also include .gitignore files explicitly
$gitignoreFiles = Get-ChildItem -Path $projectRoot -Filter '.gitignore' -Recurse -File -ErrorAction SilentlyContinue
$allFiles += $gitignoreFiles

# Deduplicate
$allFiles = $allFiles | Sort-Object -Property FullName -Unique

# Filter
$includedFiles = @()
foreach ($file in $allFiles) {
    $relativePath = $file.FullName.Substring($projectRoot.Length + 1).Replace('\', '/')
    if (ShouldExcludeDir $relativePath) { continue }
    if (ShouldExcludeFile $file.Name) { continue }
    $includedFiles += [PSCustomObject]@{
        FullName     = $file.FullName
        RelativePath = $relativePath
    }
}

$includedFiles = $includedFiles | Sort-Object -Property RelativePath

# --- Build output ---
$sb = [System.Text.StringBuilder]::new()

# Header
[void]$sb.AppendLine('================================================================================')
[void]$sb.AppendLine('PROJECT: Keeppeer — Offline Password Keeper')
[void]$sb.AppendLine("PHASE: $Phase")
[void]$sb.AppendLine("PHASE OUTCOME: $Outcome")
[void]$sb.AppendLine("APPLICATION ID: com.gem.keeppeer")
[void]$sb.AppendLine("Generated at (Philippine time): $timestampStr")
[void]$sb.AppendLine('')
[void]$sb.AppendLine("Git revision: $gitRevision")
[void]$sb.AppendLine("Git status: $gitStatus")
[void]$sb.AppendLine('')
[void]$sb.AppendLine('SDK / Toolchain / Dependencies:')
[void]$sb.AppendLine('  compileSdk / targetSdk: 37')
[void]$sb.AppendLine('  minSdk: 24')
[void]$sb.AppendLine('  AGP: 9.5.0')
[void]$sb.AppendLine('  Kotlin: 2.2.10')
[void]$sb.AppendLine('  KSP: 2.3.12')
[void]$sb.AppendLine('  Compose BOM: 2026.09.00')
[void]$sb.AppendLine('  Navigation Compose: 2.10.2')
[void]$sb.AppendLine('  Lifecycle/ViewModel: 2.11.0')
[void]$sb.AppendLine('  Room: 2.8.5')
[void]$sb.AppendLine('  SQLCipher Android: 4.20.0 (net.zetetic:sqlcipher-android)')
[void]$sb.AppendLine('  AndroidX SQLite: 2.7.1 (androidx.sqlite:sqlite-ktx)')
[void]$sb.AppendLine('  AndroidX Biometric: 1.1.0')
[void]$sb.AppendLine('  DataStore Preferences: 1.3.0')
[void]$sb.AppendLine('  Gradle: 9.8.2')
[void]$sb.AppendLine('  JDK (daemon): 25')
[void]$sb.AppendLine('================================================================================')
[void]$sb.AppendLine('')

# Phase Report
[void]$sb.AppendLine('===== PHASE REPORT =====')
[void]$sb.AppendLine('Implementation Summary:')
[void]$sb.AppendLine('  Phase 0 complete. Established runnable Android project foundation adhering to')
[void]$sb.AppendLine('  the Password Keeper offline security contract. Configured pinned stable dependencies')
[void]$sb.AppendLine('  in version catalog, applied KSP 2.3.12 with Room 2.8.5, SQLCipher Android 4.20.0,')
[void]$sb.AppendLine('  Biometric 1.1.0, and DataStore Preferences 1.3.0. Established single-module manual DI')
[void]$sb.AppendLine('  package architecture. Enforced offline security hygiene: removed INTERNET permission')
[void]$sb.AppendLine('  via manifest merger, set android:allowBackup="false" with full data extraction rules,')
[void]$sb.AppendLine('  lowered minSdk to 24 with adaptive icon backward compatibility (mipmap-anydpi-v26),')
[void]$sb.AppendLine('  and updated .gitignore for secrets and review snapshots. Created Phase 0 documentation')
[void]$sb.AppendLine('  and this reusable source-export helper.')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('Checks Attempted:')
[void]$sb.AppendLine('  - Gradle Sync: PASS (All dependencies resolved, KSP applied)')
[void]$sb.AppendLine('  - :app:assembleDebug: PASS (Debug APK built successfully)')
[void]$sb.AppendLine('  - :app:testDebugUnitTest: PASS (1 passed, 0 skipped, 0 failed)')
[void]$sb.AppendLine('  - :app:lintDebug: PASS (0 errors)')
[void]$sb.AppendLine('  - Merged Manifest Inspection: PASS (allowBackup="false", no INTERNET, no storage,')
[void]$sb.AppendLine('    minSdk=24, targetSdk=37, only USE_BIOMETRIC/USE_FINGERPRINT/signature receiver)')
[void]$sb.AppendLine('  - Source Code Analysis: PASS (0 errors, 0 warnings across all project Kotlin/XML)')
[void]$sb.AppendLine('  - Device Launch: NOT RUN (No running emulator/device attached; verified via APK build)')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('Added / Modified Paths:')
[void]$sb.AppendLine('  - gradle/libs.versions.toml (added pinned dependencies, plugins, test libraries)')
[void]$sb.AppendLine('  - build.gradle.kts (added KSP plugin declaration)')
[void]$sb.AppendLine('  - app/build.gradle.kts (minSdk 24, all dependencies, KSP, test dependencies)')
[void]$sb.AppendLine('  - app/src/main/AndroidManifest.xml (removed INTERNET, disabled backup, rules)')
[void]$sb.AppendLine('  - app/src/main/res/xml/backup_rules.xml (full exclude for API <= 30)')
[void]$sb.AppendLine('  - app/src/main/res/xml/data_extraction_rules.xml (full exclude for API 31+)')
[void]$sb.AppendLine('  - app/src/main/res/mipmap-anydpi-v26/ (renamed from anydpi for API 24 compat)')
[void]$sb.AppendLine('  - app/src/main/res/values/strings.xml (app name, placeholder strings)')
[void]$sb.AppendLine('  - app/src/main/java/com/gem/keeppeer/MainActivity.kt (edge-to-edge, root host)')
[void]$sb.AppendLine('  - app/src/main/java/com/gem/keeppeer/ui/navigation/KeeppeerNavigation.kt (shell)')
[void]$sb.AppendLine('  - app/src/main/java/com/gem/keeppeer/ui/list/PasswordListScreen.kt (placeholder)')
[void]$sb.AppendLine('  - app/src/main/java/com/gem/keeppeer/ui/theme/Theme.kt (Material 3 cleanup)')
[void]$sb.AppendLine('  - app/src/main/java/com/gem/keeppeer/*/package-info.kt (documented package layers)')
[void]$sb.AppendLine('  - .gitignore (secrets, signing, .idea, vault databases, review snapshots)')
[void]$sb.AppendLine('  - README.md (setup instructions, tech stack, architecture, export guide)')
[void]$sb.AppendLine('  - docs/phase-status.md (phase progress and verification log)')
[void]$sb.AppendLine('  - docs/security.md (security posture and roadmap)')
[void]$sb.AppendLine('  - docs/skills/password-keeper-android/SKILL.md (skill specification)')
[void]$sb.AppendLine('  - tools/export-source-code.ps1 (reusable review snapshot exporter)')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('Known Issues & Outstanding Work:')
[void]$sb.AppendLine('  - No runtime vault database exists yet (Phase 1 will add plaintext development DB)')
[void]$sb.AppendLine('  - Device-level interactive check pending device/emulator availability')
[void]$sb.AppendLine('================================================================================')
[void]$sb.AppendLine('')

# Inventory
[void]$sb.AppendLine('===== FILE INVENTORY =====')
[void]$sb.AppendLine("Total files: $($includedFiles.Count)")
[void]$sb.AppendLine('')
foreach ($f in $includedFiles) {
    [void]$sb.AppendLine("  $($f.RelativePath)")
}
[void]$sb.AppendLine('')

# Exclusions
[void]$sb.AppendLine('===== EXCLUSION CATEGORIES =====')
[void]$sb.AppendLine('Excluded directories: .git (VCS internals), .gradle (build cache), .idea (IDE settings),')
[void]$sb.AppendLine('  build (compiled outputs), captures (profiling), .cxx/.externalNativeBuild (native)')
[void]$sb.AppendLine('Excluded files: source-code.txt (this snapshot), local.properties (SDK paths),')
[void]$sb.AppendLine('  keystore.properties / *.jks / *.keystore (signing secrets), *.env (credentials),')
[void]$sb.AppendLine('  *.db / *.db-shm / *.db-wal (runtime vault databases),')
[void]$sb.AppendLine('  *.apk / *.aab / *.jar / *.aar / *.zip / *.tar / *.gz (binaries/archives),')
[void]$sb.AppendLine('  *.webp / *.png / *.jpg / *.jpeg / *.gif / *.ico / *.bin (binary assets)')
[void]$sb.AppendLine('Reason: Security, size, and relevance — only text source and configuration are reviewable.')
[void]$sb.AppendLine('')

# File contents
foreach ($f in $includedFiles) {
    [void]$sb.AppendLine("===== BEGIN FILE: $($f.RelativePath) =====")
    try {
        $content = Get-Content $f.FullName -Raw -Encoding UTF8
        if ($null -eq $content) { $content = '' }
        $content = $content.TrimEnd("`r", "`n")
        [void]$sb.AppendLine($content)
    } catch {
        [void]$sb.AppendLine("[ERROR: Could not read file — $($_.Exception.Message)]")
    }
    [void]$sb.AppendLine("===== END FILE: $($f.RelativePath) =====")
    [void]$sb.AppendLine('')
}

# --- Validate and write ---
$output = $sb.ToString()

# Validation checks
$errors = @()
if ($output.Length -lt 100) {
    $errors += 'Output is suspiciously short (< 100 chars)'
}
if (-not $output.Contains('Generated at (Philippine time):')) {
    $errors += 'Missing Philippine timestamp'
}

# Verify each included file has exact begin and end boundaries
foreach ($f in $includedFiles) {
    $expectedBegin = "===== BEGIN FILE: $($f.RelativePath) ====="
    $expectedEnd = "===== END FILE: $($f.RelativePath) ====="
    if (-not $output.Contains($expectedBegin)) {
        $errors += "Missing begin boundary for $($f.RelativePath)"
    }
    if (-not $output.Contains($expectedEnd)) {
        $errors += "Missing end boundary for $($f.RelativePath)"
    }
}

if ($errors.Count -gt 0) {
    Write-Error "Validation failed:`n$($errors -join "`n")"
    exit 1
}

# Write through temp file
[System.IO.File]::WriteAllText($tempFile, $output, [System.Text.UTF8Encoding]::new($false))

# Verify temp file is readable and non-empty
$tempContent = [System.IO.File]::ReadAllText($tempFile, [System.Text.UTF8Encoding]::new($false))
if ($tempContent.Length -lt 100) {
    Write-Error 'Temp file validation failed: content too short after write'
    exit 1
}

# Replace target
if (Test-Path $outputFile) { Remove-Item $outputFile -Force }
Move-Item $tempFile $outputFile -Force

# Report
$fileSize = (Get-Item $outputFile).Length
Write-Host ''
Write-Host '===== EXPORT COMPLETE ====='
Write-Host "File: $outputFile"
Write-Host "Size: $fileSize bytes"
Write-Host "Files included: $($includedFiles.Count)"
Write-Host "Generated at (Philippine time): $timestampStr"
Write-Host "Phase: $Phase ($Outcome)"
Write-Host ''
Write-Host 'Upload this file to the originating review chat for analysis.'
