package com.earthonline.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 流式回答用的线程池。
 *
 * 为什么不能直接用 Tomcat 的请求线程？因为 SSE 一开就是几秒钟，
 * 占着请求线程会把 Tomcat 的线程池耗光（默认 200 个线程，看起来很多，但 SSE 一挂就是几秒到几分钟）。
 * 所以：请求线程只负责“开流”，真正的生成放在这个小线程池里。
 *
 * 2 核 4G 的服务器 + 当前用户规模，2~4 个并发流完全够用。
 */
@Configuration
public class AiStreamConfig {

    @Bean("aiStreamExecutor")
    public ThreadPoolTaskExecutor aiStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("ai-stream-");
        // 队列也满了就让提交者自己执行：宁可慢一点，也不要静默丢请求
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
