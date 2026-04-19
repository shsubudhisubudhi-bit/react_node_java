@echo off
cd /d "%~dp0"
docker version >nul 2>&1
if errorlevel 1 (
  echo.
  echo [ERROR] Docker is not installed or not running.
  echo Install Docker Desktop and start it: https://www.docker.com/products/docker-desktop/
  echo.
  pause
  exit /b 1
)
echo Starting stack ^(first build may take 5-15 minutes^)...
docker compose up --build -d
if errorlevel 1 (
  echo.
  echo [FAILED] See messages above. Is Docker Desktop running? Are ports 8080, 3000, 5173 free?
  echo.
  pause
  exit /b 1
)
echo.
echo Done. Open in browser:
echo   UI:    http://localhost:5173
echo   API:   http://localhost:3000/health
echo.
pause
