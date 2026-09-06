package cn.y.yapiinterface.service;

import cn.y.yapicommon.constant.RedisKeyConstant;
import cn.y.yapimodel.entity.InterfaceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;

@Component
@Slf4j
public class RankSyncTask {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private InterfaceInfoService interfaceInfoService;

    /**
     * 每 5 分钟把排行榜 ZSET 分数覆盖同步到 interface_info.invokeCount
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void syncRankToDb() {
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .rangeWithScores(RedisKeyConstant.INTERFACE_RANK_KEY, 0, -1);
        if (tuples == null || tuples.isEmpty()) return;
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            InterfaceInfo update = new InterfaceInfo();
            update.setId(Long.valueOf(t.getValue()));
            update.setInvokeCount(t.getScore().intValue());
            interfaceInfoService.updateById(update);   // 只更新非 null 字段
        }
    }
}

