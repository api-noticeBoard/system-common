package com.portfolio.common.system.cache;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {
        RedisConfig.class
//        , TraceIdFilter.class
        , CacheUtils.class
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
}
