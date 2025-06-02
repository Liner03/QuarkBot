package com.lin.bot.data;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * @Author Lin.
 * @Date 2024/12/27
 * 采用 redis 作为缓存 缓存时间默认 30min
 */
@Component
@Slf4j
public class TempData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    private static final long DEFAULT_EXPIRATION_TIME = 30; // 默认过期时间
    private static final TimeUnit DEFAULT_EXPIRATION_UNIT = TimeUnit.MINUTES; // 默认时间单位

    /**
     * 设置配置数据（带过期时间）
     */
    public void setStringData(String key, String value) {
        redisTemplate.opsForValue().set(key, value, DEFAULT_EXPIRATION_TIME, DEFAULT_EXPIRATION_UNIT);
    }
    public void setStringData(String key, String value,long expireTime) {
        redisTemplate.opsForValue().set(key, value, expireTime, DEFAULT_EXPIRATION_UNIT);
    }
    public void setMapData(String key, Map<String,Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
        redisTemplate.expire(key, DEFAULT_EXPIRATION_TIME, DEFAULT_EXPIRATION_UNIT);
    }
    public void setMapData(String key, Map<?,?> map,long expireTime) {
        redisTemplate.opsForHash().putAll(key, map);
        redisTemplate.expire(key, expireTime, DEFAULT_EXPIRATION_UNIT);
    }
    public void setSetData(String key, Set<?> set) {
        redisTemplate.opsForSet().add(key, set);
        redisTemplate.expire(key, DEFAULT_EXPIRATION_TIME, DEFAULT_EXPIRATION_UNIT);
    }
    public void setSetData(String key, Set<?> set,long expireTime) {
        redisTemplate.opsForSet().add(key, set);
    }


    /**
     * 设置配置数据（没有过期时间）
     */
    public void setStringDataWithoutExpiration(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }
    public void setMapWithoutExpiration(String key, Map<String,?> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }
    public void setSetWithoutExpiration(String key, Set<?> set) {
        redisTemplate.opsForSet().add(key, set);
    }

    /**
     * 获取配置数据
     */
    public Map<Object, Object> getDataByMap(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public String getDataByString(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 删除配置数据
     */
    public void del(String key) {
        redisTemplate.delete(key);
    }

}


