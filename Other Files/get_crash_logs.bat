@echo off
title VortexAI Debug Logs
color 0B

echo ========================================
echo VortexAI Debug Log Viewer
echo ========================================
echo.

echo Checking ADB connection...
adb devices
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: ADB not found or not working
    echo Please ensure Android SDK is installed
    pause
    exit /b 1
)

echo.
echo Instructions:
echo 1. Install VortexAI debug APK
echo 2. Launch the app
echo 3. Watch for crash logs below
echo 4. Press Ctrl+C to stop
echo.
pause

echo Starting log capture...
echo.

REM Clear previous logs
adb logcat -c

REM Start capturing logs
adb logcat VortexApplication:D VortexMainActivity:D AndroidRuntime:E System.err:E *:S

pause 