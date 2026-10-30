<#
.SYNOPSIS
    Exports a source-code.txt review snapshot of the Keeppeer project.

.DESCRIPTION
    Discovers all reviewable project-owned source and configuration files,
    reads metadata from project files, reads structured phase report,
    performs secret redactions in the review copy (leaving source untouched),
    validates the output through a temporary file on the same volume,
    and atomically replaces the target file using System.IO.File.Replace.

.PARAMETER Phase
    The phase number (e.g. 0, 1, 2, 3, 4, 5).

.PARAMETER Outcome
    Phase outcome: complete, incomplete, or blocked.

.PARAMETER ReportFile
    Optional path to the structured phase report JSON file.
    Defaults to docs/reports/phase-<Phase>-report.json or docs/reports/phase-report.json.

.PARAMETER ProjectRoot
    Optional path to project root. Defaults to parent of tools directory.

.EXAMPLE
    .\tools\export-source-code.ps1 -Phase 0 -Outcome complete
#>
param(
    [Parameter(Mandatory = $true)]
    [int]$Phase,

    [Parameter(Mandatory = $true)]
    [ValidateSet('complete', 'incomplete', 'blocked')]
    [string]$Outcome,

    [Parameter(Mandatory = $false)]
    [string]$ReportFile,

    [Parameter(Mandatory = $false)]
    [string]$ProjectRoot
)

$ErrorActionPreference = 'Stop'

# --- Resolve Project Root ---
if (-not $ProjectRoot) {
    $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
    $ProjectRoot = Split-Path -Parent $scriptDir
}
$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path

$outputFile = Join-Path $ProjectRoot 'source-code.txt'
$tempFile = Join-Path $ProjectRoot 'source-code.txt.tmp'
$backupFile = Join-Path $ProjectRoot 'source-code.txt.bak'

# --- Clean up any leftover temp/backup files ---
if (Test-Path -LiteralPath $tempFile) {
    Remove-Item -LiteralPath $tempFile -Force -ErrorAction SilentlyContinue
}
if (Test-Path -LiteralPath $backupFile) {
    Remove-Item -LiteralPath $backupFile -Force -ErrorAction SilentlyContinue
}

# --- Resolve Phase Report File ---
if (-not $ReportFile) {
    $candidate1 = Join-Path $ProjectRoot "docs/reports/phase-$Phase-report.json"
    $candidate2 = Join-Path $ProjectRoot "docs/reports/phase-report.json"
    if (Test-Path -LiteralPath $candidate1) {
        $ReportFile = $candidate1
    } elseif (Test-Path -LiteralPath $candidate2) {
        $ReportFile = $candidate2
    } else {
        throw "Structured phase report file not found. Expected: docs/reports/phase-$Phase-report.json or docs/reports/phase-report.json"
    }
} else {
    if (-not (Test-Path -LiteralPath $ReportFile)) {
        $resolvedReport = Join-Path $ProjectRoot $ReportFile
        if (Test-Path -LiteralPath $resolvedReport) {
            $ReportFile = $resolvedReport
        } else {
            throw "Specified report file not found: $ReportFile"
        }
    }
}

# --- Parse Phase Report ---
try {
    $reportRaw = Get-Content -LiteralPath $ReportFile -Raw -Encoding UTF8
    $report = $reportRaw | ConvertFrom-Json
} catch {
    throw "Failed to parse phase report JSON at '$ReportFile': $($_.Exception.Message)"
}

if ($report.phase -ne $Phase) {
    throw "Phase mismatch: parameter -Phase is $Phase, but report '$ReportFile' declares phase $($report.phase)"
}

if ($report.outcome -ne $Outcome) {
    throw "Outcome mismatch: parameter -Outcome is '$Outcome', but report '$ReportFile' declares outcome '$($report.outcome)'"
}

