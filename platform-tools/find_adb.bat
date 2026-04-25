@echo off
echo Finding ADB installation...
echo.

REM Check if ADB is in PATH
adb version >nul 2>&1
if %errorlevel% == 0 (
    echo ✅ ADB found in PATH
    adb version
    goto :end
)

REM Check common locations
set "locations[0]=C:\Users\%USERNAME%\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set "locations[1]=C:\Android\sdk\platform-tools\adb.exe"
set "locations[2]=C:\Program Files\Android\sdk\platform-tools\adb.exe"
set "locations[3]=C:\Android\platform-tools\adb.exe"

for /L %%i in (0,1,3) do (
    call set "path=%%locations[%%i]%%"
    if exist "!path!" (
        echo ✅ ADB found at: !path!
        "!path!" version
        echo.
        echo To fix the PATH issue, copy this path:
        echo !path!
        echo.
        echo Then add it to your system PATH or copy adb.exe to:
        echo %~dp0
        goto :end
    )
)

echo ❌ ADB not found in common locations
echo.
echo Please download Android SDK Platform Tools from:
echo https://developer.android.com/studio/releases/platform-tools
echo.
echo Extract and place adb.exe in: %~dp0

:end
pause