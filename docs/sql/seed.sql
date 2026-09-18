-- SeckillCore deterministic local fixture data.
-- Login credentials: demo_user / password.
USE `seckill_core`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

SET @has_seckill_price = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_order' AND COLUMN_NAME = 'seckill_price');
SET @has_quantity = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_order' AND COLUMN_NAME = 'quantity');
SET @alter_price = IF(@has_seckill_price = 0, 'ALTER TABLE t_order ADD COLUMN seckill_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER seckill_log_id', 'SELECT 1');
PREPARE alter_price_stmt FROM @alter_price;
EXECUTE alter_price_stmt;
DEALLOCATE PREPARE alter_price_stmt;
SET @alter_quantity = IF(@has_quantity = 0, 'ALTER TABLE t_order ADD COLUMN quantity INT NOT NULL DEFAULT 1 AFTER seckill_price', 'SELECT 1');
PREPARE alter_quantity_stmt FROM @alter_quantity;
EXECUTE alter_quantity_stmt;
DEALLOCATE PREPARE alter_quantity_stmt;

INSERT INTO `t_user` (`id`, `username`, `password`, `phone`, `email`, `avatar`, `status`, `create_time`, `update_time`, `is_deleted`)
VALUES
    (1000000000000000001, 'demo_user', '$2a$10$X39F/WnL2Ef8zTGVmAFimOqpySHgQaLu8fbVogQdiV5SiQfVR62Pe', '13800000001', 'demo@example.com', NULL, 1, NOW(), NOW(), 0),
    (1000000000000000002, 'frozen_user', '$2a$10$X39F/WnL2Ef8zTGVmAFimOqpySHgQaLu8fbVogQdiV5SiQfVR62Pe', '13800000002', 'frozen@example.com', NULL, 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE username = VALUES(username), password = VALUES(password), phone = VALUES(phone), email = VALUES(email), status = VALUES(status), is_deleted = 0;

INSERT INTO `t_product` (`id`, `product_name`, `description`, `price`, `seckill_price`, `available_stock`, `total_stock`, `image_url`, `status`, `version`, `create_time`, `update_time`, `is_deleted`)
VALUES
    (2000000000000000001, 'Noise Cancelling Headphones', 'Immersive audio experience', 899.00, 599.00, 97, 100, NULL, 1, 0, NOW(), NOW(), 0),
    (2000000000000000002, 'Smart Thermos', 'Portable long-lasting insulation', 199.00, 99.00, 40, 40, NULL, 1, 0, NOW(), NOW(), 0),
    (2000000000000000003, 'Limited Gift Box', 'Discontinued product for status testing', 299.00, 199.00, 0, 20, NULL, 0, 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE product_name = VALUES(product_name), description = VALUES(description), price = VALUES(price), seckill_price = VALUES(seckill_price), available_stock = VALUES(available_stock), total_stock = VALUES(total_stock), status = VALUES(status), is_deleted = 0;

INSERT INTO `t_activity` (`id`, `activity_name`, `product_id`, `seckill_price`, `total_limit`, `remaining_limit`, `start_time`, `end_time`, `per_user_limit`, `status`, `version`, `create_time`, `update_time`, `is_deleted`)
VALUES
    (3000000000000000001, 'Upcoming Session', 2000000000000000001, 599.00, 100, 100, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), 1, 0, 0, NOW(), NOW(), 0),
    (3000000000000000002, 'Running Session', 2000000000000000001, 599.00, 100, 97, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 1 DAY), 1, 1, 0, NOW(), NOW(), 0),
    (3000000000000000003, 'Finished Session', 2000000000000000002, 99.00, 40, 0, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 1, 2, 0, NOW(), NOW(), 0),
    (3000000000000000004, 'Cancelled Session', 2000000000000000003, 199.00, 20, 20, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY), 1, 3, 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE activity_name = VALUES(activity_name), product_id = VALUES(product_id), seckill_price = VALUES(seckill_price), total_limit = VALUES(total_limit), remaining_limit = VALUES(remaining_limit), start_time = VALUES(start_time), end_time = VALUES(end_time), status = VALUES(status), is_deleted = 0;

