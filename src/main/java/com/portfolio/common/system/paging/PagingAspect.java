package com.portfolio.common.system.paging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @Paging 어노테이션을 감지하여 PagingContext를 자동으로 설정하고 해제하는 Aspect.
 * 이제 이 Aspect는 응답 객체를 생성하지 않습니다.
 */
@Slf4j
@Aspect
@Component
public class PagingAspect {

    @Around("@annotation(com.portfolio.common.system.paging.Paging)")
    public Object applyPaging(ProceedingJoinPoint joinPoint) throws Throwable {

        PageDto.Request pageRequest = Arrays.stream(joinPoint.getArgs())
                .filter(arg -> arg instanceof PageDto.Request)
                .map(arg -> (PageDto.Request) arg)
                .findFirst()
                .orElse(null);

        if (pageRequest == null) {
            log.warn("@Paging 어노테이션이 있지만 PageDto.Request 파라미터가 없어 페이징을 적용할 수 없습니다. Method: {}", joinPoint.getSignature().toShortString());
            return joinPoint.proceed();
        }

        try {
            // [Before] PagingContext 설정
            PagingContext.setPageRequest(pageRequest);

            // [Execution] 원본 서비스 메서드 실행 (이 메서드는 List를 반환해야 함)
            Object result = joinPoint.proceed();

            // [After] 원본 결과를 PageDto.Response로 포장하여 최종 반환
            if (result instanceof List) {
                List<?> content = (List<?>) result;
                Long totalCount = PagingContext.getTotalCount();
                return new PageDto.Response<>(content, pageRequest);
            } else {
                log.warn("@Paging 어노테이션이 붙은 메서드의 반환 타입이 List가 아닙니다. Method: {}", joinPoint.getSignature().toShortString());
                return result;
            }

        } finally {
            // [Finally] PagingContext 해제
            PagingContext.clear();
        }
    }
}