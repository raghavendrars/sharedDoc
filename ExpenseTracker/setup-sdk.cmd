@echo off
rem One-time setup: downloads the Android platform and build tools into D:\games\android-tools\sdk.
rem Google's SDK licence is shown first - type y and press Enter to accept it.
setlocal
set "TOOLS=D:\games\android-tools"
for /d %%J in ("%TOOLS%\jdk\jdk-*") do set "JAVA_HOME=%%J"
if not defined JAVA_HOME (
  echo JDK not found under %TOOLS%\jdk
  pause
  exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
call "%TOOLS%\sdk\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root="%TOOLS%\sdk" "platforms;android-34" "build-tools;34.0.0" "platform-tools"
echo.
if exist "%TOOLS%\sdk\build-tools\34.0.0\aapt2.exe" (
  echo Done. The Android tools are installed.
) else (
  echo The tools were NOT installed. Check the messages above.
)
pause
