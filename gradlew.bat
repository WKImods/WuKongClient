@echo off
setlocal
rem A lightweight verified Gradle bootstrap shim; see README.md.
rem This project is intentionally NOT claiming to include the official Gradle wrapper JAR.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-windows.ps1" %*
exit /b %ERRORLEVEL%
