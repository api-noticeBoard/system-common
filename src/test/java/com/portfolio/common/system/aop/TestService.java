package com.portfolio.common.system.aop;

/**
 * LogTraceAspect AOP가 적용되는지 테스트하기 위한 간단한 서비스 클래스.
 * @Service 어노테이션이 붙어있으므로, LogTraceAspect의 Pointcut("within(@Service *)")에 매칭.
 */
import org.springframework.stereotype.Service;

@Service
public class TestService {

    /**
     * AOP가 적용될 테스트 메서드.
     */
    public String doSomething() {
        // 메서드가 실제로 호출되었는지 확인하기 위해 간단한 문자열을 반환합니다.
        return "Hello, AOP!";
    }

    /**
     * 예외가 발생했을 때도 AOP의 finally 블록이 정상 동작하는지 테스트하기 위한 메서드.
     */
    public void throwException() {
        throw new RuntimeException("Test Exception");
    }
}
