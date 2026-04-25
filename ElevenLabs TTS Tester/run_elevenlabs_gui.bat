@echo off
echo ElevenLabs TTS GUI Launcher
echo ============================

cd /d "%~dp0"

echo Checking Python installation...
python --version >nul 2>&1
if errorlevel 1 (
    echo Python is not installed or not in PATH!
    echo Please install Python 3.7+ and try again.
    pause
    exit /b 1
)

echo Checking dependencies...
python -c "import requests, pygame" >nul 2>&1
if errorlevel 1 (
    echo Installing dependencies...
    pip install -r requirements.txt
    if errorlevel 1 (
        echo Failed to install dependencies!
        pause
        exit /b 1
    )
    echo Dependencies installed successfully!
) else (
    echo Dependencies already available.
)

echo Starting ElevenLabs TTS GUI...
python elevenlabs_tts_gui.py

if errorlevel 1 (
    echo Application exited with error!
    pause
)