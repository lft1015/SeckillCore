# SeckillCore 秒杀模块需求文档

> **文档版本**：V1.0
> **所属项目**：SeckillCore 高并发秒杀系统
> **模块名称**：秒杀模块（Seckill Module）
> **目标读者**：产品经理、后端开发、前端开发、测试工程师


## 1. 模块概述

### 1.1 模块定位

秒杀模块是 SeckillCore 系统的**核心执行模块**，负责处理用户秒杀请求的高并发链路。模块采用「Redis 预减库存 + RocketMQ 异步下单」架构，在极低延迟下完成库存校验、防重复校验和异步下单投递，与 Order 模块通过 RocketMQ 异步解耦，**不直接依赖任何其他微服务**。

### 1.2 业务目标

| 序号 | 目标 | 说明 |
| :---: | :--- | :--- |
| 1 | **高吞吐秒杀** | 单机支撑 5000+ QPS，通过 Redis Lua 原子预扣 + 异步下单实现 |
| 2 | **防超卖** | Redis Lua 原子操作 + DB 唯一索引双保险，确保库存永不 < 0 |
| 3 | **防重复秒杀** | 同一用户同一活动仅允许秒杀一次，Redis setnx + DB 唯一索引兜底 |
| 4 | **低延迟响应** | 秒杀接口 P99 响应时间 < 100ms，纯 Redis 操作 + 立即返回排队状态 |
| 5 | **最终一致性** | RocketMQ 可靠消息投递，确保已扣库存的秒杀请求最终生成订单 |

### 1.3 用户角色

| 角色 | 说明 |
| :--- | :--- |
| **普通用户（C端）** | 执行秒杀、查询秒杀结果 |
| **内部服务** | Order 模块通过 RocketMQ 消费秒杀消息创建订单 |

### 1.4 模块边界

| 包含内容 | 不包含内容 |
| :--- | :--- |
| 秒杀执行（校验 → 预扣 → 投递消息） | 活动信息管理（由 activity 模块负责） |
| 秒杀结果查询（缓存 + DB） | 订单创建与支付（由 order 模块负责） |
| 秒杀日志持久化 | 活动缓存写入 Redis（由 activity 模块负责） |
| Redis 库存预扣与回滚 | JWT 鉴权（由 gateway 模块负责） |
| RocketMQ 秒杀消息发送 | 库存对账与补偿（二期规划） |

### 1.5 模块依赖

| 依赖模块 | 依赖方式 | 说明 |
| :--- | :--- | :--- |
| seckill-common | 直接引用（POM） | 基础组件：BaseEntity、Result、ResultCode |
| MySQL | JDBC/MyBatis Plus | 秒杀日志持久化（t_seckill_log） |
| Redis | Jedis/Lettuce | 活动缓存读取、库存预扣、防重复校验、结果缓存 |
| RocketMQ | Spring Cloud Stream | 秒杀成功消息投递（Topic: seckill-order-topic） |
| Nacos | Spring Cloud Alibaba | 服务注册与配置中心 |

> **注意**：秒杀模块不依赖任何 Feign 客户端，不直接调用 user / product / order / activity 微服务。活动信息和库存由 activity 模块预先写入 Redis，秒杀模块仅读取 Redis 缓存。


## 2. 功能需求

### 2.1 执行秒杀

| 项目 | 说明 |
| :--- | :--- |
| **功能编号** | SK-001 |
| **功能名称** | 执行秒杀 |
| **触发条件** | 用户在活动进行中点击「立即秒杀」按钮 |
| **前置条件** | 活动缓存已由 activity 模块写入 Redis（`activity:info:{activityId}` + `seckill:stock:{activityId}`） |
| **后置条件** | Redis 库存 -1，秒杀日志写入 DB，RocketMQ 消息投递，返回「排队中」状态 |

**输入字段：**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
| :--- | :--- | :---: | :--- | :--- |
| `activityId` | Long | ✅ | 有效的活动ID | 秒杀目标活动 |
| `userId` | Long | ✅ | 有效用户ID | 执行秒杀的用户 |

