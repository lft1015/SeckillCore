# SeckillCore 活动模块需求文档

> **文档版本**：V1.0
> **所属项目**：SeckillCore 高并发秒杀系统
> **模块名称**：活动模块（Activity Module）
> **目标读者**：产品经理、后端开发、前端开发、测试工程师


## 1. 模块概述

### 1.1 模块定位

活动模块是 SeckillCore 系统的核心业务模块，负责**秒杀活动的管理和库存控制**。活动是连接商品与秒杀执行的中间层——一个活动关联一个商品、定义秒杀价格、活动时间窗口、库存限额和用户限购策略。秒杀模块和订单模块均依赖活动模块提供的数据和服务。

### 1.2 业务目标

| 序号 | 目标 | 说明 |
| :---: | :--- | :--- |
| 1 | **活动信息管理** | 提供活动的查询、分页搜索能力，支撑前端活动列表页和详情页 |
| 2 | **活动库存精确扣减** | 使用乐观锁（版本号）保证高并发场景下活动库存扣减的准确性，杜绝超卖 |
| 3 | **时间窗口控制** | 根据活动的开始/结束时间动态判断活动状态，确保秒杀仅在有效窗口内执行 |
| 4 | **用户限购保障** | 支持每用户限购数量配置，防止单一用户囤货 |
| 5 | **高可用查询** | 活动查询接口需支撑峰值 10 万 QPS，P99 响应时间 < 50ms |

### 1.3 用户角色

| 角色 | 说明 |
| :--- | :--- |
| **普通用户（C端）** | 浏览活动列表、查看活动详情 |
| **管理员（后台）** | 创建/编辑/上下线活动（二期规划） |
| **内部服务** | 秒杀模块通过 Feign 调用活动库存扣减接口 |

### 1.4 模块边界

| 包含内容 | 不包含内容 |
| :--- | :--- |
| 活动分页查询、详情查询 | 活动创建/编辑/删除（二期后台管理） |
| 活动剩余库存扣减（乐观锁） | 活动审批流/风控审核（二期规划） |
| 活动信息 Redis 缓存 | 活动数据分析/报表（独立模块负责） |
| 内部 Feign 库存扣减接口 | 活动推送/消息通知（由通知模块负责） |

### 1.5 模块依赖

| 依赖模块 | 依赖方式 | 说明 |
| :--- | :--- | :--- |
| seckill-common | 直接引用（POM） | 基础组件：BaseEntity、Result、ResultCode、JwtUtil |
| seckill-product | Feign 调用（后期集成） | 活动详情需关联查询商品名称、原价、图片等信息 |
| seckill-user | Feign 调用（后期集成） | 需验证用户身份和状态 |
| MySQL | JDBC/MyBatis Plus | 活动数据持久化 |
| Redis | Jedis/Lettuce | 活动信息缓存 + 库存缓存 |
| Nacos | Spring Cloud Alibaba | 服务注册与配置中心 |


## 2. 功能需求

### 2.1 活动列表查询

| 项目 | 说明 |
| :--- | :--- |
| **功能编号** | AC-001 |
| **功能名称** | 活动列表分页查询 |
| **触发条件** | 用户进入活动列表页 |
| **前置条件** | 无 |
| **后置条件** | 返回分页活动列表 |

**输入字段：**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
| :--- | :--- | :---: | :--- | :--- |
| `page` | Integer | ❌ | ≥1，默认 1 | 当前页码 |
| `size` | Integer | ❌ | 1-100，默认 10 | 每页条数 |
| `status` | Integer | ❌ | 0=未开始 / 1=进行中 / 2=已结束 / 3=已取消 | 按状态筛选 |

