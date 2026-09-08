# y-api-microservice（yApiRelay — 接口开放平台的后端微服务工程）

## 一、工程简介

`y-api-microservice` 是 yApiRelay — 接口开放平台的后端微服务工程，将平台的用户体系、接口管理、调用关系与网关鉴权拆分为独立服务。前端页面与客户端 SDK 的全部流量都经 `y-api-gateway`（18098）这一唯一入口进入，网关完成签名鉴权、防重放、限流后按规则路由到各业务服务；业务数据落 MySQL，登录态、限流、nonce 判重、调用次数与排行榜等热点数据落 Redis。调用链路上通过高性能异步架构优化：在线调用由线程池异步执行并限时等待，网关鉴权并行化，调用计数与调用日志通过 RabbitMQ 消息异步消费（计数幂等、日志落库），不阻塞请求主流程。

## 二、技术栈

以下内容以各模块 `pom.xml` 为准：

| 分类 | 技术 |
| ---- | ---- |
| 基础框架 | Spring Boot 2.6.13、Spring Cloud 2021.0.5、Spring Cloud Alibaba 2021.0.5.0（Java 8） |
| 网关 | Spring Cloud Gateway（WebFlux 响应式）、Spring Cloud LoadBalancer、Resilience4j CircuitBreaker |
| RPC | Apache Dubbo 3.2.19，注册中心使用 Nacos |
| 注册/配置中心 | Nacos（discovery + config，网关配置支持 `optional:nacos:` 本地兜底） |
| ORM | MyBatis-Plus 3.5.2、mybatis-spring-boot-starter 2.2.2 |
| 数据库 | MySQL（库名 `yapi`），连接池 HikariCP |
| 缓存 | Redis（排除 Lettuce、使用 Jedis）、Redisson（限流器、分布式锁、Lua 脚本）、Spring Cache + Caffeine 本地缓存 |
| 消息队列 | RabbitMQ（Spring AMQP）：调用计数与调用日志异步消费，Fanout 广播 + 消费者幂等 |
| 线程池 | Spring `ThreadPoolTaskExecutor`：在线调用 `invokeExecutor`、网关并行鉴权 `gatewayAsyncExecutor` |
| 会话 | Spring Session + Redis（30 天有效期，跨服务共享登录态） |
| 接口文档 | Knife4j openapi3 4.3.0 + 网关聚合（`/doc.html`） |
| 对象存储 | 腾讯云 COS（cos_api，用户头像上传） |
| 工具库 | Hutool 5.8.38、Lombok、commons-lang3 |

## 三、模块架构总览

### 3.1 模块一览

| 模块 | 职责 | 一句话定位 |
| ---- | ---- | ---------- |
| y-api-gateway | 统一入口：路由转发、AK/SK 签名鉴权（查用户与查接口并行）、防重放、多维限流、MQ 异步调用计数 | 全部流量的唯一入口与安全边界 |
| y-api-user | 用户注册/登录/管理、accessKey/secretKey 分配、头像上传 | 账号体系服务 |
| y-api-interface | 接口登记、发布/下线、在线调用（线程池异步执行）、调用排行榜 | 接口资产服务 |
| y-api-userinterface | 用户-接口调用关系、申请开通、次数额度扣减与回写、MQ 消费计数与日志落库 | 调用关系与计费服务 |
| y-api-client | 三个 Dubbo 内部服务接口（Inner*Service）定义 | 服务间 RPC 契约，不独立部署 |
| y-api-common | 统一响应/异常/权限 AOP/限流/缓存/COS/消息队列配置等公共组件 | 各业务模块的公共底座 |
| y-api-model | 实体、DTO、VO、枚举等模型类 | 跨模块共享的数据模型 |

### 3.2 模块间调用关系

一次完整调用的链路：

