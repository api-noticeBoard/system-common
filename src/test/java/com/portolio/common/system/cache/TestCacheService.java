package com.portolio.common.system.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Spring Cache 기능(@Cacheable, @CacheEvict)을 테스트하기 위한 서비스 클래스.
 */
@Slf4j
@Service
public class TestCacheService {

    /**
     * 데이터를 조회하는 메서드. @Cacheable 어노테이션이 붙어있습니다.
     * 이 메서드가 처음 호출되면, 실제 로직(시간이 걸리는 작업)을 수행하고
     * 그 결과를 "post" 캐시에 (key = id)로 저장합니다.
     *
     * 두 번째부터 동일한 id로 호출되면, 실제 로직을 수행하지 않고
     * Redis 캐시에 저장된 값을 즉시 반환합니다.
     *
     * @param id 게시글 ID
     * @return 게시글 내용
     */
    @Cacheable(cacheNames = CacheKey.POST, key = "#id")
    public String findPostById(Long id) {
        log.info("--- (Cache Miss) Finding post from DB for id: {} ---", id);
        // 데이터베이스에서 데이터를 조회하는 것처럼 보이기 위해 잠시 대기
        try {
            Thread.sleep(1000L); // 1초 대기
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "Post content for id " + id;
    }

    /**
     * 캐시를 삭제하는 메서드. @CacheEvict 어노테이션이 붙어있습니다.
     * 이 메서드가 호출되면, "post" 캐시에서 (key = id)에 해당하는 데이터를 삭제합니다.
     *
     * @param id 삭제할 캐시의 키가 되는 게시글 ID
     */
    @CacheEvict(cacheNames = CacheKey.POST, key = "#id")
    public void evictPostCache(Long id) {
        log.info("--- Evicting cache for post with id: {} ---", id);
        // 실제로는 DB 업데이트 등의 로직이 있을 수 있음
    }
}
