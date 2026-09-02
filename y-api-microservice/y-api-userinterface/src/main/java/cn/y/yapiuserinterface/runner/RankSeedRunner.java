package cn.y.yapiuserinterface.runner;

import cn.y.yapicommon.constant.RedisKeyConstant;
import cn.y.yapimodel.entity.UserInterface;
import cn.y.yapiuserinterface.mapper.UserInterfaceMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 排行榜初始化工具类
 */
@Component
@Slf4j
public class RankSeedRunner implements ApplicationRunner {

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private UserInterfaceMapper userInterfaceMapper;

    @Override
    public void run(ApplicationArguments args){
        RLock lock = redissonClient.getLock("invoke:rank:seedLock");
        try {
            if (!lock.tryLock(0, 60, TimeUnit.SECONDS)) {
                return;  // 别的实例在初始化
            }
            RScoredSortedSet<String> rankSet = redissonClient.getScoredSortedSet(
                    RedisKeyConstant.INTERFACE_RANK_KEY, StringCodec.INSTANCE);
            if (!rankSet.isEmpty()) {
                return;  // 已有数据（灌过或已累积实时调用），跳过
            }
            // 聚合每个接口的历史总调用次数
            List<Map<String, Object>> rows = userInterfaceMapper.selectMaps(
                    new QueryWrapper<UserInterface>()
                            .select("interfaceId", "SUM(totalNum) AS total")
                            .groupBy("interfaceId"));
            for (Map<String, Object> row : rows) {
                Long interfaceId = ((Number) row.get("interfaceId")).longValue();
                Long total = row.get("total") == null ? 0 : ((Number) row.get("total")).longValue();
                rankSet.addScore(String.valueOf(interfaceId), total);  // ZINCRBY 累加，不覆盖
            }
            log.info("排行榜初始化完成，共 {} 个接口", rows.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // 每天凌晨 4:30 对账一次
    @Scheduled(cron = "0 30 4 * * *")
    public void reconcileRank() {
        RLock lock = redissonClient.getLock("invoke:rank:reconcileLock");
        try {
            if (!lock.tryLock(0, 60, TimeUnit.SECONDS)) {
                return;  // 别的实例在初始化
            }
            RScoredSortedSet<String> rankSet = redissonClient.getScoredSortedSet(
                    RedisKeyConstant.INTERFACE_RANK_KEY, StringCodec.INSTANCE);
            List<Map<String, Object>> rows = userInterfaceMapper.selectMaps(
                    new QueryWrapper<UserInterface>()
                            .select("interfaceId", "SUM(totalNum) AS total")
                            .groupBy("interfaceId"));
            for (Map<String, Object> row : rows) {
                Long interfaceId = ((Number) row.get("interfaceId")).longValue();
                long dbTotal = row.get("total") == null ? 0 : ((Number) row.get("total")).longValue();
                Double zsetScore = rankSet.getScore(String.valueOf(interfaceId));
                long current = zsetScore == null ? 0 : zsetScore.longValue();
                // 只补差值（ZINCRBY），不覆盖——对账期间的新调用不会被抹掉
                rankSet.addScore(String.valueOf(interfaceId), dbTotal - current);
            }
        } catch  (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }
}
