package com.portfolio.common.system.cache;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {
        RedisConfig.class
//        , TraceIdFilter.class
        , CacheUtils.class
        , DynamicCacheResolver.class
})
@TestPropertySource(properties = {
        "spring.redis.host=localhost",
        "spring.redis.port=6379",
        "spring.cache.type=redis",
        "spring.redis.database=1"
})
@Slf4j
public class CacheUtilsTest {

    @Autowired
    private CacheUtils cacheUtils;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp(){
        // 테스트 전 캐시 초기화
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()){
            redisTemplate.delete(keys);
        }
    }

    @Test
    void testAndGet(){
        // given
        String key = "testKey";
        String value = "testValue";
        long timeout = 60L;

        // when
        cacheUtils.set(key, value, timeout);
        Object result = cacheUtils.get(key);
        log.debug("result : {}", result);

        // then
        assertEquals(value, result);

    }

    @Test
    void testDelete(){
        // given
        String key = "testKey";
        String value = "testValue";
        cacheUtils.set(key, value, 60L);

        // when
        log.debug("cache : {}", cacheUtils.get(key));
        boolean deleted = cacheUtils.delete(key);

        // then
        assertTrue(deleted);
        assertNull(cacheUtils.get(key));
    }

    @Test
    void testHasKey(){
        // given
        String key = "testKey";
        String value = "testValue";
        cacheUtils.set(key, value, 60L);
        log.debug("cache : {}", cacheUtils.get(key));

        // when
        boolean exists = cacheUtils.hasKey(key);

        // then
        assertTrue(exists);
    }

    @Test
    void testCacheable(){
        // given
        String cacheName = "users";
        String key = "123";
        String expected = "users-Data-123";

        // when
        String result1 = cacheUtils.getKeyByCacheName(cacheName, key);
        String result2 = cacheUtils.getKeyByCacheName(cacheName, key);    // 캐시로 가져옴
        log.debug("result1: {}, result2: {}", result1, result2);

        // then
        assertEquals(expected, result1);
        assertEquals(expected, result2);
        Object cachedValue = redisTemplate.opsForValue().get(cacheName + "::" + key);
//        Object cachedValue2 = redisTemplate.opsForValue().get("users::123");
        log.debug("cacheValue: {}", cachedValue);
//        log.debug("cacheValue2: {}", cachedValue2);
        assertNotNull(cachedValue);

    }
}
