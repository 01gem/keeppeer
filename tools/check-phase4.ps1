param(
    [ValidateSet('Verify','Install','Phase4','Generator','GeneratorState','Regression','Csv','Window','ThemeWrite','ThemeRead','Stop')]
    [string]$Action = 'Verify'
)
$ErrorActionPreference = 'Stop'
$phase4Serial = 'emulator-5556'
$phase4Avd = 'Keeppeer_Guard_API31_Phase4_20261031_f4b782'
$project = Split-Path $PSScriptRoot -Parent
function Read-Adb([string[]]$Arguments) {
    $result = & adb -s $phase4Serial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'ADB command failed; no alternate device will be used.' }
    return ($result -join "`n").Trim()
}
function Assert-Phase4Target {
    $name = Read-Adb -Arguments @('emu','avd','name')
    if ($name -notmatch ('(?m)^' + [regex]::Escape($phase4Avd) + '\r?$')) { throw 'Unexpected AVD identity' }
    if ((Read-Adb -Arguments @('get-state')) -ne 'device' -or
        (Read-Adb -Arguments @('shell','getprop','ro.kernel.qemu')) -ne '1' -or
        (Read-Adb -Arguments @('shell','getprop','ro.build.version.sdk')) -ne '31' -or
        (Read-Adb -Arguments @('shell','getprop','ro.product.cpu.abi')) -ne 'x86_64' -or
        (Read-Adb -Arguments @('shell','getprop','sys.boot_completed')) -ne '1') { throw 'Unexpected emulator/API/ABI/boot state' }
    $policy = Read-Adb -Arguments @('shell','dumpsys','window','policy')
    if ($policy -notmatch 'inputRestricted=false' -or $policy -notmatch 'mIsShowing=false') {
        throw 'Fixture is not available without credential input. Stop; do not bypass/reset.'
    }
    Write-Output "GUARDED: $phase4Serial / $phase4Avd / qemu1 / API31 / x86_64 / boot1 / unrestricted"
}
Assert-Phase4Target
$app = Join-Path $project 'app/build/outputs/apk/debug/app-debug.apk'
$test = Join-Path $project 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
if ($Action -eq 'Verify') { exit 0 }
if ($Action -eq 'Stop') {
    Read-Adb -Arguments @('emu','kill')
    exit 0
}
foreach ($artifact in @($app, $test)) {
    Write-Output ((Split-Path $artifact -Leaf) + ' SHA256 ' + (Get-FileHash -LiteralPath $artifact -Algorithm SHA256).Hash.ToLowerInvariant())
}
if ($Action -eq 'Install') {
    foreach ($artifact in @($app, $test)) {
        Assert-Phase4Target
        Read-Adb -Arguments @('install','-r',$artifact)
    }
    foreach ($package in @('com.gem.keeppeer','com.gem.keeppeer.test')) {
        $installed = Read-Adb -Arguments @('shell','pm','path',$package)
        $apk = (($installed -split "`n")[0] -replace '^package:', '').Trim()
        if (-not $apk.StartsWith('/data/app/')) { throw 'Unexpected installed APK path' }
        Write-Output (Read-Adb -Arguments @('shell','sha256sum',$apk))
    }
    exit 0
}
$groups = @{
    Phase4 = @('com.gem.keeppeer.Phase4UiTest','com.gem.keeppeer.Phase4LayoutTest')
    Generator = @('com.gem.keeppeer.Phase4UiTest#generatorDraftThemeRotationCopyAndLockJourney')
    GeneratorState = @('com.gem.keeppeer.Phase4UiTest#generatorCancelUseThemeAndLockWithoutClipboard')
    Regression = @('com.gem.keeppeer.EncryptedVaultTest','com.gem.keeppeer.EntryPersistenceTest',
        'com.gem.keeppeer.EntryDraftRotationTest','com.gem.keeppeer.VaultLifecycleUiTest',
        'com.gem.keeppeer.WindowRegistrationTest','com.gem.keeppeer.CsvSafUiTest',
        'com.gem.keeppeer.VaultSecurityTest#passwordChangeRestartWrongPasswordAndStaleRepositoryRevocation',
        'com.gem.keeppeer.VaultSecurityTest#monotonicReturnEnforcesAllTimeoutOptionsAndDeviceLock')
    Csv = @('com.gem.keeppeer.CsvSafUiTest')
    Window = @('com.gem.keeppeer.WindowRegistrationTest')
    ThemeWrite = @('com.gem.keeppeer.ThemeProcessTest#persistDarkForNextProcess')
    ThemeRead = @('com.gem.keeppeer.ThemeProcessTest#newProcessReadsDarkThenRestoreSystem')
}
$expected = @{ Phase4 = 4; Generator = 1; GeneratorState = 1; Regression = 11; Csv = 2; Window = 1; ThemeWrite = 1; ThemeRead = 1 }[$Action]
$selection = $groups[$Action] -join ','
Assert-Phase4Target
Write-Output "SELECTION: $selection / EXPECTED: $expected"
$lines = [System.Collections.Generic.List[string]]::new()
& adb -s $phase4Serial shell am instrument -w -r -e class $selection com.gem.keeppeer.test/com.gem.keeppeer.SyntheticTestRunner 2>&1 |
    ForEach-Object { $line = $_.ToString(); $lines.Add($line); Write-Output $line }
$shellExit = $LASTEXITCODE
$output = $lines -join "`n"
$passed = [regex]::Matches($output, '(?m)^INSTRUMENTATION_STATUS_CODE: 0\r?$').Count
$started = [regex]::Matches($output, '(?m)^INSTRUMENTATION_STATUS_CODE: 1\r?$').Count
$bad = [regex]::Matches($output, '(?m)^INSTRUMENTATION_STATUS_CODE: -[1-4]\r?$').Count
if ($shellExit -ne 0 -or $passed -ne $expected -or $started -ne $expected -or $bad -ne 0 -or
    $output -notmatch ('OK \(' + $expected + ' tests?\)') -or
    $output -notmatch '(?m)^INSTRUMENTATION_CODE: -1\r?$') {
    throw "Native check incomplete/failed: expected=$expected started=$started passed=$passed failureOrSkipStatuses=$bad shell=$shellExit"
}
Write-Output "VERIFIED NATIVE COMPLETION: $expected/$expected passed; no failure/skip statuses."
