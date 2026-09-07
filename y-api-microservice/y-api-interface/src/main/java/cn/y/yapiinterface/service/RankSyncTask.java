package cn.y.yapiinterface.service;

import cn.y.yapicommon.constant.RedisKeyConstant;
import cn.y.yapimodel.entity.InterfaceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    @Scheduled(fixedDelay = 600_000, initialDelay = 60_000)
    public void syncRankToDb() {
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .rangeWithScores(RedisKeyConstant.INTERFACE_RANK_KEY, 0, -1);
        if (tuples == null || tuples.isEmpty()) return;
        // 一次性查出当前 DB 值，内存对比（一条 SELECT）
        Map<Long, Integer> dbMap = interfaceInfoService.list().stream()
                .collect(Collectors.toMap(InterfaceInfo::getId,
                        i -> i.getInvokeCount() == null ? 0 : i.getInvokeCount()));
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            Long id = Long.valueOf(t.getValue());
            int score = t.getScore().intValue();
            Integer dbValue = dbMap.get(id);
            if (dbValue == null || dbValue != score) {   // ← 变了才写
                InterfaceInfo update = new InterfaceInfo();
                update.setId(id);
                update.setInvokeCount(score);
                interfaceInfoService.updateById(update);
            }
        }
    }

}

