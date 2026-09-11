-- =============================================
-- SeckillCore 全部建表语句（MySQL 8.0+）
-- 执行顺序：按依赖关系依次创建
-- =============================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `seckill_core`
    DEFAULT CHARACTER SET = `utf8mb4`
    DEFAULT COLLATE = `utf8mb4_unicode_ci`;

-- 切换到该数据库
USE `seckill_core`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 用户表
CREATE TABLE `t_user` (
    `id`                BIGINT(20)   NOT NULL COMMENT '用户ID（雪花ID）',
    `username`          VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`          VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
    `phone`             VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `email`             VARCHAR(64)  DEFAULT NULL COMMENT '邮箱',
    `avatar`            VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `status`            TINYINT(1)   DEFAULT 1 COMMENT '状态：0冻结 / 1正常',
    `last_login_ip`     VARCHAR(64)  DEFAULT NULL COMMENT '最后登录IP',
    `last_login_time`   DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 商品表
CREATE TABLE IF NOT EXISTS `t_product` (
                                           `id`                BIGINT(20)    NOT NULL COMMENT '商品ID（雪花ID）',
    `product_name`      VARCHAR(128)  NOT NULL COMMENT '商品名称',
    `description`       VARCHAR(500)  DEFAULT NULL COMMENT '商品描述',
    `price`             DECIMAL(10,2) NOT NULL COMMENT '原价',
    `seckill_price`     DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
    `available_stock`   INT(11)       NOT NULL DEFAULT 0 COMMENT '物理可用库存',
    `total_stock`       INT(11)       NOT NULL DEFAULT 0 COMMENT '总库存',
    `image_url`         VARCHAR(500)  DEFAULT NULL COMMENT '商品主图URL',
    `status`            TINYINT(1)    DEFAULT 1 COMMENT '状态：0下架 / 1上架',
    `version`           INT(11)       NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
-- 商品名索引，支持模糊搜索
    KEY `idx_product_name` (`product_name`),
-- 状态+逻辑删除联合索引，方便查询上架商品
    KEY `idx_status_del` (`status`, `is_deleted`),
-- 秒杀价索引，方便按价格区间查询
    KEY `idx_seckill_price` (`seckill_price`),
-- 逻辑删除+创建时间联合索引，方便按时间范围查询
    KEY `idx_del_create_time` (`is_deleted`, `create_time`),
-- 更新时间索引，方便查询最近更新商品
    KEY `idx_update_time` (`update_time`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 3. 秒杀活动表
CREATE TABLE IF NOT EXISTS `t_activity` (
                                            `id`                BIGINT(20)    NOT NULL COMMENT '活动ID（雪花ID）',
    `activity_name`     VARCHAR(128)  NOT NULL COMMENT '活动名称',
    `product_id`        BIGINT(20)    NOT NULL COMMENT '关联商品ID',
    `seckill_price`     DECIMAL(10,2) NOT NULL COMMENT '秒杀价（冗余）',
    `total_limit`       INT(11)       NOT NULL COMMENT '秒杀总限额',
    `remaining_limit`   INT(11)       NOT NULL COMMENT '剩余限额',
    `start_time`        DATETIME      NOT NULL COMMENT '活动开始时间',
    `end_time`          DATETIME      NOT NULL COMMENT '活动结束时间',
    `per_user_limit`    INT(11)       DEFAULT 1 COMMENT '每用户限购数量',
    `status`            TINYINT(1)    DEFAULT 0 COMMENT '状态：0未开始/1进行中/2已结束/3已取消',
    `version`           INT(11)       NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（防超卖最后防线）',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
-- 关联商品+逻辑删除索引，方便查询指定商品的活动
    KEY `idx_product_id_del` (`product_id`, `is_deleted`),
-- 状态+开始时间联合索引，查询进行中活动
    KEY `idx_status_start_time` (`status`, `start_time`),
-- 开始+结束时间联合索引，查询时间范围内的活动
    KEY `idx_start_end_time` (`start_time`, `end_time`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀活动表';

-- 4. 秒杀日志表
CREATE TABLE IF NOT EXISTS `t_seckill_log` (
                                               `id`                BIGINT(20)   NOT NULL COMMENT '记录ID（雪花ID）',
    `user_id`           BIGINT(20)   NOT NULL COMMENT '用户ID',
    `product_id`        BIGINT(20)   NOT NULL COMMENT '商品ID',
    `activity_id`       BIGINT(20)   NOT NULL COMMENT '活动ID',
    `status`            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '状态：0待处理/1已下单/2已失败/3已取消',
    `fail_reason`       VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    `seckill_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '抢购时间',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
-- 用户+活动唯一索引，确保同一用户同一活动只能秒杀一次（同时覆盖按user_id查询）
    UNIQUE KEY `uk_user_activity` (`user_id`, `activity_id`),
-- 关联活动+逻辑删除索引，方便查询指定活动的秒杀记录
    KEY `idx_activity_id_del` (`activity_id`, `is_deleted`),
-- 状态+逻辑删除索引
    KEY `idx_status_del` (`status`, `is_deleted`),
-- 逻辑删除+秒杀时间索引，方便按时间范围查询
    KEY `idx_del_seckill_time` (`is_deleted`, `seckill_time`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀日志表';

-- 5. 订单表（分库分表，以 user_id 为分片键）
CREATE TABLE IF NOT EXISTS `t_order` (
                                         `id`                BIGINT(20)    NOT NULL COMMENT '订单ID（雪花ID）',
    `order_no`          VARCHAR(64)   NOT NULL COMMENT '订单编号',
    `user_id`           BIGINT(20)    NOT NULL COMMENT '用户ID（分库分表键）',
    `product_id`        BIGINT(20)    NOT NULL COMMENT '商品ID',
    `activity_id`       BIGINT(20)    NOT NULL COMMENT '活动ID',
    `seckill_log_id`    BIGINT(20)    DEFAULT NULL COMMENT '关联秒杀日志ID',
    `order_status`      TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '状态：0待支付/1已支付/2已取消/3已退款',
    `pay_amount`        DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    `pay_time`          DATETIME      DEFAULT NULL COMMENT '支付时间',
    `expire_time`       DATETIME      DEFAULT NULL COMMENT '订单过期时间',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
-- 订单编号唯一索引
    UNIQUE KEY `uk_order_no` (`order_no`),
-- 用户+订单状态+逻辑删除联合索引，最高频查询（查用户待支付订单）
    KEY `idx_user_status_del` (`user_id`, `order_status`, `is_deleted`),
-- 关联活动+逻辑删除索引
    KEY `idx_activity_id_del` (`activity_id`, `is_deleted`),
-- 订单状态+逻辑删除索引，方便后台按状态查询
    KEY `idx_order_status_del` (`order_status`, `is_deleted`),
-- 逻辑删除+创建时间联合索引，方便按时间范围查询
    KEY `idx_del_create_time` (`is_deleted`, `create_time`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 6. 支付流水表
CREATE TABLE IF NOT EXISTS `t_payment` (
    `id`                BIGINT(20)    NOT NULL COMMENT '支付ID（雪花ID）',
    `payment_no`        VARCHAR(64)   NOT NULL COMMENT '支付编号（业务唯一标识）',
    `order_id`          BIGINT(20)    NOT NULL COMMENT '关联订单ID',
    `user_id`           BIGINT(20)    NOT NULL COMMENT '用户ID',
    `pay_amount`        DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `pay_channel`       TINYINT(1)    NOT NULL COMMENT '支付渠道：0微信 / 1支付宝',
    `pay_status`        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '支付状态：0待支付 / 1支付成功 / 2支付失败 / 3已退款',
    `trade_no`          VARCHAR(64)   DEFAULT NULL COMMENT '第三方交易号',
    `pay_time`          DATETIME      DEFAULT NULL COMMENT '支付时间',
    `callback_time`     DATETIME      DEFAULT NULL COMMENT '回调时间',
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_no` (`payment_no`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id_del` (`user_id`, `is_deleted`),
    KEY `idx_pay_status_del` (`pay_status`, `is_deleted`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付流水表';

SET FOREIGN_KEY_CHECKS = 1;