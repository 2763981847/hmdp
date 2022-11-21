package com.hmdp.utils.lock;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
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
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }


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
        //调用lua脚本执行释放锁操作
        stringRedisTemplate
                .execute(UNLOCK_SCRIPT,
                        Collections.singletonList(KEY_PREFIX + key),
                        ID_PREFIX + Thread.currentThread().getId());
    }
}
