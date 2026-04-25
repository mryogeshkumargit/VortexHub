# VortexAI Crash Log Viewer
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VortexAI Debug Log Viewer" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if ADB is available
try {
    $adbPath = Get-Command adb -ErrorAction Stop
    Write-Host "✅ ADB found at: $($adbPath.Source)" -ForegroundColor Green
} catch {
    Write-Host "❌ ADB not found. Please ensure Android SDK is installed and ADB is in PATH." -ForegroundColor Red
    Write-Host "You can also run this from Android Studio terminal." -ForegroundColor Yellow
    pause
    exit 1
}

# Check if device is connected
Write-Host "Checking for connected devices..." -ForegroundColor Yellow
$devices = & adb devices
if ($devices -match "device$") {
    Write-Host "✅ Android device detected" -ForegroundColor Green
} else {
    Write-Host "❌ No Android device found. Please:" -ForegroundColor Red
    Write-Host "   1. Connect your device via USB" -ForegroundColor White
    Write-Host "   2. Enable USB Debugging" -ForegroundColor White
    Write-Host "   3. Start the Android emulator" -ForegroundColor White
    Write-Host ""
    Write-Host "Current devices:" -ForegroundColor Yellow
    & adb devices
    pause
    exit 1
}

Write-Host ""
Write-Host "📱 Ready to capture VortexAI logs!" -ForegroundColor Green
Write-Host "Instructions:" -ForegroundColor Yellow
Write-Host "1. This script will start capturing logs" -ForegroundColor White
Write-Host "2. Install and launch VortexAI app" -ForegroundColor White
Write-Host "3. Watch for crash logs below" -ForegroundColor White
Write-Host "4. Press Ctrl+C to stop logging" -ForegroundColor White
Write-Host ""
Read-Host "Press Enter to start logging..."

Write-Host "🔍 Starting VortexAI log capture..." -ForegroundColor Cyan
Write-Host "Looking for: VortexApplication, VortexMainActivity, AndroidRuntime" -ForegroundColor Gray
Write-Host "----------------------------------------" -ForegroundColor Gray

# Clear previous logs
& adb logcat -c

# Start filtered logging
& adb logcat -s VortexApplication VortexMainActivity AndroidRuntime System.err | ForEach-Object {
    $line = $_
    if ($line -match "VortexApplication|VortexMainActivity") {
        Write-Host $line -ForegroundColor Cyan
    } elseif ($line -match "FATAL|AndroidRuntime") {
        Write-Host $line -ForegroundColor Red
    } elseif ($line -match "ERROR") {
        Write-Host $line -ForegroundColor Yellow
    } else {
        Write-Host $line -ForegroundColor White
    }
}

Write-Host ""
Write-Host "Log capture stopped." -ForegroundColor Green
pause 