# --- Read Project Metadata ---
$declaredAppId = 'unknown'
$declaredMinSdk = 'unknown'
$declaredTargetSdk = 'unknown'
$declaredCompileSdk = 'unknown'
$appBuildFile = Join-Path $ProjectRoot 'app/build.gradle.kts'
if (Test-Path -LiteralPath $appBuildFile) {
    $appBuildContent = Get-Content -LiteralPath $appBuildFile -Raw -Encoding UTF8
    if ($appBuildContent -match 'applicationId\s*=\s*"([^"]+)"') { $declaredAppId = $matches[1] }
    elseif ($appBuildContent -match 'namespace\s*=\s*"([^"]+)"') { $declaredAppId = $matches[1] }
    if ($appBuildContent -match 'minSdk\s*=\s*(\d+)') { $declaredMinSdk = $matches[1] }
    if ($appBuildContent -match 'targetSdk\s*=\s*(\d+)') { $declaredTargetSdk = $matches[1] }
    if ($appBuildContent -match 'compileSdk\s*\{\s*version\s*=\s*release\((\d+)\)') { $declaredCompileSdk = $matches[1] }
    elseif ($appBuildContent -match 'compileSdk\s*=\s*(\d+)') { $declaredCompileSdk = $matches[1] }
}

$gradleWrapperVersion = 'unknown'
$wrapperPropsFile = Join-Path $ProjectRoot 'gradle/wrapper/gradle-wrapper.properties'
if (Test-Path -LiteralPath $wrapperPropsFile) {
    $wrapperContent = Get-Content -LiteralPath $wrapperPropsFile -Raw -Encoding UTF8
    if ($wrapperContent -match 'gradle-([0-9\.]+)-(bin|all)\.zip') {
        $gradleWrapperVersion = $matches[1]
    }
}

$daemonJdkVersion = 'unknown'
$daemonJvmFile = Join-Path $ProjectRoot 'gradle/gradle-daemon-jvm.properties'
if (Test-Path -LiteralPath $daemonJvmFile) {
    $daemonContent = Get-Content -LiteralPath $daemonJvmFile -Raw -Encoding UTF8
    if ($daemonContent -match 'toolchainVersion\s*=\s*([0-9]+)') {
        $daemonJdkVersion = $matches[1]
    }
}

$catalogVersions = [System.Collections.Generic.Dictionary[string, string]]::new()
$catalogFile = Join-Path $ProjectRoot 'gradle/libs.versions.toml'
if (Test-Path -LiteralPath $catalogFile) {
    $catalogContent = Get-Content -LiteralPath $catalogFile -Raw -Encoding UTF8
    $inVersions = $false
    foreach ($line in ($catalogContent -split "`r?`n")) {
        $trimmed = $line.Trim()
        if ($trimmed -eq '[versions]') { $inVersions = $true; continue }
        if ($trimmed.StartsWith('[') -and $trimmed -ne '[versions]') { $inVersions = $false; continue }
        if ($inVersions -and $trimmed -match '^([a-zA-Z0-9_\-]+)\s*=\s*"([^"]+)"') {
            $catalogVersions[$matches[1]] = $matches[2]
        }
    }
}

# --- Timestamp in Philippine Time (UTC+08:00) ---
$reviewTimestamp = [DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(8))
$timestampStr = $reviewTimestamp.ToString('yyyy-MM-dd hh:mm:ss tt', [Globalization.CultureInfo]::InvariantCulture) + ' PHT (UTC+08:00)'

# --- Git probe using -C ---
$gitRevision = '(no commits yet)'
$gitStatus = '(uncommitted workspace)'
$prevEAP = $ErrorActionPreference
try {
    $ErrorActionPreference = 'Continue'
    $revOutput = git -C $ProjectRoot rev-parse --short HEAD 2>&1
    if ($LASTEXITCODE -eq 0 -and $revOutput) {
        $gitRevision = "$revOutput".Trim()
    }
    $statusOutput = git -C $ProjectRoot status -s 2>&1
    if ($LASTEXITCODE -eq 0) {
        if (-not $statusOutput -or $statusOutput.Count -eq 0) {
            $gitStatus = 'clean'
        } else {
            $gitStatus = "$($statusOutput.Count) path(s) modified/untracked"
        }
    }
} catch {
    # Git probe failed
} finally {
    $ErrorActionPreference = $prevEAP
}

