package com.portfolio.common.system.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.stream.Collectors;

/**
 * [Exception Handling - Step 3]
 * API에서 예외가 발생했을 때, 클라이언트(프론트엔드 등)에게 반환될 표준 에러 응답 DTO.
 * 이 클래스는 애플리케이션의 모든 에러 응답 형식을 통일하여, 클라이언트 측에서 일관된 방식으로
 * 에러를 처리할 수 있도록 도움.
 */
@Getter
// @NoArgsConstructor(access = AccessLevel.PROTECTED): 외부에서 new ErrorResponse()를 통한 직접 생성을 막고,
// Jackson 라이브러리(JSON <-> Object 변환) 등 내부적인 용도로만 생성자를 사용할 수 있도록 접근 수준을 protected로 제한.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// @JsonInclude(JsonInclude.Include.NON_NULL): 이 객체를 JSON으로 변환할 때,
// 값이 null인 필드는 결과 JSON에서 아예 생략하여 응답의 크기를 줄이고 깔끔하게 만듬.
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * ErrorCode Enum에 정의된 애플리케이션 고유 에러 코드. (예: "C001", "U002")
     * 클라이언트가 이 코드를 보고 특정 에러 상황에 대한 분기 처리. (예: 토큰 만료 시 재로그인 페이지로 이동)
     */
    private String code;

    /**
     * 에러에 대한 사람이 읽을 수 있는 기본 메시지. (예: "유효하지 않은 입력 값입니다.")
     * 주로 사용자에게 직접 보여주기보다는 개발 디버깅용으로 사용.
     */
    private String message;

    /**
     * 요청 추적을 위한 고유 ID(TraceID).
     * 이 ID를 통해 분산 로깅 시스템에서 특정 요청의 전체 처리 과정을 쉽게 추적.
     * 값은 TraceIdFilter에서 생성되어 MDC에 저장된 값을 가져 옴.
     */
    private String traceId;

    /**
     * DTO의 필드 유효성 검증(@Valid)에 실패했을 때, 어떤 필드가 어떤 이유로 실패했는지에 대한
     * 상세 정보를 담는 리스트입니다. 일반적인 비즈니스 예외 발생 시에는 이 필드는 null.
     */
    private List<CustomFieldError> errors;

    /**
     * 필드 에러 정보를 포함하는 경우에 사용되는 private 생성자.
     * 정적 팩토리 메서드(of)를 통해서만 호출.
     *
     * @param code   발생한 에러의 종류를 나타내는 ErrorCode
     * @param errors 유효성 검증 실패에 대한 상세 정보 리스트
     */
    private ErrorResponse(final ErrorCode code, final List<CustomFieldError> errors) {
        this.message = code.getMessage();
        this.code = code.getCode();
        this.traceId = MDC.get("traceId"); // 현재 스레드의 MDC에서 traceId를 가져 옴.
        this.errors = errors;
    }

    /**
     * 일반적인 비즈니스 에러(필드 에러 정보가 없는 경우)에 사용되는 private 생성자.
     *
     * @param code 발생한 에러의 종류를 나타내는 ErrorCode
     */
    private ErrorResponse(final ErrorCode code) {
        this.message = code.getMessage();
        this.code = code.getCode();
        this.traceId = MDC.get("traceId");
        this.errors = null; // 필드 에러가 없으므로 null로 설정 (@JsonInclude에 의해 최종 JSON에서 생략됨)
    }

    private ErrorResponse(final ErrorCode code, final String detailMessage) {
        this.message = detailMessage;
        this.code = code.getCode();
        this.traceId = MDC.get("traceId");
        this.errors = null; // 필드 에러가 없으므로 null로 설정 (@JsonInclude에 의해 최종 JSON에서 생략됨)
    }

    /**
     * [정적 팩토리 메서드]
     * 외부에서 ErrorResponse 객체를 생성할 때 사용하는 기본 진입점. (필드 에러 없음)
     * new 키워드를 직접 사용하는 것보다 의미를 명확하게 전달.
     *
     * @param code 발생한 에러의 종류를 나타내는 ErrorCode
     * @return 생성된 ErrorResponse 객체
     */
    public static ErrorResponse of(final ErrorCode code) {
        return new ErrorResponse(code);
    }

    /**
     BusinessException 객체 자체를 받아 ErrorResponse를 생성하는 정적 팩토리 메서드.
     이 메서드는 예외 객체에 저장된 최종 메시지를 사용합니다.
     */
    public static ErrorResponse of(final BusinessException e) {
        return new ErrorResponse(e.getErrorCode(), e.getMessage());
    }

    /**
     * [정적 팩토리 메서드]
     *
     * @param code          ErrorCode.INVALID_INPUT_VALUE 와 같은 에러 코드
     * @param bindingResult 컨트롤러에서 발생한 유효성 검증 실패 정보가 모두 담겨있는 객체
     * @return 필드 에러 정보가 포함된 ErrorResponse 객체
     * @Valid 유효성 검증 실패 시 사용되는 진입점. (필드 에러 정보 포함)
     */
    public static ErrorResponse of(final ErrorCode code, final BindingResult bindingResult) {
        // 내부 private 헬퍼 메서드를 호출하여 CustomFieldError 리스트를 생성하고, 이를 생성자에 전달.
        return new ErrorResponse(code, CustomFieldError.from(bindingResult));
    }

    /**
     * DTO의 특정 필드에서 유효성 검증 에러가 발생했을 때, 그 상세 정보를 담는 중첩 클래스(Nested Class).
     * 이 클래스는 ErrorResponse의 일부로만 사용되므로, 강한 연관성을 표현하기 위해 내부에 선언.
     * Spring의 FieldError와 이름 충돌을 피하고, API 응답에 필요한 정보만 담기 위해 직접 정의.
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class CustomFieldError {
        /**
         * 에러가 발생한 DTO의 필드명 (예: "title", "email")
         */
        private String field;

        /**
         * 클라이언트가 보낸 잘못된 값 (예: "", "not-an-email")
         */
        private String value;

        /**
         * 유효성 검증에 실패한 이유 (DTO의 어노테이션에 정의된 message)
         * (예: "제목은 필수 입력 항목입니다.", "올바른 이메일 형식이 아닙니다.")
         */
        private String reason;

        /**
         * CustomFieldError 객체 생성을 위한 private 생성자
         */
        private CustomFieldError(final String field, final String value, final String reason) {
            this.field = field;
            this.value = value;
            this.reason = reason;
        }

        /**
         * Spring의 BindingResult 객체를 우리가 정의한 CustomFieldError 리스트로 변환하는 private 헬퍼 메서드.
         * 이 변환 로직은 ErrorResponse 클래스 내부에서만 알면 되므로, 외부로 노출하지 않기 위해 private으로 선언. (캡슐화)
         *
         * @param bindingResult 컨트롤러에서 전달받은 유효성 검증 결과 객체
         * @return API 응답에 적합한 형태로 변환된 필드 에러 리스트
         */
        private static List<CustomFieldError> from(final BindingResult bindingResult) {
            // 1. BindingResult에서 Spring의 FieldError 객체 리스트를 가져 옴.
            final List<FieldError> fieldErrors = bindingResult.getFieldErrors();

            // 2. Java Stream API를 사용하여 각 FieldError 객체를 CustomFieldError 객체로 변환(map)하고,
            //    그 결과를 새로운 리스트(List)로 수집(collect).
            return fieldErrors.stream()
                    .map(error -> new CustomFieldError(
                            error.getField(), // Spring FieldError에서 필드명 가져오기
                            error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(), // 거부된 값 가져오기
                            error.getDefaultMessage())) // 어노테이션에 설정된 에러 메시지 가져오기
                    .collect(Collectors.toList());
        }
    }
}
