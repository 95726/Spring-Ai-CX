package com.example.springai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RedisConnectionTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void testRedisConnection() {
        String key = "test:key";
        String value = "test-value-" + System.currentTimeMillis();

        // 存储
        redisTemplate.opsForValue().set(key, value);

        // 读取
        String retrievedValue = redisTemplate.opsForValue().get(key);

        // 验证
        assertEquals(value, retrievedValue, "Redis 存储和读取的值应该一致");

        // 删除
        Boolean deleted = redisTemplate.delete(key);
        assertTrue(deleted, "Key 应该被成功删除");

        // 验证删除
        assertNull(redisTemplate.opsForValue().get(key), "删除后 key 应该不存在");

        System.out.println("Redis 连接测试通过！");
    }
}