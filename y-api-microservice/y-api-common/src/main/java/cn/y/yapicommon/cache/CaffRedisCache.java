package cn.y.yapicommon.cache;


import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

/**
 * 两级缓存：本地（Caffeine）→ Redis → DB
 */
public class CaffRedisCache implements Cache {

    private final String name;
    private final Cache local;
    private final Cache remote;

    public CaffRedisCache(String name, Cache local, Cache remote) {
        this.name = name;
        this.local = local;
        this.remote = remote;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper wrapper = local.get(key);
        if (wrapper != null) {
            return wrapper;
        }
        wrapper = remote.get(key);
        if (wrapper != null) {
            Object value = wrapper.get();
            if (value != null) {
                local.put(key, value);   // Redis 命中，回填本地
            }
            return wrapper;
        }
        return null;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper wrapper = get(key);
        return wrapper == null ? null : type.cast(wrapper.get());
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        local.put(key, value);
        remote.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper existing = get(key);
        if (existing == null) {
            put(key, value);
        }
        return existing;
    }

    @Override
    public void evict(Object key) {
        local.evict(key);
        remote.evict(key);
    }

    @Override
    public void clear() {
        local.clear();
        remote.clear();
    }
}
