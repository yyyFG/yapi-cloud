package cn.y.yapicommon.cache;


import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;

@Configuration
public class CaffRedisCacheConfig {

    @Bean
    @Primary   // 三个 CacheManager 并存时，@Cacheable/@CacheEvict 走这个
    public CacheManager cacheManager(CaffeineCacheManager caffeineCacheManager,
                                     RedisCacheManager redisCacheManager) {
        return new CaffRedisCacheManager(caffeineCacheManager, redisCacheManager);
    }
}
