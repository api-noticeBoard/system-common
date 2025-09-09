package com.portfolio.common.system.exception;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.RequestContextFilter; // MDC(TraceId) 테스트를 위해 필요

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 모듈 프로젝트에서 @SpringBootTest를 사용하여 예외 처리 로직을 통합 테스트하는 클래스.
 *
 * 이 모듈은 @SpringBootApplication 클래스가 없으므로, @SpringBootTest가 컨텍스트를 로드할
 * 기본 대상을 찾지 못합니다. 따라서 'classes' 속성을 사용하여 명시적인 설정 클래스를 지정해야 합니다.
 */
@Slf4j
// @SpringBootTest 어노테이션을 사용하여 Spring 애플리케이션 컨텍스트를 로드합니다.
// 'classes' 속성에 테스트에 필요한 최소한의 설정을 제공하는 내부 @Configuration 클래스를 지정합니다.
@SpringBootTest(classes = BusinessExceptionTest.TestConfig.class)
// MockMvc를 자동으로 구성하여 HTTP 요청을 시뮬레이션할 수 있도록 합니다.
@AutoConfigureMockMvc
@DisplayName("BusinessException 및 GlobalExceptionHandler 통합 테스트 (모듈 프로젝트)")
public class BusinessExceptionTest {

    // MockMvc는 컨트롤러 엔드포인트를 호출하는 데 사용됩니다.
    @Autowired
    private MockMvc mockMvc;

    // WebApplicationContext는 MockMvc를 설정할 때 필터 등을 적용하기 위해 필요할 수 있습니다.
//    @Autowired
//    private WebApplicationContext webApplicationContext;

    /**
     * 각 테스트 메서드 실행 전에 MockMvc 설정을 초기화합니다.
     * 특히 MDC (Mapped Diagnostic Context)에 traceId를 설정하는 필터를 추가하여
     * ErrorResponse에 traceId가 올바르게 포함되는지 테스트할 수 있도록 합니다.
     * 실제 프로젝트의 TraceIdFilter가 있다면 해당 필터를 여기에 추가하는 것이 가장 정확합니다.
     */
//    @BeforeEach
//    void setup() {
//        mockMvc = MockMvcBuilders
//                .webAppContextSetup(webApplicationContext)
//                // RequestContextFilter는 MDC를 설정하는 일반적인 Spring 필터입니다.
//                // 만약 프로젝트에 자체 TraceIdFilter가 있다면 RequestContextFilter 대신 해당 필터를 추가하세요.
//                // 예: .addFilter(new TraceIdFilter())
//                .addFilter(new RequestContextFilter())
//                .build();
//    }

    /**
     * 테스트용 임시 컨트롤러입니다.
     * 실제 애플리케이션의 컨트롤러 대신, 테스트를 위해 필요한 예외를 발생시키는 엔드포인트를 제공합니다.
     * 이 컨트롤러는 TestConfig에서 빈으로 등록되어 Spring 컨텍스트에 포함됩니다.
     */
    @RestController
    static class TestController {
        @GetMapping("/test/business-exception-message")
        public String testBusinessExceptionWithMessage() {
            // ErrorCode.INVALID_INPUT_VALUE의 메시지 템플릿에 동적 인자를 전달하여 예외 발생
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, ": 테스트용");
//            return null;
        }

        @GetMapping("/test/business-exception-simple")
        public String testBusinessExceptionSimple() {
            // 기본 메시지를 사용하는 BusinessException 예외 발생
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
//            return null;
        }

