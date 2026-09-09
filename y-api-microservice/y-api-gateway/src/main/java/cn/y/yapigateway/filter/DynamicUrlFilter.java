package cn.y.yapigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * 过滤第三方转发的host
 */
@Component
@Slf4j
public class DynamicUrlFilter implements GlobalFilter, Ordered {

    /** ApiAuthFilter 查库后把真实地址放进这个属性 */
    public static final String DYNAMIC_TARGET_URL_ATTR = "dynamicTargetUrl";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI target = exchange.getAttribute(DYNAMIC_TARGET_URL_ATTR);
        if (target != null) {
            // 覆盖 RouteToRequestUrlFilter 写好的地址，host 以数据库为准
            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, target);
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 10001;  // 必须晚于 RouteToRequestUrlFilter（10000）
    }
}
