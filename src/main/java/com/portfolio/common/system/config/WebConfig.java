package com.portfolio.common.system.config;

import com.portfolio.common.system.trace.TraceIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * [Web MVC Configuration]
 * Spring Web MVC의 전역적인 설정을 담당하는 클래스.
 * CORS 정책, 인터셉터 등록, 필터 등록 등을 처리.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    /**
     * 전역 CORS(Cross-Origin Resource Sharing) 설정을 정의.
     * 브라우저에서 실행되는 프론트엔드 JavaScript 코드가 다른 도메인(Origin)의 API 서버를 호출할 수 있도록 허용하는 정책.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // '/api/'로 시작하는 모든 경로에 대해 이 정책을 적용.
                .allowedOrigins("http://localhost:3000") // 'http://localhost:3000'에서 오는 요청만 허용. (React/Vue 개발 서버 주소)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // 허용할 HTTP 메서드를 명시.
                .allowedHeaders("*") // 모든 종류의 요청 헤더를 허용.
                .allowCredentials(true) // 요청에 쿠키 등 인증 정보를 포함하는 것을 허용.
                .maxAge(3600); // Pre-flight(사전 요청) 결과를 브라우저에 캐시할 시간(초)을 설정하여 불필요한 요청을 줄임.
    }

    /**
     * 우리가 직접 만든 TraceIdFilter를 서블릿 필터로 Spring에 등록하는 Bean.
     * @return FilterRegistrationBean 필터에 대한 상세 등록 정보를 담은 객체
     */
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        // 필터를 등록하고 상세 설정을 하기 위한 Spring Boot의 헬퍼 클래스.
        FilterRegistrationBean<TraceIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TraceIdFilter()); // 등록할 필터의 인스턴스를 지정.
        registrationBean.setOrder(1); // 필터 체인에서 실행 순서를 지정합니다. 숫자가 낮을수록 먼저 실행. (1은 매우 높은 우선순위)
        registrationBean.addUrlPatterns("/*"); // 모든 요청 URL('/*')에 대해 이 필터가 동작하도록 설정.
        return registrationBean;
    }
}
