@echo off
echo ========================================
echo VortexAI Companion App - Modern Build Test
echo ========================================
echo.
echo Testing modern architecture with KSP...
echo.

REM Clean previous builds
echo [1/4] Cleaning previous builds...
call gradlew clean
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Clean failed!
    pause
    exit /b 1
)
echo ✅ Clean completed

REM Build core modules first
echo.
echo [2/4] Building core modules...
call gradlew :core:common:build :core:design:build :core:model:build :core:database:build
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Core modules build failed!
    echo.
    echo This might indicate:
    echo - Missing dependencies in version catalog
    echo - KSP annotation processing issues
    echo - Module configuration problems
    echo.
    pause
    exit /b 1
)
echo ✅ Core modules built successfully

REM Test KSP annotation processing
echo.
echo [3/4] Testing KSP annotation processing...
call gradlew :core:database:kspDebugKotlin
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: KSP processing failed!
    echo.
    echo This indicates annotation processing issues.
    echo Check the error logs above for details.
    echo.
    pause
    exit /b 1
)
echo ✅ KSP annotation processing successful

REM Build entire project
echo.
echo [4/4] Building entire project...
call gradlew build
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Full project build failed!
    echo.
    echo Check the error logs above for details.
    echo Common issues:
    echo - Missing module dependencies
    echo - Circular dependencies
    echo - Configuration problems
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo ✅ BUILD SUCCESSFUL!
echo ========================================
echo.
echo Modern architecture is working correctly:
echo ✅ KSP instead of KAPT (no more build issues!)
echo ✅ Modular structure compiles
echo ✅ Room database with KSP annotations
echo ✅ Version catalog dependencies resolved
echo ✅ Hilt dependency injection configured
echo.
echo Your companion app is ready for development!
echo.
pause 