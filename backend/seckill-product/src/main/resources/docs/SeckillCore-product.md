# SeckillCore 商品模块需求文档

> **文档版本**：V1.0
> **所属项目**：SeckillCore 高并发秒杀系统
> **模块名称**：商品模块（Product Module）
> **目标读者**：产品经理、后端开发、前端开发、测试工程师


## 1. 模块概述

### 1.1 模块定位

商品模块是 SeckillCore 系统的基础数据模块，负责**商品信息管理、商品查询展示、库存管理**等核心能力。商品是秒杀活动的数据载体，所有秒杀活动均需关联商品信息，是活动模块和秒杀模块的依赖基础。

### 1.2 业务目标

| 序号 | 目标 | 说明 |
| :---: | :--- | :--- |
| 1 | **商品信息管理** | 提供商品的查询、分页搜索能力，支撑前端商品列表页和详情页 |
| 2 | **库存精确扣减** | 使用乐观锁（版本号）保证高并发场景下库存扣减的准确性 |
| 3 | **缓存加速** | 热点商品信息缓存到 Redis，降低数据库压力 |
| 4 | **高可用查询** | 商品查询接口需支撑峰值 10 万 QPS，P99 响应时间 < 50ms |

### 1.3 用户角色

| 角色 | 说明 |
| :--- | :--- |
| **普通用户（C端）** | 浏览商品列表、查看商品详情 |
| **管理员（后台）** | 管理商品上下架（二期规划） |
| **内部服务** | 秒杀模块通过 Feign 调用库存扣减接口 |

### 1.4 模块边界

| 包含内容 | 不包含内容 |
| :--- | :--- |
| 商品分页查询、详情查询 | 商品添加/编辑/删除（二期后台管理） |
| 库存扣减（乐观锁） | 商品分类/标签管理（二期规划） |
| 商品信息 Redis 缓存 | 商品评价/收藏（独立模块负责） |
| 内部 Feign 库存扣减接口 | 商品图片上传（由文件服务负责） |


## 2. 功能需求

### 2.1 商品列表查询

| 项目 | 说明 |
| :--- | :--- |
| **功能编号** | PC-001 |
| **功能名称** | 商品列表分页查询 |
| **触发条件** | 用户进入商品列表页 |
| **前置条件** | 无 |
| **后置条件** | 返回分页商品列表 |

**输入字段：**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
| :--- | :--- | :---: | :--- | :--- |
| `page` | Integer | ❌ | ≥1，默认 1 | 当前页码 |
| `size` | Integer | ❌ | 1-100，默认 10 | 每页条数 |
| `keyword` | String | ❌ | 长度 ≤ 100 | 商品名模糊搜索关键字 |
| `status` | Integer | ❌ | 0=下架 / 1=上架 | 按状态筛选 |

**响应信息（ProductVO）：**

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | Long | 商品ID（雪花ID） |
| `productName` | String | 商品名称 |
| `description` | String | 商品描述 |
| `price` | BigDecimal | 原价 |
| `seckillPrice` | BigDecimal | 秒杀价 |
| `availableStock` | Integer | 可用库存 |
| `totalStock` | Integer | 总库存 |
| `imageUrl` | String | 商品主图URL |
| `status` | Integer | 状态：0=下架 / 1=上架 |
| `createTime` | LocalDateTime | 创建时间 |
| `updateTime` | LocalDateTime | 更新时间 |

**业务规则：**

1. 默认按创建时间倒序排列（最新商品在前）
2. 支持按关键字模糊匹配商品名称
3. 支持按状态（上架/下架）筛选，不传则查询全部
4. 已逻辑删除的商品（`is_deleted=1`）不返回

**异常场景：**

| 异常场景 | 业务错误码 | HTTP状态码 | 错误信息 |
| :--- | :---: | :---: | :--- |
| 分页参数非法（page<1 或 size<1） | 400 | 400 | 分页参数非法 |
| 每页条数超过最大值（size>100） | 400 | 400 | 每页条数最多100条 |


### 2.2 商品详情查询

| 项目 | 说明 |
| :--- | :--- |
| **功能编号** | PC-002 |
| **功能名称** | 商品详情查询 |
| **触发条件** | 用户点击商品进入详情页 |
| **前置条件** | 商品ID有效 |
| **后置条件** | 返回商品完整信息 |

**输入参数（路径参数）：**

| 参数 | 类型 | 必填 | 说明 |
| :--- | :--- | :---: | :--- |
| `id` | Long | ✅ | 商品ID |

