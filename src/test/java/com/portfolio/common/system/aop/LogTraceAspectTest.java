package com.portfolio.common.system.aop;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * [AOP Test]
 * LogTraceAspect의 동작을 검증하는 테스트 클래스.
 *
 * @SpringBootTest 어노테이션을 사용하여 테스트를 위한 Spring 컨테이너(ApplicationContext)를 생성.
 * 이 컨테이너는 main 소스의 컴포넌트뿐만 아니라, 테스트 소스의 컴포넌트까지 스캔하여 Bean으로 등록.
 * @ExtendWith(OutputCaptureExtension.class) JUnit 5 확장 기능으로, 테스트 실행 중 콘솔에 출력되는 모든 내용을 캡처하여 검증할 수 있게 함.
 * 이를 통해 log.info()가 실제로 호출되었는지 확인할 수 있음.
 */
@Slf4j
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(classes = AopTestConfig.class)
public class LogTraceAspectTest {

    // Spring 컨테이너에서 LogTraceAspect가 적용된 TestService의 프록시 Bean 주입.
    @Autowired
    private TestService testService;

    @Test
    @DisplayName("정상 호출 시: 메서드 시작/종료 로그와 실행 시간이 정상적으로 출력되어야 한다.")
        // CapturedOutput 파라미터는 OutputCaptureExtension이 주입해주는 객체로, 콘솔 출력 내용을 담고 있습니다.
    void logExecution_whenMethodSucceeds(CapturedOutput output) {
        // --- given ---

        // --- when ---
        // AOP가 적용된 testService의 메서드를 호출.
        String result = testService.doSomething();

        // --- then ---
        // 1. 메서드의 원래 기능이 정상적으로 동작했는지 확인.
        assertThat(result).isEqualTo("Hello, AOP!");

        // 2. 캡처된 콘솔 출력을 확인하여, AOP 로그가 정상적으로 찍혔는지 검증.
        String consoleOutput = output.getAll();

        System.out.println("--- Captured Console Output ---");
        System.out.println(consoleOutput);
        System.out.println("-----------------------------");

        // "[START]" 로그가 포함되어 있는지 확인.
        assertThat(consoleOutput).contains("[START] TestService.doSomething()");
        // "[END]" 로그가 포함되어 있는지 확인.
        assertThat(consoleOutput).contains("[END] TestService.doSomething()");
        // "EXECUTION_TIME" 메시지가 포함되어 있는지 확인.
        assertThat(consoleOutput).contains("EXECUTION_TIME =");
    }

    @Test
    @DisplayName("예외 발생 시: 메서드 시작/종료 로그가 모두 출력되고, 예외는 그대로 호출 측에 전파되어야 한다.")
    void logExecution_whenMethodThrowsException(CapturedOutput output) {
        // --- given ---

        // --- when & then ---
        // 1. 예외가 발생하는 메서드를 호출했을 때, RuntimeException이 정상적으로 발생하는지 확인.
        //    assertThrows는 특정 예외가 발생하는 것을 기대하고 검증하는 JUnit 5의 기능.
        //    AOP가 예외를 중간에 삼키지 않고, 그대로 호출자에게 다시 던져주는지(re-throw) 확인하는 것이 중요.
        assertThrows(RuntimeException.class, () -> {
            testService.throwException();
        });

        // 2. 캡처된 콘솔 출력을 확인하여, 예외가 발생했음에도 불구하고 AOP의 finally 블록이 정상 동작했는지 검증.
        String consoleOutput = output.getAll();

        System.out.println("--- Captured Console Output ---");
        System.out.println(consoleOutput);
        System.out.println("-----------------------------");

        // "[START]" 로그가 포함되어 있는지 확인.
        assertThat(consoleOutput).contains("[START] TestService.throwException()");
        log.debug("consoleOutput : {}", consoleOutput);
        // 예외가 발생했지만, @Around 어드바이스의 finally 블록 덕분에 "[END]" 로그도 포함되어 있어야 함.
        assertThat(consoleOutput).contains("[END] TestService.throwException() | EXECUTION_TIME =");
        // 실행 시간도 정상적으로 측정되어야 함.
        assertThat(consoleOutput).contains("EXECUTION_TIME =");
    }
}






