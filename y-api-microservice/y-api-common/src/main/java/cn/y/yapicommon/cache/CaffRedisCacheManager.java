package cn.y.yapicommon.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.AbstractCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.util.Collection;
import java.util.Collections;

/**
 * 两级缓存管理器：每个缓存名组装成一个 CaffRedisCache（本地 + Redis）
 */
public class CaffRedisCacheManager extends AbstractCacheManager {

    private final CaffeineCacheManager localManager;
    private final RedisCacheManager remoteManager;

    public CaffRedisCacheManager(CaffeineCacheManager localManager, RedisCacheManager remoteManager) {
        this.localManager = localManager;
        this.remoteManager = remoteManager;
    }

    @Override
    protected Collection<? extends Cache> loadCaches() {
        return Collections.emptyList();
    }

    @Override
    protected Cache getMissingCache(String name) {
        return new CaffRedisCache(name, localManager.getCache(name), remoteManager.getCache(name));
    }
}