```text
客户端 SDK / 平台前端
        │  携带 accessKey、nonce、timestamp、body、sign 请求头
        ▼
y-api-gateway（18098）
        │  WebTrafficFilter：记录请求日志，文档路径直接放行，平台管理端路径按 IP 限流后放行
        │  ApiAuthFilter（处理 API 流量）：IP 限流 → 查用户与查接口并行（CompletableFuture +
        │  gatewayAsyncExecutor 线程池）→ nonce 判重 → timestamp 校验 → 验签 →
        │  校验调用资格 → 接口限流 → 转发
        ├── /user/**、/interfaceInfo/**、/userInterface/**  →  lb:// 按服务名转发各业务服务
        └── /api/**（第三方 SDK 调用路径）                   →  模拟接口提供方（8123）
        ▼
各业务服务（y-api-user 8210 / y-api-interface 8211 / y-api-userinterface 8212）
        │  服务间数据互查不经过 HTTP，直接走 Dubbo（Inner*Service）
        │  y-api-interface 在线调用由 invokeExecutor 线程池异步执行（5 秒超时、限时等待）
        ▼
MySQL（业务数据）/ Redis（Session、限流器、nonce 判重、调用次数缓存、排行榜 ZSet、消息幂等 key）

调用成功后的异步链路（网关发 MQ，y-api-userinterface 消费）：
y-api-gateway 在响应 200 时构造 InvokeMessage，异步发送到 yapi.invoke.exchange（Fanout 广播）
        ├── yapi.invoke.count.queue → InvokeMessageConsumer：Redis setIfAbsent 幂等 → 扣减调用次数
        └── yapi.invoke.log.queue   → InvokeMessageConsumer：写入 InvokeLog 表
```

网关的入口角色体现在两点：一是平台前端与接口文档走 `/user/**`、`/interfaceInfo/**`、`/userInterface/**` 路由，登录态校验交给各服务的 `@AuthCheck`；二是第三方开发者用 SDK 调用的 `/api/**` 路径由 `ApiAuthFilter` 完成鉴权（其中查用户与查接口并行执行）后，按 `interface_info` 表登记的 path+method 匹配接口并转发到真实地址。调用成功后网关不再同步更新计数，而是发 MQ 消息，由 y-api-userinterface 异步完成调用次数扣减与调用日志落库。三个业务服务端口只绑定 127.0.0.1，不直接对外暴露，公网只能通过网关访问。

## 四、各模块详细介绍

### 4.1 y-api-common

**模块职责**：为各业务模块提供统一技术底座，避免每个服务重复实现通用逻辑。统一响应结构、全局异常处理、权限校验 AOP、分布式限流、多级缓存、COS 上传等都在此实现，业务模块按需依赖。

**核心功能点**：

- `BaseResponse`/`ResultUtils`/`ErrorCode` 统一返回结构与错误码，`GlobalExceptionHandler` 把未捕获异常兜底成标准响应；
- `@AuthCheck` 注解 + `AuthInterceptor` 切面实现声明式权限校验（user/admin/ban 三种角色），`LogInterceptor` 记录接口调用日志；
- `@RateLimit` 注解 + `RedissonRateLimiterManager` 提供基于 Redisson 的分布式限流，`RateLimitType` 枚举定义用户/接口/IP/平台/服务五种限流维度及 Redis key 前缀；
- 多级缓存：`CaffRedisCache`（Redis 缓存管理器）+ Caffeine 本地缓存（`LocalCacheConfig`），`CacheKeyUtils` 以「对象转 JSON 再 MD5」生成缓存 key；
- `CosManager` 封装腾讯云 COS 上传，供头像等文件存储使用；
- `RabbitMqConfig` 定义消息队列拓扑：`yapi.invoke.exchange` Fanout 交换机，一条调用消息同时广播到 `yapi.invoke.count.queue`（计数）与 `yapi.invoke.log.queue`（日志）两个持久化队列。

**关键类/组件**：

