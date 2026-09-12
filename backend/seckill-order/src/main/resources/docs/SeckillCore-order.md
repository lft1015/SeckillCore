# SeckillCore 订单模块需求文档

> **文档版本**：V1.0
> **所属项目**：SeckillCore 高并发秒杀系统
> **模块名称**：订单模块（Order Module）
> **目标读者**：产品经理、后端开发、前端开发、测试工程师


## 1. 模块概述

### 1.1 模块定位

订单模块是 SeckillCore 系统的**交易核心模块**，负责秒杀订单的创建、查询、状态管理和支付记录。订单是秒杀流程的最终产出——用户秒杀成功后由秒杀模块异步调用订单模块创建订单，用户在订单模块完成支付、查看订单状态。订单模块同时承载支付流水记录的职责。

### 1.2 业务目标

| 序号 | 目标 | 说明 |
| :---: | :--- | :--- |
| 1 | **快速订单创建** | 秒杀成功后异步创建订单，订单号全局唯一，响应时间 < 50ms |
| 2 | **超时自动取消** | 订单创建后 15 分钟内未支付则自动取消，释放库存 |
| 3 | **订单查询** | 支持按订单ID、订单号、用户ID多维度查询 |
| 4 | **支付流水记录** | 记录每笔支付的完整生命周期（发起→回调→对账） |
| 5 | **分库分表支撑** | 按用户ID水平分片，支撑海量订单数据的写入和查询 |

### 1.3 用户角色

| 角色 | 说明 |
| :--- | :--- |
| **普通用户（C端）** | 查看自己的订单列表和订单详情、发起支付 |
| **内部服务** | 秒杀模块通过 Feign 调用订单创建接口 |
| **支付回调（系统）** | 第三方支付平台异步通知支付结果 |

### 1.4 模块边界

| 包含内容 | 不包含内容 |
| :--- | :--- |
| 订单创建、状态变更 | 退款流程（二期规划） |
| 超时未支付自动取消 | 支付渠道接入（由支付模块负责） |
| 支付流水记录和查询 | 对账系统（独立模块） |
| 按用户ID分库分表 | 订单数据分析/报表（独立模块负责） |

### 1.5 模块依赖

| 依赖模块 | 依赖方式 | 说明 |
| :--- | :--- | :--- |
| seckill-common | 直接引用（POM） | 基础组件：BaseEntity、Result、ResultCode |
| seckill-user | Feign 调用 | 验证用户身份和状态 |
| seckill-product | Feign 调用（后期集成） | 订单详情关联商品名称、图片 |
| seckill-activity | Feign 调用（后期集成） | 订单详情关联活动名称、秒杀价 |
| seckill-seckill | Feign 调用 | 秒杀模块回调创建订单 |
| MySQL | JDBC/MyBatis Plus + ShardingSphere | 分库分表持久化 |
| RocketMQ | Spring Cloud Stream | 延时消息（15分钟未支付自动取消） |
| Redis | Jedis/Lettuce | 订单缓存 |
| Nacos | Spring Cloud Alibaba | 服务注册与配置中心 |


## 2. 功能需求

### 2.1 创建秒杀订单

| 项目 | 说明 |
| :--- | :--- |
| **功能编号** | OD-001 |
| **功能名称** | 创建秒杀订单 |
| **触发条件** | 秒杀模块成功扣减库存后异步调用 |
| **前置条件** | 用户已登录，库存已扣减成功，活动处于进行中 |
| **后置条件** | 订单创建成功，发送 15 分钟延时消息到 RocketMQ |

**输入字段（CreateOrderDTO）：**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
| :--- | :--- | :---: | :--- | :--- |
| `userId` | Long | ✅ | 有效用户ID | 下单用户 |
| `activityId` | Long | ✅ | 有效的活动ID | 秒杀活动 |
| `productId` | Long | ✅ | 有效的商品ID | 秒杀商品 |
| `seckillPrice` | BigDecimal | ✅ | > 0 | 秒杀价格 |
| `quantity` | Integer | ✅ | ≥ 1，默认 1 | 购买数量 |

**响应信息（OrderVO）：**

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | Long | 订单ID（雪花ID） |
| `orderNo` | String | 订单号（业务唯一标识，格式：SK + 时间戳 + 随机串） |
| `userId` | Long | 用户ID |
| `activityId` | Long | 活动ID |
| `productId` | Long | 商品ID |
| `seckillPrice` | BigDecimal | 秒杀价格 |
| `quantity` | Integer | 购买数量 |
| `totalAmount` | BigDecimal | 订单总金额（秒杀价 × 数量） |
| `status` | Integer | 订单状态：0=待支付 / 1=已支付 / 2=已取消 / 3=已退款 |
| `createTime` | LocalDateTime | 创建时间 |
| `payTime` | LocalDateTime | 支付时间（初始为 null） |

