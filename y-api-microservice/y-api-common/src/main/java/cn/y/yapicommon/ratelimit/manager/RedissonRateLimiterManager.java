package cn.y.yapicommon.ratelimit.manager;


import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class RedissonRateLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 执行限流
     * @param key             限流键（调用方自行拼接，如 yapi:ratelimit:user:123）
     * @param rate            每个时间窗口允许的请求数
     * @param rateInterval    时间窗口（秒）
     * @return true-放行，false-被限流
     */
    public boolean doRateLimit(String key, long rate, long rateInterval) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL, rate, rateInterval, RateIntervalUnit.SECONDS);
        return rateLimiter.tryAcquire();
    }
}
