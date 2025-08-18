package com.portolio.common.system.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * [AOP (Aspect-Oriented Programming)]
 * 애플리케이션의 핵심 비즈니스 로직(Core Concern)에서 횡단 관심사(Cross-cutting Concerns)를 분리.
 * 이 클래스는 로깅 및 성능 측정이라는 횡단 관심사를 담당하는 Aspect.
 */
@Slf4j
@Aspect     // 이 클래스가 AOP의 Aspect(어드바이스 + 포인트컷)임을 선언.
@Component  // Spring이 이 클래스를 Bean으로 관리.
public class LogTraceAspect {

    // @RestController, @Service가 붙은 클래스 내부의 publice 메서드가 이 어드바이스에 적용
    @Around("within(@org.springframework.web.bind.annotation.RestController *) " +
            "|| within(@org.springframework.stereotype.Service *)")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable{

        // 실행되는 메서드의 시그니처(메서드 정보)를 가져옴.
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        StopWatch stopWatch = new StopWatch();

        String taskName = className + "." + methodName + "()";
        try{
//            log.info("[START] {}.{}()", className, methodName);       // TEST = false
            log.info("[START] {}", taskName);                           // TEST = true
            stopWatch.start();
            return joinPoint.proceed();
        }finally {
            stopWatch.stop();
            long totalTimeMillis = stopWatch.getTotalTimeMillis();
//            log.info("[END] {}.{}(), {}ms", className, methodName, totalTimeMillis);      // TEST = false
            log.info("[END] {} | EXECUTION_TIME = {}ms", taskName, totalTimeMillis);        // TEST = true

        }
    }
}