**业务规则：**

1. 订单号生成规则：`SK` + `yyyyMMddHHmmss` + 6位随机字符串（保证全局唯一）
2. 订单总金额 = `seckillPrice` × `quantity`
3. 订单初始状态为「待支付」（status=0）
4. 订单过期时间 = `createTime` + 15 分钟
5. 创建成功后发送 RocketMQ 延时消息（延迟级别 15 分钟），用于超时未支付自动取消
6. 订单创建是幂等的：同一秒杀操作重复调用不应产生重复订单
7. 订单表按 `user_id` 进行水平分片（ShardingSphere），保证同一用户的订单落在同一分片

**异常场景：**

| 异常场景 | 业务错误码 | HTTP状态码 | 错误信息 |
| :--- | :---: | :---: | :--- |
| 用户ID为空 | 400 | 400 | 用户ID不能为空 |
| 活动ID为空 | 400 | 400 | 活动ID不能为空 |
| 秒杀价格非法（≤0） | 400 | 400 | 秒杀价格非法 |
| 订单创建失败 | 500 | 500 | 订单创建失败，请稍后重试 |


### 2.2 订单详情查询（按订单ID）

| 项目 | 说明 |
| :--- | :--- |
| **功能编号** | OD-002 |
| **功能名称** | 按订单ID查询订单详情 |
| **触发条件** | 用户查看订单详情页 |
| **前置条件** | 订单ID有效 |
| **后置条件** | 返回订单完整信息 |

**输入参数（路径参数）：**

| 参数 | 类型 | 必填 | 说明 |
| :--- | :--- | :---: | :--- |
| `id` | Long | ✅ | 订单ID |

