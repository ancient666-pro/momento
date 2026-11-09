# Connects a USB-attached Android device/emulator to the local backend.
#
# The app talks to http://127.0.0.1:8080/ (see app/build.gradle.kts). `adb reverse`
# forwards that port on the device back to this machine, so it works over USB even
# when the device is on mobile data and cannot reach the PC's LAN address.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts/dev-connect.ps1
#
# Requires: backend running on http://127.0.0.1:8080 and USB debugging enabled.

$ErrorActionPreference = "Stop"

$adbCandidates = @(
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    "$env:ANDROID_HOME\platform-tools\adb.exe",
    "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe"
)

$adb = $adbCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $adb) {
    $adbCommand = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbCommand) { $adb = $adbCommand.Source }
}
if (-not $adb) {
    Write-Error "adb not found. Install Android platform-tools or set ANDROID_HOME."
}

# 1. Confirm the backend answers locally.
try {
    $health = (Invoke-WebRequest -Uri "http://127.0.0.1:8080/health" -TimeoutSec 5 -UseBasicParsing).Content
    Write-Host "Backend: $health"
} catch {
    Write-Warning "Backend is not responding on http://127.0.0.1:8080/health."
    Write-Warning "Start it with: cd backend; .\gradlew run"
}

# 2. Confirm a device is attached.
$devices = & $adb devices | Select-String "\sdevice$"
if (-not $devices) {
    Write-Error "No Android device connected. Enable USB debugging and accept the authorization prompt, then re-run."
}

# 3. Forward device loopback:8080 -> host loopback:8080.
& $adb reverse tcp:8080 tcp:8080
if ($LASTEXITCODE -ne 0) {
    Write-Error "adb reverse failed. Try: $adb kill-server; $adb start-server"
}

Write-Host ""
Write-Host "Reverse tunnel active:" -ForegroundColor Green
& $adb reverse --list

# 4. Optionally relaunch the installed debug app so it picks up the tunnel.
$packageName = "com.momento.app"
& $adb shell pm list packages $packageName | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "Relaunching $packageName ..."
    & $adb shell am force-stop $packageName
    & $adb shell monkey -p $packageName -c android.intent.category.LAUNCHER 1 | Out-Null
}

Write-Host ""
Write-Host "Tip: override the host with:" -ForegroundColor Cyan
Write-Host "  cd android; .\gradlew :app:assembleDebug -Pmomento.apiBaseUrl=http://<pc-lan-ip>:8080/"
