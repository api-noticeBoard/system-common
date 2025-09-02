package com.portfolio.common.system.paging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @Paging 어노테이션을 감지하여 MyBatis 페이징을 자동으로 처리하는 AOP Aspect.
 */
@Slf4j
@Aspect
@Component
public class PagingAspect {
    /**
     * @Around: 대상 메서드 실행 전후에 개입하여 로직을 추가합니다.
     * "@annotation(com.portfolio.common.system.paging.Paging)": @Paging 어노테이션이 붙은 모든 메서드를 대상으로 합니다.
     */
    @Around("@annotation(com.portfolio.common.system.paging.Paging")
    public Object applyPaging(ProceedingJoinPoint joinPoint) throws Throwable{
        // 1. 대상 메서드의 파라미터에서 PageDto.Request 객체를 찾습니다.
        PageDto.Request pageRequest = Arrays.stream(joinPoint.getArgs())
                .filter(arg -> arg instanceof PageDto.Request)
                .map(arg -> (PageDto.Request)arg)
                .findFirst()
                .orElse(null);

        // PageDto.Request 파라미터가 없으면 AOP를 적용하지 않고 원본 메서드를 그대로 실행합니다.
        if (pageRequest == null) {
            log.warn("@Paging 어노테이션이 있지만, PageDto.Request 파라미터가 없어 페이징을 적용할 수 없습니다. Method: {}", joinPoint.getSignature().toShortString());
            return joinPoint.proceed();
        }

        try {
            // 2. [Before] 대상 메서드 실행 전에 PagingContext를 설정합니다. (인터셉터 활성화)
            PagingContext.setPageRequest(pageRequest);
            log.debug("PagingAspect: PagingContext set for page {}, size {}", pageRequest.getPage(), pageRequest.getSize());

            // 3. [Execution] 원본 서비스 메서드(e.g., postMapper.findAll())를 실행합니다.
            //    이 과정에서 PagingInterceptor가 동작하여 페이징 쿼리와 카운트 쿼리를 실행합니다.
            Object result = joinPoint.proceed();

            // 4. [After] 원본 메서드가 반환한 데이터 목록(List)과 PagingContext에 저장된 totalCount를 조합하여
            //    최종적인 PageDto.Response 객체를 생성합니다.
            if (result instanceof List) {
                List<?> content = (List<?>) result;
                Long totalCount = PagingContext.getTotalCount();
                return new PageDto.Response<>(content, pageRequest, totalCount);
            } else {
                log.warn("@Paging 어노테이션이 붙은 메서드의 반환 타입이 List가 아닙니다. 페이징 응답 객체를 생성할 수 없습니다. Method: {}", joinPoint.getSignature().toShortString());
                return result; // List가 아니면 원본 결과를 그대로 반환
            }

        } finally {
            // 5. [Finally] 메서드 실행이 성공하든 실패하든, 반드시 PagingContext를 초기화합니다.
            PagingContext.clear();
            log.debug("PagingAspect: PagingContext cleared.");
        }
    }
}