**响应信息（SeckillResultVO）：**

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `seckillLogId` | Long | 秒杀日志ID（用于后续轮询查询结果） |
| `activityId` | Long | 活动ID |
| `productId` | Long | 商品ID（从活动缓存中获取） |
| `seckillPrice` | BigDecimal | 秒杀价格（从活动缓存中获取） |
| `status` | Integer | 秒杀状态：0=排队中 / 1=已下单 / 2=已失败 |
| `createTime` | LocalDateTime | 秒杀时间 |

**业务流程（共 7 步）：**

```
Step 1 ─ 读取活动缓存
  GET activity:info:{activityId}
  ├─ 不存在 → 返回「活动不存在」
  ├─ status != 1（非进行中）→ 返回对应错误
  └─ 校验当前时间是否在 startTime ~ endTime 之间
      ├─ 未开始 → 返回「活动未开始」
      └─ 已结束 → 返回「活动已结束」

Step 2 ─ 防重复秒杀
  SETNX seckill:record:{activityId}:{userId} "1"
  ├─ false → 返回「请勿重复秒杀」
  └─ true  → 继续

Step 3 ─ Redis Lua 原子预扣库存
  EVAL seckill_deduct.lua seckill:stock:{activityId}
  ├─ -1（Key 不存在）→ 删除 record Key → 返回「活动不存在」
  ├─  0（库存不足）  → 删除 record Key → 返回「库存不足」
  └─  1（扣减成功）  → 继续

Step 4 ─ 更新防重复标记
  SET seckill:record:{activityId}:{userId} "{seckillLogId}" EX {活动剩余秒数}

Step 5 ─ 插入秒杀日志（DB）
  INSERT INTO t_seckill_log (user_id, product_id, activity_id, status, seckill_time)
  VALUES (userId, productId, activityId, 0, now())
  ├─ DuplicateKeyException（唯一索引冲突）
  │   → INCR seckill:stock:{activityId}（回滚库存）
  │   → DEL seckill:record:{activityId}:{userId}
  │   → 返回「请勿重复秒杀」
  └─ 插入成功 → 继续

Step 6 ─ 发送 RocketMQ 异步下单消息
  syncSend("seckill-order-topic", SeckillOrderMessage)
  ├─ 发送成功 → 继续
  └─ 发送失败 → 记录 ERROR 日志，不阻塞用户请求，继续返回排队中

Step 7 ─ 写结果缓存 + 返回
  SET seckill:result:{seckillLogId} → SeckillResultVO JSON
  返回 Result.success("排队中，请稍后查询结果", vo)
```

**业务规则：**

1. 活动信息完全从 Redis 缓存读取，不查询数据库
2. 库存扣减使用 Redis Lua 脚本保证原子性，单个命令内完成「检查 → 扣减」
3. 防重复使用 Redis setnx，Step 2 仅设占位值，Step 4 替换为秒杀日志ID
4. 秒杀日志插入使用数据库唯一索引 `uk_user_activity (user_id, activity_id)` 做最终防重复兜底
5. RocketMQ 采用同步发送（syncSend），确保消息不丢失
6. MQ 发送失败不回滚库存（避免用户看到「秒杀成功但没订单」），由补偿机制处理
7. 秒杀成功后立即返回「排队中」状态，前端需轮询查询最终结果

**异常场景：**

| 异常场景 | 业务错误码 | HTTP状态码 | 错误信息 |
| :--- | :---: | :---: | :--- |
| activityId 为空 | 400 | 400 | 活动ID不能为空 |
| userId 为空 | 400 | 400 | 用户ID不能为空 |
| 活动不存在 | 3001 | 404 | 活动不存在 |
| 活动未开始 | 3002 | 400 | 活动未开始 |
| 活动已结束 | 3003 | 400 | 活动已结束 |
| 库存不足 | 2002 | 409 | 库存不足 |
| 重复秒杀 | 4001 | 409 | 请勿重复秒杀 |


