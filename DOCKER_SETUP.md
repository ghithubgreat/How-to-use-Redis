# Docker + RabbitMQ 安装指南

## 🐳 第一步：安装Docker Desktop

### 下载Docker Desktop
1. 访问官方下载页面：https://www.docker.com/products/docker-desktop/
2. 或直接下载：https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe

### 安装步骤
1. **下载安装包**：Docker Desktop Installer.exe
2. **以管理员身份运行**安装程序
3. **安装选项**：
   - ✅ 启用WSL 2功能
   - ✅ 添加桌面快捷方式
4. **完成安装**后重启电脑
5. **启动Docker Desktop**

### 验证安装
打开PowerShell或命令提示符，运行：
```bash
docker --version
docker-compose --version
```

## 🐰 第二步：启动RabbitMQ服务

### 方法1：使用提供的脚本（推荐）
双击运行项目根目录下的 `start-services.bat` 文件

### 方法2：手动命令
在项目根目录打开命令提示符，运行：
```bash
docker-compose up -d
```

### 验证服务启动
1. **检查容器状态**：
   ```bash
   docker-compose ps
   ```

2. **访问RabbitMQ管理界面**：
   - 地址：http://localhost:15672
   - 用户名：admin
   - 密码：admin123

## 🚀 第三步：启动Spring Boot应用

在项目目录运行：
```bash
./mvnw spring-boot:run
```

## 📊 第四步：测试MQ功能

1. **访问MQ监控页面**：http://localhost:8080/monitor/mq-status
2. **测试消息发送**：
   - 输入商品ID和数量
   - 点击"测试扣减"或"测试回滚"
   - 观察日志输出

3. **查看RabbitMQ管理界面**：
   - 访问：http://localhost:15672
   - 查看Queues标签页
   - 观察消息的发送和消费情况

## 🛠️ 常用命令

### 启动服务
```bash
docker-compose up -d
```

### 停止服务
```bash
docker-compose down
```

### 查看日志
```bash
docker-compose logs rabbitmq
```

### 重启服务
```bash
docker-compose restart
```

## 🔧 故障排除

### Docker未启动
- 确保Docker Desktop正在运行
- 检查系统托盘中的Docker图标

### 端口冲突
如果5672或15672端口被占用：
1. 修改docker-compose.yml中的端口映射
2. 相应修改application.properties中的配置

### 连接失败
1. 检查防火墙设置
2. 确认RabbitMQ容器正在运行
3. 检查用户名密码是否正确

## 📈 监控和管理

### RabbitMQ管理界面功能
- **Overview**：系统概览和统计信息
- **Connections**：查看连接状态
- **Channels**：查看通道信息
- **Exchanges**：查看交换机
- **Queues**：查看队列状态和消息
- **Admin**：用户和权限管理

### 应用监控页面
- **MQ状态监控**：http://localhost:8080/monitor/mq-status
- **库存监控**：http://localhost:8080/monitor/stock
- **库存日志**：http://localhost:8080/monitor/stock-log

## 🎯 完成后的功能

✅ 真实的RabbitMQ消息队列
✅ 异步库存扣减和回滚
✅ 消息持久化和可靠传递
✅ 完整的监控和管理界面
✅ 高可用性和故障恢复
