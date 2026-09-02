package cn.y.yapigateway.filter;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.y.yapiclient.innerservice.InnerInterfaceInfoService;
import cn.y.yapiclient.innerservice.InnerUserInterfaceService;
import cn.y.yapiclient.innerservice.InnerUserService;
import cn.y.yapiclientsdk.utils.SignUtils;
import cn.y.yapicommon.constant.RedisKeyConstant;
import cn.y.yapicommon.exception.BusinessException;
import cn.y.yapicommon.ratelimit.enums.RateLimitType;
import cn.y.yapicommon.ratelimit.manager.RedissonRateLimiterManager;
import cn.y.yapigateway.filter.support.GatewayPathMatcher;
import cn.y.yapimodel.entity.InterfaceInfo;
import cn.y.yapimodel.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * SDK 调用流量：IP 限流 + AK/SK 鉴权 + 用户限流 + 防重放/时间戳/签名校验 + 接口校验限流 + 转发调用计数
 */
@Component
@Slf4j
public class ApiAuthFilter implements GlobalFilter, Ordered {

    @DubboReference
    private InnerUserInterfaceService innerUserInterfaceService;

    @DubboReference
    private InnerInterfaceInfoService innerInterfaceInfoService;

    @DubboReference
    private InnerUserService innerUserService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonRateLimiterManager rateLimiterManager;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = request.getId();
        String path = request.getPath().value();
        String method = request.getMethod().toString();

        // 文档/管理端流量不属于本过滤器，直接放行（已由 AdminTrafficFilter 处理）
        if (GatewayPathMatcher.isWebOrDocPath(path)) {
            return chain.filter(exchange);
        }

        // 拿到响应对象
        ServerHttpResponse response = exchange.getResponse();
        // 2. 访问控制 -（黑白名单）：客户端 IP 需在白名单内
        String clientIp = request.getRemoteAddress().getAddress().getHostAddress();
        // 匿名 IP 限流：防无效请求刷网关（鉴权失败的流量也拦得住）
        boolean allowed = rateLimiterManager.doRateLimit(
                RateLimitType.IP.getPrefix() + clientIp, 20, 1);
        if (!allowed) {
            log.warn("IP 限流: ip={}", clientIp);
            return handleError(response, HttpStatus.TOO_MANY_REQUESTS, 42900,"请求过于频繁，请稍后再试", requestId);
        }