**响应信息（ActivityVO）：**

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | Long | 活动ID（雪花ID） |
| `activityName` | String | 活动名称 |
| `productId` | Long | 关联商品ID |
| `seckillPrice` | BigDecimal | 秒杀价格 |
| `totalLimit` | Integer | 秒杀总限额 |
| `remainingLimit` | Integer | 剩余限额 |
| `perUserLimit` | Integer | 每用户限购数量 |
| `startTime` | LocalDateTime | 活动开始时间 |
| `endTime` | LocalDateTime | 活动结束时间 |
| `status` | Integer | 状态：0=未开始 / 1=进行中 / 2=已结束 / 3=已取消 |
| `createTime` | LocalDateTime | 创建时间 |
| `updateTime` | LocalDateTime | 更新时间 |

**业务规则：**

1. 默认按创建时间倒序排列（最新活动在前）
2. 支持按状态（未开始/进行中/已结束/已取消）筛选，不传则查询全部
3. 已逻辑删除的活动（`is_deleted=1`）不返回
4. 前端应根据活动开始/结束时间和服务端状态联合判断，使用倒计时等视觉呈现

**异常场景：**

| 异常场景 | 业务错误码 | HTTP状态码 | 错误信息 |
| :--- | :---: | :---: | :--- |
| 分页参数非法（page<1 或 size<1） | 400 | 400 | 分页参数非法 |
| 每页条数超过最大值（size>100） | 400 | 400 | 每页条数最多100条 |


### 2.2 活动详情查询

| 项目 | 说明 |
| :--- | :--- |
| **功能编号** | AC-002 |
| **功能名称** | 活动详情查询 |
| **触发条件** | 用户点击活动进入详情页 |
| **前置条件** | 活动ID有效 |
| **后置条件** | 返回活动完整信息（含关联商品快照） |

**输入参数（路径参数）：**

| 参数 | 类型 | 必填 | 说明 |
| :--- | :--- | :---: | :--- |
| `id` | Long | ✅ | 活动ID |

