@echo off
echo ========================================
echo MochiMobileOS Forge MOD Builder
echo ========================================
echo.
echo Building Forge MOD JAR file...
echo This will create a distributable MOD file
echo that can be installed in Minecraft Forge.
echo.

cd /d "%~dp0"

echo Step 1: Clean previous builds...
call gradlew.bat clean

echo.
echo Step 2: Build MOD JAR...
call gradlew.bat build

echo.
echo Step 3: Check build results...
if exist "build\libs\*.jar" (
    echo ✅ BUILD SUCCESSFUL!
    echo.
    echo MOD JAR files created:
    dir "build\libs\*.jar" /B
    echo.
    echo Location: %CD%\build\libs\
    echo.
    echo Install these JAR files in your Minecraft mods folder:
    echo %APPDATA%\.minecraft\mods\
) else (
    echo ❌ BUILD FAILED!
    echo Check the output above for errors.
)

echo.
pause