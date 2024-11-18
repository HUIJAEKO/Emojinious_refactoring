package com.example.blogpractice;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
public class RedisConnectionTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    public void testRedisConnection(){
        String key = "key";
        String value = "value";

        redisTemplate.opsForValue().set(key,value);
        Object retrievedValue = redisTemplate.opsForValue().get(key);

        Assertions.assertEquals(value, retrievedValue);
    }
}