| 类 | 作用 |
| -- | ---- |
| `BaseResponse` / `ResultUtils` / `ErrorCode` | 统一响应结构与错误码 |
| `BusinessException` / `GlobalExceptionHandler` / `ThrowUtils` | 业务异常体系与全局兜底 |
| `AuthInterceptor` / `@AuthCheck` | 权限校验 AOP（依赖 `LoginUserService` 接口，各服务自行实现） |
| `RedissonRateLimiterManager` / `@RateLimit` / `RateLimitType` / `RedissonConfig` | 分布式限流组件 |
| `CaffRedisCache` / `CaffRedisCacheConfig` / `CaffRedisCacheManager` / `LocalCacheConfig` | Redis 与 Caffeine 缓存 |
| `CacheKeyUtils` / `RedisKeyConstant` / `UserConstant` / `UserInterfaceInfoConstant` | 缓存 key 工具与各类常量 |
| `CosManager` / `CosClientConfig` | COS 对象存储客户端 |
| `RabbitMqConfig` | 消息队列拓扑配置（Fanout 交换机 + 计数/日志两个持久化队列） |
| `MyBatisPlusConfig` / `SessionSerializerConfig` / `JsonConfig` / `CorsConfig` | 公共配置类 |

**依赖的其他模块**：y-api-model

### 4.2 y-api-model

**模块职责**：集中存放全工程共享的数据模型（实体、DTO、VO、枚举），避免各服务各自维护模型导致字段漂移。无启动类，处于依赖链最底层，被所有业务模块与网关依赖。

**核心功能点**：

- 三张核心表的 MyBatis-Plus 实体：`User`（含 accessKey/secretKey）、`InterfaceInfo`（含 path/url/status/method/invokeCount）、`UserInterface`（含 totalNum/leftNum/status），均带 `@TableLogic` 逻辑删除；
- 用户、接口、调用关系的请求 DTO（Add/Update/Query/Invoke/Apply）与展示 VO（`UserVO`、`LoginUserVO`、`InterfaceInfoVO`、`InterfaceRankVO`）；
- 枚举：`UserRoleEnum`（user/admin/ban）、`InterfaceStatusEnum`（0-关闭、1-发布、2-管理员下架）、`FileUploadBizEnum`。

**关键类/组件**：

| 类 | 作用 |
| -- | ---- |
| `User` | user 表实体 |
| `InterfaceInfo` | interface_info 表实体 |
| `UserInterface` | user_interface 表实体 |
| `InterfaceStatusEnum` / `UserRoleEnum` | 接口状态、用户角色枚举 |
| `InterfaceRankVO` | 排行榜条目 VO（id/interfaceName/description/invokeCount） |
| 各类 `*Request` DTO | 各业务接口入参 |

**依赖的其他模块**：无（最底层模块）

### 4.3 y-api-client

**模块职责**：定义服务间 RPC 契约。三个 Dubbo 内部服务接口被各业务模块与网关共享：消费方用 `@DubboReference` 注入，提供方用 `@DubboService` 实现，服务间调用走 Dubbo 而非 HTTP。

**核心功能点**：

- `InnerUserService`：按 id 查用户、按 accessKey 查调用用户（`getInvokeUser`，网关验 accessKey 时使用）、判断是否管理员；
- `InnerInterfaceInfoService`：按 id 查接口、按 path+method 查接口（网关转发路由时使用）；
- `InnerUserInterfaceService`：申请接口、校验是否可调用（`checkInvokable`）、扣减调用次数（`invokeCount`）、按用户查调用记录、按接口统计申请人数。

**关键类/组件**：

| 类 | 作用 |
| -- | ---- |
| `InnerUserService` | 用户服务 Dubbo 接口 |
| `InnerInterfaceInfoService` | 接口服务 Dubbo 接口 |
| `InnerUserInterfaceService` | 用户接口调用关系服务 Dubbo 接口 |

**依赖的其他模块**：y-api-common、y-api-model

### 4.4 y-api-user

**模块职责**：平台账号体系服务。负责注册登录与用户管理，并在注册时为用户生成调用第三方接口所需的 accessKey/secretKey 密钥对。端口 8210，context-path `/user`。

**核心功能点**：

