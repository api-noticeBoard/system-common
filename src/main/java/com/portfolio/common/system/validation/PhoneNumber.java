package com.portfolio.common.system.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * [Custom Validation Annotation]
 * 전화번호 형식(예: "010-1234-5678")을 검증하기 위한 커스텀 유효성 검증 어노테이션.
 * 이 어노테이션을 DTO의 String 타입 필드 위에 붙이면, 해당 필드의 값이 지정된 전화번호 형식에
 * 맞는지 자동으로 검증.
 * <pre>
 *     public class UserDto{
 *          @PhonNumber
 *          private String mobile;
 *     }
 * </pre>
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumber.PhoneNumberValidator.class)
public @interface PhoneNumber {

    /**
     * 유효성 검증에 실패했을 때 반환될 기본 에러 메시지.
     * ValidationMessages 클래스의 상수를 사용하거나 직접 메시지를 지정.
     */
    String message() default "올바른 전화번호 형식이 아닙니다.";

    /**
     * 유효성 검증 그룹을 지정할 때 사용. (고급 기능)
     * 특정 상황(예: '등록' 시에만 검증)에서만 이 유효성 검사를 활성화하고 싶을 때 사용.
     */
    Class<?>[] groups() default {};

    /**
     * 유효성 검증에 대한 심각도 등 메타데이터를 전달할 때 사용합니다. (고급 기능)
     */
    Class<? extends Payload>[] payload() default {};

    class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {
        private static final String PHONE_NUMBER_REGEX = "^\\d{3}-\\d{3,4}-\\d{4}$";

        /**
         * 유효성 검증을 수행하는 핵심 메서드.
         *
         * @param value   어노테이션이 붙은 필드의 실제 값 (사용자가 입력한 값)
         * @param context 유효성 검증 컨텍스트 정보 (메시지 커스터마이징 등에 사용 가능)
         * @return 유효하면 true, 유효하지 않으면 false를 반환.
         */
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.isBlank()) {
                return true; // null 이나 blank는 @NotBlank로 검증
            }
            return value.matches(PHONE_NUMBER_REGEX);
        }
    }
}
