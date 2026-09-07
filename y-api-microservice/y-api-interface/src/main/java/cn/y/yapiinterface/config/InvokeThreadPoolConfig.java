package cn.y.yapiinterface.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class InvokeThreadPoolConfig {

    @Bean("invokeExecutor")
    public ThreadPoolTaskExecutor invokeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);       // 核心线程数
        executor.setMaxPoolSize(50);        // 最大线程数
        executor.setQueueCapacity(200);     // 等待队列
        executor.setThreadNamePrefix("invoke-");
        // 队列满时的拒绝策略：由调用线程自己执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
