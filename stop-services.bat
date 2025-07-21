@echo off
echo ========================================
echo      停止 RabbitMQ 和 Redis 服务
echo ========================================
echo.

echo 停止服务...
docker-compose down

echo.
echo ✅ 服务已停止
echo.

pause
