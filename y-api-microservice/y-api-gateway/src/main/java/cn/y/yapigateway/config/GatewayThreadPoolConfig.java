package cn.y.yapigateway.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class GatewayThreadPoolConfig {

    @Bean("gatewayAsyncExecutor")
    public ThreadPoolTaskExecutor gatewayAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);       // 核心线程数
        executor.setMaxPoolSize(50);        // 最大线程数
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("gateway-async-");
        executor.initialize();
        return executor;
    }
}