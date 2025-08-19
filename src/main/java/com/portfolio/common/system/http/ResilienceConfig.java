package com.portfolio.common.system.http;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * [External API Stability]
 * 외부 API 호출 시 발생할 수 있는 장애에 대비하기 위한 Resilience4j 라이브러리의 전역 설정 클래스입니다.
 * 여기에 정의된 Bean들은 HttpClient에서 어노테이션을 통해 사용될 기본 정책(Default Policy)이 됩니다.
 */
@Slf4j
@Configuration
public class ResilienceConfig {
    /**
     * Circuit Breaker(회로 차단기)의 전역 기본 동작 규칙을 정의하는 Bean.
     * @return CircuitBreakerConfig 전역 설정 객체
     */
    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        // "앞으로 만들 모든 서킷 브레이커는 기본적으로 이렇게 동작시켜라"는 규칙을 정의
        return CircuitBreakerConfig.custom()
                // -- 서킷을 OPEN 상태로 전환할 조건 --
                .failureRateThreshold(50) // 실패율 50%가 넘으면
                .waitDurationInOpenState(Duration.ofMillis(10000)) // 10초간 호출을 차단해라
                .build();
    }

    /**
     * Retry(재시도)의 전역 기본 동작 규칙을 정의하는 Bean.
     * @return RetryConfig 전역 설정 객체
     */
    @Bean
    public RetryConfig retryConfig() {
        // "앞으로 만들 모든 재시도 장치는 기본적으로 이렇게 동작시켜라"는 규칙을 정의
        return RetryConfig.custom()
                .maxAttempts(3) // 최대 3번 시도하고
                .waitDuration(Duration.ofMillis(500)) // 0.5초 간격으로 시도해라
                .build();
    }

    /**
     * 서킷 브레이커의 상태(OPEN, HALF-OPEN, CLOSED)가 변경될 때마다 로그를 남기기 위한 이벤트 리스너 Bean.
     * 운영 중 장애 상황을 모니터링하는 데 매우 유용.
     */
    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                entryAddedEvent.getAddedEntry().getEventPublisher().onStateTransition(event ->
                        log.warn("CircuitBreaker state changed: name={}, type={}", event.getCircuitBreakerName(), event.getStateTransition()));
            }
            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {}
            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {}
        };
    }
}
