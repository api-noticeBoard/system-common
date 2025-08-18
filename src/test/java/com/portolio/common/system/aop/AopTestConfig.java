package com.portolio.common.system.aop;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * [AOP Test Configuration]
 * LogTraceAspect의 AOP 동작을 테스트하기 위한 Spring 설정 클래스.
 *
 * @Configuration : 이 클래스가 Spring Bean 설정 클래스임을 명시.
 * @EnableAspectJAutoProxy : AOP 프록시를 자동으로 생성하도록 활성화.
 * @ComponentScan : LogTraceAspect (@Aspect, @Component)와 TestService (@Service)를 Bean으로 등록하기 위해 스캔 범위를 지정합니다.
 */
@Configuration
@EnableAspectJAutoProxy // 이 어노테이션이 Java 코드 내에서 AOP 기능을 활성화합니다.
@ComponentScan(basePackageClasses = { LogTraceAspect.class, TestService.class })
public class AopTestConfig {
}
