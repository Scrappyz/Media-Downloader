package com.scrappyz.ytdlp.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncDownloadConfig {
    
    @Bean(name = "downloadExecutor")
    public ExecutorService downloadsExecutor(
        @Value("${downloads.pool.core:2}") int core,
        @Value("${downloads.pool.max:4}") int max,
        @Value("${downloads.queue.capacity:50}") int queueCap) {

        ExecutorService ex = new ThreadPoolExecutor(core, max, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(queueCap),
        tf -> {
            Thread t = new Thread(tf);
            t.setName("download-" + t.threadId());
            t.setDaemon(false);
            return t;
        }, 
        new ThreadPoolExecutor.AbortPolicy());
        
        return ex;
    }

    /** Optional global hard cap across the JVM (extra guard). */
    @Bean
    public Semaphore downloadPermits(@Value("${downloads.max.concurrent:4}") int n) {
        return new Semaphore(n);
    }
}
