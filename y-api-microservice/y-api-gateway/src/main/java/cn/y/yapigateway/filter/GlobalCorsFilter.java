package cn.y.yapigateway.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

// 过滤器：统一 CORS 跨域策略
@Configuration
public class GlobalCorsFilter {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter filter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedMethod(HttpMethod.GET);                 // 允许 GET 请求
        config.addAllowedMethod(HttpMethod.POST);                // 允许 POST 请求
        config.addAllowedMethod(HttpMethod.OPTIONS);             // 允许 OPTIONS 预检请求
        config.addAllowedHeader(CorsConfiguration.ALL);          // 允许所有请求头
        config.addAllowedOriginPattern(CorsConfiguration.ALL);   // 允许所有来源
        config.setAllowCredentials(true);                        // 允许携带 Cookie（前端 withCredentials: true）
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.registerCorsConfiguration("/**", config);         // 应用到所有路径
        return new CorsWebFilter(source);
    }
}
