# 项目设计文档：SeckillCore —— 高并发秒杀系统

> **项目名称**：SeckillCore
> **文档版本**：V3.0
> **JDK 版本**：17 LTS
> **适用场景**：高并发、微服务、分布式系统设计参考
> **核心主题**：秒杀场景下的流量削峰、防超卖、最终一致性
> **文档状态**：完整版（含开发里程碑与实施计划）


## 1. 项目概述

### 1.1 项目背景
SeckillCore 是一个旨在承载**百万级并发流量**的电商秒杀系统。系统需在极短时间内（如双11整点）处理海量用户的抢购请求，确保服务不崩溃，同时严格保证数据的一致性和准确性。

### 1.2 核心业务目标
- **高可用**：峰值流量（设计目标：10万 QPS）下系统核心链路不断服。
- **防超卖**：库存扣减精准，数据库物理库存绝不为负数。
- **低延迟**：用户点击抢购后，前端响应时间控制在 2 秒以内（异步轮询结果除外）。
- **最终一致性**：允许订单状态的短暂延迟，但保证最终数据绝对一致。


## 2. 技术架构选型

| 组件类型 | 技术选型 | 版本 |
| :--- | :--- | :--- |
| **基础框架** | Spring Boot | 3.2.x |
| **微服务框架** | Spring Cloud | 2023.0.x |
| **服务治理** | Spring Cloud Alibaba | 2023.0.x |
| **注册/配置中心** | Nacos | **3.2.3** 或 2.4.3 |
| **网关** | Spring Cloud Gateway | 4.1.x |
| **远程调用** | OpenFeign + Dubbo | Dubbo 3.2.x |
| **消息队列** | **Apache RocketMQ** | 5.3.1（服务端）/ 2.3.4（Spring Boot Starter） |
| **分布式缓存** | **Redis** (Cluster集群) | 7.0.x |
| **持久化存储** | MySQL | 8.0.33 |
| **分库分表** | ShardingSphere-JDBC | **5.5.2** |
| **熔断限流** | Sentinel | **1.8.8** |
| **分布式事务** | Seata | 2.3.0 |
| **链路观测** | SkyWalking | 9.7.0（OAP）/ 8.15.0+（Agent） |
| **容器编排** | Kubernetes | 1.28+ |


## 3. 微服务模块划分

项目采用 Maven 多模块结构，按业务领域垂直拆分：

```text
seckill-core/
├── pom.xml                                     # 根聚合 POM（IDE 识别用）
├── .gitignore
│
├── backend/                                    # 🔧 后端工程（Java 17 + Spring Boot 3.2）
│   ├── pom.xml                                 # Maven 父 POM（依赖管理）
│   ├── seckill-common/                         # 公共模块（工具类、常量、异常）
│   ├── seckill-gateway/                        # 网关（端口 8080，Spring Cloud Gateway）
│   ├── seckill-interface/                      # 接口聚合层（端口 8081，OpenFeign）
│   ├── seckill-user/                           # 用户服务（端口 8082，MySQL）
│   ├── seckill-product/                        # 商品服务（端口 8083，MySQL + Redis）
│   ├── seckill-order/                          # 订单服务（端口 8084，MySQL + ShardingSphere）
│   ├── seckill-seckill/                        # 秒杀服务（端口 8085，MySQL + Redis + RocketMQ + Sentinel）
│   ├── seckill-activity/                       # 活动服务（端口 8086，MySQL + Redis）
│   └── seckill-launcher/                       # 统一启动器（端口 8080，开发环境一键启动）
│
├── frontend/                                   # 🎨 前端工程（Vue 3 + Vite）
│   ├── seckill-portal/                         # 用户端（端口 3000）
│   │   ├── src/
│   │   │   ├── App.vue
│   │   │   └── main.js
│   │   ├── index.html
│   │   ├── package.json
│   │   ├── tsconfig.json
│   │   └── vite.config.ts
│   ├── seckill-admin/                          # 管理端（端口 3001）
│   │   ├── src/
│   │   │   ├── App.vue
│   │   │   └── main.js
│   │   ├── index.html
│   │   ├── package.json
│   │   ├── tsconfig.json
│   │   └── vite.config.ts
│   └── seckill-monitor/                        # 监控端（端口 3002）
│       ├── src/
│       │   ├── App.vue
│       │   └── main.js
│       ├── index.html
│       ├── package.json
│       ├── tsconfig.json
│       └── vite.config.ts
│
├── deploy/                                     # 🚀 部署配置
│   ├── docker/
│   │   ├── backend/Dockerfile                  # 后端 Spring Boot 镜像
│   │   └── frontend/Dockerfile                 # 前端 Nginx 镜像
│   └── k8s/
│       └── gateway-deployment.yaml             # K8s 部署示例
│
└── docs/                                       # 📄 文档
    ├── SeckillCore_Project_Design.md           # 主设计文档
    ├── api/
    │   └── api_documentation.md                # API 接口文档
    └── sql/
        └── schema.sql                          # 数据库初始化脚本
```

