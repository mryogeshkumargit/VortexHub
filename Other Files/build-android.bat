@echo off
setlocal

echo VortexAndroid Build Script
echo ==========================

:: Set Gradle home to avoid path issues
set GRADLE_USER_HOME=%CD%\gradle-home
if not exist "%GRADLE_USER_HOME%" mkdir "%GRADLE_USER_HOME%"

echo Setting Gradle home to: %GRADLE_USER_HOME%
echo.

:: Try to build using the wrapper
echo Attempting to build with Gradle Wrapper...
echo.

if exist gradlew.bat (
    echo Using gradlew.bat...
    call gradlew.bat --version
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo Gradle Wrapper is working! Building project...
        call gradlew.bat assembleDebug
    ) else (
        echo.
        echo Gradle Wrapper failed. Trying alternative approach...
        goto :alternative
    )
) else (
    echo gradlew.bat not found!
    goto :alternative
)

goto :end

:alternative
echo.
echo Alternative: Use Android Studio
echo ==============================
echo.
echo The project is ready for Android Studio:
echo 1. Open Android Studio
echo 2. Select "Open an existing project"
echo 3. Navigate to: %CD%
echo 4. Let Android Studio handle Gradle setup automatically
echo.
echo Android Studio will:
echo - Download Gradle automatically
echo - Sync all dependencies
echo - Build the project
echo - Set up the development environment
echo.

:end
echo.
echo Build script completed.
pause 