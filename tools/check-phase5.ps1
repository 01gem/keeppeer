[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$Serial,
    [Parameter(Mandatory=$true)][string]$Avd,
    [Parameter(Mandatory=$true)][int]$Api,
    [Parameter(Mandatory=$true)]
    [ValidateSet('Inspect','InstallValidation','InstallDebug','Test','Stop')][string]$Mode,
    [string]$Selection,
    [int]$ExpectedTests = 0,
    [switch]$OrdinaryApplication
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$adb = 'C:/Users/gemdu/AppData/Local/Android/Sdk/platform-tools/adb.exe'
if ($Avd -notmatch '^Keeppeer_Guard_Phase5_(24|25|31)_20261031_[a-z0-9]+$') {
    throw 'Only explicitly recorded new Phase5 fixtures are eligible.'
}
if ($Serial -notmatch '^emulator-[0-9]+$') { throw 'Explicit emulator serial required.' }
function Adb([string[]]$Arguments) {
    $result = & $adb -s $Serial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw "ADB operation failed: $($Arguments[0])" }
    return ($result -join "`n").Trim()
}
function Verify-Target {
    $name = Adb @('emu','avd','name')
    if (($name -split "\r?\n")[0] -ne $Avd) { throw 'AVD identity mismatch.' }
    if ((Adb @('shell','getprop','ro.kernel.qemu')) -ne '1') { throw 'Not qemu.' }
    if ((Adb @('shell','getprop','ro.build.version.sdk')) -ne "$Api") { throw 'API mismatch.' }
    if ((Adb @('shell','getprop','ro.product.cpu.abi')) -ne 'x86_64') { throw 'ABI mismatch.' }
    if ((Adb @('shell','getprop','sys.boot_completed')) -ne '1') { throw 'Boot incomplete.' }
}
Verify-Target
Write-Output "Target: $Avd / $Serial / API$Api / x86_64 / qemu1"
try { Write-Output ("PageSize=" + (Adb @('shell','getconf','PAGE_SIZE'))) }
catch { Write-Output "PageSize shell query unavailable; use instrumentation sysconf evidence." }
Write-Output ("BootCount=" + (Adb @('shell','settings','get','global','boot_count')))
Write-Output ("UTC=" + [DateTimeOffset]::UtcNow.ToString('o'))
if ($Mode -eq 'Stop') {
    Verify-Target
    Write-Output (Adb @('emu','kill'))
    exit 0
}
if ($Mode -like 'Install*') {
    $variant = if ($Mode -eq 'InstallValidation') { 'validation' } else { 'debug' }
    $apk = Join-Path $root "app/build/outputs/apk/$variant/app-$variant.apk"
    $test = Join-Path $root 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
    foreach ($item in @($apk, $test)) {
        Verify-Target
        Write-Output ("HostSHA256=" + (Get-FileHash -LiteralPath $item -Algorithm SHA256).Hash.ToLowerInvariant())
        Write-Output (Adb @('install','-r',$item))
    }
    # Older emulators can expose a package path unavailable to the shell mount namespace.
    # The ordinary runner hashes both installed APKs in their owning app namespace
    # and compares them with host hashes before any UI journey. Do not repeat pulls.
    Write-Output "Installed-byte attestation is required from the ordinary runner."

    Write-Output (Adb @('shell','pm','list','instrumentation'))
    exit 0
}
if ($Mode -eq 'Test') {
    if (-not $Selection -or $ExpectedTests -lt 1) { throw 'Explicit selection/count required.' }
    Verify-Target
    $runner = if ($OrdinaryApplication) { 'com.gem.keeppeer.Phase5StandaloneRunner' } else { 'com.gem.keeppeer.SyntheticTestRunner' }
    $args = @('shell','am','instrument','-w','-r','-e','class',$Selection)
    if ($OrdinaryApplication) {
        $appHash = (Get-FileHash -LiteralPath (Join-Path $root 'app/build/outputs/apk/validation/app-validation.apk') -Algorithm SHA256).Hash.ToLowerInvariant()
        $testHash = (Get-FileHash -LiteralPath (Join-Path $root 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk') -Algorithm SHA256).Hash.ToLowerInvariant()
        $args += @('-e','phase5Avd',$Avd,'-e','phase5ExpectedAppSha256',$appHash,'-e','phase5ExpectedTestSha256',$testHash,'-e','phase5Journey',($Selection -split '#')[-1])
    }
    $args += "com.gem.keeppeer.test/$runner"
    $watch = [Diagnostics.Stopwatch]::StartNew()
    $result = Adb $args
    $watch.Stop()
    Write-Output $result
    Write-Output ("HostElapsedSeconds=" + $watch.Elapsed.TotalSeconds)
    $completed = if ($OrdinaryApplication) {
        $ExpectedTests -eq 1 -and $result -match 'Phase5Harness: PASS expected=1 started=1 completed=1 passed=1 failed=0 errors=0 skipped=0'
    } else { $result -match ('OK \(' + $ExpectedTests + ' tests?\)') }
    if (-not $completed -or $result -notmatch 'INSTRUMENTATION_CODE: -1' -or
        $result -match 'FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=Process crashed') {
        throw 'Native completion/count did not establish the selected pass.'
    }
}
