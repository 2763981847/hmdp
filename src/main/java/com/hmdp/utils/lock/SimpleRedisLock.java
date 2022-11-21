package com.hmdp.utils.lock;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @author : Fu QiuJie
 * @since : 2022/11/21 12:13
 */
public class SimpleRedisLock implements ILock {

    private StringRedisTemplate stringRedisTemplate;
    private String key;

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String key) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.key = key;
    }

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "_";

    @Override
    public boolean tryLock(long timeout, TimeUnit timeUnit) {
        //获取到当前线程id
        long id = Thread.currentThread().getId();
        //拼接线程标识
        String threadId = ID_PREFIX + id;
        //尝试获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + key, threadId, timeout, timeUnit);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unLock() {
        String ownerId = stringRedisTemplate.opsForValue().get(KEY_PREFIX + key);
        if ((ID_PREFIX + Thread.currentThread().getId()).equals(ownerId)) {
            //当前锁持有者是自己才释放锁
            stringRedisTemplate.delete(KEY_PREFIX + key);
        }
    }
}