- 注册：账号 ≥4 位、密码 ≥8 位且二次确认一致，密码以 `MD5(SALT + password)` 加盐存储，同时自动分配密钥对（`accessKey = MD5(SALT + userAccount + 5位随机数)`、`secretKey = MD5(SALT + userAccount + 8位随机数)`）；
- 登录/登出：登录态写入 Session（Spring Session + Redis，30 天），`/get/login` 返回脱敏后的 `LoginUserVO`；
- 用户管理：管理员增删改查由 `@AuthCheck(mustRole = admin)` 保护，普通用户通过 `/update/my` 自助修改个人信息；
- 头像上传：`ImageController` 校验大小（≤5MB）与后缀（jpg/png/jpeg/gif/bmp）后经 `CosManager` 上传 COS 并返回 URL；
- 内部服务：`InnerUserServiceImpl` 以 `@DubboService` 暴露 `getById`/`getInvokeUser`/`isAdmin` 等能力，`getInvokeUser` 带 `@Cacheable("invokeUser")` 缓存，用户信息变更时通过 `@CacheEvict` 失效。

**关键类/组件**：

| 类 | 作用 |
| -- | ---- |
| `UserController` | 注册/登录/登出/用户 CRUD 的 HTTP 入口 |
| `ImageController` | 头像图片上传 |
| `UserService` / `UserServiceImpl` | 用户业务逻辑（注册分配密钥、登录、查询） |
| `InnerUserServiceImpl` | `InnerUserService` 的 Dubbo 提供方实现 |
| `UserMapper` | user 表 Mapper |

**依赖的其他模块**：y-api-common、y-api-model、y-api-client

### 4.5 y-api-interface

**模块职责**：平台接口资产服务。管理接口的登记、发布、下线与在线调用，提供调用排行榜查询，同时承担排行榜种子数据初始化与 DB 同步任务。端口 8211，context-path `/interfaceInfo`。

**核心功能点**：

- 接口 CRUD：新增时校验 URL 合法性并做 SSRF 防护（禁止指向 localhost/内网，开关 `platform.ssrf.check-enabled`），自动生成唯一对外 path `/api/u{userId}/{真实path}`，天然隔离不同用户的重复 path；
- 发布/下线：发布前平台按真实地址实际发起一次调用（5 秒超时），请求发出且拿到响应体才允许置为已发布；下线区分普通下线（状态 0）与管理员下架（状态 2）；
- 在线调用：按 path+method 查接口（带 `@Cacheable`）→ 校验已发布 → 非创建者/管理员需校验已申请且有剩余次数 → 真实请求提交 `invokeExecutor` 线程池异步执行（GET 拼 query、POST 放 body，请求层 5 秒超时 + `future.get(10s)` 限时等待）→ 扣减 1 次额度并计入排行榜 → 返回响应；入口带 `@RateLimit` 限流（按用户 100 次/分钟）；
- 申请开通：`/apply` 经 Dubbo 调用 y-api-userinterface 写入调用关系（默认额度 10000 次）；
- 排行榜：`/rank` 返回 Top 15（读 Redis ZSet），`RankSeedRunner` 负责启动种子数据，`RankSyncTask` 定时回写 DB（详见 5.3）；
- 已发布接口分页列表带 `@Cacheable`（前 10 页缓存，key 由请求参数生成）与爬虫限制（pageSize ≤ 20）。

**关键类/组件**：

| 类 | 作用 |
| -- | ---- |
| `InterfaceInfoController` | 接口 CRUD/发布/下线/在线调用/申请/排行榜的 HTTP 入口 |
| `InterfaceInfoService` / `InterfaceInfoServiceImpl` | 接口业务逻辑（SSRF 校验、path 生成、发布验证、在线调用、排行榜查询） |
| `InnerInterfaceInfoServiceImpl` | `InnerInterfaceInfoService` 的 Dubbo 提供方实现 |
| `RankSeedRunner` | 启动时把 DB 中 invokeCount > 0 的接口灌入排行榜 ZSet |
| `RankSyncTask` | 定时把 ZSet 分数覆盖同步回 interface_info.invokeCount |
| `RateLimitAspect` | `@RateLimit` 注解限流切面 |
| `InvokeThreadPoolConfig` | 在线调用线程池 `invokeExecutor`（核心 10/最大 50/队列 200，`CallerRunsPolicy` 拒绝策略） |
| `InterfaceInfoMapper` | interface_info 表 Mapper |

**依赖的其他模块**：y-api-common、y-api-model、y-api-client、y-api-user

### 4.6 y-api-userinterface