        @GetMapping("/test/unhandled-exception")
        public String testUnhandledException() {
            // GlobalExceptionHandler의 fallback 메서드(handleException)에 의해 처리될 일반 예외 발생
            throw new RuntimeException("예상치 못한 런타임 오류!");
//            return null;
        }
    }

    /**
     * `@SpringBootApplication`이 없는 모듈에서 Spring 컨텍스트를 로드하기 위한 최소한의 설정 클래스입니다.
     * `@Configuration` 어노테이션이 붙어 이 클래스가 Spring 설정 파일임을 나타냅니다.
     * `@Import` 어노테이션을 사용하여 Spring 컨텍스트에 포함시킬 빈들을 명시적으로 등록합니다.
     */
    @Configuration
    @Import({
            WebMvcAutoConfiguration.class, // ✨ 이 부분을 추가합니다.
            GlobalExceptionHandler.class, // 예외 처리를 담당하는 GlobalExceptionHandler를 빈으로 등록
            TestController.class          // 위에서 정의한 테스트용 컨트롤러를 빈으로 등록
            // 만약 @Valid 테스트를 위한 DTO와 함께 사용하려면 spring-boot-starter-validation 의존성도 필요합니다.
            // 그리고 해당 DTO도 적절히 등록하거나, @WebMvcTest를 사용하는 것이 더 적합할 수 있습니다.
    })
    static class TestConfig {
        // 여기에 필요하다면 추가적인 @Bean 메서드를 정의할 수 있습니다.
        /*
         * ✨ [핵심 수정]
         * RequestContextFilter를 Spring 컨테이너의 빈(Bean)으로 등록합니다.
         * 이 필터는 HTTP 요청 정보를 현재 스레드에서 사용할 수 있도록 설정해주는 역할을 하며,
         * MDC(Mapped Diagnostic Context)에 traceId와 같은 요청별 데이터를 저장하기 위해 필수적입니다.
         * @AutoConfigureMockMvc는 여기에 등록된 필터 빈을 자동으로 MockMvc 테스트에 적용해줍니다.
         */
//        @Bean
//        public RequestContextFilter requestContextFilter() {
//            return new RequestContextFilter();
//        }
    }


    /**
     * `ErrorCode.INVALID_INPUT_VALUE`와 추가 메시지를 포함하는 `BusinessException`이
     * `GlobalExceptionHandler`에 의해 올바르게 처리되고, 예상된 JSON 응답을 반환하는지 테스트합니다.
     */
    @Test
    @DisplayName("INVALID_INPUT_VALUE와 추가 메시지를 포함하는 BusinessException 처리 테스트")
    void callException() throws Exception {
        log.info("테스트 시작: INVALID_INPUT_VALUE with message");
        mockMvc.perform(get("/test/business-exception-message") // TestController의 엔드포인트 호출
                        .contentType(MediaType.APPLICATION_JSON)) // 요청 Content-Type 설정
                .andDo(print()) // 요청 및 응답 상세 내용을 콘솔에 출력 (디버깅용)
                .andExpect(status().isBadRequest()) // HTTP 상태 코드가 400 Bad Request인지 확인
                .andExpect(jsonPath("$.code").value("C001")) // 응답 JSON의 'code' 필드가 "C001"인지 확인
                .andExpect(jsonPath("$.message").value("유효하지 않은 입력 값입니다. : 테스트용")) // 'message' 필드 확인
//                .andExpect(jsonPath("$.traceId").exists()) // 'traceId' 필드가 존재하는지 확인
                .andExpect(jsonPath("$.errors").doesNotExist()); // 'errors' 필드는 없어야 함 (유효성 검증 오류가 아님)
        log.info("테스트 완료: INVALID_INPUT_VALUE with message");
    }

    /**
     * `ErrorCode.USER_NOT_FOUND`와 같이 기본 메시지를 사용하는 `BusinessException`이
     * `GlobalExceptionHandler`에 의해 올바르게 처리되는지 테스트합니다.
     */
    @Test
    @DisplayName("기본 메시지를 가지는 BusinessException 처리 테스트 (USER_NOT_FOUND)")
    void testBusinessExceptionSimple() throws Exception {
        log.info("테스트 시작: USER_NOT_FOUND simple");
        mockMvc.perform(get("/test/business-exception-simple")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound()) // HTTP 상태 코드가 404 Not Found인지 확인
                .andExpect(jsonPath("$.code").value("U001"))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."))
//                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.errors").doesNotExist());
        log.info("테스트 완료: USER_NOT_FOUND simple");
    }

    /**
     * `@ExceptionHandler(Exception.class)`에 의해 처리될 예측하지 못한 일반 예외가
     * `GlobalExceptionHandler`에 의해 올바르게 처리되는지 테스트합니다.
     */
    @Test
    @DisplayName("예측하지 못한 일반 예외 (RuntimeException) 처리 테스트")
    void testUnhandledException() throws Exception {
        log.info("테스트 시작: Unhandled Exception");
        mockMvc.perform(get("/test/unhandled-exception")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError()) // HTTP 상태 코드가 500 Internal Server Error인지 확인
                .andExpect(jsonPath("$.code").value("C003"))
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."))
//                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.errors").doesNotExist());
        log.info("테스트 완료: Unhandled Exception");
    }
}
