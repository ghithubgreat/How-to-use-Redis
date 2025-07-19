-- 插入测试数据
-- 商品数据 (status: 1-上架，0-下架)
INSERT INTO product (name, price, stock, description, image_url, status) VALUES
('iPhone 15', 6999.00, 100, 'Apple iPhone 15 128GB', 'https://store.storeimages.cdn-apple.com/8756/as-images.apple.com/is/iphone-15-finish-select-202309-6-1inch-blue?wid=5120&hei=2880&fmt=p-jpg&qlt=80&.v=1692923777978', 1),
('MacBook Pro', 12999.00, 50, 'Apple MacBook Pro 14英寸 M2芯片', 'https://store.storeimages.cdn-apple.com/8756/as-images.apple.com/is/mbp14-spacegray-select-202310?wid=5120&hei=2880&fmt=p-jpg&qlt=80&.v=1697236293613', 0),
('iPad Air', 4799.00, 80, 'Apple iPad Air 10.9英寸', 'https://store.storeimages.cdn-apple.com/8756/as-images.apple.com/is/ipad-air-finish-unselect-gallery-1-202207?wid=5120&hei=2880&fmt=p-jpg&qlt=80&.v=1654904002362', 1),
('AirPods Pro', 1999.00, 200, 'Apple AirPods Pro 第二代', 'https://store.storeimages.cdn-apple.com/8756/as-images.apple.com/is/MTJV3?wid=572&hei=572&fmt=jpeg&qlt=95&.v=1694014871985', 1),
('Apple Watch', 2999.00, 60, 'Apple Watch Series 8', 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8M3x8YXBwbGUlMjB3YXRjaHxlbnwwfHwwfHx8MA%3D%3D&auto=format&fit=crop&w=500&q=60', 0);

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
