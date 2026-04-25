@echo off
echo VortexAI Debug Setup
echo ====================

echo.
echo 1. Make sure your phone and PC are on the same WiFi network
echo 2. Enable Developer Options on your phone:
echo    - Settings ^> About Phone ^> Tap "Build Number" 7 times
echo.
echo 3. Enable WiFi Debugging:
echo    - Settings ^> Developer Options ^> "Wireless debugging" ON
echo    - Tap "Wireless debugging" ^> "Pair device with pairing code"
echo    - Note the IP and port shown
echo.
echo 4. Run the debug viewer:
python debug_viewer.py

pause