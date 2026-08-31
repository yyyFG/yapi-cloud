package cn.y.yapigateway.filter;


import cn.hutool.core.util.StrUtil;
import cn.y.yapiclient.innerservice.InnerInterfaceInfoService;
import cn.y.yapiclient.innerservice.InnerUserInterfaceService;
import cn.y.yapiclient.innerservice.InnerUserService;
import cn.y.yapiclientsdk.utils.SignUtils;
import cn.y.yapicommon.exception.BusinessException;
import cn.y.yapicommon.ratelimit.enums.RateLimitType;
import cn.y.yapicommon.ratelimit.manager.RedissonRateLimiterManager;
import cn.y.yapigateway.filter.support.GatewayPathMatcher;
import cn.y.yapimodel.entity.InterfaceInfo;
import cn.y.yapimodel.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import java.util.List;


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
            return handleRateLimit(response);
        }

        // 3. 用户鉴权（判断 ak、sk 是否合法）
        // 从请求头中获取名为 "accessKey" 的值
        HttpHeaders headers = request.getHeaders();
        String accessKey = headers.getFirst("accessKey");
        String nonce = headers.getFirst("nonce");
//        String body = headers.getFirst("body");
        String body = null;
        try {
            body = URLDecoder.decode(headers.getFirst("body"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return handleNoAuth(response);
        }
        String timestamp = headers.getFirst("timestamp");
        String sign = headers.getFirst("sign");

        // todo 2. 校验权限，从数据库中判断是否与用户的 accessKey 相同
        User invokeUser = null;
        try {
            // 调用内部服务，根据密钥访问获取用户信息
            invokeUser = innerUserService.getInvokeUser(accessKey);
            if (invokeUser == null) {
                return handleNoAuth(response);
            }
            boolean userAllowed = rateLimiterManager.doRateLimit(
                    RateLimitType.USER.getPrefix() + invokeUser.getId(), 2, 1
            );
            if (!userAllowed) {
                log.warn("调用方限流: userId={}", invokeUser.getId());
                return handleRateLimit(response);
            }
        } catch (BusinessException e) {
            // 如果用户信息为空，处理未授权情况并返回响应
            return handleNoAuth(response);
        } catch (Exception e) {
            // 捕获异常，记录日志
            log.error("getinvokeUser error", e);
            return handleInvokeError(response);
        }

        // todo 3. 校验随机数，随机数可以用 hashMap 或 redis 存储
        // 3. 防重放：nonce 只能用一次，Redis SETNX 原子判重
        if (StrUtil.isBlank(nonce)) {
            return handleNoAuth(response);
        }
        try {
            Long.parseLong(nonce);
        } catch (NumberFormatException e) {
            return handleNoAuth(response);
        }
        String nonceKey = "yapi:nonce:" + accessKey + ":" + nonce;
        Boolean firstUse = stringRedisTemplate.opsForValue()
                .setIfAbsent(nonceKey, "1", Duration.ofMinutes(5));
        if (!Boolean.TRUE.equals(firstUse)) {
            log.warn("疑似重放请求被拦截, accessKey: {}, nonce: {}", accessKey, nonce);
            return handleNoAuth(response);
        }

        // todo 4. 校验时间戳和当前时间的差距，和当前时间不能超过五分钟
        if (StrUtil.isBlank(timestamp)) {
            return handleNoAuth(response);
        }
        long requestTime;
        try {
            requestTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return handleNoAuth(response);
        }
        long currentTime = System.currentTimeMillis() / 1000;
        final long FIVE_MINUTES = 60 * 5L;
        // 绝对值后，过去超过 5 分钟、未来超过 5 分钟都拒
        if (Math.abs(currentTime - requestTime) >= FIVE_MINUTES) {
            return handleNoAuth(response);
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
            return handleNoAuth(response);
        }

        // todo 5. 请求的接口是否存在
        // todo 从数据库中查询接口是否存在，以及请求方法是否匹配（还可以校验请求参数）
        InterfaceInfo interfaceInfo = null;
        try {
            // 调用内部服务，获取接口信息
            interfaceInfo = innerInterfaceInfoService.getInterfaceInfo(path, method);
            if (interfaceInfo == null) {
                return handleInvokeError(response);
            }
            // 接口调用限流
            boolean interfaceAllowed = rateLimiterManager.doRateLimit(
                    RateLimitType.INTERFACE.getPrefix() + interfaceInfo.getId(), 10, 1
            );
            if (!interfaceAllowed) {
                log.warn("接口限流: interfaceId={}", interfaceInfo.getId());
                return handleRateLimit(response);
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
            // 如果未获取到接口信息，返回处理未授权的响应
            return handleNoAuth(response);
        } catch (Exception e) {
            // 如果获取接口信息出现异常，记录错误日志
            log.error("getInterfaceInfo error", e);
            return handleInvokeError(response);
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
        }catch (Exception e){
            // 处理异常情况，记录错误日志
            log.error("网关处理响应异常" + e);
            return chain.filter(exchange);
        }
    }

    // 调用失败处理
    public Mono<Void> handleNoAuth(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    // 接口调用失败处理
    public Mono<Void> handleInvokeError(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return response.setComplete();
    }

    // 限流处理
    public Mono<Void> handleRateLimit(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