**模块职责**：管理用户与接口的调用关系（谁申请了哪个接口、还剩多少次调用），并以「Redis + Lua」实现高并发下的次数扣减与定时回写。端口 8212，context-path `/userInterface`。网关调用成功后的次数计数与调用日志落库由本模块的 MQ 消费者异步完成。

**核心功能点**：

- 调用关系 CRUD：管理员可新增/修改/删除用户接口调用关系（调整额度、禁用状态），修改后清空对应次数缓存；
- 申请开通：用户申请接口后默认 totalNum=0、leftNum=10000、状态正常；
- 资格校验：`checkInvokable` 校验存在记录、未被禁用、剩余次数充足；
- 次数扣减：`InvokeCountRedisService` 用 Redis Hash + Lua 脚本原子完成「检查剩余 → 扣 1 → 记差量」，缓存 miss 时懒加载 DB，Redis 异常时自动降级为 DB 直接扣减；
- 定时回写：每 5 分钟把扣减差量批量写回 user_interface 表（leftNum 减、totalNum 加），Redisson 分布式锁保证多实例下只有一个执行；
- 排行榜计数：扣减成功后对 `yapi:rank:interface` ZSet 加 1；
- MQ 消费：`InvokeMessageConsumer` 监听 `INVOKE_COUNT_QUEUE` 与 `INVOKE_LOG_QUEUE`——计数消息消费前用 Redis `setIfAbsent` 记录 msgId（30 分钟）做幂等，重复投递直接丢弃；日志消息写入 InvokeLog 表，与主流程解耦。

**关键类/组件**：

| 类 | 作用 |
| -- | ---- |
| `UserInterfaceController` | 调用关系分页查询/删除的 HTTP 入口 |
| `UserInterfaceService` / `UserInterfaceServiceImpl` | 调用关系业务逻辑（申请、校验、DB 降级扣减） |
| `InvokeCountRedisService` | 次数缓存懒加载、Lua 原子扣减、差量回写、排行榜计数 |
| `InnerUserInterfaceServiceImpl` | `InnerUserInterfaceService` 的 Dubbo 提供方实现 |
| `InvokeMessageConsumer` | MQ 消费者：消费计数队列（Redis 幂等扣减）与日志队列（InvokeLog 落库） |
| `UserInterfaceMapper` | user_interface 表 Mapper |

**依赖的其他模块**：y-api-common、y-api-model、y-api-client、y-api-user

### 4.7 y-api-gateway

**模块职责**：全平台唯一对外入口（18098）。把「谁在调用、能不能调用、调用多少次」统一在网关拦截完成，业务服务不再重复实现安全逻辑。基于 Spring Cloud Gateway（WebFlux 响应式）构建。

**核心功能点**：

- 路由：`/user/**`、`/interfaceInfo/**`、`/userInterface/**` 按服务名 lb:// 负载均衡转发，`/api/**` 转发到模拟接口提供方（8123）；
- 流量分流：`WebTrafficFilter`（order -10）记录全量请求日志；文档路径直接放行；平台管理端路径按 IP 限流（20 次/秒）后放行，权限交由各服务 `@AuthCheck`；
- API 鉴权（`ApiAuthFilter`）：IP 限流（20 次/秒）→ 查用户与查接口两个互不依赖的 Dubbo 调用经 `CompletableFuture` 在 `gatewayAsyncExecutor` 线程池上并行执行（无效/封禁/不存在拒绝）→ 用户级限流（2 次/秒）→ nonce 用 Redis SETNX 判重防重放（5 分钟 TTL）→ timestamp 与当前时间差须小于 5 分钟 → 用用户 secretKey 按 `SHA256(path + "\n" + method + "\n" + body + "." + secretKey)` 重新计算并比对 sign → 校验调用资格 → 接口级限流（10 次/秒）→ 改写 URI 转发真实地址；
- 响应装饰与异步计数：包装响应 `writeWith`，HTTP 200 时对非管理员调用者构造 `InvokeMessage`（msgId/userId/interfaceId/timestamp），经 `Schedulers.boundedElastic()` 异步发送到 `yapi.invoke.exchange`，不阻塞响应线程；
- 接口文档聚合：Knife4j 网关模式按服务发现聚合各服务文档（`/doc.html`）；
- `GlobalCorsFilter` 统一 CORS 跨域策略（允许携带 Cookie）。

