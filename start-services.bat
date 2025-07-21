@echo off
chcp 65001 >nul
echo ========================================
echo    Starting RabbitMQ and Redis Services
echo ========================================
echo.

echo Checking Docker...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker not installed or not running
    echo Please install and start Docker Desktop first
    pause
    exit /b 1
)

echo [OK] Docker is ready
echo.

echo Starting services...
docker-compose up -d

echo.
echo Waiting for services to start...
timeout /t 10 /nobreak >nul

echo.
echo ========================================
echo           Service Status
echo ========================================
docker-compose ps

echo.
echo ========================================
echo           Access Information
echo ========================================
echo RabbitMQ Management: http://localhost:15672
echo    Username: admin
echo    Password: admin123
echo.
echo Redis: localhost:6379
echo.
echo Application: http://localhost:8080
echo    MQ Monitor: http://localhost:8080/monitor/mq-status
echo.
echo ========================================

pause