# --- Reviewable Source Discovery ---
# Prunes excluded directories before traversal
$excludedDirNames = @('.git', '.gradle', '.idea', 'build', 'captures', '.cxx', '.externalNativeBuild',
    'keeppeer-preservation', 'incident-payloads', 'private-captures', 'no_backup', 'no-backup')

function Test-IsReviewableFile($fileItem, $relPath) {
    $name = $fileItem.Name
    $ext = [System.IO.Path]::GetExtension($name).ToLowerInvariant()

    # Incident payloads/manifests remain private even if given a reviewable extension.
    if ($name -like 'incident-payload*' -or $name -like 'private-capture*' -or $name -like 'preservation-manifest*') { return $false }
    if ($name -like '*.db*' -or $name -like '*.sqlite*' -or $name -like 'vault-envelope.bin*' -or $name -like 'biometric-envelope.bin*') { return $false }

    # Explicit exclusions
    if ($name -like 'source-code.txt*' -or $name -like '*.tmp' -or $name -like '*.bak') { return $false }
    if ($name -like '.env*' -or $name -like '*.env') { return $false }
    if ($name -in @('local.properties', 'secrets.properties', 'signing.properties', 'keystore.properties')) { return $false }
    if ($name -like '*secret*.properties' -or $name -like '*signing*.properties' -or $name -like '*credential*.properties') { return $false }
    if ($ext -in @('.jks', '.keystore', '.p12', '.key', '.pem', '.pkcs12')) { return $false }
    if ($ext -in @('.db', '.db-shm', '.db-wal', '.sqlite', '.csv')) { return $false }
    if ($ext -in @('.apk', '.aab', '.jar', '.aar', '.zip', '.tar', '.gz', '.bin', '.webp', '.png', '.jpg', '.jpeg', '.gif', '.ico', '.class')) { return $false }

    # Explicit inclusions
    if ($name -in @('gradlew', 'gradlew.bat', '.gitignore')) { return $true }
    if ($ext -in @('.kt', '.java', '.xml', '.kts', '.toml', '.keep', '.pro', '.md', '.ps1', '.bat', '.sh')) { return $true }

    # Properties files: only safe build properties
    if ($ext -eq '.properties') {
        if ($name -in @('gradle.properties', 'gradle-wrapper.properties', 'gradle-daemon-jvm.properties')) {
            return $true
        }
        return $false
    }

    # JSON files: only Room schemas and docs reports
    if ($ext -eq '.json') {
        if ($relPath -like '*schemas*/*.json' -or $relPath -like 'docs/reports/*.json') {
            return $true
        }
        return $false
    }

    return $false
}

$script:discoveredFiles = [System.Collections.Generic.List[PSCustomObject]]::new()