### 2.2 查询秒杀结果

| 项目 | 说明 |
| :--- | :--- |
| **功能编号** | SK-002 |
| **功能名称** | 查询秒杀结果 |
| **触发条件** | 用户秒杀返回「排队中」后，前端轮询查询最终结果 |
| **前置条件** | 已调用秒杀执行接口并获得 seckillLogId |
| **后置条件** | 返回秒杀最终结果（成功/失败/排队中） |

**输入参数（路径参数）：**

| 参数 | 类型 | 必填 | 说明 |
| :--- | :--- | :---: | :--- |
| `seckillLogId` | Long | ✅ | 秒杀日志ID |

> 注意：路径参数沿用设计文档中的命名 `orderId`，实际取值为 `seckillLogId`（秒杀执行时尚未生成订单）。

**响应信息（SeckillResultVO）：**

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `seckillLogId` | Long | 秒杀日志ID |
| `orderId` | Long | 订单ID（status=1 时有值，由 Order 服务回写） |
| `orderNo` | String | 订单号（status=1 时有值，由 Order 服务回写） |
| `activityId` | Long | 活动ID |
| `productId` | Long | 商品ID |
| `seckillPrice` | BigDecimal | 秒杀价格 |
| `status` | Integer | 0=排队中 / 1=已下单（秒杀成功）/ 2=已失败 |
| `failReason` | String | 失败原因（status=2 时有值） |
| `createTime` | LocalDateTime | 秒杀时间 |

**业务规则：**

1. 优先从 Redis 缓存读取（key: `seckill:result:{seckillLogId}`，TTL 30 分钟）
2. 缓存命中且 status != 0 → 直接返回
3. 缓存未命中或 status=0（仍在排队）→ 查询 t_seckill_log 数据库
4. status=1（已下单）时，订单号由 Order 服务消费 RocketMQ 后回写；若 Order 尚未消费，status 仍为 0
5. 查询到 status=1 或 status=2 的结果后，回写 Redis 缓存

**异常场景：**

| 异常场景 | 业务错误码 | HTTP状态码 | 错误信息 |
| :--- | :---: | :---: | :--- |
| seckillLogId 无效 | 400 | 400 | 参数无效 |
| 秒杀记录不存在 | 4002 | 404 | 秒杀记录不存在 |


## 3. 数据模型

### 3.1 秒杀日志实体（t_seckill_log）

| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :---: | :--- |
| `id` | BIGINT(20) | ✅ | 记录ID（雪花ID） |
| `user_id` | BIGINT(20) | ✅ | 用户ID |
| `product_id` | BIGINT(20) | ✅ | 商品ID |
| `activity_id` | BIGINT(20) | ✅ | 活动ID |
| `status` | TINYINT(1) | ✅ | 状态：0=待处理 / 1=已下单 / 2=已失败 / 3=已取消 |
| `fail_reason` | VARCHAR(255) | ❌ | 失败原因 |
| `seckill_time` | DATETIME | ✅ | 抢购时间 |
| `create_time` | DATETIME | ✅ | 创建时间 |
| `update_time` | DATETIME | ✅ | 更新时间 |
| `is_deleted` | TINYINT(1) | ✅ | 逻辑删除 |

**状态流转：**

```
┌──────────┐
│ 0 排队中  │  ← 执行秒杀时 INSERT
└─────┬────┘
      │
  ┌───┼───┐
  ▼   ▼   ▼
┌──────┐ ┌──────┐ ┌──────┐
│1 已下单│ │2 已失败│ │3 已取消│
└──────┘ └──────┘ └──────┘
Order消费  超卖/异常  用户取消/超时
后更新     时更新     时更新
```

> 状态 1、2、3 由 Order 服务的 RocketMQ 消费者回写，秒杀模块只负责写入状态 0。

**索引设计：**

