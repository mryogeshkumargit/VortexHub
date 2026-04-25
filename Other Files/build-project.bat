@echo off
echo VortexAndroid Build Script
echo ========================

echo.
echo Project Structure Check:
echo ------------------------

if exist "app\build.gradle" (
    echo ✅ App build.gradle found
) else (
    echo ❌ App build.gradle missing
    goto :error
)

if exist "app\src\main\AndroidManifest.xml" (
    echo ✅ AndroidManifest.xml found
) else (
    echo ❌ AndroidManifest.xml missing
    goto :error
)

if exist "app\src\main\java\com\vortexai\android\MainActivity.kt" (
    echo ✅ MainActivity.kt found
) else (
    echo ❌ MainActivity.kt missing
    goto :error
)

if exist "gradle\wrapper\gradle-wrapper.jar" (
    echo ✅ Gradle Wrapper JAR found
) else (
    echo ❌ Gradle Wrapper JAR missing
    goto :error
)

echo.
echo All essential files are present!
echo.
echo To build this project:
echo 1. Open Android Studio
echo 2. Select "Open an existing project"
echo 3. Navigate to this folder: %CD%
echo 4. Let Android Studio sync and build the project
echo.
echo Alternatively, if you have Gradle installed globally:
echo   gradle assembleDebug
echo.
goto :end

:error
echo.
echo ❌ Project setup incomplete. Please check missing files.
echo.

:end
pause 