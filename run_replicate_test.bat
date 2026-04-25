@echo off
echo Installing dependencies...
pip install -r requirements_replicate_test.txt
echo.
echo Starting Replicate Test GUI...
python test_replicate_gui.py
pause
