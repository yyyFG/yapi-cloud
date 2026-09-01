package cn.y.yapiuserinterface.service;


import cn.y.yapiclient.innerservice.InnerUserInterfaceService;
import cn.y.yapimodel.entity.UserInterface;
import cn.y.yapiuserinterface.mapper.UserInterfaceMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.redisson.client.codec.StringCodec;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class InvokeCountRedisService {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserInterfaceMapper userInterfaceMapper;

    private RMap<String, Long> leftCache;

    private RMap<String, Long> deltaMap;

    private static final String LEFT_MAP = "invoke:leftNum";
    private static final String DELTA_MAP = "invoke:delta";

    /**
     * 扣减脚本：原子完成「检查剩余次数 → 扣 1 → 记差量」
     * 返回：1 成功 / 0 次数不足 / -1 未申请（字段不存在）
     */
    private static final String DEDUCT_LUA =
            "local left = redis.call('HGET', KEYS[1], ARGV[1]) " +
                    "if left == false then return -1 end " +
                    "if tonumber(left) <= 0 then return 0 end " +
                    "redis.call('HINCRBY', KEYS[1], ARGV[1], -1) " +
                    "redis.call('HINCRBY', KEYS[2], ARGV[1], 1) " +
                    "return 1";

    /**
     * 取差量脚本：原子完成「读出差量 → 清零」两步，供定时任务回写 DB 使用
     * 返回：本次取走的差量（字段不存在返回 0）
     */
    private static final String TAKE_DELTA_LUA =
            "local d = redis.call('HGET', KEYS[1], ARGV[1]) " +
                    "if d == false then return 0 end " +
                    "redis.call('HDEL', KEYS[1], ARGV[1]) " +
                    "return tonumber(d)";


    @PostConstruct
    public void init() {
        leftCache = redissonClient.getMap(LEFT_MAP, StringCodec.INSTANCE);
        deltaMap = redissonClient.getMap(DELTA_MAP, StringCodec.INSTANCE);
    }

    /**
     * 懒加载：缓存 miss 时查 DB。返回 null 表示用户没有申请记录
     */
    private Long loadLeftNumFromDb(String key) {
        String[] parts = key.split(":");
        long userId = Long.parseLong(parts[0]);
        long interfaceId = Long.parseLong(parts[1]);
        // 查 user_interface 表的 leftNum
        UserInterface userInterface = userInterfaceMapper.selectOne(new QueryWrapper<UserInterface>()
                .eq("userId", userId).eq("interfaceId", interfaceId)
                .select("leftNum"));
        return userInterface == null ? null : userInterface.getLeftNum().longValue();
    }

    /**
     * 扣减一次。返回：1 成功 / 0 次数不足 / -1 未申请
     */
    public int deduct(long userId, long interfaceId) {
        String key = key(userId, interfaceId);
        // 手动懒加载：miss → 查 DB → 塞回缓存
        Long cur = leftCache.get(key);
        if (cur == null) {
            Long dbValue = loadLeftNumFromDb(key);
            if (dbValue == null) {
                return -1; //未申请
            }
            // 并发 miss 时多个线程都查到同一个 DB 值，只有一个写入生效，
            // 之后 Lua 会在 Redis 里重新读当前值做原子扣减，所以不会丢
            leftCache.fastPutIfAbsent(key, dbValue);
        }
        // Lua 原子扣减（检查 + 扣减 + 记差量，一次完成）
        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        Long result = script.eval(RScript.Mode.READ_WRITE, DEDUCT_LUA,
                RScript.ReturnType.INTEGER,
                Arrays.asList(LEFT_MAP, DELTA_MAP), key);
        return result == null ? 0 : result.intValue();
    }

    /**
     * 定时任务：每 5 分钟把差量批量回写 DB
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void syncDeltaToDb() {
        RLock lock = redissonClient.getLock("invoke:syncLock");
        try {
            if (!lock.tryLock(0, 60, TimeUnit.SECONDS)) {
                return;
            }
            RScript script = redissonClient.getScript(StringCodec.INSTANCE);
            for (Map.Entry<String, Long> entry : deltaMap.entrySet()) {
                String key = entry.getKey();
                Long delta = script.eval(RScript.Mode.READ_WRITE, TAKE_DELTA_LUA,
                        RScript.ReturnType.INTEGER,
                        Collections.singletonList(DELTA_MAP), key);
                if (delta == null || delta <= 0) continue;

                String[] parts = key.split(":");
                long userId = Long.parseLong(parts[0]);
                long interfaceId = Long.parseLong(parts[1]);

                UpdateWrapper<UserInterface> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("userId", userId).eq("interfaceId", interfaceId)
                        .ge("leftNum", delta)
                        .setSql("leftNum = leftNum - " + delta);
                boolean update = userInterfaceMapper.update(null, updateWrapper) > 0;
                if (!update) {
                    deltaMap.addAndGet(key, delta);
                    log.error("invoke count 回写失败，key={}, delta={}", key, delta);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 管理员修改/删除记录后调用：清缓存，下次扣减重新懒加载
     */
    public void evict(long userId, long interfaceId) {
        leftCache.remove(key(userId, interfaceId));
    }

    private static String key(long userId, long interfaceId) {
        return userId + ":" + interfaceId;
    }
}