INSERT INTO `t_seckill_log` (`id`, `user_id`, `product_id`, `activity_id`, `status`, `fail_reason`, `seckill_time`, `create_time`, `update_time`, `is_deleted`)
VALUES
    (4000000000000000001, 1000000000000000001, 2000000000000000001, 3000000000000000002, 0, NULL, NOW(), NOW(), NOW(), 0),
    (4000000000000000002, 1000000000000000002, 2000000000000000001, 3000000000000000002, 1, NULL, DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_SUB(NOW(), INTERVAL 10 MINUTE), NOW(), 0),
    (4000000000000000003, 1000000000000000001, 2000000000000000002, 3000000000000000003, 2, 'Activity finished', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 0),
    (4000000000000000004, 1000000000000000002, 2000000000000000003, 3000000000000000004, 3, 'Activity cancelled', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 0)
ON DUPLICATE KEY UPDATE status = VALUES(status), fail_reason = VALUES(fail_reason), update_time = NOW(), is_deleted = 0;

INSERT INTO `t_order` (`id`, `order_no`, `user_id`, `product_id`, `activity_id`, `seckill_log_id`, `seckill_price`, `quantity`, `order_status`, `pay_amount`, `pay_time`, `expire_time`, `create_time`, `update_time`, `is_deleted`)
VALUES
    (5000000000000000001, 'SKFIXTUREPENDING01', 1000000000000000001, 2000000000000000001, 3000000000000000002, 4000000000000000001, 599.00, 1, 0, 599.00, NULL, DATE_ADD(NOW(), INTERVAL 15 MINUTE), NOW(), NOW(), 0),
    (5000000000000000002, 'SKFIXTUREPAID00002', 1000000000000000002, 2000000000000000001, 3000000000000000002, 4000000000000000002, 599.00, 1, 1, 599.00, DATE_SUB(NOW(), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW(), 0),
    (5000000000000000003, 'SKFIXTURECANCEL03', 1000000000000000001, 2000000000000000002, 3000000000000000003, 4000000000000000003, 99.00, 1, 2, 99.00, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 0),
    (5000000000000000004, 'SKFIXTUREREFUND04', 1000000000000000002, 2000000000000000003, 3000000000000000004, 4000000000000000004, 199.00, 1, 3, 199.00, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), NOW(), 0)
ON DUPLICATE KEY UPDATE order_no = VALUES(order_no), user_id = VALUES(user_id), product_id = VALUES(product_id), activity_id = VALUES(activity_id), seckill_log_id = VALUES(seckill_log_id), seckill_price = VALUES(seckill_price), quantity = VALUES(quantity), order_status = VALUES(order_status), pay_amount = VALUES(pay_amount), is_deleted = 0;

INSERT INTO `t_payment` (`id`, `payment_no`, `order_id`, `user_id`, `pay_amount`, `pay_channel`, `pay_status`, `trade_no`, `pay_time`, `callback_time`, `create_time`, `update_time`, `is_deleted`)
VALUES
    (6000000000000000001, 'PYFIXTUREPENDING01', 5000000000000000001, 1000000000000000001, 599.00, 0, 0, NULL, NULL, NULL, NOW(), NOW(), 0),
    (6000000000000000002, 'PYFIXTURESUCCESS02', 5000000000000000002, 1000000000000000002, 599.00, 1, 1, 'TRADE-FIXTURE-02', DATE_SUB(NOW(), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 5 MINUTE), NOW(), NOW(), 0),
    (6000000000000000003, 'PYFIXTUREFAILED03', 5000000000000000003, 1000000000000000001, 99.00, 0, 2, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), NOW(), 0),
    (6000000000000000004, 'PYFIXTUREREFUND04', 5000000000000000004, 1000000000000000002, 199.00, 1, 3, 'TRADE-FIXTURE-04', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE payment_no = VALUES(payment_no), order_id = VALUES(order_id), user_id = VALUES(user_id), pay_amount = VALUES(pay_amount), pay_channel = VALUES(pay_channel), pay_status = VALUES(pay_status), trade_no = VALUES(trade_no), pay_time = VALUES(pay_time), callback_time = VALUES(callback_time), is_deleted = 0;

SET FOREIGN_KEY_CHECKS = 1;
