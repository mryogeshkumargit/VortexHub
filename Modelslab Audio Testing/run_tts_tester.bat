@echo off
echo Installing dependencies...
pip install -r requirements.txt

echo Starting ModelsLab TTS Tester...
python modelslab_tts_tester.py

pause