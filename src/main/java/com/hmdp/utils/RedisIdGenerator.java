package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author : Fu QiuJie
 * @since : 2022/11/18 11:17
 */
@Component
public class RedisIdGenerator {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final long BEGIN_TIME = 1640995200L;
    private static final int BITS_COUNT = 32;

    public long nextId(String keyPrefix) {
        //得到当前时间秒数
        long nowSeconds = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        //得到时间戳
        long timeStamp = nowSeconds - BEGIN_TIME;
        //得到当前时间（精确到天）
        String nowFormat = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //生成获取序列号的key
        String key = "icr:" + keyPrefix + ":" + nowFormat;
        //获取到序列号
        long count = stringRedisTemplate.opsForValue().increment(key);
        //拼接时间戳与序列号生成id返回
        return (timeStamp << BITS_COUNT) | count;
    }
}