**关键类/组件**：

| 类 | 作用 |
| -- | ---- |
| `ApiAuthFilter` | API 流量核心过滤器：并行鉴权 + 多维限流 + 转发 + MQ 异步计数 |
| `WebTrafficFilter` | 平台网站流量过滤器：请求日志 + 文档放行 + 管理端 IP 限流 |
| `GatewayPathMatcher` | 管理端/文档/API 三类路径的分流判定 |
| `GlobalCorsFilter` | 全局 CORS 配置 |
| `GatewayThreadPoolConfig` | 网关异步线程池 `gatewayAsyncExecutor`（核心 10/最大 50/队列 100），供鉴权并行任务使用 |

**依赖的其他模块**：yapi-client-sdk（复用其 `SignUtils` 验签）、y-api-common、y-api-model、y-api-client

## 五、核心功能亮点

### 5.1 用户与密钥管理

注册登录与 accessKey/secretKey 的生成都在 y-api-user 完成：

1. 注册时校验账号密码规则，密码以 `MD5(SALT + password)` 加盐后落库，不存明文；
2. 同一事务内为用户分配密钥对：`accessKey = MD5(SALT + userAccount + 5位随机数)`、`secretKey = MD5(SALT + userAccount + 8位随机数)`，随用户记录写入 user 表，用户可在平台页面查看；
3. 密钥用途：`accessKey` 标识调用者身份，网关据此查用户、做用户级限流与调用计数；`secretKey` 不随请求传输，仅由客户端 SDK 在本地参与签名，网关用同一算法验签，保证请求未被篡改且确实来自密钥持有者；
4. 登录态：Session 由 Spring Session 存入 Redis（30 天），各服务共享同一登录态；网关按 accessKey 查用户的结果带缓存（`@Cacheable("invokeUser")`），用户信息变更时 `@CacheEvict` 失效。

### 5.2 接口管理

**发布流程**：用户在平台登记接口（真实 url、method、请求参数等）→ 校验 URL 合法且非内网地址（SSRF 防护）→ 自动生成唯一对外 path `/api/u{userId}/{真实path}`（path 冲突则拒绝）→ 发布时平台按真实地址实际发起一次调用（5 秒超时），请求能发出且拿到响应体才置为「发布」状态，目标不可达则拒绝发布。

**在线调用流程**（平台调试页）：`POST /interfaceInfo/invoke` → 登录校验 → 按 path+method 查接口（缓存）→ 校验状态为已发布 → 非创建者/管理员需已申请且有剩余次数 → 按 method 组装请求（GET 拼 query、POST 放 body）转发真实地址 → 扣减 1 次额度并计入排行榜 → 返回响应。

**下线**：创建者或管理员可下线；普通用户下线置状态 0（关闭），管理员下线置状态 2（管理员下架）。已下线接口在网关转发与平台在线调用两侧都会被状态校验拒绝，立即生效。

### 5.3 接口调用排行榜

排行榜是「Redis 实时计数 + 定时回写 MySQL」的典型实现，涉及 y-api-userinterface（计数）、y-api-interface（种子数据、同步、查询）、y-api-common（key 常量）三个模块。

**Redis 中存什么、用什么数据结构**：key 为 `yapi:rank:interface`（常量 `RedisKeyConstant.INTERFACE_RANK_KEY`）的 ZSet（Sorted Set），member 为接口 id 字符串，score 为该接口累计调用次数。ZSet 天然支持按 score 倒序取 Top N，查询无需额外排序。

**调用次数如何统计**（写入侧，`InvokeCountRedisService`）：

- 网关 SDK 调用链路：`ApiAuthFilter` 在响应 200 时构造 `InvokeMessage` 异步发送到 `yapi.invoke.exchange`，`InvokeMessageConsumer` 消费计数队列（Redis `setIfAbsent` 幂等）后执行扣减，扣减成功后 `incrInterfaceRank` 对 ZSet 加 1（详见 5.4）；
- 平台在线调试链路：`InterfaceInfoServiceImpl.invokeInterface` 扣减额度成功后，直接对同一 ZSet 再加 1（与网关链路共用同一 key）；
- Redis 不可用时，`UserInterfaceServiceImpl.invokeCount` 捕获异常降级为 DB 直接扣减，此降级路径不计入排行榜。

