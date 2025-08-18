package com.portolio.common.system.cache;


import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * [Cache Configuration]
 * Spring의 캐시 추상화(@Cacheable, @CacheEvict 등)를 사용하기 위한 설정 클래스.
 * 이 라이브러리를 사용하는 애플리케이션은 Redis 의존성만 추가하면 별도 설정 없이 캐시를 사용할 수 있음.
 */
@Configuration
@EnableCaching  // Spring Boot에 캐시 기능을 활성화하도록 지시.
public class CacheConfig {

    /**
     * CacheManager Bean을 생성하여 Spring 컨테이너에 등록.
     * 이 Bean이 실제 캐시 저장, 조회, 삭제 등의 동작을 관리.
     * @param redisConnectionFactory Spring Boot가 자동으로 설정해주는 Redis 연결 팩토리
     * @return 커스텀 설정이 적용된 CacheManager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory){

        // 1. 기본 설계도(defaultConfig)를 만듭니다.
        //    - 이 설정은 CacheKey Enum에 정의되지 않은 캐시나,
        //      @Cacheable에 이름을 지정하지 않았을 때 사용될 기본값입니다.
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                // 2. 설계도에 규칙을 추가합니다: "기본 유효시간(TTL)은 1시간으로 한다."
                .entryTtl(Duration.ofHours(1))
                // 3. 설계도에 규칙을 추가합니다: "Key는 문자열(String) 형태로 저장한다."
                //    - Redis CLI 등에서 Key를 쉽게 알아볼 수 있어 디버깅에 유리합니다.
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // 4. 설계도에 규칙을 추가합니다: "Value는 범용적인 JSON 형태로 저장한다."
                //    - 객체, 리스트, 배열 등 다양한 형태의 데이터를 온전히 저장하고 복원할 수 있습니다.
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // 5. CacheKey Enum을 바탕으로 캐시 종류별로 특별한 설계도를 만듭니다.
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        for (CacheKey key : CacheKey.values()) {
            // 기본 설계도를 복사한 뒤, 유효시간만 각 캐시에 맞게 특별히 변경하여 Map에 추가합니다.
            // 예: "post" 캐시는 600초, "userInfo" 캐시는 3600초 TTL을 갖게 됩니다.
            cacheConfigurations.put(key.getCacheName(), defaultConfig.entryTtl(Duration.ofSeconds(key.getTtlSeconds())));
        }

        // 6. 완성된 설계도들을 CacheManager(캐시 공장장)에게 넘겨줍니다.
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig) // 기본 설계도 전달
                .withInitialCacheConfigurations(cacheConfigurations) // 특별 설계도 목록 전달
                .build();
    }
}
