package cn.y.yapicommon.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String INVOKE_EXCHANGE = "yapi.invoke.exchange";
    public static final String INVOKE_COUNT_QUEUE = "yapi.invoke.count.queue";
    public static final String INVOKE_LOG_QUEUE = "yapi.invoke.log.queue";

    // 使用 Fanout 交换机，一条消息广播给计数队列和日志队列
    @Bean
    public FanoutExchange invokeExchange() {
        return new FanoutExchange(INVOKE_EXCHANGE);
    }

    @Bean
    public Queue invokeCountQueue() {
        return new Queue(INVOKE_COUNT_QUEUE, true);
    }

    @Bean
    public Queue invokeLogQueue() {
        return new Queue(INVOKE_LOG_QUEUE, true);
    }

    @Bean
    public Binding countBinding() {
        return BindingBuilder.bind(invokeCountQueue()).to(invokeExchange());
    }

    @Bean
    public Binding logBinding() {
        return BindingBuilder.bind(invokeLogQueue()).to(invokeExchange());
    }
}
