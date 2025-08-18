package com.portolio.common.system.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * [Utility Class]
 * 날짜 및 시간(java.time 패키지) 관련 처리를 위한 편의 기능을 제공하는 유틸리티 클래스.
 *
 * 이 클래스는 다음과 같은 설계 원칙을 따름.
 * 1. `final class`: 다른 클래스가 이 유틸리티 클래스를 상속받을 수 없도록 하여 의도치 않은 변경 방지.
 * 2. `private constructor`: 외부에서 `new DateTimeUtils()`를 통해 불필요한 인스턴스를 생성 제한.
 *    모든 메서드는 `static`이므로 객체 생성 없이 `DateTimeUtils.toString(...)` 형태로 바로 사용.
 * 3. `static final`: 자주 사용되는 `DateTimeFormatter`를 상수로 미리 생성하여 재사용함으로써 성능을 향상시키고 코드의 일관성을 유지.
 * 4. `Null-safe`: 입력 값이 `null`일 경우 `NullPointerException`이 발생하지 않도록 방어적인 코드 작성.
 */
public final class DateTimeUtils {

    private static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /**
     * "yyyy-MM-dd HH:mm:ss" 형식의 날짜/시간 포맷터.
     * `DateTimeFormatter`는 Thread-safe(멀티스레드 환경에서 안전함)하므로,
     * static final 상수로 만들어 애플리케이션 전역에서 공유해서 사용하는 것이 가장 효율적.
     */
    private static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 유틸리티 클래스는 상태를 가지지 않는(stateless) 메서드들의 모음이므로,
     * 인스턴스화 불필요. private 생성자를 통해 외부에서의 객체 생성을 원천적으로 차단.
     */
    private DateTimeUtils() {}

    /**
     * LocalDateTime 객체를 "yyyy-MM-dd HH:mm:ss" 형식의 문자열로 변환.
     *
     * @param localDateTime 변환할 LocalDateTime 객체. null이 전달될 수 있음.
     * @return 변환된 문자열. 만약 입력된 localDateTime이 null이면, 이 메서드도 null을 반환.
     */
    public static String toString(LocalDateTime localDateTime) {
        // Java 8의 Optional을 사용하여 null 체크를 더 안전하고 선언적으로 처리.
        // if (localDateTime == null) { return null; } else { ... } 와 동일한 로직.
        return Optional.ofNullable(localDateTime)
                // localDateTime이 null이 아닐 경우에만(ifPresent) 포맷팅을 수행.
                .map(ldt -> ldt.format(YYYY_MM_DD_HH_MM_SS))
                // localDateTime이 null일 경우(orElse)에는 null 값을 반환.
                .orElse(null);
    }

    /**
     * "yyyy-MM-dd" 형식의 문자열을 LocalDate 객체로 변환.
     *
     * @param dateString 변환할 날짜/시간 문자열. null 또는 빈 문자열이 전달.
     * @return 변환된 LocalDate 객체. 만약 입력된 문자열이 null이거나 비어있으면 null을 반환.
     *         형식이 맞지 않으면 DateTimeParseException이 발생할 수 있음.
     */
    public static LocalDate toLocalDate(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return null;
        }
        return LocalDate.parse(dateString, YYYY_MM_DD);
    }

    /**
     * "yyyy-MM-dd HH:mm:ss" 형식의 문자열을 LocalDateTime 객체로 변환.
     *
     * @param dateTimeString 변환할 날짜/시간 문자열. null 또는 빈 문자열이 전달.
     * @return 변환된 LocalDateTime 객체. 만약 입력된 문자열이 null이거나 비어있으면 null을 반환.
     *         형식이 맞지 않으면 DateTimeParseException이 발생할 수 있음.
     */
    public static LocalDateTime toLocalDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeString, YYYY_MM_DD_HH_MM_SS);
    }
}
