@echo off
echo Checking dependencies...
python -c "import requests" 2>nul
if %errorlevel% neq 0 (
    echo Installing requests...
    pip install requests
) else (
    echo Dependencies already available.
)

echo.
echo Starting Together AI TTS GUI Tester...
python together_ai_tts_gui.py

pause