        // 3. 用户鉴权（判断 ak、sk 是否合法）
        // 从请求头中获取名为 "accessKey" 的值
        HttpHeaders headers = request.getHeaders();
        String accessKey = headers.getFirst("accessKey");
        String nonce = headers.getFirst("nonce");
        String bodyHeader = headers.getFirst("body");
        String body = null;
        if (bodyHeader == null) {
            return handleError(response, HttpStatus.BAD_REQUEST, 40000, "缺少 body 请求头", requestId);
        }
        try {
            body = URLDecoder.decode(headers.getFirst("body"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return handleError(response, HttpStatus.BAD_REQUEST, 40000, "请求头编码错误", requestId);
        }

        String timestamp = headers.getFirst("timestamp");
        String sign = headers.getFirst("sign");

        // todo 2. 校验权限，从数据库中判断是否与用户的 accessKey 相同
        User invokeUser = null;
        try {
            // 调用内部服务，根据密钥访问获取用户信息
            invokeUser = innerUserService.getInvokeUser(accessKey);
            if (invokeUser == null) {
                return handleError(response, HttpStatus.FORBIDDEN, 40100, "accessKey 无效或不存在", requestId);
            }
            boolean userAllowed = rateLimiterManager.doRateLimit(
                    RateLimitType.USER.getPrefix() + invokeUser.getId(), 2, 1
            );
            if (!userAllowed) {
                log.warn("调用方限流: userId={}", invokeUser.getId());
                return handleError(response, HttpStatus.TOO_MANY_REQUESTS, 42900, "请求过于频繁，请稍后再试", requestId);
            }
        } catch (BusinessException e) {
            // 如果用户信息为空，处理未授权情况并返回响应
            return handleError(response, HttpStatus.FORBIDDEN, e.getCode(), e.getMessage(), requestId);
        } catch (Exception e) {
            // 其余异常：统一 500，不泄露内部细节
            log.error("网关调用内部服务异常, requestId={}", requestId, e);
            return handleError(response, HttpStatus.INTERNAL_SERVER_ERROR, 50000, "平台内部错误，请联系管理员", requestId);
        }

        // todo 3. 校验随机数，随机数可以用 hashMap 或 redis 存储
        // 3. 防重放：nonce 只能用一次，Redis SETNX 原子判重
        if (StrUtil.isBlank(nonce)) {
            return handleError(response, HttpStatus.BAD_REQUEST, 40000, "nonce 缺失或格式错误", requestId);
        }
        try {
            Long.parseLong(nonce);
        } catch (NumberFormatException e) {
            return handleError(response, HttpStatus.BAD_REQUEST, 40000, "nonce 缺失或格式错误", requestId);
        }
        String nonceKey = "yapi:nonce:" + accessKey + ":" + nonce;
        // 判断重放的依据是"键存不存在"
        Boolean firstUse = stringRedisTemplate.opsForValue()
                .setIfAbsent(nonceKey, "1", Duration.ofMinutes(5));
        if (!Boolean.TRUE.equals(firstUse)) {
            log.warn("疑似重放请求被拦截, accessKey: {}, nonce: {}", accessKey, nonce);
            return handleError(response, HttpStatus.FORBIDDEN, 40300, "nonce 重复使用，疑似重放请求", requestId);
        }

        // todo 4. 校验时间戳和当前时间的差距，和当前时间不能超过五分钟
        if (StrUtil.isBlank(timestamp)) {
            return handleError(response, HttpStatus.BAD_REQUEST, 40000, "timestamp 缺失或格式错误", requestId);
        }
        long requestTime;
        try {
            requestTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return handleError(response, HttpStatus.BAD_REQUEST, 40000, "timestamp 缺失或格式错误", requestId);
        }
        long currentTime = System.currentTimeMillis() / 1000;
        final long FIVE_MINUTES = 60 * 5L;
        // 绝对值后，过去超过 5 分钟、未来超过 5 分钟都拒
        if (Math.abs(currentTime - requestTime) >= FIVE_MINUTES) {
            return handleError(response, HttpStatus.BAD_REQUEST, 40300, "时间戳无效，时间差超过 5 分钟", requestId);
        }

        // 我们引入 yapi-sdk 这个包
        // todo 5. 校验签名，从数据库中获取 secretKey
        // 获取用户数据库的密钥
        String secretKey = invokeUser.getSecretKey();
        // 使用获取到的密钥对请求体进行签名
        String content = path + "\n" + method + "\n" + body;
        String serverSign = SignUtils.genSign(content, secretKey);
        // 检查请求体中的签名是否为空，或者是否与服务器生成的签名不一样
        if (sign == null || !sign.equals(serverSign)) {
            // 如果签名为空或者签名不一致，返回处理未授权的响应
            return handleError(response, HttpStatus.FORBIDDEN, 40300, "签名校验失败，请检查 secretKey 与签名算法", requestId);
        }

        // todo 5. 请求的接口是否存在
        // todo 从数据库中查询接口是否存在，以及请求方法是否匹配（还可以校验请求参数）
        InterfaceInfo interfaceInfo = null;
        try {
            // 调用内部服务，获取接口信息
            interfaceInfo = innerInterfaceInfoService.getInterfaceInfo(path, method);
            if (interfaceInfo == null) {
                return handleError(response, HttpStatus.NOT_FOUND, 40400, "接口不存在", requestId);
            }
            // 判断接口是否调用过
            innerUserInterfaceService.checkInvokable(invokeUser.getId(), interfaceInfo.getId());
            // 接口调用限流
            boolean interfaceAllowed = rateLimiterManager.doRateLimit(
                    RateLimitType.INTERFACE.getPrefix() + interfaceInfo.getId(), 10, 1
            );
            if (!interfaceAllowed) {
                log.warn("接口限流: interfaceId={}", interfaceInfo.getId());
                return handleError(response, HttpStatus.TOO_MANY_REQUESTS, 42900, "请求过于频繁，请稍后再试", requestId);
            }
            // todo 6. 请求转发，调用接口
            String originalQuery = request.getURI().getRawQuery();
            String target = interfaceInfo.getUrl();
            if (StrUtil.isNotBlank(originalQuery)) {
                // url 本身可能已带 query 参数，此时用 & 连接
                target += (target.contains("?") ? "&" : "?") + originalQuery;
            }
            ServerHttpRequest newRequest = request.mutate()
                    .uri(URI.create(target))
                    .build();
            return handleResponse(exchange.mutate().request(newRequest).build(), chain, interfaceInfo.getId(), invokeUser.getId());
        } catch (BusinessException e) {
            // 其余异常：统一 500，不泄露内部细节
            log.error("网关调用内部服务异常, requestId={}", requestId, e);
            return handleError(response, HttpStatus.FORBIDDEN, e.getCode(), e.getMessage(), requestId);
        } catch (Exception e) {
            // 其余异常：统一 500，不泄露内部细节
            log.error("网关调用内部服务异常, requestId={}", requestId, e);
            return handleError(response, HttpStatus.INTERNAL_SERVER_ERROR, 50000, "平台内部错误，请联系管理员", requestId);
        }

    }

    /**
     * 处理响应
     *
     * @param exchange
     * @param chain
     * @return
     */
    public Mono<Void> handleResponse(ServerWebExchange exchange, GatewayFilterChain chain, long interfaceInfoId, long userId) {
        try {
            // 获取原始的响应对象
            ServerHttpResponse originalResponse = exchange.getResponse();
            // 获取数据缓冲工厂
            DataBufferFactory bufferFactory = originalResponse.bufferFactory();
            ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
                // 重写writeWith方法，用于处理响应体的数据
                // 这段方法就是只要当我们的模拟接口调用完成之后,等它返回结果，
                // 就会调用writeWith方法,我们就能根据响应结果做一些自己的处理
                @Override
                public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                    if (body instanceof Flux) {
                        Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                        // 返回一个处理后的响应体
                        // (这里就理解为它在拼接字符串,它把缓冲区的数据取出来，一点一点拼接好)
                        return super.writeWith(fluxBody.map(dataBuffer -> {
                            if (HttpStatus.OK.equals(getStatusCode())) {
                                if (!innerUserService.isAdmin(userId)) {
                                    // todo 7. 调用成功，接口调用次数 + 1 invokeCount
                                    try {
                                        // 调用内部用户接口信息服务，记录接口调用次数
                                        innerUserInterfaceService.invokeCount(interfaceInfoId, userId);
                                        stringRedisTemplate.opsForZSet().incrementScore(
                                                RedisKeyConstant.INTERFACE_RANK_KEY, String.valueOf(interfaceInfoId), 1);
                                    } catch (Exception e) {
                                        log.error("invokeCount error", e);
                                    }
                                }
                            }
                            // 读取响应体的内容并转换为字节数组
                            byte[] content = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(content);
                            DataBufferUtils.release(dataBuffer);//释放掉内存
                            // 构建日志
                            log.info("响应结果：{}", new String(content, StandardCharsets.UTF_8));
                            // 将处理后的内容重新包装成DataBuffer并返回
                            return bufferFactory.wrap(content);
                        }));
                    } else {
                        log.error("响应结果异常：{}", getStatusCode());
                    }
                    return super.writeWith(body);
                }
            };
            // 对于200 OK的请求,将装饰后的响应对象传递给下一个过滤器链,并继续处理(设置repsonse对象为装饰过的)
            return chain.filter(exchange.mutate().response(decoratedResponse).build());
        } catch (Exception e){
            // 处理异常情况，记录错误日志
            log.error("网关处理响应异常" + e);
            return chain.filter(exchange);
        }
    }

    // 调用失败处理
    private Mono<Void> handleError(ServerHttpResponse response, HttpStatus status,
                                   int code, String message, String requestId) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("requestId", requestId);
        DataBuffer buffer = response.bufferFactory()
                .wrap(JSONUtil.toJsonStr(body).getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
