package cn.y.yapigateway;

import cn.hutool.core.text.AntPathMatcher;
import cn.hutool.core.util.StrUtil;
import cn.y.yapiclient.innerservice.InnerInterfaceInfoService;
import cn.y.yapiclient.innerservice.InnerUserInterfaceService;
import cn.y.yapiclient.innerservice.InnerUserService;
import cn.y.yapiclientsdk.utils.SignUtils;
import cn.y.yapicommon.exception.BusinessException;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 全局过滤
 */
@Component
@Slf4j
public class CustomGlobalFilter implements GlobalFilter, Ordered {

    @DubboReference
    private InnerUserInterfaceService innerUserInterfaceService;

    @DubboReference
    private InnerInterfaceInfoService innerInterfaceInfoService;

    @DubboReference
    private InnerUserService innerUserService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    private static final List<String> IP_WHITE_LIST = Arrays.asList("127.0.0.1");

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    /** 管理端业务路径：放行，权限由各服务 @AuthCheck 负责 */
    private static final List<String> ADMIN_WHITE_LIST = Arrays.asList(
            "/user/**", "/interfaceInfo/**", "/userInterface/**");

    /** knife4j 文档相关路径，放行不经鉴权 */
    private static final List<String> DOC_WHITE_LIST = Arrays.asList(
            "/interfaceInfo/v2/api-docs/**", "/user/v2/api-docs/**", "/userInterface/v2/api-docs/**",
            "/doc.html", "/webjars/**",
            "/swagger-resources/**", "/favicon.ico"
    );

    // 判断路径是否在白名单里
    private boolean isInWhiteList(String path, List<String> patterns) {
        return patterns.stream().anyMatch(p -> ANT_PATH_MATCHER.match(p, path));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 请求日志
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().toString();
        log.info("请求唯一标识：" + request.getId());
        log.info("请求路径：" + path);
        log.info("请求方法：" + method);
        log.info("请求参数：" + request.getQueryParams());
        // 本地地址：网关自己接收这条连接的本机网卡 IP（比如服务器的 127.0.0.1:8080）
        String sourceAddress = request.getLocalAddress().getHostString();
        log.info("请求来源地址：" + sourceAddress);
        // 远端地址：发起请求的客户端的 IP:端口（比如浏览器/调用方的 192.168.1.100:54321）
        log.info("请求来源地址：" + request.getRemoteAddress());

        // 文档路径和管理端路径直接放行，不走 AK/SK 鉴权
        if (isInWhiteList(path, DOC_WHITE_LIST) || isInWhiteList(path, ADMIN_WHITE_LIST)) {
            return chain.filter(exchange);
        }

        // 拿到响应对象
        ServerHttpResponse response = exchange.getResponse();
        // 2. 访问控制 -（黑白名单）
        if (!IP_WHITE_LIST.contains(sourceAddress)) {
            // 设置响应状态码 403 Forbidden（禁止访问）
            response.setStatusCode(HttpStatus.FORBIDDEN);
            // 返回处理完成的响应
            return response.setComplete();
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
        long currentTime = System.currentTimeMillis() / 1000;
        long requestTime = Long.parseLong(timestamp);
        final long FIVE_MINUTES = 60 * 5L;
        if ((currentTime - requestTime) >= FIVE_MINUTES) {
            return handleNoAuth(response);
        }

        // 我们引入 yapi-sdk 这个包
        // todo 5. 校验签名，从数据库中获取 secretKey
        // 获取用户数据库的密钥
        String secretKey = invokeUser.getSecretKey();
        // 使用获取到的密钥对请求体进行签名
        String serverSign = SignUtils.genSign(body, secretKey);
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

    @Override
    public int getOrder() {
        return -1;
    }
}
