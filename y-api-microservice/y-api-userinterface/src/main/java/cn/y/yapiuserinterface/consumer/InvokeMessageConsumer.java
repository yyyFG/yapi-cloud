package cn.y.yapiuserinterface.consumer;

import cn.y.yapicommon.exception.BusinessException;
import cn.y.yapimodel.dto.invoke.InvokeMessage;
import cn.y.yapicommon.config.RabbitMqConfig;
import cn.y.yapimodel.entity.InvokeLog;
import cn.y.yapiuserinterface.mapper.InvokeLogMapper;
import cn.y.yapiuserinterface.service.UserInterfaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Date;

@Component
@Slf4j
public class InvokeMessageConsumer {

    @Resource
    private UserInterfaceService userInterfaceService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private InvokeLogMapper invokeLogMapper;

    @RabbitListener(queues = RabbitMqConfig.INVOKE_COUNT_QUEUE)
    public void onInvoke(InvokeMessage msg) {
        // 幂等：MQ 至少投递一次，重复消息直接丢弃（30 分钟内同 msgId 只处理一次）
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent("yapi:invoke:msg:" + msg.getMsgId(), "1", Duration.ofMinutes(30));
        if (!Boolean.TRUE.equals(first)) {
            return;
        }
        try {
            userInterfaceService.invokeCount(msg.getInterfaceId(), msg.getUserId());
        } catch (BusinessException e) {
            // 业务失败（次数不足等）：不重试，记日志即可（调用前 checkInvokable 已挡一道）
            log.warn("调用计数业务失败, userId={}, interfaceId={}, msg={}",
                    msg.getUserId(), msg.getInterfaceId(), e.getMessage());
        } catch (Exception e) {
            // 系统异常：抛出让 MQ 重试（需配重试次数，见下）
            log.error("调用计数异常", e);
            throw new AmqpRejectAndDontRequeueException(e);   // 或删掉这行用默认重试
        }
    }

    // 日志消费者
    @RabbitListener(queues = RabbitMqConfig.INVOKE_LOG_QUEUE)
    public void onInvokeLog(InvokeMessage msg) {
        InvokeLog logEntry = new InvokeLog();
        logEntry.setUserId(msg.getUserId());
        logEntry.setInterfaceId(msg.getInterfaceId());
        logEntry.setRequestTime(new Date(msg.getTimestamp()));
        logEntry.setSuccess(1);
        invokeLogMapper.insert(logEntry);
    }
}
