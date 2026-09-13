# SeckillCore 项目规则

## 项目信息

- **项目名称**: SeckillCore (秒杀核心系统)
- **技术栈**: Spring Boot 3.2.0 + Spring Cloud + MyBatis-Plus 3.5.5 + RocketMQ + Redis
- **启动类**: `com.reditickets.launcher.SeckillLauncherApplication`
- **端口**: 8080

## 模块结构

| 模块 | 说明 |
|------|------|
| seckill-common | 公共工具、DTO、VO、异常 |
| seckill-gateway | Spring Cloud Gateway 网关（独立部署） |
| seckill-user | 用户服务 |
| seckill-product | 产品/库存服务 |
| seckill-activity | 活动服务 |
| seckill-order | 订单服务（含 RocketMQ Consumer） |
| seckill-seckill | 秒杀核心服务 |
| seckill-interface | BFF 聚合层（Feign 客户端） |
| seckill-launcher | 启动器（单体聚合） |

## 编译命令

### 编译整个项目
```bash
cd E:\SeckillCore\backend
mvn clean package -DskipTests
```

### 只编译启动器及依赖
```bash
cd E:\SeckillCore\backend
mvn clean package -DskipTests -pl seckill-launcher -am
```

## 启动命令

### 启动应用（需要 MySQL + Redis）
```bash
cd E:\SeckillCore\backend\seckill-launcher
java -jar target\seckill-launcher-1.0-SNAPSHOT-exec.jar
```

注意：必须使用 `-exec.jar` 后缀的文件（classifier=exec），不带后缀的是普通 jar。

## 运行依赖

| 服务 | 地址 | 说明 |
|------|------|------|
| MySQL | 127.0.0.1:3306 | 数据库 seckill_core |
| Redis | 127.0.0.1:6379 | 缓存 |
| RocketMQ NameServer | 127.0.0.1:9876 | 消息队列（Producer 需要，Consumer 可选） |

## RocketMQ 配置说明

### 版本
- `rocketmq-spring-boot-starter`: 2.3.0
- `rocketmq-client`: 5.2.0

### Consumer 条件化加载
RocketMQ Consumer（`SeckillOrderConsumer`、`OrderTimeoutConsumer`）已添加 `@ConditionalOnProperty(prefix = "rocketmq.consumer", name = "enabled", havingValue = "true")`。

默认情况下，Consumer **不加载**，应用可正常启动。当需要启用 Consumer 时，在配置中添加：
```yaml
rocketmq:
  consumer:
    enabled: true
```

### Producer 始终加载
RocketMQ Producer 无条件加载，NameServer 不可用时不会阻止启动（仅 log 错误）。

## Nacos 配置

当前 Nacos 已禁用（`enabled: false`），使用本地配置。

## Feign 客户端

Feign 客户端使用 `@FeignClient(name = "service-name")` 配合 Spring Cloud LoadBalancer 进行服务发现。当前为单体模式，所有服务类在同一 JVM 中（通过 `seckill-interface` 模块聚合），Feign 客户端通过 load-balancing 模式调用。

## 数据库

- 数据库名: `seckill_core`
- 初始化脚本: 各模块的 `schema.sql`（如 `seckill-seckill/src/main/resources/schema.sql`）
- 使用雪花算法（Snowflake）生成分布式 ID（MyBatis-Plus `assign_id`）