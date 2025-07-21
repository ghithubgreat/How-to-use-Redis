# Spring Boot Redis库存管理系统

一个基于Spring Boot + Redis的完整库存管理系统，演示了如何使用Redis进行高并发库存管理、防止超卖、以及实现最终一致性。

## 🎯 项目特色

### 核心功能
- **防超卖机制**：下单时立即扣减Redis库存，防止超卖
- **最终一致性**：Redis与数据库的异步同步机制
- **库存锁定**：支持订单库存锁定和自动释放
- **实时监控**：完整的库存监控和日志系统
- **商品状态管理**：上架/下架商品状态控制

### 技术亮点
- **MockRedis实现**：无需安装Redis，使用内存模拟Redis操作
- **定时任务**：自动处理超时订单和库存同步
- **分页查询**：支持大数据量的分页展示
- **响应式UI**：Bootstrap + JavaScript实现的现代化界面

## 🏗️ 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   前端页面      │    │   Spring Boot   │    │   数据库层      │
│                 │    │                 │    │                 │
│ • 商品列表      │◄──►│ • 商品管理      │◄──►│ • MySQL数据库   │
│ • 库存监控      │    │ • 订单处理      │    │ • JPA Repository│
│ • 日志查看      │    │ • 库存同步      │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   Redis缓存     │
                       │                 │
                       │ • 商品库存      │
                       │ • 库存锁定      │
                       │ • 缓存管理      │
                       └─────────────────┘
```

## 🚀 快速开始

### 环境要求
- Java 17+
- Maven 3.6+
- 无需安装Redis（使用MockRedis）

### 运行步骤

1. **克隆项目**
```bash
git clone https://github.com/ghithubgreat/How-to-use-Redis.git
cd How-to-use-Redis
```

2. **启动应用**
```bash
./mvnw spring-boot:run
```

3. **访问系统**
- 商品列表：http://localhost:8080/product/list
- 库存监控：http://localhost:8080/monitor/stock
- 库存日志：http://localhost:8080/monitor/stock-log
- Redis缓存：http://localhost:8080/monitor/redis-cache

## 📋 功能模块

### 1. 商品管理
- **商品列表**：展示所有商品信息
- **商品详情**：查看单个商品详细信息
- **状态控制**：只有上架商品才能查看和购买
- **缓存管理**：商品信息Redis缓存

### 2. 库存管理
- **实时库存**：Redis中的实时可用库存
- **库存锁定**：下单时立即锁定库存
- **库存扣减**：支付成功后扣减库存
- **库存回滚**：订单取消时回滚库存

### 3. 订单处理
- **下单流程**：库存检查 → 锁定库存 → 创建订单
- **支付处理**：扣减库存 → 更新订单状态
- **超时处理**：自动取消超时订单并释放库存

### 4. 监控系统
- **库存监控**：实时查看数据库与Redis库存差异
- **操作日志**：完整的库存变更记录
- **缓存监控**：Redis缓存状态管理
- **统计分析**：各类操作的统计信息

## 🔧 核心技术实现

### 防超卖机制
```java
// 下单时立即扣减Redis库存
public boolean lockStock(Long productId, Integer quantity, String orderNo) {
    String stockKey = RedisKeyPrefix.PRODUCT_STOCK + productId;
    Integer currentStock = (Integer) mockRedisService.get(stockKey);
    
    if (currentStock != null && currentStock >= quantity) {
        // 立即扣减Redis库存
        mockRedisService.set(stockKey, currentStock - quantity);
        return true;
    }
    return false;
}
```

### 最终一致性
```java
// 定时同步Redis与数据库
@Scheduled(fixedDelay = 30000)
public void syncStockConsistency() {
    List<Product> products = productRepository.findAll();
    for (Product product : products) {
        syncProductStock(product.getId());
    }
}
```

## 📊 数据库设计

### 核心表结构
- **product**：商品信息表
- **order**：订单信息表  
- **stock_log**：库存变更日志表
- **stock_lock**：库存锁定记录表

## 🎮 使用示例

### 完整测试流程
1. 访问商品列表，查看初始库存
2. 点击商品详情，触发缓存加载
3. 进行下单操作，观察库存变化
4. 查看库存监控，确认数据一致性
5. 检查操作日志，追踪变更记录

## 🛠️ 技术栈

- **后端框架**：Spring Boot 3.x
- **数据库**：MySQL + H2 (内存数据库)
- **缓存**：MockRedis (内存模拟)
- **前端**：Thymeleaf + Bootstrap 5
- **构建工具**：Maven

## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进这个项目！

## 📄 许可证

MIT License

## 📞 联系方式

如有问题，请提交Issue或联系项目维护者。
