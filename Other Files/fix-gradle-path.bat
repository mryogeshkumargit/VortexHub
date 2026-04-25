@echo off
echo VortexAndroid - Gradle Path Fix
echo =================================
echo.

echo Creating Gradle home directory...
if not exist "C:\GradleHome" mkdir "C:\GradleHome"
echo ✅ Created C:\GradleHome

echo.
echo Setting GRADLE_USER_HOME environment variable...
setx GRADLE_USER_HOME "C:\GradleHome"
echo ✅ Set GRADLE_USER_HOME=C:\GradleHome

echo.
echo Setting for current session...
set GRADLE_USER_HOME=C:\GradleHome
echo ✅ Current session updated

echo.
echo ========================================
echo ✅ Gradle path issue FIXED!
echo ========================================
echo.
echo Next steps:
echo 1. Open Android Studio
echo 2. File → Settings → Build, Execution, Deployment → Gradle
echo 3. Set "Gradle user home" to: C:\GradleHome
echo 4. Open project: E:\AI\ChatbotMay2025\VortexAndroid
echo 5. Wait for Gradle sync
echo.
echo The project should now sync successfully!
echo.
pause 