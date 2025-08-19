package com.portfolio.common.system.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * [Request Tracing Filter]
 * 모든 HTTP 요청에 대해 고유한 추적 ID(TraceID)를 발급하고 로깅 컨텍스트(MDC)에 추가하는 서블릿 필터.
 * 이 필터는 Spring의 DispatcherServlet보다 먼저 실행되어, 들어오는 모든 요청을 가장 먼저 가로챔.
 *
 * MSA(Microservice Architecture) 환경에서 여러 서비스에 걸친 요청의 흐름을 추적하는 데 필수적임.
 * 예를 들어, API Gateway -> Service A -> Service B 로 요청이 전달될 때, 동일한 TraceID를 전파하면
 * 분산 로깅 시스템(ELK, Datadog 등)에서 특정 요청의 전체 처리 과정을 한눈에 볼 수 있음.
 */
public class TraceIdFilter extends OncePerRequestFilter {

    // 클라이언트와 서버 간에 TraceID를 주고받을 때 사용할 HTTP 헤더의 이름.
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    // MDC(Mapped Diagnostic Context)에 TraceID를 저장할 때 사용할 키(Key).
    // 이 키는 logback.xml이나 log4j2.xml 같은 로깅 설정 파일에서 %X{traceId} 형태로 사용.
    private static final String TRACE_ID_KEY = "traceId";

    /**
     * 필터의 실제 로직이 구현되는 메서드.
     * Spring이 HTTP 요청이 들어올 때마다 이 메서드를 호출.
     * @param request       들어온 HTTP 요청 정보가 담긴 객체
     * @param response      나갈 HTTP 응답 정보를 담을 객체
     * @param filterChain   다음 필터로 요청/응답을 전달하기 위한 체인 객체
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // TraceID 존재 확인
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().substring(0, 8);
        }

        // 현재 스레드의 MDC에 TraceID 저장
        MDC.put(TRACE_ID_KEY, traceId);
        // 응답헤더에 TraceID 추가
        response.addHeader(TRACE_ID_HEADER, traceId);

        try {
            // 다음 필터 또는 실제 컨트롤러로 요청을 전달.
            // 이 라인이 실행되어야 비로소 컨트롤러의 메서드가 호출.
            filterChain.doFilter(request, response);
        } finally {
            // 컨트롤러의 모든 로직이 실행되고 응답이 나가기 직전에, 반드시 MDC에서 TraceID를 제거.
            // 톰캣과 같은 WAS는 스레드를 재사용(Thread Pool)하므로, 이전 요청의 TraceID가
            // 다음 요청에 잘못 사용되는 것을 방지하기 위한 매우 중요한 작업.
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
