package cn.y.yapiinterface.runner;

import cn.y.yapicommon.constant.RedisKeyConstant;
import cn.y.yapiinterface.service.InterfaceInfoService;
import cn.y.yapimodel.entity.InterfaceInfo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
@Slf4j
public class RankSeedRunner implements ApplicationRunner {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private InterfaceInfoService interfaceInfoService;

    @Override
    public void run(ApplicationArguments args) {
        String rankKey = RedisKeyConstant.INTERFACE_RANK_KEY;
        // 幂等：ZSET 已有数据（灌过或已累积实时调用）就跳过
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(rankKey))) {
            Long size = stringRedisTemplate.opsForZSet().zCard(rankKey);
            if (size != null && size > 0) return;
        }
        List<InterfaceInfo> list = interfaceInfoService.list(
                new QueryWrapper<InterfaceInfo>()
                        .select("id", "invokeCount")
                        .gt("invokeCount", 0));   // @TableLogic 自动过滤已删
        for (InterfaceInfo info : list) {
            stringRedisTemplate.opsForZSet().add(rankKey,
                    String.valueOf(info.getId()), info.getInvokeCount());
        }
        log.info("排行榜初始化完成，共 {} 个接口", list.size());
    }
}

