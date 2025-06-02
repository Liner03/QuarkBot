package com.lin.bot.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @Author Lin.
 * @Date 2025/2/6
 */
@Configuration
@EnableScheduling
public class SchedulerConfig implements SchedulingConfigurer {
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadFactory threadFactory = new CustomizableThreadFactory("Scheduler-Thread-");
        // 5个线程
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(10, threadFactory));
    }
}

