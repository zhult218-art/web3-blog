package com.web3.aiproxy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 类名：AsyncConfig
 * 所属模块：ai-proxy-service（中转站）
 * 职责：异步任务线程池 —— 请求日志落库走异步，不阻塞主转发链路。
 */
@Configuration
public class AsyncConfig {

    @Bean("proxyLogExecutor")
    public Executor proxyLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(5000);
        executor.setThreadNamePrefix("proxy-log-");
        executor.initialize();
        return executor;
    }
}