**种子数据如何初始化**（`RankSeedRunner`，y-api-interface）：实现 `ApplicationRunner`，服务启动时若 ZSet 不存在或为空，从 interface_info 表查询 invokeCount > 0 的接口（`@TableLogic` 自动过滤已删除），把（接口 id, invokeCount）批量灌入 ZSet；ZSet 已有数据则跳过，保证 Redis 重建或清空后排行榜不丢历史数据（幂等）。

**定时同步机制**（`RankSyncTask`，y-api-interface）：每 5 分钟（`@Scheduled(fixedDelay = 300_000, initialDelay = 60_000)`）用 `rangeWithScores` 取 ZSet 全部成员，将 score 覆盖同步回 interface_info.invokeCount（`updateById` 只更新非 null 字段），使 DB 中的调用次数与 Redis 最终一致，供列表页等其他场景展示。

**对外查询接口**：`GET /interfaceInfo/rank`（需登录），服务端用 `reverseRangeWithScores` 取 ZSet 前 15 名，再按 id 批量查 DB 补全接口名称与描述，已删除的接口跳过，返回 `List<InterfaceRankVO>`：

| 字段 | 类型 | 含义 |
| ---- | ---- | ---- |
| id | Long | 接口 id |
| interfaceName | String | 接口名称 |
| description | String | 接口描述 |
| invokeCount | Long | 调用次数（来自 ZSet 的 score） |

### 5.4 高性能异步架构

平台在调用链路上做了四处异步化/并行化改造，目标是削峰、解耦、缩短请求链路耗时：

#### 在线调用线程池与超时（y-api-interface）

- `InvokeThreadPoolConfig` 定义 `invokeExecutor` 线程池：核心线程 10、最大线程 50、等待队列容量 200、线程名前缀 `invoke-`；
- 拒绝策略为 `CallerRunsPolicy`：队列满时由调用线程自己执行任务，压力大时天然限流、不丢请求；
- 平台在线调用接口时，真实请求提交到该线程池异步执行，避免高并发下第三方接口调用阻塞主流程（Tomcat 工作线程）；
- 请求层设置 5 秒连接/读取超时，并以 `future.get(10, TimeUnit.SECONDS)` 限时等待结果，防止慢接口拖垮服务。

#### 网关调用计数异步化（RabbitMQ）

- 网关收到 200 响应后不再同步经 Dubbo 更新调用次数，改为构造 `InvokeMessage`（msgId/userId/interfaceId/timestamp）发送到 `yapi.invoke.exchange`，发送动作通过 `Schedulers.boundedElastic()` 异步发出，不阻塞 WebFlux 响应线程，降低请求链路耗时；
- `RabbitMqConfig` 使用 Fanout 交换机，一条消息同时广播到 `yapi.invoke.count.queue` 与 `yapi.invoke.log.queue` 两个持久化队列；
- y-api-userinterface 的 `InvokeMessageConsumer` 消费 `INVOKE_COUNT_QUEUE` 执行次数扣减；消费前用 Redis `setIfAbsent` 记录消息 `msgId`（key `yapi:invoke:msg:{msgId}`，30 分钟过期），MQ 重复投递时直接丢弃，保证同一条消息只计数一次；
- 异常策略：业务失败（次数不足等）只记日志不重试；系统异常向上抛出，交给 MQ 的异常处理机制（代码中注明可切换为默认重试）。

#### 调用日志 MQ 落库

- 调用成功消息经 Fanout 广播到 `INVOKE_LOG_QUEUE`，由消费者写入 InvokeLog 表（用户 id、接口 id、调用时间、是否成功）；
- 日志落库与业务主流程完全解耦，数据库慢查询或抖动不阻塞用户请求。

#### 网关鉴权并行化（y-api-gateway）

