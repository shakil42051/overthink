param([switch]$Offline)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $taskRoot
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
    $taskJdk = Get-ChildItem -LiteralPath (Join-Path $taskRoot '.tooling') -Directory -Filter 'jdk-*' -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $taskJdk) { throw 'Set JAVA_HOME to a JDK 21 installation, then run this script again.' }
    $env:JAVA_HOME = $taskJdk.FullName
}
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
$gradleArgs = @(':app:assembleDebug', ':app:assembleRelease', ':app:testDebugUnitTest', ':app:lintDebug', ':app:lintRelease', '--console=plain', '--no-daemon')
if ($Offline) { $gradleArgs += '--offline' }
& ./gradlew.bat @gradleArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Push-Location server
try { node --test test/service.test.mjs; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } } finally { Pop-Location }
