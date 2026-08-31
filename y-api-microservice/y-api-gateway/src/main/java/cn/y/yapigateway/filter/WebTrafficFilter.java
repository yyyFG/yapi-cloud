package cn.y.yapigateway.filter;


import cn.y.yapicommon.ratelimit.enums.RateLimitType;
import cn.y.yapicommon.ratelimit.manager.RedissonRateLimiterManager;
import cn.y.yapigateway.filter.support.GatewayPathMatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * 平台网站流量：日志 + 文档放行 + 管理端按 IP 限流后放行
 */
@Component
@Slf4j
public class WebTrafficFilter implements GlobalFilter, Ordered {

    @Resource
    private RedissonRateLimiterManager rateLimiterManager;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 请求日志（所有流量都从这里过，日志放这最合适）
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        log.info("请求路径：{}，方法：{}，参数：{}",
                path, request.getMethod(), request.getQueryParams());

        // 2. 文档路径：直接放行
        if (GatewayPathMatcher.isDocPath(path)) {
            return chain.filter(exchange);
        }

        // 3. 平台路径：按 IP 限流后放行，权限由各服务 @AuthCheck 负责
        if (GatewayPathMatcher.isWebPath(path)) {
            String clientIp = request.getRemoteAddress().getAddress().getHostAddress();
            boolean allowed = rateLimiterManager.doRateLimit(
                    RateLimitType.WEB.getPrefix() + clientIp, 20, 1);
            if (!allowed) {
                log.warn("平台限流: ip={}", clientIp);
                return handleRateLimit(exchange.getResponse());
            }
            return chain.filter(exchange);
        }

        // 4. 其余路径不属于本过滤器，交给 ApiAuthFilter
        return chain.filter(exchange);
    }

    private Mono<Void> handleRateLimit(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -10;   // 先于 ApiAuthFilter 执行
    }
}
