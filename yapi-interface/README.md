# yapi-interface（模拟接口提供方）

## 一、模块简介

`yapi-interface` 是 yApiRelay — 接口开放平台中的示例接口提供方，对外提供可被调用、可测试的模拟接口，用于演示"用户在线调用接口"的完整流程：用户通过平台申请 `accessKey/secretKey` 后，在平台在线调用页或直接用 curl 请求本模块接口，体验"构造鉴权请求头 -> 服务端五步校验 -> 执行业务逻辑 -> 返回结果"的闭环。

本模块解决的问题：

- 为平台前端提供真实的接口后端，打通接口发布与在线调用链路；
- 通过 GET 与 POST 两类接口演示不同请求方式的参数解析；
- 通过五步签名鉴权流程，演示接口防篡改、防重放的核心思想。

## 二、接口列表

所有接口统一以 `/api/name` 为前缀（由 `application.yml` 中 `server.servlet.context-path` 配置）。

| 方法 | 路径 | 参数 | 是否鉴权 | 返回示例 |
| ---- | ---- | ---- | -------- | -------- |
| GET | `/api/name/get` | `name`（query 参数，必填） | 否 | `GET 你的名字是：xxx` |
| POST | `/api/name/post` | `name`（query 参数，必填）+ 鉴权请求头 | 是 | `POST 你的名字是：xxx` |
| POST | `/api/name/postUser` | JSON 请求体（`User` 对象）+ 鉴权请求头 | 是 | `POST 你的名字是：xxx` |

`User` 对象结构：

```json
{
  "userName": "xxx"
}
```

## 三、鉴权机制说明

POST 接口演示了五步鉴权校验流程，任一步校验失败均抛出"无权限"异常，全部通过后才执行业务逻辑：

1. **校验 accessKey**：校验请求头 `accessKey` 是否合法，用于识别调用者身份；
2. **校验 nonce**：校验请求头 `nonce` 随机数是否重复，用于防重放攻击（当前用内存 HashSet 存储）；
3. **校验 timestamp**：校验请求头 `timestamp` 与当前时间差不超过 5 分钟（300 秒），用于拒绝过期请求；
4. **校验 sign**：服务端用 `SignUtils.genSign(body, secretKey)` 按 `SHA256(body + "." + secretKey)` 重新计算签名，与请求头 `sign` 比对，用于防参数篡改；
5. **执行业务逻辑**：校验全部通过后执行接口逻辑并返回结果。

鉴权请求头一览：

| 请求头 | 说明 |
| ------ | ---- |
| `accessKey` | 调用者身份标识 |
| `nonce` | 随机数，每次请求必须不同 |
| `timestamp` | 请求时间戳（秒），与当前时间差需小于 300 秒 |
| `body` | 请求参数内容，URL 编码后传输（服务端会解码后参与签名） |
| `sign` | 签名，由 `SHA256(body + "." + secretKey)` 生成 |

## 四、快速开始

### 4.1 启动服务

在 IDE 中运行启动类 `cn.y.yapiinterface.YapiInterfaceApplication`，或使用 Maven 命令：

```bash
mvn spring-boot:run
```

端口与 context-path 配置见 `src/main/resources/application.yml`：

```yaml
server:
  address: 0.0.0.0
  port: 8123
  servlet:
    context-path: /api
```

启动后服务基地址为 `http://localhost:8123/api`。

### 4.2 测试 GET 接口（无鉴权）

```bash
curl "http://localhost:8123/api/name/get?name=xxx"
# 返回：GET 你的名字是：xxx
```

### 4.3 测试 POST 接口（带鉴权）

当前演示实现中，服务端硬编码的密钥对为：

- `accessKey`：`a6b072c7a80671c2c1e0a9fae4da16e8`
- `secretKey`：`152120b4bcab38863c3dfec04af96e29`

调用 `/api/name/post`（`body` 请求头取 URL 编码后的 `name%3Dxxx`，签名对 `name=xxx` 计算，结果为 `c7ada315264bf9a2938075a41384aaa070851cb3ad821333c0e61601a3c029c5`）：

```bash
curl -X POST "http://localhost:8123/api/name/post?name=xxx" \
  -H "accessKey: a6b072c7a80671c2c1e0a9fae4da16e8" \
  -H "nonce: $RANDOM$RANDOM" \
  -H "timestamp: $(date +%s)" \
  -H "body: name%3Dxxx" \
  -H "sign: c7ada315264bf9a2938075a41384aaa070851cb3ad821333c0e61601a3c029c5"
# 返回：POST 你的名字是：xxx
```

调用 `/api/name/postUser`（`body` 请求头为 URL 编码后的 JSON 串，签名对 `{"userName":"xxx"}` 计算，结果为 `9bba9a57964c87ab44c6e5cc70c4b38b7b65174f647bd4a03534a1efde135003`）：

```bash
curl -X POST "http://localhost:8123/api/name/postUser" \
  -H "Content-Type: application/json" \
  -H "accessKey: a6b072c7a80671c2c1e0a9fae4da16e8" \
  -H "nonce: $RANDOM$RANDOM" \
  -H "timestamp: $(date +%s)" \
  -H "body: %7B%22userName%22%3A%22xxx%22%7D" \
  -H "sign: 9bba9a57964c87ab44c6e5cc70c4b38b7b65174f647bd4a03534a1efde135003" \
  -d '{"userName":"xxx"}'
# 返回：POST 你的名字是：xxx
```

注意：每次请求的 `nonce` 不能重复（服务端内存去重），`timestamp` 必须为当前时间（Windows PowerShell 中可用 `[DateTimeOffset]::Now.ToUnixTimeSeconds()` 代替 `$(date +%s)`）。

### 4.4 通过平台在线调用测试

在平台前端进入对应接口的"在线调用"页面，填写参数后点击调用。平台侧会通过 `yapi-client-sdk` 自动构造上述鉴权请求头并发起请求，即可完整体验"申请密钥 -> 在线调用 -> 返回结果"的流程。

## 五、注意事项

1. **密钥硬编码仅用于演示**：当前 `accessKey`/`secretKey` 直接硬编码在 `NameController` 中，代码已用 `todo` 标注，将来应改为从数据库按 `accessKey` 查询用户及其 `secretKey` 进行校验；
2. **nonce 内存存储仅用于演示**：当前 nonce 去重使用内存 HashSet（`NONCE_SET`），服务重启后即失效且不支持多实例部署，生产环境应替换为 Redis 等分布式存储；
3. **application.yml 中的密钥不参与本模块校验**：`application.yml` 里的 `yapi.client.access-key/secret-key` 是平台客户端 SDK 的配置，本模块鉴权使用的是代码中硬编码的密钥对；
4. **异常提示未区分原因**：五步校验失败统一抛出"无权限"，生产环境应区分具体失败原因并返回规范错误码。

## 项目结构

```text
yapi-interface
├── pom.xml
└── src/main
    ├── java/cn/y/yapiinterface
    │   ├── YapiInterfaceApplication.java   # 启动类
    │   └── controller/NameController.java  # 3 个模拟接口 + 五步鉴权
    └── resources/application.yml           # 端口、context-path 配置
```
