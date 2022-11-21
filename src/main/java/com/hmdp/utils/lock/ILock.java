package com.hmdp.utils.lock;

import java.util.concurrent.TimeUnit;

/**
 * @author : Fu QiuJie
 * @since : 2022/11/21 11:16
 */
public interface ILock {
    /**
     * 尝试获取锁
     *
     * @param timeoutSec 锁持有的超时时间（单位：秒），过期自动释放
     * @return 是否成功获取到锁
     */
    default boolean tryLock(long timeoutSec) {
        return tryLock(timeoutSec, TimeUnit.SECONDS);
    }

    ;

    /**
     * 尝试获取锁
     *
     * @param timeout  锁持有的超时时间，过期自动释放
     * @param timeUnit 时间单位
     * @return 是否成功获取到锁
     */
    boolean tryLock(long timeout, TimeUnit timeUnit);


    /**
     * 释放锁
     */
    void unLock();
}
