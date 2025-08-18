package com.portolio.common.system.cache;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * [Cache Management]
 * 애플리케이션 전체에서 사용될 캐시의 종류와 속성을 정의하는 Enum.
 * - 캐시 이름을 상수로 관리하여 "Magic String"(하드코딩된 문자열) 사용을 방지.
 * - 캐시별 TTL(Time-To-Live, 유효시간)을 중앙에서 관리하여 정책 변경을 용이.
 */
@Getter
@RequiredArgsConstructor
public enum CacheKey {

    // 게시글 단건 조회 캐시: 이름은 "post", 유효 시간은 600초(10분)
    POST("post", 600),

    // 사용자 정보 조회 캐시: 이름은 "userInfo", 유효 시간은 3600초(1시간)
    USER_INFO("userInfo", 3600),

    // 외부 API 호출 결과 캐시: 이름은 "externalApiData", 유효 시간은 300초(5분)
    EXTERNAL_API_DATA("externalApiData", 300);

    private final String cacheName;
    private final int ttlSeconds;
}