**响应信息：** 同 [ProductVO](#21-商品列表查询)

**业务规则：**

1. 商品不存在或已逻辑删除时，返回「商品不存在」
2. 优先从 Redis 缓存读取（key: `product:info:{productId}`，TTL 30 分钟）
3. 缓存未命中时查询数据库并回写缓存

**异常场景：**

| 异常场景 | 业务错误码 | HTTP状态码 | 错误信息 |
| :--- | :---: | :---: | :--- |
| 商品不存在 | 2001 | 404 | 商品不存在 |
| 商品已删除 | 2001 | 404 | 商品不存在 |


### 2.3 库存扣减

| 项目 | 说明 |
| :--- | :--- |
| **功能编号** | PC-003 |
| **功能名称** | 库存扣减（内部接口） |
| **触发条件** | 秒杀模块执行秒杀时调用，扣减商品可用库存 |
| **前置条件** | 商品存在且未删除，库存充足 |
| **后置条件** | 数据库库存扣减成功，Redis 库存缓存同步更新 |

**输入参数：**

| 参数 | 类型 | 必填 | 校验规则 | 说明 |
| :--- | :--- | :---: | :--- | :--- |
| `productId` | Long | ✅ | 有效的商品ID | 目标商品 |
| `quantity` | Integer | ✅ | ≥1 | 扣减数量 |

**业务规则：**

1. 使用乐观锁（版本号 `version`）保证并发安全，防止超卖
2. 扣减前校验可用库存 ≥ 扣减数量
3. 扣减 SQL：`SET available_stock = available_stock - quantity, version = version + 1`
4. 乐观锁更新失败（影响行数 = 0）表示并发冲突，返回「库存不足」
5. 扣减成功后同步更新 Redis 库存缓存（`stock:{productId}`）
6. 此接口为内部 Feign 调用接口，不对外暴露

**异常场景：**

| 异常场景 | 业务错误码 | HTTP状态码 | 错误信息 |
| :--- | :---: | :---: | :--- |
| 商品不存在 | 2001 | 404 | 商品不存在 |
| 商品已删除 | 2001 | 404 | 商品不存在 |
| 库存不足 | 2002 | 409 | 库存不足 |


### 2.4 商品缓存预热（非功能需求）

| 项目 | 说明 |
| :--- | :--- |
| **功能编号** | PC-004 |
| **功能名称** | 热点商品缓存预热 |
| **触发条件** | 系统启动时 / 活动开始前 |
| **前置条件** | 数据库中有已上架的商品数据 |
| **后置条件** | 热点商品信息加载到 Redis 缓存 |

**业务规则：**

1. 系统启动时自动将已上架商品加载到 Redis（key: `product:info:{productId}`）
2. 支持手动触发缓存刷新（管理接口，二期实现）
3. 缓存预热失败不影响系统启动，降级为懒加载模式


## 3. 数据模型

### 3.1 商品实体（t_product）

| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :---: | :--- |
| `id` | BIGINT | ✅ | 主键（雪花ID） |
| `product_name` | VARCHAR(100) | ✅ | 商品名称 |
| `description` | VARCHAR(500) | ❌ | 商品描述 |
| `price` | DECIMAL(10,2) | ✅ | 原价 |
| `seckill_price` | DECIMAL(10,2) | ✅ | 秒杀价 |
| `available_stock` | INT | ✅ | 可用库存（秒杀可售数量） |
| `total_stock` | INT | ✅ | 总库存 |
| `image_url` | VARCHAR(500) | ❌ | 商品主图URL |
| `status` | TINYINT | ✅ | 0=下架 / 1=上架 |
| `version` | INT | ✅ | 乐观锁版本号 |
| `create_time` | DATETIME | ✅ | 创建时间 |
| `update_time` | DATETIME | ✅ | 更新时间 |
| `is_deleted` | TINYINT | ✅ | 0=正常 / 1=已删除 |

**唯一索引：** 商品名称添加普通索引（`idx_product_name`），用于模糊搜索优化。

**建表 SQL（参考）：**

```sql
CREATE TABLE IF NOT EXISTS `t_product` (
    `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `description` VARCHAR(500) COMMENT '商品描述',
    `price` DECIMAL(10,2) NOT NULL COMMENT '原价',
    `seckill_price` DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
    `available_stock` INT NOT NULL DEFAULT 0 COMMENT '可用库存',
    `total_stock` INT NOT NULL DEFAULT 0 COMMENT '总库存',
    `image_url` VARCHAR(500) COMMENT '商品主图URL',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0下架 / 1上架',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 / 1已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_product_name` (`product_name`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';
```


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
| 商品详情 | Redis String（JSON） | 30分钟 | `product:info:{productId}` | Cache-Aside 模式 |
| 商品库存 | Redis String | 30分钟 | `stock:{productId}` | 扣减时同步更新 |
| 商品列表（分页） | 不缓存 | — | — | 列表查询直接走数据库索引 |

**降级策略：**

| 场景 | 降级方案 |
| :--- | :--- |
| Redis 不可用 | 跳过缓存直接查数据库；库存扣减仍走数据库乐观锁 |
| Redis 恢复后 | 缓存自动重建（懒加载），无需人工干预 |

### 4.3 并发安全

| 需求项 | 方案 | 说明 |
| :--- | :--- | :--- |
| 库存超卖 | 乐观锁（version） | 库存扣减使用 `SET version = version + 1 WHERE version = ? AND stock >= ?` |
| 库存扣减原子性 | SQL 层面条件更新 | `available_stock = available_stock - ?` 在 WHERE 条件中校验 |
| 缓存一致性 | Cache-Aside | 先更新数据库，再更新/删除缓存；扣减失败不操作缓存 |

### 4.4 可观测性需求

| 需求项 | 说明 |
| :--- | :--- |
| 健康检查 | 提供 `/actuator/health` 端点，包含数据库连通性检查 |
| 指标监控 | 暴露接口 QPS、响应时间分位数（P50/P90/P99）、缓存命中率 |
| 分布式追踪 | 请求携带 TraceId，跨服务追踪 |
| 告警规则 | 缓存命中率 < 90% 触发告警；库存扣减失败率 > 5% 触发告警 |


## 5. 接口设计

### 5.1 对外 RESTful API

| 接口名称 | 方法 | 路径 | 是否需要认证 | 说明 |
| :--- | :---: | :--- | :---: | :--- |
| 商品列表查询 | GET | `/api/product/list` | ❌ | 分页 + 关键字搜索 + 状态筛选 |
| 商品详情查询 | GET | `/api/product/detail/{id}` | ❌ | 缓存加速 |

> **分页规范**：默认 `page=1, size=10`，最大 `size=100`。

### 5.2 内部 Feign 接口

| 接口名称 | 方法 | 路径 | 说明 |
| :--- | :---: | :--- | :--- |
| 库存扣减 | POST | `/api/product/internal/deductStock` | 秒杀模块调用，需携带 `X-Internal-Token` |

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

**商品模块错误码：**

| 错误码 | 说明 |
| :---: | :--- |
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 500 | 服务器内部错误 |
| 2001 | 商品不存在 |
| 2002 | 库存不足 |


## 6. 依赖关系

### 6.1 上游依赖

| 依赖服务 | 说明 |
| :--- | :--- |
| MySQL 数据库 | 商品数据持久化存储 |
| Redis | 商品信息缓存、库存缓存 |
| Nacos | 服务注册与发现 |

### 6.2 下游调用方

| 调用方模块 | 调用接口 | 说明 |
| :--- | :--- | :--- |
| seckill-activity | 商品查询 | 活动创建时关联商品 |
| seckill-seckill | 库存扣减（Feign） | 秒杀执行时扣减商品库存 |
| seckill-order | 商品查询 | 订单创建时获取商品信息 |

### 6.3 页面依赖

| 前端页面 | 调用接口 |
| :--- | :--- |
| 商品列表页 | GET `/api/product/list` |
| 商品详情页 | GET `/api/product/detail/{id}` |


## 7. 接口示例

### 7.1 商品列表查询

**请求：**
```
GET /api/product/list?page=1&size=10&keyword=手机&status=1
```

**响应：**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "records": [
            {
                "id": 123456,
                "productName": "iPhone 16 Pro",
                "description": "新一代旗舰手机",
                "price": 8999.00,
                "seckillPrice": 6999.00,
                "availableStock": 100,
                "totalStock": 500,
                "imageUrl": "https://cdn.example.com/iphone16.jpg",
                "status": 1,
                "createTime": "2026-09-01T10:00:00",
                "updateTime": "2026-09-10T12:00:00"
            }
        ],
        "total": 1,
        "size": 10,
        "current": 1
    }
}
```

### 7.2 商品详情查询

**请求：**
```
GET /api/product/detail/123456
```

**响应：**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "id": 123456,
        "productName": "iPhone 16 Pro",
        "description": "新一代旗舰手机，A18 Pro 芯片，钛金属设计",
        "price": 8999.00,
        "seckillPrice": 6999.00,
        "availableStock": 100,
        "totalStock": 500,
        "imageUrl": "https://cdn.example.com/iphone16.jpg",
        "status": 1,
        "createTime": "2026-09-01T10:00:00",
        "updateTime": "2026-09-10T12:00:00"
    }
}
```

### 7.3 库存扣减（内部接口）

**请求：**
```
POST /api/product/internal/deductStock
Content-Type: application/json
X-Internal-Token: {内部服务认证Token}

{
    "productId": 123456,
    "quantity": 1
}
```

**响应：**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": null
}
```

**库存不足响应：**
```json
{
    "code": 2002,
    "message": "库存不足",
    "data": null
}
```