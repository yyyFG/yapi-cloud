# yapi-client-sdk（API 开放平台客户端 SDK）

## 项目简介

`yapi-client-sdk` 是 API 开放平台的 Java 客户端 SDK。开发者引入本 SDK 后，使用在平台注册时获取的 `accessKey/secretKey` 即可调用平台上的接口。SDK 会自动完成签名鉴权（自动添加 `accessKey`、`nonce`、`timestamp`、`sign` 请求头）、自动区分 GET/POST 组装请求并处理响应，做到开箱即用。

## 快速开始

### 1. 本地安装 SDK

进入 SDK 目录（`yapi-client-sdk`）执行 Maven 命令，将 SDK 安装到本地 Maven 仓库：

```bash
cd yapi-client-sdk
mvn install
```

### 2. 引入 Maven 依赖

安装成功后，在业务项目的 `pom.xml` 中引入以下依赖：

```xml
<dependency>
    <groupId>cn.y.yapi-client-sdk</groupId>
    <artifactId>yapi-client-sdk</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 3. 获取密钥

在平台注册账号后，从平台获取属于你的 `accessKey` 与 `secretKey`。`accessKey` 用于标识调用者身份，`secretKey` 用于生成签名，请勿泄露。

### 4. 编写调用代码

以下示例演示调用平台 GET 接口的完整流程：

```java
import cn.y.yapiclientsdk.client.YApiClient;
import cn.y.yapiclientsdk.model.InterfaceInfo;

public class Demo {

    public static void main(String[] args) {
        // 1. 使用平台分配的 accessKey/secretKey 创建客户端
        YApiClient client = new YApiClient("你的 accessKey", "你的 secretKey");

        // 2. 构造接口信息：url 为平台上的接口地址，method 为请求类型
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        interfaceInfo.setUrl("/api/name/get");
        interfaceInfo.setMethod("GET");
        interfaceInfo.setRequestParams("name=xxx");

        // 3. 调用接口并打印结果
        String result = client.invokeInterface(interfaceInfo);
        System.out.println(result);
    }
}
```

调用 POST 接口时，`method` 设为 `POST`，`requestParams` 写请求体内容（如 JSON 字符串），并通过 `requestHeader` 指定 `Content-Type`：

```java
InterfaceInfo info = new InterfaceInfo();
info.setUrl("/api/name/postUser");
info.setMethod("POST");
info.setRequestParams("{\"userName\":\"xxx\"}");
info.setRequestHeader("{\"Content-Type\":\"application/json\"}");

String result = client.invokeInterface(info);
System.out.println(result);
```

## 核心类说明

| 类 | 职责 |
| -- | ---- |
| `cn.y.yapiclientsdk.client.YApiClient` | 客户端入口，构造时传入 `accessKey/secretKey`，负责构造鉴权请求头、发起 HTTP 请求、返回响应结果 |
| `cn.y.yapiclientsdk.model.InterfaceInfo` | 接口信息模型，描述待调用接口的 url、method、requestParams、requestHeader 等信息 |
| `cn.y.yapiclientsdk.utils.SignUtils` | 签名工具，基于 url、method、body 与 secretKey 按 SHA256 算法生成签名 |

## InterfaceInfo 模型字段说明

| 字段名 | 类型 | 含义 |
| ------ | ---- | ---- |
| `url` | `String` | 接口地址，如 `/api/name/get` |
| `method` | `String` | 请求类型，如 `GET`、`POST` |
| `requestParams` | `String` | 请求参数：GET 请求为 query 参数（拼接到 URL），其他请求为请求体内容（如 JSON 字符串） |
| `requestHeader` | `String` | 接口自身的请求头，JSON 字符串格式，如 `{"Content-Type":"application/json"}` |
| `responseHeader` | `String` | 响应头（当前仅作记录，不参与请求） |

## 调用行为说明

| 行为 | 说明 |
| ---- | ---- |
| 鉴权请求头 | 每次调用自动添加 `accessKey`、`nonce`（8 位随机数）、`timestamp`（秒级时间戳）、`body`（URL 编码后的参数）、`sign` 请求头 |
| GET 请求 | 参数拼接在 URL 上，形如 `url?requestParams` |
| POST 请求 | 参数放入请求体 |
| 超时时间 | 5 秒 |
| 响应处理 | 2xx 状态码返回响应内容，非 2xx 抛出 `RuntimeException`（异常信息包含状态码与响应内容） |

## 注意事项

1. `requestHeader` 必须是 JSON 字符串格式（如 `{"Content-Type":"application/json"}`），SDK 内部会将其解析为请求头 Map 添加到请求中；
2. 网关地址目前是 `YApiClient` 中的常量 `GATEWAY_HOST`，默认值为 `http://localhost:18098`，如需修改网关地址需要改动源码（代码中已标注后续做成可配置）；
3. `accessKey/secretKey` 属于敏感信息，请勿硬编码进代码仓库，建议通过配置文件或环境变量注入；
4. SDK 基于 Java 8 构建，业务项目需使用 JDK 8 及以上版本。
