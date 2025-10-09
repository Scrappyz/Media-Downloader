package com.scrappyz.ytdlp.config;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class DownloadAsyncConfig {
    
    @Bean(name = "downloadExecutor")
    public ThreadPoolTaskExecutor downloadsExecutor(
        @Value("${downloads.pool.core:2}") int core,
        @Value("${downloads.pool.max:4}") int max,
        @Value("${downloads.queue.capacity:50}") int queueCap) {

        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("download-");
        ex.setCorePoolSize(core);
        ex.setMaxPoolSize(max);
        ex.setQueueCapacity(queueCap);
        // Reject when saturated → you’ll return HTTP 429
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        ex.initialize();
        return ex;
    }

    /** Optional global hard cap across the JVM (extra guard). */
    @Bean
    public Semaphore downloadPermits(@Value("${downloads.max.concurrent:4}") int n) {
        return new Semaphore(n);
    }
}