| 索引名 | 字段 | 类型 | 说明 |
| :--- | :--- | :---: | :--- |
| PRIMARY | `id` | 主键 | 聚簇索引 |
| `uk_user_activity` | `user_id, activity_id` | 唯一索引 | 防同一用户重复秒杀同一活动 |
| `idx_activity_id_del` | `activity_id, is_deleted` | 普通索引 | 按活动查询有效秒杀记录 |
| `idx_status_del` | `status, is_deleted` | 普通索引 | 按状态筛选有效记录（补偿任务扫描） |
| `idx_del_seckill_time` | `is_deleted, seckill_time` | 普通索引 | 按时间范围查询有效记录 |

**建表 SQL（参考）：**

```sql
CREATE TABLE IF NOT EXISTS `t_seckill_log` (
    `id` BIGINT(20) NOT NULL COMMENT '记录ID（雪花ID）',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `product_id` BIGINT(20) NOT NULL COMMENT '商品ID',
    `activity_id` BIGINT(20) NOT NULL COMMENT '活动ID',
    `status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '状态：0待处理/1已下单/2已失败/3已取消',
    `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    `seckill_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '抢购时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_activity` (`user_id`, `activity_id`),
    KEY `idx_activity_id_del` (`activity_id`, `is_deleted`),
    KEY `idx_status_del` (`status`, `is_deleted`),
    KEY `idx_del_seckill_time` (`is_deleted`, `seckill_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀日志表';
```

### 3.2 RocketMQ 消息体（SeckillOrderMessage）

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `seckillLogId` | Long | 秒杀日志ID（关联幂等，Order 侧防重复消费） |
| `userId` | Long | 用户ID |
| `activityId` | Long | 活动ID |
| `productId` | Long | 商品ID |
| `seckillPrice` | BigDecimal | 秒杀价格 |
| `quantity` | Integer | 购买数量（默认 1） |
| `createTime` | LocalDateTime | 秒杀时间 |

**消息投递参数：**

| 参数 | 值 | 说明 |
| :--- | :--- | :--- |
| Topic | `seckill-order-topic` | 秒杀下单主题 |
| Tag | `order-create` | 订单创建标签 |
| 发送方式 | syncSend | 同步发送，确保不丢失 |


## 4. Redis 数据结构设计

### 4.1 Key 命名规范

所有秒杀相关的 Redis Key 使用 `seckill:` 前缀，按功能划分子命名空间：

| Key 模式 | 数据类型 | 说明 | TTL |
| :--- | :--- | :--- | :--- |
| `seckill:stock:{activityId}` | String（数字） | 活动库存计数器 | 活动结束后 1h |
| `seckill:record:{activityId}:{userId}` | String | 用户秒杀标记（值=秒杀日志ID） | 活动剩余秒数 |
| `seckill:result:{seckillLogId}` | String（JSON） | 秒杀结果缓存（SeckillResultVO） | 30 分钟 |

### 4.2 活动缓存（由 activity 模块写入，seckill 只读）

| Key 模式 | 数据类型 | 说明 |
| :--- | :--- | :--- |
| `activity:info:{activityId}` | String（JSON） | 活动信息（activityId, productId, seckillPrice, startTime, endTime, status） |

> **约定**：activity 模块在活动上线时将活动信息写入 Redis。seckill 模块假设此 Key 已存在，不存在则返回「活动不存在」(3001)。

### 4.3 Lua 预扣库存脚本

脚本文件路径：`resources/lua/seckill_deduct.lua`

```lua
-- KEYS[1] = seckill:stock:{activityId}
-- ARGV[1] = 扣减数量（通常为 1）
-- 返回值：1=成功，0=库存不足，-1=Key 不存在

local stockKey = KEYS[1]
local deductNum = tonumber(ARGV[1])

if redis.call('EXISTS', stockKey) == 0 then
    return -1
end

local currentStock = tonumber(redis.call('GET', stockKey))
if currentStock == nil or currentStock < deductNum then
    return 0
end

redis.call('DECRBY', stockKey, deductNum)
return 1
```

### 4.4 数据流

```
活动开始前（activity 模块完成）：
  SET activity:info:{activityId} → JSON 活动信息
  SET seckill:stock:{activityId} → 库存数

秒杀执行时（seckill 模块）：
  GET activity:info:{activityId}                     → 校验活动
  SETNX seckill:record:{activityId}:{userId} "1"    → 防重复
  EVAL seckill_deduct.lua                            → 原子预扣
  SET seckill:record:{activityId}:{userId} "{logId}" → 更新标记
  INSERT t_seckill_log                               → 持久化
  SEND RocketMQ Message                              → 异步下单
  SET seckill:result:{seckillLogId}                  → 结果缓存

秒杀结果查询时：
  GET seckill:result:{seckillLogId}  → 命中直接返回
  未命中 → SELECT t_seckill_log      → 组装返回 + 回写缓存
```


## 5. 非功能需求

### 5.1 性能需求

| 指标 | 目标值 | 说明 |
| :--- | :---: | :--- |
| 秒杀执行响应时间 | < 100ms | P99，纯 Redis 操作 + 异步投递 |
| 结果查询响应时间（缓存命中） | < 10ms | Redis 缓存命中 |
| 结果查询响应时间（缓存未命中） | < 50ms | 数据库查询 |
| 秒杀执行 QPS | ≥ 5,000 | 单机峰值 |
| 结果查询 QPS | ≥ 10,000 | 前端轮询频率高 |
| Redis 库存预扣并发 | ≥ 10,000 TPS | Lua 脚本单线程执行 |

### 5.2 缓存策略

| 缓存对象 | 缓存方式 | 过期时间 | Key | 说明 |
| :--- | :--- | :---: | :--- | :--- |
| 秒杀结果 | Redis String（JSON） | 30 分钟 | `seckill:result:{seckillLogId}` | 首次写入在秒杀执行时，Order 消费后更新 |
| 防重复标记 | Redis String | 活动剩余秒数 | `seckill:record:{activityId}:{userId}` | 活动结束后自动过期 |
| 活动库存 | Redis String | 活动结束后 1h | `seckill:stock:{activityId}` | 由 activity 模块初始化，seckill 模块扣减 |

**降级策略：**

| 场景 | 降级方案 |
| :--- | :--- |
| Redis 不可用 | 秒杀接口直接熔断，返回「系统繁忙，请稍后重试」 |
| Redis 恢复后 | 需要 activity 模块重新预热活动缓存和库存 |
| RocketMQ 不可用 | 秒杀接口熔断，不允许扣库存后消息丢失 |
| DB 不可用 | 秒杀日志无法持久化，但 Redis 预扣仍可执行（极端场景，不推荐） |

### 5.3 异常回滚策略

| 失败步骤 | 已执行操作 | 回滚动作 |
| :--- | :--- | :--- |
| Step 1 校验失败 | 无 | 直接返回错误，无需回滚 |
| Step 2 重复秒杀 | 无 | 直接返回错误，无需回滚 |
| Step 3 库存不足 | Step 2 SETNX 成功 | `DEL seckill:record:{activityId}:{userId}` |
| Step 5 DB 插入冲突 | Step 2,3,4 成功 | `INCR seckill:stock:{activityId}`（回滚库存）+ 删 record Key |
| Step 6 MQ 发送失败 | Step 2,3,4,5 成功 | **不回滚**，记录 ERROR 日志，由补偿机制处理 |

### 5.4 补偿机制（二期规划）

| 补偿场景 | 方案 |
| :--- | :--- |
| MQ 发送失败 | 定时任务扫描 status=0 且超过 5 分钟的记录，重发 MQ 消息或标记失败并回滚库存 |
| 库存对账 | 定时任务对比 Redis 库存与 DB 库存，不一致时以 DB 为准修复 Redis |

### 5.5 Sentinel 流控规则（后续配置）

| 资源名 | 规则类型 | 阈值 | 说明 |
| :--- | :--- | :---: | :--- |
| `seckillExecute` | QPS 限流 | 5,000 | 单机秒杀接口 QPS 上限 |
| `seckillExecute` | 熔断降级 | 慢调用比例 50% | 超过 50% 请求 RT > 200ms 时熔断 |
| `seckillResult` | QPS 限流 | 10,000 | 结果查询接口 QPS 上限 |


## 6. 测试用例

### 6.1 正常场景

| 编号 | 场景 | 预期结果 |
| :---: | :--- | :--- |
| TC-01 | 活动进行中，有库存，首次秒杀 | status=0（排队中），返回 seckillLogId |
| TC-02 | 轮询查询结果，Order 已消费 | status=1（已下单），含订单号 |
| TC-03 | 轮询查询结果，Order 未消费 | status=0（排队中） |

### 6.2 异常场景

| 编号 | 场景 | 预期 code | 预期 message |
| :---: | :--- | :---: | :--- |
| TC-04 | 活动ID不存在 | 3001 | 活动不存在 |
| TC-05 | 活动未开始 | 3002 | 活动未开始 |
| TC-06 | 活动已结束 | 3003 | 活动已结束 |
| TC-07 | 库存不足 | 2002 | 库存不足 |
| TC-08 | 同一用户重复秒杀同一活动 | 4001 | 请勿重复秒杀 |
| TC-09 | 传入无效 activityId（null） | 400 | 活动ID不能为空 |

### 6.3 并发场景

| 编号 | 场景 | 预期结果 |
| :---: | :--- | :--- |
| TC-10 | 100 并发抢 50 库存 | 50 成功（status=0），50 失败（库存不足） |
| TC-11 | 1000 并发，单用户多次请求 | 每个用户最多成功 1 次 |
| TC-12 | 库存为 1，2 并发请求 | 1 成功 + 1 失败，Redis 库存最终 = 0 |


## 7. 附录

### 7.1 错误码汇总

| 业务错误码 | HTTP状态码 | 对应场景 | 来源 |
| :---: | :---: | :--- | :--- |
| 2002 | 409 | 库存不足 | 与 product 模块保持一致 |
| 3001 | 404 | 活动不存在 | 与 activity 模块保持一致 |
| 3002 | 400 | 活动未开始 | 与 activity 模块保持一致 |
| 3003 | 400 | 活动已结束 | 与 activity 模块保持一致 |
| 4001 | 409 | 请勿重复秒杀 | 秒杀模块自有 |
| 4002 | 404 | 秒杀记录不存在 | 秒杀模块自有 |

### 7.2 实现清单

**需新建文件：**

| 文件 | 路径 | 说明 |
| :--- | :--- | :--- |
| `seckill_deduct.lua` | `src/main/resources/lua/` | Redis Lua 预扣库存脚本 |
| `SeckillOrderMessage.java` | `message/` | RocketMQ 消息体 |

**需改造文件：**

| 文件 | 改造内容 |
| :--- | :--- |
| `SeckillServiceImpl.java` | 实现 executeSeckill() 和 getSeckillResult() 完整逻辑 |
| `SeckillController.java` | 调整路径参数、添加参数校验、添加 Sentinel 注解 |
| `application.yml` | 补充 RocketMQ Producer 配置 |

### 7.3 本模块不负责（由其他模块完成）

| 功能 | 所属模块 | 说明 |
| :--- | :--- | :--- |
| 活动缓存写入 Redis | seckill-activity | 活动发布/开始时将信息缓存到 Redis |
| 库存初始化到 Redis | seckill-activity | 活动开始时将 totalLimit 加载到 Redis |
| RocketMQ 消费下单 | seckill-order | 消费消息、创建订单、回写秒杀日志 status=1 |
| JWT 鉴权 | seckill-gateway | 网关层统一校验 Token |
| Sentinel Dashboard 配置 | 运维 | 流控规则的持久化和管理 |