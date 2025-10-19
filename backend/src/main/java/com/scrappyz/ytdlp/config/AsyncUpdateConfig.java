package com.scrappyz.ytdlp.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncUpdateConfig {

    @Bean(name = "updateExecutor")
    public ThreadPoolTaskExecutor downloadsExecutor(
        @Value("${resource.pool.core:1}") int core,
        @Value("${resource.pool.max:1}") int max,
        @Value("${resource.queue.capacity:1}") int queueCap) {

        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("updater-");
        ex.setCorePoolSize(core);
        ex.setMaxPoolSize(max);
        ex.setQueueCapacity(queueCap);
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        ex.initialize();
        return ex;
    }

}
