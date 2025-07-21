@echo off
echo ========================================
echo        系统环境检查
echo ========================================
echo.

echo 1. 检查Docker安装...
docker --version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Docker已安装
    docker --version
) else (
    echo ❌ Docker未安装
    echo 请先安装Docker Desktop
)
echo.

echo 2. 检查Docker Compose...
docker-compose --version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Docker Compose可用
    docker-compose --version
) else (
    echo ❌ Docker Compose不可用
)
echo.

echo 3. 检查Java环境...
java -version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Java已安装
    java -version
) else (
    echo ❌ Java未安装
)
echo.

echo 4. 检查Maven...
call mvnw --version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Maven可用
    call mvnw --version | findstr "Apache Maven"
) else (
    echo ❌ Maven不可用
)
echo.

echo 5. 检查端口占用...
echo 检查8080端口（应用端口）...
netstat -an | findstr ":8080" >nul 2>&1
if %errorlevel% equ 0 (
    echo ⚠️  端口8080已被占用
) else (
    echo ✅ 端口8080可用
)

echo 检查5672端口（RabbitMQ AMQP）...
netstat -an | findstr ":5672" >nul 2>&1
if %errorlevel% equ 0 (
    echo ⚠️  端口5672已被占用
) else (
    echo ✅ 端口5672可用
)

echo 检查15672端口（RabbitMQ管理界面）...
netstat -an | findstr ":15672" >nul 2>&1
if %errorlevel% equ 0 (
    echo ⚠️  端口15672已被占用
) else (
    echo ✅ 端口15672可用
)
echo.

echo ========================================
echo           安装建议
echo ========================================
echo 1. 如果Docker未安装，请运行Docker-Desktop-Installer.exe
echo 2. 安装完成后，运行 start-services.bat 启动RabbitMQ
echo 3. 然后运行 mvnw spring-boot:run 启动应用
echo 4. 访问 http://localhost:8080/monitor/mq-status 测试MQ功能
echo ========================================

pause
