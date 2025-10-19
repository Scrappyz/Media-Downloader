package com.scrappyz.ytdlp.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncResourceManagerConfig {

    @Bean(name = "resourceExecutor")
    public ThreadPoolTaskExecutor downloadsExecutor(
        @Value("${resource.pool.core:2}") int core,
        @Value("${resource.pool.max:4}") int max,
        @Value("${resource.queue.capacity:50}") int queueCap) {

        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("resource-");
        ex.setCorePoolSize(core);
        ex.setMaxPoolSize(max);
        ex.setQueueCapacity(queueCap);
        // Reject when saturated → you’ll return HTTP 429
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        ex.initialize();
        return ex;
    }

}
