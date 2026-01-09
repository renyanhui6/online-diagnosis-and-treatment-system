@echo off
setlocal enabledelayedexpansion

set NAME=medical-rabbit
set IMAGE=rabbitmq:3-management

set SCRIPT_DIR=%~dp0
set LOG_DIR=%SCRIPT_DIR%logs
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%" >nul 2>&1
for /f %%i in ('powershell -NoProfile -Command "(Get-Date).ToString(\"yyyyMMdd-HHmmss\")"') do set TS=%%i
set LOGFILE=%LOG_DIR%\start-rabbitmq-%TS%.log

call :log "[INFO] Log file: %LOGFILE%"

docker version >nul 2>&1
if not %errorlevel%==0 (
  call :log "[ERROR] Docker is not available. Please start Docker Desktop first."
  call :maybe_pause %*
  exit /b 1
)

docker ps -a --format "{{.Names}}" | findstr /i /x "%NAME%" >nul
if %errorlevel%==0 (
  call :log "[INFO] Container ""%NAME%"" exists, starting..."
  docker start "%NAME%" >> "%LOGFILE%" 2>&1
  if not %errorlevel%==0 (
    call :log "[ERROR] Failed to start ""%NAME%""."
    docker logs --tail 80 "%NAME%" >> "%LOGFILE%" 2>&1
    call :maybe_pause %*
    exit /b 1
  )
  goto :verify
)

call :log "[INFO] Creating container ""%NAME%""..."
docker run -d --name "%NAME%" -p 5672:5672 -p 15672:15672 "%IMAGE%" >> "%LOGFILE%" 2>&1
if not %errorlevel%==0 (
  call :log "[ERROR] Failed to create ""%NAME%""."
  call :maybe_pause %*
  exit /b 1
)

:verify
for /f "usebackq delims=" %%R in (`docker inspect -f "{{.State.Running}}" "%NAME%" 2^>nul`) do set RUNNING=%%R
if /i not "%RUNNING%"=="true" (
  call :log "[ERROR] ""%NAME%"" is not running."
  docker logs --tail 80 "%NAME%" >> "%LOGFILE%" 2>&1
  call :maybe_pause %*
  exit /b 1
)

call :log "[OK] RabbitMQ is running:"
call :log "     AMQP: http://localhost:5672"
call :log "     UI:   http://localhost:15672  (guest/guest)"
call :maybe_pause %*
exit /b 0

:log
set MSG=%~1
echo %MSG%
echo %MSG%>>"%LOGFILE%"
exit /b 0

:maybe_pause
if /i "%~1"=="--no-pause" exit /b 0
if /i "%~2"=="--no-pause" exit /b 0
pause
exit /b 0
