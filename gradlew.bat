@echo off
setlocal EnableDelayedExpansion
set "GRADLE_BIN="
for /d %%D in ("%USERPROFILE%\.gradle\wrapper\dists\gradle-8.4-bin\*") do (
  if exist "%%D\gradle-8.4\bin\gradle.bat" set "GRADLE_BIN=%%D\gradle-8.4\bin\gradle.bat"
)
if defined GRADLE_BIN goto run
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
echo.
echo [mo.co hub] Gradle 8.4 ainda nao foi encontrado neste computador.
echo Abra o projeto no Android Studio e execute "Sync Project with Gradle Files" uma vez.
echo Depois rode novamente: .\gradlew.bat clean assembleDebug
echo.
exit /b 1
:run
call "%GRADLE_BIN%" %*
exit /b %ERRORLEVEL%
