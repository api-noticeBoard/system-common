package com.portfolio.common.system.paging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

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

        if (pageRequest == null) { return joinPoint.proceed(); }

        try {
            PagingContext.setPageRequest(pageRequest);

            // 1. 원본 메서드(List 반환)를 실행합니다.
            Object result = joinPoint.proceed();

            // 2. ✨ [핵심] 반환된 List를 PageDto.Response로 포장하여 최종 반환합니다.
            if (result instanceof List) {
                List<?> content = (List<?>) result;
                Long totalCount = PagingContext.getTotalCount();
                return new PageDto.Response<>(content, pageRequest, totalCount);
            }
            return result;

        } finally {
            PagingContext.clear();
        }
    }
}