**响应信息：** 同 [OrderVO](#21-创建秒杀订单)

**业务规则：**

1. 订单不存在时返回「订单不存在」
2. 优先从 Redis 缓存读取，TTL 30 分钟
3. 缓存未命中时查询数据库并回写缓存

**异常场景：**

| 异常场景 | 业务错误码 | HTTP状态码 | 错误信息 |
| :--- | :---: | :---: | :--- |
| 订单不存在 | 5001 | 404 | 订单不存在 |


### 2.3 订单详情查询（按订单号）

| 项目 | 说明 |
| :--- | :--- |
| **功能编号** | OD-003 |
| **功能名称** | 按订单号查询订单详情 |
| **触发条件** | 秒杀成功后前端轮询查订单结果、支付回调时查订单 |
| **前置条件** | 订单号有效 |
| **后置条件** | 返回订单完整信息 |

**输入参数（路径参数）：**

| 参数 | 类型 | 必填 | 说明 |
| :--- | :--- | :---: | :--- |
| `orderNo` | String | ✅ | 订单号 |

**响应信息：** 同 [OrderVO](#21-创建秒杀订单)

**业务规则：**

1. 订单号查询是业务唯一键查询，走索引
2. Redis 缓存 key：`order:no:{orderNo}`，值为订单ID，先查 ID 再查详情

**异常场景：**

| 异常场景 | 业务错误码 | HTTP状态码 | 错误信息 |
| :--- | :---: | :---: | :--- |
| 订单号不存在 | 5001 | 404 | 订单不存在 |


### 2.4 用户订单列表查询

| 项目 | 说明 |
| :--- | :--- |
| **功能编号** | OD-004 |
| **功能名称** | 用户订单分页查询 |
| **触发条件** | 用户进入「我的订单」列表页 |
| **前置条件** | 用户已登录 |
| **后置条件** | 返回分页订单列表 |

**输入字段（OrderListDTO）：**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
| :--- | :--- | :---: | :--- | :--- |
| `page` | Integer | ❌ | ≥1，默认 1 | 当前页码 |
| `size` | Integer | ❌ | 1-100，默认 10 | 每页条数 |
| `userId` | Long | ✅ | 有效用户ID | 查询指定用户的订单 |

**响应信息：** 分页 [OrderVO](#21-创建秒杀订单) 列表

**业务规则：**

1. 按创建时间倒序排列（最新订单在前）
2. 仅返回当前用户自己的订单（需要与 Token 中的 userId 交叉校验）
3. 分库分表环境下，`userId` 是分片键，查询能精确路由到目标分片

**异常场景：**

| 异常场景 | 业务错误码 | HTTP状态码 | 错误信息 |
| :--- | :---: | :---: | :--- |
| 用户ID为空 | 400 | 400 | 用户ID不能为空 |
| 分页参数非法 | 400 | 400 | 分页参数非法 |
| 每页条数超过最大值 | 400 | 400 | 每页条数最多100条 |


### 2.5 订单超时自动取消

| 项目 | 说明 |
| :--- | :--- |
| **功能编号** | OD-005 |
| **功能名称** | 待支付订单超时自动取消 |
| **触发条件** | RocketMQ 延时消息到达 |
| **前置条件** | 订单创建时已投递 15 分钟延时消息 |
| **后置条件** | 超时订单状态变更为「已取消」 |

**业务规则：**

1. 消费 `order-delay-topic` 消息，解析订单号
2. 查询订单当前状态，仅「待支付」（status=0）的订单执行取消
3. 已支付/已取消的订单忽略本次消息（幂等处理）
4. 取消成功后：
   - 订单状态：`order_status` → 2（已取消）
   - 通知活动模块回滚库存（Feign 调用补偿）
   - 通知商品模块回滚库存（Feign 调用补偿）
5. 消息消费失败需支持重试（RocketMQ 消费者自带重试机制）
6. 超时时间可配置（默认 15 分钟，对应 RocketMQ 延时级别 5）

### 2.6 支付流水记录

| 项目 | 说明 |
| :--- | :--- |
| **功能编号** | OD-006 |
| **功能名称** | 支付流水创建与回调处理 |
| **触发条件** | 用户发起支付 / 第三方支付平台异步通知 |
| **前置条件** | 订单存在且状态为「待支付」 |
| **后置条件** | 支付流水记录创建/更新，订单状态变更 |

**业务规则：**

1. **发起支付**：创建支付流水记录（`t_payment`），状态为「待支付」
2. **支付成功回调**：
   - 更新支付流水状态为「支付成功」，记录第三方交易号和支付时间
   - 更新订单状态为「已支付」，记录支付时间
3. **支付失败回调**：更新支付流水状态为「支付失败」
4. 支付金额 = 订单实付金额（`pay_amount`），从订单冗余过来
5. 支付流水同样按 `user_id` 分片

### 2.7 支付流水查询

| 项目 | 说明 |
| :--- | :--- |
| **功能编号** | OD-007 |
| **功能名称** | 按订单ID查询支付流水 |
| **触发条件** | 用户在订单详情页查看支付状态 |
| **前置条件** | 订单ID有效 |
| **后置条件** | 返回支付流水信息 |

**响应信息（PaymentVO）：**

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | Long | 支付流水ID |
| `orderId` | Long | 关联订单ID |
| `orderNo` | String | 关联订单号 |
| `amount` | BigDecimal | 支付金额 |
| `payChannel` | Integer | 支付渠道：0=微信 / 1=支付宝 |
| `payStatus` | Integer | 支付状态：0=待支付 / 1=支付成功 / 2=支付失败 / 3=已退款 |
| `payTime` | LocalDateTime | 支付时间 |
| `createTime` | LocalDateTime | 创建时间 |


## 3. 数据模型

### 3.1 订单实体（t_order）

| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :---: | :--- |
| `id` | BIGINT | ✅ | 主键（雪花ID） |
| `order_no` | VARCHAR(32) | ✅ | 订单号（业务唯一键，格式：SK2024091214300028A3F7B1） |
| `user_id` | BIGINT | ✅ | 用户ID（**分库分表键**） |
| `product_id` | BIGINT | ✅ | 商品ID |
| `activity_id` | BIGINT | ✅ | 活动ID |
| `seckill_log_id` | BIGINT | ❌ | 关联秒杀日志ID（用于数据溯源） |
| `order_status` | TINYINT | ✅ | 0=待支付 / 1=已支付 / 2=已取消 / 3=已退款 |
| `pay_amount` | DECIMAL(10,2) | ✅ | 实付金额（秒杀价 × 数量） |
| `pay_time` | DATETIME | ❌ | 支付时间 |
| `expire_time` | DATETIME | ✅ | 订单过期时间（创建时间 + 15分钟） |
| `create_time` | DATETIME | ✅ | 创建时间 |
| `update_time` | DATETIME | ✅ | 更新时间 |
| `is_deleted` | TINYINT | ✅ | 0=正常 / 1=已删除 |

**索引设计：**

| 索引名 | 字段 | 类型 | 说明 |
| :--- | :--- | :---: | :--- |
| PRIMARY | `id` | 主键 | 聚簇索引 |
| `uk_order_no` | `order_no` | 唯一索引 | 订单号全局唯一 |
| `idx_user_id` | `user_id` | 普通索引 | 分片键 + 用户订单查询 |
| `idx_user_status` | `user_id, order_status` | 组合索引 | 按用户 + 状态筛选 |
| `idx_expire_time` | `expire_time` | 普通索引 | 定时任务扫描过期订单 |

**建表 SQL（参考）：**

```sql
CREATE TABLE IF NOT EXISTS `t_order` (
    `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号（业务唯一键）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（分库分表键）',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `seckill_log_id` BIGINT COMMENT '关联秒杀日志ID',
    `order_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付/1已支付/2已取消/3已退款',
    `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    `pay_time` DATETIME COMMENT '支付时间',
    `expire_time` DATETIME NOT NULL COMMENT '订单过期时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常/1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_status` (`user_id`, `order_status`),
    INDEX `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';
```

### 3.2 支付流水实体（t_payment）

| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :---: | :--- |
| `id` | BIGINT | ✅ | 主键（雪花ID） |
| `payment_no` | VARCHAR(32) | ✅ | 支付编号（业务唯一键） |
| `order_id` | BIGINT | ✅ | 关联订单ID |
| `user_id` | BIGINT | ✅ | 用户ID（**分库分表键**） |
| `pay_amount` | DECIMAL(10,2) | ✅ | 支付金额 |
| `pay_channel` | TINYINT | ✅ | 支付渠道：0=微信 / 1=支付宝 |
| `pay_status` | TINYINT | ✅ | 0=待支付 / 1=支付成功 / 2=支付失败 / 3=已退款 |
| `trade_no` | VARCHAR(64) | ❌ | 第三方交易号 |
| `pay_time` | DATETIME | ❌ | 支付时间 |
| `callback_time` | DATETIME | ❌ | 回调时间 |
| `create_time` | DATETIME | ✅ | 创建时间 |
| `update_time` | DATETIME | ✅ | 更新时间 |
| `is_deleted` | TINYINT | ✅ | 0=正常 / 1=已删除 |

**索引设计：**

| 索引名 | 字段 | 类型 | 说明 |
| :--- | :--- | :---: | :--- |
| PRIMARY | `id` | 主键 | 聚簇索引 |
| `uk_payment_no` | `payment_no` | 唯一索引 | 支付编号全局唯一 |
| `idx_order_id` | `order_id` | 普通索引 | 按订单查支付流水 |
| `idx_user_id` | `user_id` | 普通索引 | 分片键 |

**建表 SQL（参考）：**

```sql
CREATE TABLE IF NOT EXISTS `t_payment` (
    `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
    `payment_no` VARCHAR(32) NOT NULL COMMENT '支付编号（业务唯一键）',
    `order_id` BIGINT NOT NULL COMMENT '关联订单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（分库分表键）',
    `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `pay_channel` TINYINT NOT NULL COMMENT '支付渠道：0微信/1支付宝',
    `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付/1支付成功/2支付失败/3已退款',
    `trade_no` VARCHAR(64) COMMENT '第三方交易号',
    `pay_time` DATETIME COMMENT '支付时间',
    `callback_time` DATETIME COMMENT '回调时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常/1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_no` (`payment_no`),
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';
```

### 3.3 订单状态流转

```
              ┌──────────────────┐
              │   0.待支付        │
              └────┬─────┬───────┘
                   │     │
        用户支付成功│     │ 超时15分钟
                   │     │
                   ▼     ▼
         ┌──────────┐   ┌──────────┐
         │ 1.已支付  │   │ 2.已取消  │
         └────┬─────┘   └──────────┘
              │
       管理员退款│（二期）
              │
              ▼
         ┌──────────┐
         │ 3.已退款  │
         └──────────┘
```

**状态规则：**

1. **待支付 → 已支付**：支付回调成功，记录支付时间和交易号
2. **待支付 → 已取消**：15 分钟未支付，RocketMQ 延时消息触发自动取消
3. **已支付 → 已退款**：管理员发起退款（二期实现）
4. **已支付 / 已取消 / 已退款**：终态，不可逆（除已支付→已退款外）

### 3.4 分库分表策略

| 配置项 | 说明 |
| :--- | :--- |
| 分片键 | `user_id` |
| 分片算法 | `user_id % 分片数`（取模算法） |
| 分库数（一期） | 1 库（`seckill_core`），未来可按需扩展至 N 库 |
| 分表数 | 2 表（`t_order_0`、`t_order_1`） |
| 中间件 | Apache ShardingSphere-JDBC |
| 路由规则 | DQL/DML 语句中携带 `user_id` 时精确路由；无 `user_id` 时广播查询 |

> 一期采用单库多表（逻辑分表）降低运维复杂度，后续流量增长时平滑升级为多库多表。


## 4. 非功能需求

### 4.1 性能需求

| 指标 | 目标值 | 说明 |
| :--- | :---: | :--- |
| 订单创建响应时间 | < 50ms | 同步写入数据库 + 异步发送延时消息 |
| 订单查询响应时间（缓存命中） | < 10ms | Redis 缓存命中 |
| 订单查询响应时间（缓存未命中） | < 50ms | ShardingSphere 精确路由 + 单表查询 |
| 订单创建 TPS | ≥ 10,000 | 秒杀峰值，需支撑批量异步写入 |
| 订单列表查询 QPS | ≥ 10,000 | 每个用户查自己的订单，可分片并行 |

### 4.2 缓存策略

| 缓存对象 | 缓存方式 | 过期时间 | Key | 说明 |
| :--- | :--- | :---: | :--- | :--- |
| 订单详情 | Redis String（JSON） | 30分钟 | `order:info:{orderId}` | Cache-Aside 模式 |
| 订单号→订单ID | Redis String | 30分钟 | `order:no:{orderNo}` | 映射查询加速 |
| 订单列表 | 不缓存 | — | — | 涉及分页和分片，直接查库更简单可靠 |

### 4.3 消息可靠性

| 需求项 | 方案 | 说明 |
| :--- | :--- | :--- |
| 延时消息可靠投递 | RocketMQ 同步发送 + 持久化 | 消息成功落盘后订单创建才算成功 |
| 消费幂等 | 按订单号去重 | 消费前检查订单状态，已支付/已取消则跳过 |
| 消费重试 | RocketMQ 消费者自动重试 | 最多重试 16 次，最终进入死信队列 |
| 死信兜底 | 定时任务扫描 `expire_time < NOW()` 的待支付订单 | 每 1 分钟执行一次，作为消息丢失的最后兜底 |

### 4.4 数据一致性

| 场景 | 一致性要求 | 方案 |
| :--- | :--- | :--- |
| 订单创建 | 最终一致性 | 秒杀模块异步调用，失败重试 |
| 超时取消 → 库存回滚 | 最终一致性 | 取消成功后异步通知活动/商品模块回滚库存，失败重试 |
| 支付回调 → 订单状态 | 强一致性 | 同一数据库事务内更新支付流水 + 订单状态 |

### 4.5 可观测性需求

| 需求项 | 说明 |
| :--- | :--- |
| 健康检查 | 提供 `/actuator/health` 端点，包含数据库和 ShardingSphere 连通性检查 |
| 指标监控 | 订单创建 TPS、订单状态分布、超时取消率、消息消费延迟 |
| 分布式追踪 | 请求携带 TraceId，跨服务追踪（秒杀→订单→支付） |
| 告警规则 | 超时取消率 > 20% 告警；死信队列堆积 > 100 告警；订单创建 TPS 突降告警 |


## 5. 接口设计

### 5.1 对外 RESTful API

| 接口名称 | 方法 | 路径 | 是否需要认证 | 说明 |
| :--- | :---: | :--- | :---: | :--- |
| 用户订单列表 | GET | `/api/v1/order/list` | ✅ | 分页 + 按用户筛选 |
| 订单详情（按ID） | GET | `/api/v1/order/detail/{id}` | ✅ | 缓存加速 |
| 订单详情（按订单号） | GET | `/api/v1/order/no/{orderNo}` | ✅ | 秒杀结果查询 |

> **分页规范**：默认 `page=1, size=10`，最大 `size=100`。

### 5.2 内部 Feign 接口

| 接口名称 | 方法 | 路径 | 说明 |
| :--- | :---: | :--- | :--- |
| 创建秒杀订单 | POST | `/api/v1/internal/order/create` | 秒杀模块调用，异步创建订单 |

> 内部接口在 Gateway 层配置为仅允许内网服务访问。

### 5.3 RocketMQ 消息

| Topic | 消息类型 | 生产者 | 消费者 | 说明 |
| :--- | :---: | :---: | :---: | :--- |
| `order-delay-topic` | 延时消息（15分钟） | 订单模块（创建订单时发送） | 订单模块（自身消费） | 超时未支付自动取消 |
| `order-stock-rollback-topic` | 普通消息 | 订单模块（取消时发送） | 活动模块 / 商品模块 | 库存回滚补偿 |

### 5.4 统一响应格式

```json
{
    "code": 200,
    "message": "操作成功",
    "data": {},
    "timestamp": 1726041600000,
    "traceId": "abc123def456"
}
```

### 5.5 错误码参考

| 错误码 | 含义 | 适用场景 |
| :---: | :--- | :--- |
| 200 | 操作成功 | 正常返回 |
| 400 | 请求参数错误 | 分页参数非法、必填字段缺失 |
| 404 | 资源不存在 | 订单不存在 |
| 500 | 服务器内部错误 | 订单创建失败 |
| 5001 | 订单不存在 | 订单ID或订单号无效 |
| 5002 | 订单取消失败 | 订单状态不允许取消（已支付/已取消） |


## 6. 订单模块特有设计

### 6.1 与秒杀模块的协作流程

```
秒杀模块                          订单模块                         RocketMQ
   │                                │                                │
   │ ── Feign: createOrder ────────▶│                                │
   │                                │ 1. 生成订单号                    │
   │                                │ 2. save(order)                 │
   │                                │ 3. send delay msg ────────────▶│
   │ ◀── Result<OrderVO> ──────────│                                │
   │                                │                                │
   │      ... 15分钟后 ...           │                                │
   │                                │ ◀── delay msg arrives ────────│
   │                                │ 检查状态，待支付→取消              │
   │                                │ Feign: rollback stock ────────▶ 活动/商品模块
```

### 6.2 订单号的非自增设计

订单号使用 **SK + 时间戳 + 随机串** 而非自增ID，原因是：
1. **安全性**：自增ID容易遍历，暴露订单量
2. **分库分表友好**：自增ID在分库场景下需要额外协调
3. **业务标识**：前缀 `SK` 一眼区分秒杀订单，便于日志搜索和监控统计

### 6.3 延时消息兜底机制

RocketMQ 延时消息不能保证 100% 投递（极端情况下可能丢失），因此增加了双重保障：
- **主路径**：RocketMQ 延时消息（15分钟），精确到期触发
- **兜底路径**：定时任务每分钟扫描 `expire_time < NOW() AND order_status = 0` 的订单，批量取消

两者配合保证「待支付订单绝对不会永远挂着」。


## 7. 与已完成模块的对齐对照

| 对照项 | 商品模块 | 用户模块 | 活动模块 | 订单模块（本文档） |
| :--- | :---: | :---: | :---: | :---: |
| 文档版本 | V1.0 | V2.0 | V1.0 | V1.0 |
| 功能编号前缀 | PC- | UC- | AC- | OD- |
| 错误码段 | 2000+ | 1000+ / 40000+ | 3000+ | 5000+ |
| 对外接口数 | 2 | 7 | 2 | 3 |
| 内部 Feign 接口数 | 1 | 2 | 1 | 1 |
| 缓存 Key 前缀 | `product:info:` | `user:info:` / `token:` | `activity:info:` / `activity:stock:` | `order:info:` / `order:no:` |
| 缓存 TTL | 30分钟 | 30分钟 | 30分钟 | 30分钟 |
| 乐观锁 | ✅ version | — | ✅ version | —（订单无需乐观锁） |
| 分页规范 | page/size 1-100 | page/size 1-100 | page/size 1-100 | page/size 1-100 |
| 逻辑删除 | ✅ is_deleted | ✅ is_deleted | ✅ is_deleted | ✅ is_deleted |
| 雪花ID | ✅ | ✅ | ✅ | ✅ |
| DDL SQL | ✅ | ❌ | ✅ | ✅（双表） |
| 状态机 | 0下架/1上架 | 0冻结/1正常 | 0未开始/1进行中/2已结束/3已取消 | 0待支付/1已支付/2已取消/3已退款 |
| 特有技术 | — | JWT + BCrypt | 时间窗口动态状态 | ShardingSphere 分表 + RocketMQ 延时消息 |
| 外部中间件 | MySQL + Redis | MySQL + Redis | MySQL + Redis | MySQL + Redis + RocketMQ |