- `ApiAuthFilter` 将原本串行的两个互不依赖的 Dubbo 查询——按 accessKey 查用户（`getInvokeUser`）与按 path+method 查接口（`getInterfaceInfo`）——改为 `CompletableFuture.supplyAsync` 并行执行后再 `join` 取结果，缩短鉴权耗时；
- `GatewayThreadPoolConfig` 配置独立的 `gatewayAsyncExecutor` 线程池（核心 10、最大 50、队列 100、线程名前缀 `gateway-async-`），并行任务不占用 Netty 事件循环线程。

#### 异步架构数据流

```text
用户请求
  → 网关并行鉴权（查用户与查接口并行执行，nonce/timestamp/sign 校验串行）
  → 接口服务 invokeExecutor 线程池执行真实调用（5 秒超时、限时等待）
  → 调用成功
  → 网关发送 MQ 消息（Fanout 广播）
       ├── 计数消费者：Redis setIfAbsent 幂等 → 扣减调用次数
       └── 日志消费者：写入 InvokeLog 表
```

## 六、各模块文档

各模块详细文档见各自 README.md（陆续补充中）：

| 模块 | 文档 |
| ---- | ---- |
| y-api-common | [y-api-common/README.md](./y-api-common/README.md)（待补充） |
| y-api-model | [y-api-model/README.md](./y-api-model/README.md)（待补充） |
| y-api-client | [y-api-client/README.md](./y-api-client/README.md)（待补充） |
| y-api-user | [y-api-user/README.md](./y-api-user/README.md)（待补充） |
| y-api-interface | [y-api-interface/README.md](./y-api-interface/README.md)（待补充） |
| y-api-userinterface | [y-api-userinterface/README.md](./y-api-userinterface/README.md)（待补充） |
| y-api-gateway | [y-api-gateway/README.md](./y-api-gateway/README.md)（待补充） |

## 七、快速开始

### 7.1 前置依赖

| 依赖 | 地址/版本 | 说明 |
| ---- | --------- | ---- |
| JDK | 1.8+ | 工程编译版本 |
| Maven | 3.6+ | 根目录聚合构建 |
| MySQL | localhost:3306，库名 `yapi`，root/123456 | 三个业务服务共用 |
| Redis | localhost:6379，database 5 | Session/限流/nonce 判重/次数缓存/排行榜 |
| Nacos | 127.0.0.1:8848，nacos/nacos | Spring Cloud 服务发现与 Dubbo 注册中心 |
| RabbitMQ | localhost:5672，guest/guest | 调用计数与调用日志消息队列（网关发送、y-api-userinterface 消费） |
| yapi-client-sdk | 需先在 SDK 目录执行 `mvn install` | 网关依赖其 `SignUtils` 验签 |

### 7.2 服务端口

以下端口与 context-path 以各模块 `application.yml` 为准：

| 服务 | 端口 | context-path | 绑定地址 |
| ---- | ---- | ------------ | -------- |
| y-api-gateway | 18098 | - | 0.0.0.0（对外入口） |
| y-api-user | 8210 | /user | 127.0.0.1 |
| y-api-interface | 8211 | /interfaceInfo | 127.0.0.1 |
| y-api-userinterface | 8212 | /userInterface | 127.0.0.1 |
| 模拟接口提供方（独立工程 yapi-interface） | 8123 | /api | 网关 `/api/**` 路由的转发目标 |

### 7.3 启动顺序

原则：先启动全部中间件（MySQL、Redis、Nacos、RabbitMQ），再启动各业务服务与网关。

1. 启动中间件：MySQL、Redis、Nacos、RabbitMQ；
2. 启动模拟接口提供方（8123，独立工程，见其 README）；
3. 依次启动 y-api-user（8210）、y-api-interface（8211）、y-api-userinterface（8212）——后两者通过 Dubbo 消费 y-api-user 的能力，先启动用户服务可避免启动期报错；y-api-userinterface 的 MQ 消费者依赖 RabbitMQ 就绪，需保证步骤 1 已完成；
4. 最后启动 y-api-gateway（18098）。

验证：访问 Knife4j 聚合文档 `http://localhost:18098/doc.html`，应能看到各服务接口分组；前端与 SDK 的所有请求都通过 `http://localhost:18098` 进入。
