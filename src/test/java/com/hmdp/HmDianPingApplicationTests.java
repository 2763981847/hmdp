package com.hmdp;

import com.hmdp.utils.RedisIdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private RedisIdGenerator redisIdGenerator;

    @Test
    void testIdGenerator() {
        long id = redisIdGenerator.nextId("test");
        System.out.println(id);
    }

}
