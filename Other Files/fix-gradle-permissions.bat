@echo off
echo Fixing Gradle permission issues...

REM Create gradle wrapper distribution directory in project
mkdir "%~dp0gradle-wrapper-dists" 2>nul

REM Remove any existing gradle cache that might be causing issues
rmdir /s /q "%~dp0.gradle" 2>nul

REM Update gradle wrapper properties to use project directory
echo #Updated to avoid GRADLE_USER_HOME permission issues > "%~dp0gradle\wrapper\gradle-wrapper.properties"
echo distributionBase=PROJECT >> "%~dp0gradle\wrapper\gradle-wrapper.properties"
echo distributionPath=gradle-wrapper-dists >> "%~dp0gradle\wrapper\gradle-wrapper.properties"
echo distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip >> "%~dp0gradle\wrapper\gradle-wrapper.properties"
echo zipStoreBase=PROJECT >> "%~dp0gradle\wrapper\gradle-wrapper.properties"
echo zipStorePath=gradle-wrapper-dists >> "%~dp0gradle\wrapper\gradle-wrapper.properties"

REM Set GRADLE_USER_HOME to a writable location temporarily
set GRADLE_USER_HOME=%USERPROFILE%\.gradle

echo Gradle configuration updated successfully!
echo You can now run: gradlew build
echo Or open the project in Android Studio

pause 