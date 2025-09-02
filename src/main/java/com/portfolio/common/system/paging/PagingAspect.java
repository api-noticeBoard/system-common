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
            return joinPoint.proceed();
        }

        try {
            // [Before] PagingContext 설정
            PagingContext.setPageRequest(pageRequest);

            // [Execution] 원본 메서드를 그대로 실행하고 결과를 반환
            return joinPoint.proceed();

        } finally {
            // [Finally] PagingContext 해제
            PagingContext.clear();
        }
    }
}