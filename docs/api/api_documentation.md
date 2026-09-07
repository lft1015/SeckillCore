# SeckillCore API 接口文档

> 本文档描述 SeckillCore 微服务系统的全部 RESTful API 接口。

---

## 1. 网关入口

| 环境 | 地址 |
|:---|:---|
| 开发环境 | `http://localhost:8080` |
| 生产环境 | `https://api.seckillcore.com` |

---

## 2. 用户服务 (seckill-user)

### 2.1 用户注册

- **URL**: `POST /api/user/register`
- **描述**: 新用户注册

### 2.2 用户登录

- **URL**: `POST /api/user/login`
- **描述**: 用户登录获取 Token

---

## 3. 商品服务 (seckill-product)

### 3.1 商品列表

- **URL**: `GET /api/product/list`
- **描述**: 获取秒杀商品列表

### 3.2 商品详情

- **URL**: `GET /api/product/detail/{id}`
- **描述**: 获取商品详情与库存

---

## 4. 秒杀服务 (seckill-seckill)

### 4.1 执行秒杀

- **URL**: `POST /api/seckill/execute`
- **描述**: 用户提交秒杀请求

### 4.2 查询秒杀结果

- **URL**: `GET /api/seckill/result/{orderId}`
- **描述**: 轮询秒杀结果

---

## 5. 订单服务 (seckill-order)

### 5.1 订单列表

- **URL**: `GET /api/order/list`
- **描述**: 查询用户订单列表

### 5.2 订单详情

- **URL**: `GET /api/order/detail/{id}`
- **描述**: 查询订单详情

---

## 6. 活动服务 (seckill-activity)

### 6.1 活动列表

- **URL**: `GET /api/activity/list`
- **描述**: 获取秒杀活动列表

### 6.2 活动详情

- **URL**: `GET /api/activity/detail/{id}`
- **描述**: 获取活动详情与场次信息