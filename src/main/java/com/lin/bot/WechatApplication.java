package com.lin.bot;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableAsync
@EnableScheduling
@EnableRedisRepositories
@SpringBootApplication(scanBasePackages = "com.lin.bot")
public class WechatApplication {
    public static void main(String[] args) {
        try {
            new SpringApplicationBuilder(WechatApplication.class).run(args);
        } catch (Exception e) {
            log.error("启动异常", e);
        }
    }
}
