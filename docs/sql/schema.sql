-- ============================================
-- SeckillCore 数据库初始化脚本
-- ============================================

-- 用户库
CREATE DATABASE IF NOT EXISTS seckill_user
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

-- 商品库
CREATE DATABASE IF NOT EXISTS seckill_product
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

-- 订单库
CREATE DATABASE IF NOT EXISTS seckill_order
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

-- 秒杀库
CREATE DATABASE IF NOT EXISTS seckill_seckill
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

-- 活动库
CREATE DATABASE IF NOT EXISTS seckill_activity
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;