### 3.1 后端模块职责

| 模块 | 端口 | 核心依赖 | 职责说明 |
|:---|:---|:---|:---|
| **seckill-common** | - | Lombok, Jackson | 公共工具类、常量定义、统一异常处理 |
| **seckill-gateway** | 8080 | Spring Cloud Gateway, Nacos, LoadBalancer | 统一入口，路由转发，请求过滤 |
| **seckill-interface** | 8081 | Spring Web, Nacos, OpenFeign | BFF 聚合层，编排各微服务接口 |
| **seckill-user** | 8082 | Spring Web, Nacos, MyBatis Plus, MySQL | 用户注册/登录/信息管理 |
| **seckill-product** | 8083 | Spring Web, Nacos, MyBatis Plus, MySQL, Redis | 商品查询、库存缓存预热 |
| **seckill-order** | 8084 | Spring Web, Nacos, MyBatis Plus, MySQL, ShardingSphere | 订单创建/查询，分库分表 |
| **seckill-seckill** | 8085 | Spring Web, Nacos, MyBatis Plus, MySQL, Redis, RocketMQ, Sentinel | 核心秒杀逻辑、库存扣减、流量削峰 |
| **seckill-activity** | 8086 | Spring Web, Nacos, MyBatis Plus, MySQL, Redis | 秒杀活动配置、场次管理 |
| **seckill-launcher** | 8080 | 聚合以上所有模块 | 开发环境一键启动所有服务（不含 Gateway） |

### 3.2 前端模块职责

| 模块 | 端口 | 框架 | 说明 |
|:---|:---|:---|:---|
| **seckill-portal** | 3000 | Vue 3 + Vite | 用户端，商品浏览、秒杀抢购、订单查询 |
| **seckill-admin** | 3001 | Vue 3 + Vite | 管理端，商品管理、活动配置、数据报表 |
| **seckill-monitor** | 3002 | Vue 3 + Vite + ECharts | 监控端，实时 QPS、系统健康度、告警大盘 |

### 3.3 数据库设计

按业务垂直拆分，每个微服务独享一个数据库：

| 数据库 | 所属模块 | 说明 |
|:---|:---|:---|
| `seckill_user` | seckill-user | 用户表、登录日志 |
| `seckill_product` | seckill-product | 商品表、库存表 |
| `seckill_order` | seckill-order | 订单表（ShardingSphere 分库分表） |
| `seckill_seckill` | seckill-seckill | 秒杀记录表、幂等校验表 |
| `seckill_activity` | seckill-activity | 活动表、场次表、活动商品关联表 |

## 4. 快速启动

### 4.1 环境要求

| 组件 | 版本 | 说明 |
|:---|:---|:---|
| JDK | 17+ | 编译与运行 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| MySQL | 8.0+ | 持久化存储 |
| Redis | 7.0+ | 缓存 |
| Nacos | 2.4.3+ | 注册/配置中心 |
| RocketMQ | 5.3.1+ | 消息队列 |

### 4.2 后端启动

```bash
# 方式一：统一启动（开发环境推荐，一个 JVM 启动所有服务）
cd backend
mvn clean package -pl seckill-launcher -am -DskipTests
java -jar seckill-launcher/target/seckill-launcher-1.0-SNAPSHOT.jar

# 方式二：逐个启动（生产环境模式）
cd backend
mvn clean package -DskipTests
java -jar seckill-gateway/target/seckill-gateway-1.0-SNAPSHOT.jar
java -jar seckill-interface/target/seckill-interface-1.0-SNAPSHOT.jar
java -jar seckill-user/target/seckill-user-1.0-SNAPSHOT.jar
# ... 依次启动其他模块
```

### 4.3 前端启动

```bash
cd frontend/seckill-portal
npm install
npm run dev

cd frontend/seckill-admin
npm install
npm run dev

cd frontend/seckill-monitor
npm install
npm run dev
```

## 5. 核心链路

```
用户请求 → Gateway(8080) → Interface(8081) → Seckill(8085)
                                                  ↓
                                             Redis 预减库存
                                                  ↓
                                          RocketMQ 异步下单
                                                  ↓
                                             Order(8084) 消费落库
```

## 6. 开发规范

### 6.1 包名规范

| 模块 | 包名 |
|:---|:---|
| seckill-common | `com.reditickets.common` |
| seckill-gateway | `com.reditickets.gateway` |
| seckill-interface | `com.reditickets.interfaces` |
| seckill-user | `com.reditickets.user` |
| seckill-product | `com.reditickets.product` |
| seckill-order | `com.reditickets.order` |
| seckill-seckill | `com.reditickets.seckill` |
| seckill-activity | `com.reditickets.activity` |
| seckill-launcher | `com.reditickets.launcher` |

> 注意：`seckill-interface` 模块因 `interface` 是 Java 关键字，包名使用 `interfaces`。

### 6.2 版本依赖

所有依赖版本统一在 `backend/pom.xml` 的 `<properties>` 和 `<dependencyManagement>` 中管理，子模块不允许单独声明版本号。
