@echo off
setlocal enabledelayedexpansion

set NAME=medical-minio
set IMAGE=minio/minio:latest
set API_PORT=9000
set CONSOLE_PORT=9002

set SCRIPT_DIR=%~dp0
set LOG_DIR=%SCRIPT_DIR%logs
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%" >nul 2>&1
for /f %%i in ('powershell -NoProfile -Command "(Get-Date).ToString(\"yyyyMMdd-HHmmss\")"') do set TS=%%i
set LOGFILE=%LOG_DIR%\start-minio-%TS%.log

call :log "[INFO] Log file: %LOGFILE%"

docker version >nul 2>&1
if not %errorlevel%==0 (
  call :log "[ERROR] Docker is not available. Please start Docker Desktop first."
  call :maybe_pause %*
  exit /b 1
)

docker ps -a --format "{{.Names}}" | findstr /i /x "%NAME%" >nul
if %errorlevel%==0 (
  set NEED_RECREATE=
  for /f "usebackq delims=" %%P in (`docker port "%NAME%" 2^>nul ^| findstr /i /c:"9001/tcp -> 0.0.0.0:9001" /c:"9001/tcp -> [::]:9001"`) do set NEED_RECREATE=1
  if defined NEED_RECREATE (
    call :log "[INFO] Container ""%NAME%"" uses legacy console port 9001, recreating to avoid Netty conflict..."
    docker rm -f "%NAME%" >> "%LOGFILE%" 2>&1
    if not %errorlevel%==0 (
      call :log "[ERROR] Failed to remove legacy container ""%NAME%""."
      call :maybe_pause %*
      exit /b 1
    )
    goto :create
  )
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

:create
call :log "[INFO] Creating container ""%NAME%""..."
docker run -d --name "%NAME%" ^
  -p %API_PORT%:9000 -p %CONSOLE_PORT%:9001 ^
  -e MINIO_ROOT_USER=minioadmin ^
  -e MINIO_ROOT_PASSWORD=minioadmin ^
  -v medical-minio-data:/data ^
  "%IMAGE%" server /data --console-address ":9001" >> "%LOGFILE%" 2>&1

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

call :log "[OK] MinIO is running:"
call :log "     API:     http://localhost:%API_PORT%"
call :log "     Console: http://localhost:%CONSOLE_PORT%  (minioadmin/minioadmin)"
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
