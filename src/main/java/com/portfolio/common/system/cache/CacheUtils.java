package com.portfolio.common.system.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class CacheUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 캐시에 데이터를 저장
     * @param key 캐시 키
     * @param value 저장할 데이터
     * @param timeout 캐시 만료 시간 (초 단위)
     */
    public void set(String key, Object value, long timeout){
        try{
            redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
        }catch (Exception e){
            throw new RuntimeException("Failed to set cache for key: " + key, e);
        }
    }

    /**
     * 캐시에서 데이터를 조회
     * @param key 캐시 키
     * @return 캐시된 데이터, 없으면 null
     */
    public Object get(String key){
        try{
            return redisTemplate.opsForValue().get(key);
        }catch (Exception e){
            throw new RuntimeException("Failed to get cache for key: " + key, e);
        }
    }

    /**
     * 캐시에서 데이터 삭제
     * @param key 캐시 키
     * @return 삭제 성공 여부
     */
    public boolean delete(String key){
        try{
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        }catch (Exception e){
            throw new RuntimeException("Failed to delete cache for key: " + key, e);
        }
    }
    /**
     * 캐시 키 존재 여부 확인
     * @param key 캐시 키
     * @return 키 존재 여부
     */
    public boolean hasKey(String key){
        try{
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        }catch (Exception e){
            throw new RuntimeException("Failed to check cache ke existence: " + key, e);
        }
    }

    /**
     * @Cacheable을 사용한 캐시 조회 예시
     * users 캐시에 ID를 키로 저장, 캐시 히트 시 메서드 실행 생략
     * @param cacheName 조회할 cacheName
     * @param key       조회한 key
     * @return 캐시된 데이터 또는 새로 생성된 데이터
     */
    @Cacheable(value = "#cacheName", key = "#key")
    public String getUserById(String cacheName, String key) {
        // 실제로는 DB 조회 등 무거운 작업 수행
        // 여기서는 예시로 ID 기반 문자열 반환
        return cacheName + "-Data-" + key;
    }
}
