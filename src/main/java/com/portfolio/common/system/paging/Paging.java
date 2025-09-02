package com.portfolio.common.system.paging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 서비스 메서드에 이 어노테이션을 붙이면,
 * PagingAspect에 의해 MyBatis 페이징이 자동으로 활성화됩니다.
 *
 * @Target(ElementType.METHOD): 이 어노테이션은 메서드에만 붙일 수 있습니다.
 * @Retention(RetentionPolicy.RUNTIME): 런타임 시에 AOP가 이 어노테이션 정보를 읽을 수 있도록 합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Paging {
}
