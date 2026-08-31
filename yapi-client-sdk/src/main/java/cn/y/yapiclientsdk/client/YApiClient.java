package cn.y.yapiclientsdk.client;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.y.yapiclientsdk.model.InterfaceInfo;
import cn.y.yapiclientsdk.model.User;
import cn.y.yapiclientsdk.utils.SignUtils;

import java.util.HashMap;
import java.util.Map;


/**
 * 调用第三方接口的客户端
 */
public class YApiClient {

    private String accessKey;

    private String secretKey;

    // 新增常量（以后做成可配置）
    private static final String GATEWAY_HOST = "http://localhost:18098";

    public YApiClient() {
    }

    public YApiClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    // 使用 GET 方法从服务器获取名称信息
    public String getNameByGet(String name) {
        // 可以单独传入 http 参数，这样参数会自动做 URL 编码，拼接在 URL 中
        HashMap<String, Object> paramMap = new HashMap<>();
        // 将 name 参数添加到映射中
        paramMap.put("name", name);
        // 使用 HttpUtil 工具发起 GET 请求，并获取服务器返回的结果
        String result = HttpUtil.get("http://localhost:8123/api/name/", paramMap);
        // 打印服务器返回的结果
        System.out.println(result);
        // 返回服务器返回的结果
        return result;
    }

    // 使用 POST 方法从服务器获取名称信息
    public String getNameByPost(String name) {
        // 可以单独传入 http 参数，这样参数会自动做 URL 编码，拼接在 URL 中
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", name);
        // 使用 HttpUtil 工具发起 POST 请求，并获取服务器返回的结果
        String result = HttpUtil.post("http://localhost:8123/api/name/post", paramMap);
        System.out.println(result);
        return result;
    }

    // 创建私有方法，用于构造请求头
    private Map<String, String> getHeaderMap(String url, String method, String body) {
        // 创建一个新的 HashMap 对象
        Map<String, String> hashMap = new HashMap<>();
        // 将 "accessKey" 和其对应的值放入 map 中
        hashMap.put("accessKey", accessKey);
        // 生成随机数（生成一个包含 4 个随机数字的字符串）
        String nonce = RandomUtil.randomNumbers(8);
        hashMap.put("nonce", nonce);
        // 请求体内容，URL 编码避免中文乱码
        hashMap.put("body", URLUtil.encode(body));
        // 当前时间戳
        // System.currentTimeMillis() 返回当前时间的毫秒数，通过除以 1000， 可以将毫秒数转换成秒数，以得到当前时间的秒数
        // String.valueOf() 方法用于将数值转换成字符串
        hashMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        // 生成签名
        String content = url + "\n" + method + "\n" + body;
        hashMap.put("sign", SignUtils.genSign(content, secretKey));
        // 返回构造的请求头 map
        return hashMap;
    }

    // 获取接口地址和参数，使用 POST 或者 GET 方式像服务器发送请求来判断接口是否能调用
    public String invokeInterface(InterfaceInfo interfaceInfo) {
        // 获取参数
        String url = interfaceInfo.getUrl();
        String requestHeader = interfaceInfo.getRequestHeader();
        String method = interfaceInfo.getMethod();
        String requestParams = interfaceInfo.getRequestParams();
        HttpRequest httpRequest;
        // 根据请求类型构造请求：GET 请求将参数拼接在 URL 中（形如 name=xxx&age=18），其他请求将参数放入请求体（JSON 字符串）
        if ("GET".equalsIgnoreCase(method)) {
            httpRequest = HttpRequest.get(GATEWAY_HOST + url + "?" + requestParams);
        } else {
            httpRequest = HttpRequest.post(GATEWAY_HOST + url).body(requestParams);
        }
        // 添加接口本身配置的请求头（requestHeader 为 JSON 格式字符串，例如 {"Content-Type":"application/json"}）
        if (StrUtil.isNotBlank(requestHeader)) {
            // 用 toBean 和 TypeReference 将 requestHeader 转换成 map 格式
            Map<String, String> headerMap = JSONUtil.toBean(requestHeader, new TypeReference<Map<String, String>>() {
            }, false);
            httpRequest.addHeaders(headerMap);
        }

        // 添加鉴权请求头（accessKey、nonce、timestamp、sign 等）
        httpRequest.addHeaders(getHeaderMap(url, method, requestParams));

        // 执行请求
        HttpResponse httpResponse = httpRequest.execute();

        String result = httpResponse.body();
        int status = httpResponse.getStatus();
//        System.out.println(status);
        // 2xx 表示调用成功，返回响应内容供展示；否则抛异常，调用方捕获后不允许发布
        if (httpResponse.isOk()) {
            return result;
        } else {
            throw new RuntimeException("接口调用失败，状态码: " + status + ", 响应：" + result);
        }
    }
}
