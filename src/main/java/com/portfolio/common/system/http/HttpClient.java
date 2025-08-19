package com.portfolio.common.system.http;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * [External API Client]
 * RestTemplate을 래핑하여 외부 API를 호출하는 표준 클라이언트.
 * Resilience4j의 어노테이션을 사용하여 선언적으로 서킷 브레이커와 재시도 로직을 적용.
 * 이 클래스를 사용하면 API 호출 코드와 장애 대응 코드를 분리하여 코드 정리.
 */
@Component
@RequiredArgsConstructor
public class HttpClient {

    private final RestTemplate restTemplate;

    /**
     * 지정된 URL로 GET 요청을 보냄.
     * @param url           요청할 URL
     * @param responseType  응답을 변환할 클래스 타입
     * @return 변환된 응답 객체
     */
    // "external"이라는 이름의 서킷 브레이커와 재시도 설정을 이 메서드에 적용.
    // Resilience4j가 이 어노테이션을 보고 ResilienceConfig에 정의된 기본 정책과
    // application.yml에 정의된 "external" 인스턴스 정책을 조합하여 적용.
    @CircuitBreaker(name = "external")
    @Retry(name = "external")
    public <T> T get(String url, Class<T> responseType) {
        return restTemplate.getForObject(url, responseType);
    }

    /**
     * 지정된 URL로 POST 요청을 보냄.
     * @param url           요청할 URL
     * @param request       요청 본문에 포함될 객체
     * @param responseType  응답을 변환할 클래스 타입
     * @return 변환된 응답 객체
     */
    @CircuitBreaker(name = "external")
    @Retry(name = "external")
    public <T> T post(String url, Object request, Class<T> responseType) {
        return restTemplate.postForObject(url, request, responseType);
    }
}
