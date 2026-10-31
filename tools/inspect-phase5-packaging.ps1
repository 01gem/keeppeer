[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$env:JAVA_HOME = 'C:/Users/gemdu/.gradle/jdks/eclipse_adoptium-25-amd64-windows.2'
$analyzer = 'C:/Users/gemdu/AppData/Local/Android/Sdk/cmdline-tools/23.0/bin/apkanalyzer.bat'
foreach ($variant in @('release','validation')) {
    $name = if ($variant -eq 'release') { 'app-release-unsigned.apk' } else { 'app-validation.apk' }
    $apk = Join-Path $root "app/build/outputs/apk/$variant/$name"
    $text = (& $analyzer manifest print $apk) -join "`n"
    if ($LASTEXITCODE -ne 0) { throw 'APK manifest extraction failed.' }
    [xml]$manifest = $text
    $ns = 'http://schemas.android.com/apk/res/android'
    $app = $manifest.manifest.application
    if ($app.GetAttribute('debuggable',$ns) -eq 'true') { throw 'Candidate is debuggable.' }
    if ($app.GetAttribute('allowBackup',$ns) -ne 'false') { throw 'Backup must be disabled.' }
    $permissions = @($manifest.manifest.'uses-permission' | ForEach-Object { $_.GetAttribute('name',$ns) })
    if ($permissions -match 'INTERNET|READ_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE|MANAGE_EXTERNAL_STORAGE|READ_MEDIA') {
        throw 'Network/broad storage permission in candidate.'
    }
    if ($manifest.manifest.instrumentation) { throw 'Instrumentation in candidate.' }
    Write-Output "Manifest $variant package=$($manifest.manifest.package) debuggable=false allowBackup=false"
    Write-Output ("Permissions=" + ($permissions -join ','))
    Write-Output ("Application=" + $app.GetAttribute('name',$ns))
    Write-Output ("BackupRules=" + $app.GetAttribute('fullBackupContent',$ns) + " TransferRules=" + $app.GetAttribute('dataExtractionRules',$ns))
    foreach ($type in @('activity','service','receiver','provider')) {
        foreach ($component in @($app.$type)) {
            if ($null -eq $component) { continue }
            $componentName = $component.GetAttribute('name',$ns)
            if ($componentName -match 'Synthetic|TestRunner|Phase5|Debug') { throw 'Test component in candidate.' }
            Write-Output ("Component=$type " + $componentName + " exported=" + $component.GetAttribute('exported',$ns))
        }
    }
    $packages = (& $analyzer dex packages --defined-only $apk) -join "`n"
    if ($LASTEXITCODE -ne 0) { throw 'DEX inspection failed.' }
    if ($packages -match 'SyntheticTestApplication|SyntheticTestRunner|Phase5NormalAppTest|androidx\.test\.') {
        throw 'Test code defined in candidate DEX.'
    }
    Write-Output 'Packaged DEX has no defined test application, runner or androidx.test classes.'
    $map = Join-Path $root "app/build/outputs/mapping/$variant/mapping.txt"
    $appClasses=0; $appRenamed=0; $bcClasses=0; $roomClasses=0
    foreach ($line in [IO.File]::ReadLines($map)) {
        if ($line -match '^(com\.gem\.keeppeer\.[^ ]+) -> ([^:]+):$') {
            $appClasses++
            if ($matches[1] -ne $matches[2]) { $appRenamed++ }
        }
        if ($line -match '^org\.bouncycastle\.[^ ]+ -> ') { $bcClasses++ }
        if ($line -match '^androidx\.room\.[^ ]+ -> ') { $roomClasses++ }
    }
    if ($appRenamed -lt 1 -or $bcClasses -lt 1 -or $roomClasses -lt 1) { throw 'Expected whole-app/dependency R8 coverage absent.' }
    Write-Output "R8 summary $($variant): appClasses=$appClasses renamedAppClasses=$appRenamed BCClasses=$bcClasses RoomClasses=$roomClasses"
    Write-Output 'Private mapping content is not emitted; hashes are in the artifact inspection.'
}
