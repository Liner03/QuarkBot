package com.lin.bot.config;


import com.alibaba.fastjson2.JSONObject;
import com.lin.bot.data.TempData;
import com.lin.bot.service.LoginService;
import com.lin.bot.util.constants.LoginConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @Author Lin.
 * @Date 2024/12/29
 * 在springboot关闭时自动退出登录
 */
@Slf4j
//@Component
public class AutoLogout implements SmartLifecycle {
//    @Autowired
    private LoginService loginService;
//    @Autowired
    private TempData data;
//    @Autowired
    private RedisTemplate redisTemplate;
    private boolean running = false;

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        try {
            if (String.valueOf(LoginConst.LOGIN_IN).equals(data.getDataByString("loginStatus"))) {
                log.warn("程序即将销毁，{} 即将退出登录", JSONObject.parse(data.getDataByString("loginInfo")).getString("nickName"));
                loginService.logout();
            }
        } catch (IllegalStateException e) {
            log.error("Redis 已关闭，无法执行退出登录操作：{}", e.getMessage());
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
