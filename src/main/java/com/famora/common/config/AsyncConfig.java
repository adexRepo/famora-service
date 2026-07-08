package com.famora.common.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
  
  @Bean(name = "auditTaskExecutor")
  public Executor auditTaskExecutor(
      @Value("${app.async.thread.core-pool-size:5}") int corePoolSize,
      @Value("${app.async.thread.max-pool-size:10}") int maxPoolSize,
      @Value("${app.async.thread.queue-capacity:20}") int queueCapacity
  ) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix("audit-");
    executor.initialize();
    return executor;
  }
}
