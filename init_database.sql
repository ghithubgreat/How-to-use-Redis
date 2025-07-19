-- 删除数据库（如果存在）
DROP DATABASE IF EXISTS skillsystem;

-- 创建数据库
CREATE DATABASE skillsystem DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE skillsystem;

-- 商品表
CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '商品名称',
    price DECIMAL(10, 2) NOT NULL COMMENT '商品价格',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    description TEXT COMMENT '商品描述',
    image_url VARCHAR(255) COMMENT '商品图片URL',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-上架，0-下架',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '商品表';

-- 订单表
CREATE TABLE `order` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(50) NOT NULL COMMENT '订单编号',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    product_price DECIMAL(10, 2) NOT NULL COMMENT '商品价格',
    quantity INT NOT NULL COMMENT '购买数量',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '订单总金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0-待支付，1-已支付，2-已取消',
    payment_time DATETIME COMMENT '支付时间',
    expire_time DATETIME NOT NULL COMMENT '订单过期时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no)
) COMMENT '订单表';

-- 库存日志表
CREATE TABLE stock_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL COMMENT '商品ID',
    before_stock INT NOT NULL COMMENT '变更前库存',
    after_stock INT NOT NULL COMMENT '变更后库存',
    change_amount INT NOT NULL COMMENT '变更数量',
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型：DEDUCT-扣减，INCREASE-增加，SYNC-同步',
    order_id VARCHAR(50) COMMENT '关联订单ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    synced BOOLEAN DEFAULT FALSE COMMENT '是否已同步到Redis',
    remark VARCHAR(255) COMMENT '备注'
) COMMENT '库存日志表';

-- 插入测试数据
-- 商品数据
INSERT INTO product (name, price, stock, description, image_url) VALUES
('iPhone 15', 6999.00, 100, 'Apple iPhone 15 128GB', 'https://store.storeimages.cdn-apple.com/8756/as-images.apple.com/is/iphone-15-finish-select-202309-6-1inch-blue?wid=5120&hei=2880&fmt=p-jpg&qlt=80&.v=1692923777978'),
('MacBook Pro', 12999.00, 50, 'Apple MacBook Pro 14英寸 M2芯片', 'https://store.storeimages.cdn-apple.com/8756/as-images.apple.com/is/mbp14-spacegray-select-202310?wid=5120&hei=2880&fmt=p-jpg&qlt=80&.v=1697236293613'),
('iPad Air', 4799.00, 80, 'Apple iPad Air 10.9英寸', 'https://store.storeimages.cdn-apple.com/8756/as-images.apple.com/is/ipad-air-finish-unselect-gallery-1-202207?wid=5120&hei=2880&fmt=p-jpg&qlt=80&.v=1654904002362'),
('AirPods Pro', 1999.00, 200, 'Apple AirPods Pro 第二代', 'https://store.storeimages.cdn-apple.com/8756/as-images.apple.com/is/MTJV3?wid=572&hei=572&fmt=jpeg&qlt=95&.v=1694014871985'),
('Apple Watch', 2999.00, 60, 'Apple Watch Series 8', 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8M3x8YXBwbGUlMjB3YXRjaHxlbnwwfHwwfHx8MA%3D%3D&auto=format&fit=crop&w=500&q=60');

-- 订单数据 - 使用当前时间生成订单
INSERT INTO `order` (order_no, product_id, product_name, product_price, quantity, total_amount, status, expire_time)
VALUES
(CONCAT('ORD', DATE_FORMAT(NOW(), '%Y%m%d'), '0001'), 1, 'iPhone 15', 6999.00, 1, 6999.00, 0, DATE_ADD(NOW(), INTERVAL 15 MINUTE)),
(CONCAT('ORD', DATE_FORMAT(NOW(), '%Y%m%d'), '0002'), 3, 'iPad Air', 4799.00, 2, 9598.00, 1, DATE_ADD(NOW(), INTERVAL -30 MINUTE));

-- 更新已支付订单的支付时间
UPDATE `order` SET payment_time = DATE_ADD(create_time, INTERVAL 5 MINUTE) WHERE status = 1;

-- 库存日志数据
INSERT INTO stock_log (product_id, before_stock, after_stock, change_amount, operation_type, order_id, synced)
VALUES
(1, 101, 100, 1, 'DEDUCT', (SELECT order_no FROM `order` WHERE product_id = 1 LIMIT 1), true),
(3, 82, 80, 2, 'DEDUCT', (SELECT order_no FROM `order` WHERE product_id = 3 LIMIT 1), true); 