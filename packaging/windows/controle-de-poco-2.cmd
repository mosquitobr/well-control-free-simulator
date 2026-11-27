@echo off
setlocal
set "ROOT=%~dp0..\.."
cd /d "%ROOT%"
call gradlew.bat :desktopApp:run %*
endlocal