**响应信息：** 同 [ActivityVO](#21-活动列表查询)

**业务规则：**

1. 活动不存在或已逻辑删除时，返回「活动不存在」
2. 优先从 Redis 缓存读取（key: `activity:info:{activityId}`，TTL 30 分钟）
3. 缓存未命中时查询数据库并回写缓存（Cache-Aside 模式）
4. 活动状态按以下逻辑自动计算：
   - 当前时间 < `startTime` → 未开始（status=0）
   - `startTime` ≤ 当前时间 ≤ `endTime` → 进行中（status=1）
   - 当前时间 > `endTime` → 已结束（status=2）
5. 状态 3（已取消）由管理员手动设置

**异常场景：**

| 异常场景 | 业务错误码 | HTTP状态码 | 错误信息 |
| :--- | :---: | :---: | :--- |
| 活动不存在 | 3001 | 404 | 活动不存在 |
| 活动已删除 | 3001 | 404 | 活动不存在 |


### 2.3 活动库存扣减（内部接口）

| 项目 | 说明 |
| :--- | :--- |
| **功能编号** | AC-003 |
| **功能名称** | 活动库存扣减 |
| **触发条件** | 秒杀模块执行秒杀时调用，扣减活动剩余库存 |
| **前置条件** | 活动存在且未删除，活动状态为「进行中」，剩余库存 ≥ 1 |
| **后置条件** | 剩余库存 -1，Redis 库存缓存同步更新 |

**输入参数：**

| 参数 | 类型 | 必填 | 校验规则 | 说明 |
| :--- | :--- | :---: | :--- | :--- |
| `activityId` | Long | ✅ | 有效的活动ID | 目标活动 |

> 每次调用默认扣减 1 个库存单位（对应单次秒杀操作），前端 Feign 接口仅需传入活动ID。

**业务规则：**

1. 使用乐观锁（版本号 `version`）保证并发安全，防止超卖
2. 扣减前校验活动状态：仅「进行中」的活动允许扣减
3. 扣减前校验剩余库存：`remaining_limit` ≥ 1
4. 扣减 SQL 逻辑：`SET remaining_limit = remaining_limit - 1, version = version + 1 WHERE id = ? AND version = ? AND remaining_limit >= 1`
5. 乐观锁更新失败（影响行数 = 0）表示并发冲突，返回「库存不足」
6. 若 `remaining_limit` 扣减后归零，活动状态自动变更为「已结束」（status=2）
7. 扣减成功后同步更新 Redis 库存缓存（`activity:stock:{activityId}`）
8. 此接口为内部 Feign 调用接口，不对外暴露

**异常场景：**

| 异常场景 | 业务错误码 | HTTP状态码 | 错误信息 |
| :--- | :---: | :---: | :--- |
| 活动不存在 | 3001 | 404 | 活动不存在 |
| 活动已删除 | 3001 | 404 | 活动不存在 |
| 活动未开始 | 3002 | 400 | 活动未开始 |
| 活动已结束 | 3003 | 400 | 活动已结束 |
| 活动已取消 | 3003 | 400 | 活动已结束 |
| 库存不足 | 2002 | 409 | 库存不足 |


### 2.4 活动库存预热（非功能需求）

| 项目 | 说明 |
| :--- | :--- |
| **功能编号** | AC-004 |
| **功能名称** | 活动库存缓存预热 |
| **触发条件** | 系统启动时 / 活动状态变更为「进行中」时 |
| **前置条件** | 数据库中有状态为「进行中」或「未开始（即将开始）」的活动数据 |
| **后置条件** | 活动信息和库存加载到 Redis 缓存 |

**业务规则：**

1. 系统启动时自动将「进行中」活动加载到 Redis（key: `activity:info:{activityId}`，`activity:stock:{activityId}`）
2. 活动开始时由定时任务或状态变更 Hook 触发缓存预热
3. 缓存预热失败不影响系统启动，降级为懒加载模式（首次请求时回源数据库）


## 3. 数据模型

### 3.1 活动实体（t_activity）

| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :---: | :--- |
| `id` | BIGINT | ✅ | 主键（雪花ID） |
| `activity_name` | VARCHAR(100) | ✅ | 活动名称 |
| `product_id` | BIGINT | ✅ | 关联商品ID（外键关联 t_product.id） |
| `seckill_price` | DECIMAL(10,2) | ✅ | 秒杀价（冗余自商品，避免每次查询需跨服务） |
| `total_limit` | INT | ✅ | 秒杀总限额（活动总可售数量） |
| `remaining_limit` | INT | ✅ | 剩余限额（秒杀进行中实时递减） |
| `per_user_limit` | INT | ✅ | 每用户限购数量 |
| `start_time` | DATETIME | ✅ | 活动开始时间 |
| `end_time` | DATETIME | ✅ | 活动结束时间 |
| `status` | TINYINT | ✅ | 0=未开始 / 1=进行中 / 2=已结束 / 3=已取消 |
| `version` | INT | ✅ | 乐观锁版本号（防超卖最后防线） |
| `create_time` | DATETIME | ✅ | 创建时间 |
| `update_time` | DATETIME | ✅ | 更新时间 |
| `is_deleted` | TINYINT | ✅ | 0=正常 / 1=已删除 |

**索引设计：**

| 索引名 | 字段 | 类型 | 说明 |
| :--- | :--- | :---: | :--- |
| PRIMARY | `id` | 主键 | 聚簇索引 |
| `idx_product_id` | `product_id` | 普通索引 | 按商品查活动 |
| `idx_status` | `status` | 普通索引 | 按状态筛选 |
| `idx_start_time` | `start_time` | 普通索引 | 按开始时间排序 |
| `idx_end_time` | `end_time` | 普通索引 | 按结束时间排序 |

**建表 SQL（参考）：**

```sql
CREATE TABLE IF NOT EXISTS `t_activity` (
    `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
    `activity_name` VARCHAR(100) NOT NULL COMMENT '活动名称',
    `product_id` BIGINT NOT NULL COMMENT '关联商品ID',
    `seckill_price` DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
    `total_limit` INT NOT NULL DEFAULT 0 COMMENT '秒杀总限额',
    `remaining_limit` INT NOT NULL DEFAULT 0 COMMENT '剩余限额',
    `per_user_limit` INT NOT NULL DEFAULT 1 COMMENT '每用户限购数量',
    `start_time` DATETIME NOT NULL COMMENT '活动开始时间',
    `end_time` DATETIME NOT NULL COMMENT '活动结束时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0未开始/1进行中/2已结束/3已取消',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常/1已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_start_time` (`start_time`),
    INDEX `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动表';
```

### 3.2 活动状态流转

```
                 ┌──────────┐
                 │  0.未开始  │
                 └────┬─────┘
                      │ 到达 startTime
                      ▼
  ┌──────────┐     ┌──────────┐
  │ 3.已取消  │◄────│ 1.进行中  │──────┐
  └──────────┘     └────┬─────┘      │
                 管理员取消    │ 到达 endTime  库存归零
                        │          │
                        ▼          ▼
                 ┌──────────────────┐
                 │    2.已结束        │
                 └──────────────────┘
```

**状态规则：**

1. **未开始 → 进行中**：系统时间到达 `startTime` 时自动变更为「进行中」（由缓存读取时动态计算，数据库状态由定时任务或懒更新触发）
2. **进行中 → 已结束**：系统时间超过 `endTime` 或 `remaining_limit` 归零时自动变更为「已结束」
3. **未开始/进行中 → 已取消**：管理员手动取消活动（二期实现）
4. **已结束/已取消**：终态，不可逆转


## 4. 非功能需求

### 4.1 性能需求

| 指标 | 目标值 | 说明 |
| :--- | :---: | :--- |
| 列表查询响应时间 | < 100ms | 正常负载下（P99） |
| 详情查询响应时间（缓存命中） | < 10ms | Redis 缓存命中 |
| 详情查询响应时间（缓存未命中） | < 50ms | 数据库查询 |
| 列表查询 QPS | ≥ 50,000 | 峰值流量 |
| 库存扣减响应时间 | < 50ms | 数据库乐观锁操作 |
| 库存扣减并发量 | ≥ 10,000 TPS | 秒杀峰值 |

### 4.2 缓存策略

| 缓存对象 | 缓存方式 | 过期时间 | Key | 说明 |
| :--- | :--- | :---: | :--- | :--- |
| 活动详情 | Redis String（JSON） | 30分钟 | `activity:info:{activityId}` | Cache-Aside 模式 |
| 活动库存 | Redis String | 30分钟 | `activity:stock:{activityId}` | 扣减时同步更新 |
| 活动列表（分页） | 不缓存 | — | — | 列表查询直接走数据库索引 |

**降级策略：**

| 场景 | 降级方案 |
| :--- | :--- |
| Redis 不可用 | 跳过缓存直接查数据库；库存扣减仍走数据库乐观锁 |
| Redis 恢复后 | 缓存自动重建（懒加载），无需人工干预 |

### 4.3 并发安全

| 需求项 | 方案 | 说明 |
| :--- | :--- | :--- |
| 库存超卖 | 乐观锁（version） | 库存扣减使用 `SET remaining_limit = remaining_limit - 1, version = version + 1 WHERE version = ? AND remaining_limit >= 1` |
| 库存扣减原子性 | SQL 层面条件更新 | `remaining_limit = remaining_limit - 1` 在 WHERE 条件中校验 |
| 缓存一致性 | Cache-Aside | 先更新数据库，再更新/删除缓存；扣减失败不操作缓存 |
| 活动状态一致性 | 动态计算 + 数据库存储 | 展示时根据时间窗口动态计算，库存归零时主动更新数据库状态 |

### 4.4 活动与商品模块的职责分工

| 职责 | 商品模块（Product） | 活动模块（Activity） |
| :--- | :---: | :---: |
| 静态信息（名称、描述、图片） | ✅ 数据源 | 通过 productId 关联查询 |
| 原价 | ✅ `price` | — |
| 秒杀价 | ✅ 存储 | ✅ 冗余存储（`seckill_price`） |
| 物理库存 | ✅ `available_stock` / `total_stock` | — |
| 活动库存 | — | ✅ `total_limit` / `remaining_limit` |
| 时间窗口 | — | ✅ `start_time` / `end_time` |
| 用户限购 | — | ✅ `per_user_limit` |
| 库存扣减 | 秒杀成功后扣减商品物理库存 | 秒杀进行中扣减活动限额（先行） |

> **设计原则**：活动库存（`total_limit` / `remaining_limit`）是秒杀层面的"虚拟库存"，由活动模块管理。商品物理库存（`available_stock`）由商品模块管理，在订单创建成功后异步扣减。两个库存独立管理，避免跨模块事务。

### 4.5 可观测性需求

| 需求项 | 说明 |
| :--- | :--- |
| 健康检查 | 提供 `/actuator/health` 端点，包含数据库连通性检查 |
| 指标监控 | 暴露接口 QPS、响应时间分位数（P50/P90/P99）、缓存命中率 |
| 分布式追踪 | 请求携带 TraceId，跨服务追踪 |
| 告警规则 | 缓存命中率 < 90% 触发告警；库存扣减失败率 > 5% 触发告警；库存归零时发送事件通知 |


## 5. 接口设计

### 5.1 对外 RESTful API

| 接口名称 | 方法 | 路径 | 是否需要认证 | 说明 |
| :--- | :---: | :--- | :---: | :--- |
| 活动列表查询 | GET | `/api/v1/activity/list` | ❌ | 分页 + 状态筛选 |
| 活动详情查询 | GET | `/api/v1/activity/detail/{id}` | ❌ | 缓存加速 |

> **分页规范**：默认 `page=1, size=10`，最大 `size=100`。

### 5.2 内部 Feign 接口

| 接口名称 | 方法 | 路径 | 说明 |
| :--- | :---: | :--- | :--- |
| 活动库存扣减 | POST | `/api/v1/internal/activity/deductStock` | 秒杀模块调用，需携带 `X-Internal-Token` |

> 内部接口在 Gateway 层配置为仅允许内网服务访问。

### 5.3 统一响应格式

```json
{
    "code": 200,
    "message": "操作成功",
    "data": {},
    "timestamp": 1726041600000,
    "traceId": "abc123def456"
}
```

### 5.4 错误码参考

| 错误码 | 含义 | 适用场景 |
| :---: | :--- | :--- |
| 200 | 操作成功 | 正常返回 |
| 400 | 请求参数错误 | 分页参数非法 |
| 404 | 资源不存在 | 活动不存在或已删除 |
| 409 | 数据冲突 | 库存不足（并发冲突） |
| 2001 | 库存不足 | 商品/活动库存耗尽 |
| 3001 | 活动不存在 | 活动ID无效或已删除 |
| 3002 | 活动未开始 | 秒杀在活动开始前触发 |
| 3003 | 活动已结束 | 活动已过期或已取消 |

> 错误码定义统一由 `com.reditickets.common.result.ResultCode` 枚举管理。


## 6. 与一期已完成模块的对齐对照

| 对照项 | 商品模块 | 用户模块 | 活动模块（本文档） |
| :--- | :---: | :---: | :---: |
| 文档版本 | V1.0 | V2.0 | V1.0 |
| 功能编号前缀 | PC- | UC- | AC- |
| 错误码段 | 2000+ | 1000+ / 40000+ | 3000+（复用商品 2001） |
| 对外接口数 | 2 | 7 | 2 |
| 内部 Feign 接口数 | 1 | 2 | 1 |
| 缓存 Key 前缀 | `product:info:` | `user:info:` / `token:` | `activity:info:` / `activity:stock:` |
| 缓存 TTL | 30分钟 | 30分钟 | 30分钟 |
| 乐观锁 | ✅ version | — | ✅ version |
| 分页规范 | page/size 1-100 | page/size 1-100 | page/size 1-100 |
| 逻辑删除 | ✅ is_deleted | ✅ is_deleted | ✅ is_deleted |
| 雪花ID | ✅ | ✅ | ✅ |
| DDL SQL | ✅ | ❌ | ✅ |
| 状态机 | 0下架/1上架 | 0冻结/1正常 | 0未开始/1进行中/2已结束/3已取消 |