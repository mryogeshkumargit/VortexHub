@echo off
echo ========================================
echo VortexAI Debug Log Viewer
echo ========================================
echo.
echo This will show live logs from your Android device.
echo Make sure your device is connected via USB debugging.
echo.
echo Press Ctrl+C to stop logging.
echo.
pause

echo Starting logcat filtering for VortexAI...
echo.

REM Clear previous logs and start fresh
adb logcat -c

REM Filter logs for VortexAI app
adb logcat -s VortexMainActivity VortexApplication AndroidRuntime System.err

pause 