package com.portolio.common.system.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Cache Test with Testcontainers]
 * CacheConfig와 Spring Cache 어노테이션(@Cacheable, @CacheEvict)의 동작을 검증합니다.
 *
 * @Testcontainers: 이 클래스가 Testcontainers를 사용한 JUnit 5 테스트임을 선언합니다.
 *                  테스트 시작 전에 @Container 어노테이션이 붙은 컨테이너를 실행하고,
 *                  테스트 종료 후에 컨테이너를 자동으로 중지시켜줍니다.
 */
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(classes = {CacheConfig.class, TestCacheService.class}) // 테스트에 필요한 설정과 서비스 클래스만 로드
public class CacheConfigTest {


    // 테스트에 사용할 TestCacheService Bean을 주입받습니다.
    @Autowired
    private TestCacheService testCacheService;

    /**
     * @Container 어노테이션은 이 필드가 관리할 Docker 컨테이너임을 나타냅니다.
     * GenericContainer를 사용하여 Redis 공식 이미지를 기반으로 컨테이너를 생성합니다.
     * withExposedPorts(6379): 컨테이너 내부의 6379 포트를 외부로 노출시킵니다.
     */
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:latest"))
            .withExposedPorts(6379);

    /**
     * @DynamicPropertySource
     * Spring Boot 테스트가 시작되기 전에, 동적으로 애플리케이션 속성(properties)을 설정하는 메서드입니다.
     * Testcontainers가 실행한 Redis 컨테이너의 실제 주소와 포트 정보를
     * Spring Boot의 Redis 설정값(spring.data.redis.host, spring.data.redis.port)에 주입하는 역할을 합니다.
     * 이를 통해 우리 애플리케이션이 테스트용으로 실행된 Redis 컨테이너에 연결할 수 있게 됩니다.
     */
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Test
    @DisplayName("@Cacheable: 동일한 키로 반복 호출 시, 두 번째부터는 캐시를 사용하여 메서드 본문을 실행하지 않아야 한다.")
    void cacheable_shouldNotExecuteMethodBody_onSecondCall(CapturedOutput output) {
        // --- given ---
        Long postId = 1L;

        // --- when ---
        // 1. 첫 번째 호출: 캐시가 없으므로 메서드 본문이 실행되어야 함
        String post1 = testCacheService.findPostById(postId);

        // 2. 두 번째 호출: 캐시가 있으므로 메서드 본문이 실행되지 않아야 함
        String post2 = testCacheService.findPostById(postId);

        // --- then ---
        // 1. 두 번의 호출 결과가 동일한지 확인
        assertThat(post1).isEqualTo("Post content for id 1");
        assertThat(post2).isEqualTo(post1);

        // 2. 캡처된 로그를 확인하여 메서드 본문이 "한 번만" 실행되었는지 검증
        String consoleOutput = output.getAll();

        // "(Cache Miss)" 로그가 정확히 한 번만 찍혔는지 확인
        assertThat(countOccurrences(consoleOutput, "(Cache Miss)")).isEqualTo(1);
    }

    @Test
    @DisplayName("@CacheEvict: 캐시 삭제 후 다시 호출하면, 캐시가 없어 메서드 본문이 다시 실행되어야 한다.")
    void cacheEvict_shouldExecuteMethodBody_afterEviction(CapturedOutput output) {
        // --- given ---
        Long postId = 2L;

        // --- when ---
        // 1. 첫 번째 호출로 캐시를 생성
        testCacheService.findPostById(postId);

        // 2. 캐시 삭제
        testCacheService.evictPostCache(postId);

        // 3. 캐시 삭제 후 다시 호출
        testCacheService.findPostById(postId);

        // --- then ---
        // 캡처된 로그를 확인하여 "(Cache Miss)" 로그가 "두 번" 찍혔는지 검증
        String consoleOutput = output.getAll();
        assertThat(countOccurrences(consoleOutput, "(Cache Miss)")).isEqualTo(2);
    }

    /**
     * 테스트 검증을 위한 헬퍼 메서드: 문자열에서 특정 부분 문자열이 몇 번 나타나는지 계산합니다.
     */
    private int countOccurrences(String str, String subStr) {
        return str.split(subStr, -1).length - 1;
    }
}