function Discover-Files($currentDir) {
    $entries = Get-ChildItem -LiteralPath $currentDir -Force -ErrorAction Stop
    foreach ($entry in $entries) {
        # Never follow a link/junction into private captures or storage outside this tree.
        if (($entry.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) { continue }
        if ($entry.PSIsContainer) {
            if ($entry.Name -in $excludedDirNames) {
                continue
            }
            Discover-Files $entry.FullName
        } else {
            $relPath = $entry.FullName.Substring($ProjectRoot.Length + 1).Replace('\', '/')
            if (Test-IsReviewableFile $entry $relPath) {
                $script:discoveredFiles.Add([PSCustomObject]@{
                    FullName     = $entry.FullName
                    RelativePath = $relPath
                })
            }
        }
    }
}

Discover-Files $ProjectRoot
$includedFiles = $script:discoveredFiles | Sort-Object -Property RelativePath

if ($includedFiles.Count -eq 0) {
    throw "Discovery error: No reviewable files found in project root '$ProjectRoot'."
}

# --- Secret Redaction Engine ---
$redactions = [System.Collections.Generic.List[string]]::new()

function Process-FileContentForReview($filePath, $relPath) {
    # Read with explicit error propagation (do not swallow errors)
    $lines = [System.IO.File]::ReadAllLines($filePath, [System.Text.Encoding]::UTF8)
    $processedLines = [System.Collections.Generic.List[string]]::new($lines.Length)
    $fileExtension = [System.IO.Path]::GetExtension($relPath).ToLowerInvariant()

    for ($i = 0; $i -lt $lines.Length; $i++) {
        $line = $lines[$i]
        $lineNum = $i + 1
        $redactedLine = $line

        # Heuristic 1A: Kotlin property assignment with double quotes (typed or untyped)
        # Matches: [modifiers] val/var <name> [: <Type>] = "<literal>"
        if ($fileExtension -in @('.kt', '.kts') -and $line -match '(?i)^\s*(?:(?:private|protected|internal|public)\s+)?(?:(?:const|override|lateinit|final)\s+)?(?:val|var)\s+([a-zA-Z0-9_]+)(?:\s*:\s*[^=]+)?\s*=\s*"([^"]{6,})"') {
            $varName = $matches[1]
            $val = $matches[2]
            if ($varName -match '(?i)(secret|token|api_?key|private_?key|master_?password|vault_?password|store_?pass|key_?pass)') {
                if ($val -notmatch '(?i)(placeholder|example|android|keeppeer|sample|stringResource|\$|\{|route|screen)' -and
                    $varName -notmatch '(?i)(route|screen|path|tag|name|label|id|action)') {
                    $targetLiteral = """$val"""
                    $replacementLiteral = """[REDACTED: potential secret at $relPath`:$lineNum]"""
                    $redactedLine = $line.Replace($targetLiteral, $replacementLiteral)
                    $redactions.Add("$relPath`:$lineNum ($varName)")
                }
            }
        }
        # Heuristic 1B: Kotlin property assignment with single quotes (typed or untyped)
        elseif ($fileExtension -in @('.kt', '.kts') -and $line -match "(?i)^\s*(?:(?:private|protected|internal|public)\s+)?(?:(?:const|override|lateinit|final)\s+)?(?:val|var)\s+([a-zA-Z0-9_]+)(?:\s*:\s*[^=]+)?\s*=\s*'([^']{6,})'") {
            $varName = $matches[1]
            $val = $matches[2]
            if ($varName -match '(?i)(secret|token|api_?key|private_?key|master_?password|vault_?password|store_?pass|key_?pass)') {
                if ($val -notmatch '(?i)(placeholder|example|android|keeppeer|sample|stringResource|\$|\{|route|screen)' -and
                    $varName -notmatch '(?i)(route|screen|path|tag|name|label|id|action)') {
                    $targetLiteral = "'$val'"
                    $replacementLiteral = "'[REDACTED: potential secret at $relPath`:$lineNum]'"
                    $redactedLine = $line.Replace($targetLiteral, $replacementLiteral)
                    $redactions.Add("$relPath`:$lineNum ($varName)")
                }
            }
        }
        # Heuristic 2: Config key-value pairs only; never redact Kotlin named arguments.
        elseif ($fileExtension -in @('.properties', '.toml') -and $line -match '(?i)^\s*([a-zA-Z0-9_.\-]*(?:password|secret|apikey|auth_token|private_key))\s*=\s*(.+)$') {
            $key = $matches[1]
            $val = $matches[2].Trim()
            if ($val -and $val -notmatch '(?i)(placeholder|example|\$)') {
                $redactedLine = "$key = [REDACTED: potential secret at $relPath`:$lineNum]"
                $redactions.Add("$relPath`:$lineNum ($key)")
            }
        }

        $processedLines.Add($redactedLine)
    }

    return ($processedLines -join "`n")
}

# Pre-process all files first to accumulate redactions
$fileDataList = [System.Collections.Generic.List[PSCustomObject]]::new($includedFiles.Count)
foreach ($f in $includedFiles) {
    $processed = Process-FileContentForReview $f.FullName $f.RelativePath
    $fileDataList.Add([PSCustomObject]@{
        RelativePath = $f.RelativePath
        Content      = $processed
    })
}

# --- Build Output Document ---
$sb = [System.Text.StringBuilder]::new()

# Header
[void]$sb.AppendLine('================================================================================')
[void]$sb.AppendLine('PROJECT: Keeppeer — Offline Password Keeper')
[void]$sb.AppendLine("PHASE: $Phase")
[void]$sb.AppendLine("PHASE OUTCOME: $Outcome")
[void]$sb.AppendLine("APPLICATION ID: $declaredAppId")
[void]$sb.AppendLine("Generated at (Philippine time): $timestampStr")
[void]$sb.AppendLine('')
[void]$sb.AppendLine("Git revision: $gitRevision")
[void]$sb.AppendLine("Git status: $gitStatus")
[void]$sb.AppendLine('')
[void]$sb.AppendLine('Declared SDK / Toolchain Versions (from build configuration):')
[void]$sb.AppendLine("  compileSdk: $declaredCompileSdk")
[void]$sb.AppendLine("  targetSdk: $declaredTargetSdk")
[void]$sb.AppendLine("  minSdk: $declaredMinSdk")
[void]$sb.AppendLine("  Gradle Wrapper: $gradleWrapperVersion")
[void]$sb.AppendLine("  Daemon JDK: $daemonJdkVersion")
[void]$sb.AppendLine('Declared Dependency Versions (from gradle/libs.versions.toml):')
foreach ($kvp in $catalogVersions.GetEnumerator()) {
    [void]$sb.AppendLine("  $($kvp.Key): $($kvp.Value)")
}
[void]$sb.AppendLine('')
[void]$sb.AppendLine('Runtime / Toolchain Note:')
[void]$sb.AppendLine('  Declared versions above are parsed directly from repository configuration.')
[void]$sb.AppendLine('  Actual resolution occurs at Gradle sync/build time against configured repositories.')
[void]$sb.AppendLine('================================================================================')
[void]$sb.AppendLine('')

# Phase Report Section (from structured report)
[void]$sb.AppendLine('===== PHASE REPORT =====')
[void]$sb.AppendLine("Report source: $($ReportFile.Substring($ProjectRoot.Length + 1).Replace('\', '/'))")
[void]$sb.AppendLine("Implementation Summary:")
[void]$sb.AppendLine("  $($report.summary)")
[void]$sb.AppendLine('')
[void]$sb.AppendLine('Checks Executed & Results:')
foreach ($chk in $report.checks) {
    [void]$sb.AppendLine("  - $($chk.name): $($chk.result)")
    [void]$sb.AppendLine("    Details: $($chk.details)")
}
[void]$sb.AppendLine('')
[void]$sb.AppendLine('Added Paths:')
foreach ($p in $report.addedPaths) {
    [void]$sb.AppendLine("  + $p")
}
[void]$sb.AppendLine('')
[void]$sb.AppendLine('Modified Paths:')
foreach ($p in $report.modifiedPaths) {
    [void]$sb.AppendLine("  * $p")
}
[void]$sb.AppendLine('')
if ($report.deletedPaths -and $report.deletedPaths.Count -gt 0) {
    [void]$sb.AppendLine('Deleted Paths:')
    foreach ($p in $report.deletedPaths) {
        [void]$sb.AppendLine("  - $p")
    }
    [void]$sb.AppendLine('')
}
[void]$sb.AppendLine('Unresolved Issues:')
if ($report.unresolvedIssues -and $report.unresolvedIssues.Count -gt 0) {
    foreach ($issue in $report.unresolvedIssues) {
        [void]$sb.AppendLine("  ! $issue")
    }
} else {
    [void]$sb.AppendLine('  (none)')
}
[void]$sb.AppendLine('')
[void]$sb.AppendLine("Device / Hardware Checks:")
[void]$sb.AppendLine("  $($report.deviceChecks)")
[void]$sb.AppendLine('================================================================================')
[void]$sb.AppendLine('')

# Secret Redaction Report
[void]$sb.AppendLine('===== SECRET REDACTION & PRIVACY NOTICE =====')
if ($redactions.Count -gt 0) {
    [void]$sb.AppendLine("Redactions applied to review snapshot ($($redactions.Count) detected):")
    foreach ($r in $redactions) {
        [void]$sb.AppendLine("  - Redacted: $r")
    }
} else {
    [void]$sb.AppendLine('No embedded secrets detected by heuristic scanner.')
}
[void]$sb.AppendLine('Notice: Redaction occurs solely within this review copy; underlying repository files')
[void]$sb.AppendLine('are not modified. Heuristic secret detection does not replace disciplined exclusion of')
[void]$sb.AppendLine('credentials, signing keys, and runtime vault data.')
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
[void]$sb.AppendLine('Excluded directories (pruned before traversal):')
[void]$sb.AppendLine('  .git (VCS internals), .gradle (Gradle daemon/cache), .idea (IDE workspace/settings),')
[void]$sb.AppendLine('  build (compiled outputs, intermediate artifacts), captures (profiler outputs),')
[void]$sb.AppendLine('  .cxx/.externalNativeBuild (native build intermediates)')
[void]$sb.AppendLine('  keeppeer-preservation, incident-payloads, private-captures, no_backup/no-backup (private incident/runtime material)')
[void]$sb.AppendLine('  Reparse points (links/junctions are never followed)')
[void]$sb.AppendLine('  incident-payload*, private-capture*, preservation-manifest*, *.db*, *.sqlite*, *-envelope.bin* (private payload/manifest families)')
[void]$sb.AppendLine('Excluded files & patterns:')
[void]$sb.AppendLine('  source-code.txt, source-code.txt.tmp, source-code.txt.bak, *.tmp, *.bak (snapshot artifacts)')
[void]$sb.AppendLine('  local.properties, secrets.properties, signing.properties, keystore.properties (secrets)')
[void]$sb.AppendLine('  *.jks, *.keystore, *.p12, *.key, *.pem, *.pkcs12 (signing keys/certificates)')
[void]$sb.AppendLine('  *.env*, .env (environment files)')
[void]$sb.AppendLine('  *.db, *.db-shm, *.db-wal, *.sqlite (runtime vault databases)')
[void]$sb.AppendLine('  *.csv (plaintext credential exports)')
[void]$sb.AppendLine('  *.apk, *.aab, *.jar, *.aar, *.zip, *.tar, *.gz, *.bin, *.webp, *.png, *.jpg, *.jpeg, *.gif, *.ico, *.class (binaries/assets)')
[void]$sb.AppendLine('Reason: Preserves review focus on source and configuration, while strictly protecting credentials,')
[void]$sb.AppendLine('signing materials, and vault data from exposure.')
[void]$sb.AppendLine('')

# File Contents
foreach ($entry in $fileDataList) {
    [void]$sb.AppendLine("===== BEGIN FILE: $($entry.RelativePath) =====")
    [void]$sb.AppendLine($entry.Content)
    [void]$sb.AppendLine("===== END FILE: $($entry.RelativePath) =====")
    [void]$sb.AppendLine('')
}

$output = $sb.ToString()

# --- Validation Engine ---
$validationErrors = [System.Collections.Generic.List[string]]::new()

if ($output.Length -lt 1000) {
    $validationErrors.Add("Generated output is suspiciously short ($($output.Length) characters).")
}

if (-not $output.Contains("Generated at (Philippine time): $timestampStr")) {
    $validationErrors.Add('Timestamp validation failed: Philippine timestamp string missing or corrupted.')
}

if (-not $output.Contains("PHASE: $Phase")) {
    $validationErrors.Add("Phase header validation failed: expected 'PHASE: $Phase'.")
}

if (-not $output.Contains("PHASE OUTCOME: $Outcome")) {
    $validationErrors.Add("Outcome header validation failed: expected 'PHASE OUTCOME: $Outcome'.")
}

# Verify matched file boundaries for EVERY discovered file
foreach ($f in $includedFiles) {
    $expectedBegin = "===== BEGIN FILE: $($f.RelativePath) ====="
    $expectedEnd = "===== END FILE: $($f.RelativePath) ====="
    if (-not $output.Contains($expectedBegin)) {
        $validationErrors.Add("Boundary check failed: missing begin marker for '$($f.RelativePath)'.")
    }
    if (-not $output.Contains($expectedEnd)) {
        $validationErrors.Add("Boundary check failed: missing end marker for '$($f.RelativePath)'.")
    }
}

if ($validationErrors.Count -gt 0) {
    throw "Snapshot validation failed with $($validationErrors.Count) error(s):`n" + ($validationErrors -join "`n")
}

# --- Write to Temp File on Same Volume ---
try {
    [System.IO.File]::WriteAllText($tempFile, $output, [System.Text.UTF8Encoding]::new($false))
} catch {
    throw "Failed to write temporary snapshot file '$tempFile': $($_.Exception.Message)"
}

# --- Validate Temp File on Disk ---
try {
    $tempInfo = Get-Item -LiteralPath $tempFile
    if ($tempInfo.Length -lt 1000) {
        throw "Written temp file is suspiciously small ($($tempInfo.Length) bytes)."
    }
    # Test UTF-8 read back
    $readBack = [System.IO.File]::ReadAllText($tempFile, [System.Text.UTF8Encoding]::new($false))
    if ($readBack.Length -ne $output.Length) {
        throw "Read-back length mismatch: expected $($output.Length) chars, read $($readBack.Length) chars."
    }
} catch {
    Remove-Item -LiteralPath $tempFile -Force -ErrorAction SilentlyContinue
    throw "Temp file validation failed: $($_.Exception.Message). Target snapshot not modified."
}

# --- Atomic Target Replacement / Safe First Write ---
# Uses System.IO.File.Replace for existing target (atomic same-volume replacement without pre-deletion)
# Uses System.IO.File.Move for first-write path when target does not exist yet.
try {
    if (Test-Path -LiteralPath $outputFile) {
        [System.IO.File]::Replace($tempFile, $outputFile, $backupFile, $true)
        # Clean up temporary backup file after successful atomic replacement
        if (Test-Path -LiteralPath $backupFile) {
            Remove-Item -LiteralPath $backupFile -Force -ErrorAction SilentlyContinue
        }
    } else {
        [System.IO.File]::Move($tempFile, $outputFile)
    }
} catch {
    $replaceErr = $_.Exception.Message
    $targetExists = Test-Path -LiteralPath $outputFile
    if (Test-Path -LiteralPath $tempFile) {
        Remove-Item -LiteralPath $tempFile -Force -ErrorAction SilentlyContinue
    }
    if (Test-Path -LiteralPath $backupFile) {
        Remove-Item -LiteralPath $backupFile -Force -ErrorAction SilentlyContinue
    }
    if ($targetExists) {
        throw "Failed to replace target file '$outputFile': $replaceErr. Previous snapshot was retained on disk."
    } else {
        throw "Failed to create target file '$outputFile': $replaceErr. Target snapshot does not exist."
    }
}

# --- Report Success ---
$finalItem = Get-Item -LiteralPath $outputFile
Write-Host ''
Write-Host '============================= EXPORT COMPLETE =============================' -ForegroundColor Green
Write-Host "Snapshot Path: $($finalItem.FullName)"
Write-Host "Size:          $($finalItem.Length) bytes"
Write-Host "Files:         $($includedFiles.Count) reviewable files included"
Write-Host "Redactions:    $($redactions.Count) potential secret(s) redacted"
Write-Host "Timestamp:     $timestampStr"
Write-Host "Phase:         $Phase ($Outcome)"
Write-Host '==========================================================================='
Write-Host 'Upload this snapshot file to the originating review chat for analysis.'
