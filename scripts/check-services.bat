@echo off
setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set LOG_DIR=%SCRIPT_DIR%logs
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%" >nul 2>&1
for /f %%i in ('powershell -NoProfile -Command "(Get-Date).ToString(\"yyyyMMdd-HHmmss\")"') do set TS=%%i
set LOGFILE=%LOG_DIR%\check-services-%TS%.log

call :log "[INFO] Log file: %LOGFILE%"

docker version >nul 2>&1
if not %errorlevel%==0 (
  call :log "[ERROR] Docker is not available. Please start Docker Desktop first."
  call :maybe_pause %*
  exit /b 1
)

call :check_container medical-redis Redis 6379
if not %errorlevel%==0 exit /b 1

call :check_container medical-rabbit RabbitMQ 5672
if not %errorlevel%==0 exit /b 1

call :check_container medical-minio MinIO 9000
if not %errorlevel%==0 exit /b 1

call :log "[OK] All services look running."
call :log "     Redis:    localhost:6379"
call :log "     RabbitMQ: localhost:5672  UI http://localhost:15672"
call :log "     MinIO:    http://localhost:9000  Console http://localhost:9001"
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

:check_container
set NAME=%~1
set LABEL=%~2
set PORT=%~3

docker ps -a --format "{{.Names}}" | findstr /i /x "%NAME%" >nul
if not %errorlevel%==0 (
  call :log "[ERROR] %LABEL% container ""%NAME%"" not found."
  call :log "       Run: scripts/start-redis.bat / scripts/start-rabbitmq.bat / scripts/start-minio.bat"
  exit /b 1
)

for /f "usebackq delims=" %%R in (`docker inspect -f "{{.State.Running}}" "%NAME%" 2^>nul`) do set RUNNING=%%R
if /i not "%RUNNING%"=="true" (
  call :log "[ERROR] %LABEL% container ""%NAME%"" exists but is NOT running."
  call :log "[INFO] Last logs:"
  docker logs --tail 40 "%NAME%" >> "%LOGFILE%" 2>&1
  exit /b 1
)

call :log "[OK] %LABEL% is running (%NAME%), port %PORT%"
exit /b 0
