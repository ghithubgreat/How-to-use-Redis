-- 删除现有表（按依赖关系顺序删除）
DROP TABLE IF EXISTS stock_lock;
DROP TABLE IF EXISTS stock_log;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS product;

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
    remark VARCHAR(255) COMMENT '备注',
    INDEX idx_product_id (product_id),
    INDEX idx_order_id (order_id),
    INDEX idx_create_time (create_time)
) COMMENT '库存日志表';

-- 库存锁定表
CREATE TABLE stock_lock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL COMMENT '商品ID',
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号',
    locked_quantity INT NOT NULL COMMENT '锁定数量',
    status INT NOT NULL DEFAULT 0 COMMENT '状态：0-锁定中，1-已释放，2-已扣减',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    release_time DATETIME COMMENT '释放时间',
    expire_time DATETIME COMMENT '过期时间',
    remark VARCHAR(255) COMMENT '备注',
    INDEX idx_product_id (product_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    INDEX idx_expire_time (expire_time)
) COMMENT '库存锁定表';
