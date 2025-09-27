@echo off
echo ========================================
echo MochiMobileOS Forge Client Launcher
echo ========================================
echo.
echo Starting Minecraft 1.20.1 with Forge...
echo MochiMobileOS MOD will be loaded automatically.
echo.
echo Use this to test the Forge integration:
echo 1. Other MODs can register apps via PhoneAppRegistryEvent
echo 2. AppStore will show available MOD apps
echo 3. Install/manage apps through the MochiMobileOS interface
echo.
pause
echo.

cd /d "%~dp0"
call gradlew.bat runClient

pause