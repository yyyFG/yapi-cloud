package cn.y.yapicommon.cache;


import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存（L1）：Caffeine，TTL 需短于 Redis 层
 */
@Configuration
@EnableCaching
public class LocalCacheConfig {


    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)                                 // 最多 1000 条，防内存膨胀
                .expireAfterWrite(5, TimeUnit.MINUTES));   // 写入 5 分钟后自动过期（兜底）
        return caffeineCacheManager;
    }
}
