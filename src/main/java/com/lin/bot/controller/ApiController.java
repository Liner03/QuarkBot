package com.lin.bot.controller;


import cn.hutool.core.util.StrUtil;
import com.lin.bot.data.TempData;
import com.lin.bot.model.VO.CommonVO;
import com.lin.bot.service.DuanjuService;
import com.lin.bot.util.Quark;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author Lin.
 * @Date 2025/2/23
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class ApiController {
    private final Quark quark;
    private final DuanjuService duanjuService;
    @Value("${quark.config.save-path-fid:0}")
    private String savePathFid;
    private final RedisTemplate<String , String > redisTemplate;
    private final TempData data;

    public ApiController(Quark quark, RedisTemplate<String , String > redisTemplate, TempData tempData, DuanjuService duanjuService) {
        this.quark = quark;
        this.redisTemplate = redisTemplate;
        this.data = tempData;
        this.duanjuService = duanjuService;
    }

    @GetMapping("/transfer")
    public CommonVO<?> transferToQuark(@RequestParam String url,
                                       @RequestParam(value = "del", defaultValue = "0") Integer del,
                                       @RequestParam(value = "cookie", required = true) String cookie,
                                       @RequestParam(value = "fid", required = true) String fid,
                                       @RequestParam(value = "adFid", required = false) String adFid)
            throws IOException, InterruptedException {
        long cacheExpireSeconds = 3600;
        long lockExpireSeconds = 60;

        if (StrUtil.isEmptyIfStr(url)) {
            return CommonVO.FAIL("url为空");
        }
        if (!url.contains("pan.quark.cn")) {
            return CommonVO.OK("url错误");
        }
        boolean delFlag = del != 0;
        if (StrUtil.isNotEmpty(fid) && StrUtil.isNotEmpty(cookie)) {
            savePathFid = fid;
        } else {
            return CommonVO.OK("参数缺失");
        }

        // 定义缓存 key，根据 URL 生成（若需要可进行 MD5 加密以避免特殊字符问题）
        String redisKey = "shareUrl:" + url;
        // 先从 Redis 中获取分享链接
        String cachedShareUrl = redisTemplate.opsForValue().get(redisKey);
        if (cachedShareUrl != null) {
            return CommonVO.OK(cachedShareUrl);
        }

        // 定义分布式锁 key，建议加上前缀 "lock:"
        String lockKey = "lock:" + redisKey;
        // 使用 UUID 生成唯一锁值
        String lockValue = UUID.randomUUID().toString();

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, lockExpireSeconds, TimeUnit.SECONDS);
        if (lockAcquired != null && lockAcquired) {
            try {
                // 锁内双重检查
                cachedShareUrl = redisTemplate.opsForValue().get(redisKey);
                if (cachedShareUrl != null) {
                    return CommonVO.OK(cachedShareUrl);
                }
                // 调用业务逻辑生成分享链接
                String shareUrl = quark.saveAndShareAndDelCustomize(url, savePathFid, adFid, cookie, "", "1", delFlag);
                if (StrUtil.isNotEmpty(shareUrl)) {
                    redisTemplate.opsForValue().set(redisKey, shareUrl, cacheExpireSeconds, TimeUnit.SECONDS);
                }
                return CommonVO.OK(shareUrl);
            } finally {
                // 安全释放锁（只有持有 lockValue 的才能删除）
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) else return 0 end";
                redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                        Collections.singletonList(lockKey), lockValue);
            }
        } else {
            // 未能获取到锁，则等待直到缓存中有值（最多等待20秒）
            int maxWaitMillis = 20000; // 20秒
            int waited = 0;
            while (waited < maxWaitMillis) {
                Thread.sleep(100);
                waited += 100;
                cachedShareUrl = redisTemplate.opsForValue().get(redisKey);
                if (cachedShareUrl != null) {
                    return CommonVO.OK(cachedShareUrl);
                }
            }
            // 如果超时后仍没有拿到缓存，则再次尝试获取锁
            lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, lockExpireSeconds, TimeUnit.SECONDS);
            if (lockAcquired != null && lockAcquired) {
                try {
                    cachedShareUrl = redisTemplate.opsForValue().get(redisKey);
                    if (cachedShareUrl != null) {
                        return CommonVO.OK(cachedShareUrl);
                    }
                    String shareUrl = quark.saveAndShareAndDelCustomize(url, savePathFid, adFid, cookie, "", "1", delFlag);
                    if (StrUtil.isNotEmpty(shareUrl)) {
                        redisTemplate.opsForValue().set(redisKey, shareUrl, cacheExpireSeconds, TimeUnit.SECONDS);
                    }
                    return CommonVO.OK(shareUrl);
                } finally {
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                            "return redis.call('del', KEYS[1]) else return 0 end";
                    redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                            Collections.singletonList(lockKey), lockValue);
                }
            } else {
                // 如果再次无法获取锁，则直接返回当前缓存值（可能还是 null），或提示错误
                cachedShareUrl = redisTemplate.opsForValue().get(redisKey);
                return CommonVO.OK(cachedShareUrl);
            }
        }
    }
    @GetMapping("/transferToMd")
    public CommonVO<? > transferToMd(@RequestParam(defaultValue = "") String url) {
        if (StrUtil.isEmpty(url) | !url.contains("pan.quark.cn")) {
            return CommonVO.OK("url不合法");
        }
        Map<Object, Object> map = this.data.getDataByMap(url);
        CommonVO<Object> vo = CommonVO.OK("获取成功");
        vo.setData(map);
        return vo;
    }

    @GetMapping("/update")
    public CommonVO<?> update() throws IOException, InterruptedException {
        duanjuService.updDuanjuDaily();
        return CommonVO.OK();
    }
}
