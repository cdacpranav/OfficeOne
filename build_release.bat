@echo off
setlocal enabledelayedexpansion

echo ====================================================================
echo                   OFFICE ONE - RELEASE BUILD AUTOMATION
echo ====================================================================
echo.
echo Checking build prerequisites...

powershell -ExecutionPolicy Bypass -File "%~dp0build_release.ps1" %*

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Build encountered an issue. See details above.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Build script completed successfully!
pause
