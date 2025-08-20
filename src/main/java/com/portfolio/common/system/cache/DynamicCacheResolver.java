package com.portfolio.common.system.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

/**
 * 동적 cacheName을 지원하는 CacheResolver
 * @Cacheable 메서드의 첫 번째 파라미터(cacheName)를 캐시 이름으로 사용
 * - 이유: @Cacheable의 value 속성에서 SpEL(#cacheName)이 제대로 평가되지 않으므로, CacheResolver를 사용해 런타임에 캐시 이름 결정.
 * - 사용 방법: @Cacheable(cacheResolver = "dynamicCacheResolver")로 지정.
 */
@Component("dynamicCacheResolver")  // Spring Bean으로 등록 + 이름을 "dynamicCacheResolver"로 지정
@RequiredArgsConstructor  // Lombok을 사용해 CacheManager 생성자 주입
public class DynamicCacheResolver implements CacheResolver {

    private final CacheManager cacheManager;    // 필드에 바로 주입

//    // 생성자를 통해 CacheManager 주입 (Spring Boot에서는 RedisCacheManager 등 자동 주입됨)
//    public DynamicCacheResolver(CacheManager cacheManager) {
//        this.cacheManager = cacheManager;
//    }

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        // context.getArgs() : 캐시 대상 메소드의 인자들을 배열로 가져옴
        // 여기서는 메소드의 첫 번째 인자를 cacheName 으로 사용한다고 가정
        String cacheName = (String) context.getArgs()[0]; // 첫 parameter 사용

        // cacheManager.getCache(cacheName) : 실제 캐시 이름으로 캐시 객체 가져오기
        // Collections.singletonList(...) : 단일 캐시를 반환 (Spring Cache는 Collection 타입 요구)
        return Collections.singletonList(cacheManager.getCache(cacheName));
    }

}
