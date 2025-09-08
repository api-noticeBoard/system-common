package com.portfolio.common.system.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 엑셀 다운로드 시 DTO의 특정 필드를 엑셀 컬럼으로 매핑하기 위한 커스텀 어노테이션입니다.
 */
@Target(ElementType.FIELD)          // 이 어노테이션은 필드에만 붙일 수 있습니다.
@Retention(RetentionPolicy.RUNTIME) // 런타임 시에 이 어노테이션 정보를 읽을 수 있어야 합니다.
public @interface ExcelColumn {
    // 엑셀의 헤더(첫 번째 행)에 표시될 이름을 지정.
    String headerName() default  "";
    // 엑셀 컬럼의 순서를 지정합니다. 낮은 숫자가 앞에 옴.
    int order() default Integer.MAX_VALUE;
    // 업로드 시, 몇 번째 열의 데이터인지 지정 (0부터 시작)
    int colIndex() default -1;
}
