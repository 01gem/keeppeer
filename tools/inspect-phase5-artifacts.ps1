[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$tools = 'C:/Users/gemdu/AppData/Local/Android/Sdk/build-tools/36.0.0'
$env:JAVA_HOME = 'C:/Users/gemdu/.gradle/jdks/eclipse_adoptium-25-amd64-windows.2'
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach ($variant in @('release', 'validation', 'debug')) {
    $name = if ($variant -eq 'release') { 'app-release-unsigned.apk' } else { "app-$variant.apk" }
    $file = Join-Path $root "app/build/outputs/apk/$variant/$name"
    if (-not (Test-Path -LiteralPath $file)) { throw "Missing $variant artifact." }
    $item = Get-Item -LiteralPath $file
    Write-Output "Artifact=$file"
    Write-Output "Bytes=$($item.Length)"
    Write-Output ("SHA256=" + (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant())
    & "$tools/aapt2.exe" dump badging $file
    if ($LASTEXITCODE -ne 0) { throw 'aapt2 badging failed.' }
    & "$tools/aapt2.exe" dump permissions $file
    & "$tools/zipalign.exe" -c -P 16 4 $file
    if ($LASTEXITCODE -ne 0) { throw '16KiB ZIP alignment failed.' }
    Write-Output 'ZIP alignment check passed (-c -P 16 4).'
    & "$tools/apksigner.bat" verify --print-certs $file
    if ($variant -eq 'release') {
        if ($LASTEXITCODE -eq 0) { throw 'Expected unsigned production candidate; signing was not authorized.' }
        Write-Output 'Expected unsigned verification failure (no production identity used).'
    } elseif ($LASTEXITCODE -ne 0) { throw 'Development-signed artifact verification failed.' }
    $zip = [IO.Compression.ZipFile]::OpenRead($file)
    try {
        $libraries = @($zip.Entries | Where-Object { $_.FullName -match '^lib/[^/]+/[^/]+\.so$' })
        Write-Output "NativeLibraryCount=$($libraries.Count)"
        foreach ($entry in $libraries) {
            $stream = $entry.Open()
            $memory = [IO.MemoryStream]::new()
            try { $stream.CopyTo($memory); $bytes = $memory.ToArray() } finally { $stream.Dispose(); $memory.Dispose() }
            # Supplemental ELF program-header inspection in memory. This does not
            # replace runtime testing; SDK zipalign is the supported ZIP check.
            if ($bytes[0] -ne 127 -or $bytes[1] -ne 69 -or $bytes[2] -ne 76 -or $bytes[3] -ne 70 -or $bytes[5] -ne 1) {
                throw 'Unexpected ELF format.'
            }
            $is64 = $bytes[4] -eq 2
            $offset = if ($is64) { [BitConverter]::ToUInt64($bytes,32) } else { [BitConverter]::ToUInt32($bytes,28) }
            $size = [BitConverter]::ToUInt16($bytes, $(if ($is64) {54} else {42}))
            $count = [BitConverter]::ToUInt16($bytes, $(if ($is64) {56} else {44}))
            $alignments = @()
            for ($i = 0; $i -lt $count; $i++) {
                $at = [int]($offset + $i * $size)
                if ([BitConverter]::ToUInt32($bytes,$at) -eq 1) {
                    $alignment = if ($is64) { [BitConverter]::ToUInt64($bytes,$at+48) } else { [BitConverter]::ToUInt32($bytes,$at+28) }
                    $alignments += $alignment
                    if ($is64 -and $alignment -lt 16384) { throw "64-bit ELF load alignment below 16KiB: $($entry.FullName)" }
                }
            }
            Write-Output ("ELF " + $entry.FullName + " PT_LOAD align=" + ($alignments -join ','))
        }
        $forbidden = @($zip.Entries | Where-Object { $_.FullName -match '(SyntheticTest|Phase5NormalAppTest|androidTest|keeppeer-validation\.properties|\.jks$)' })
        if ($variant -ne 'debug' -and $forbidden.Count -ne 0) { throw 'Validation/test payload name found in candidate.' }
        Write-Output ("DEX count=" + @($zip.Entries | Where-Object { $_.FullName -match '^classes[0-9]*\.dex$' }).Count)
    } finally { $zip.Dispose() }
}
foreach ($variant in @('release','validation')) {
    $folder = Join-Path $root "app/build/outputs/mapping/$variant"
    foreach ($name in @('mapping.txt','configuration.txt','usage.txt')) {
        $file = Join-Path $folder $name
        if (Test-Path -LiteralPath $file) {
            Write-Output ("PrivateR8 $variant/$name SHA256=" + (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant())
        }
    }
}
