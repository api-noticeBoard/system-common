package com.portolio.common.system.exception;

import lombok.Getter;

/**
 * [Exception Handling - Step 2]
 * 비즈니스 로직(Service 계층 등)을 처리하는 과정에서 발생하는 모든 예외를 대표하는 부모 클래스입니다.
 *
 * 'Exception'이 아닌 'RuntimeException'을 상속받는 이유는,
 * 비즈니스 예외는 대부분 복구 불가능한(Unchecked) 예외이며,
 * 서비스 로직에서 불필요한 try-catch 문을 줄여 코드의 가독성을 높이기 위함입니다.
 *
 * 이 클래스는 직접 사용되기보다는, 더 구체적인 비즈니스 예외 클래스들이 이 클래스를 상속받아 만들어집니다.
 * (예: `UserNotFoundException extends BusinessException { ... }`)
 *
 * 또는 간단한 경우에는 이 클래스를 직접 생성하여 사용하기도 합니다.
 * (예: `throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);`)
 */
@Getter // 외부에서 `getErrorCode()` 메서드를 통해 ErrorCode에 접근할 수 있도록 합니다.
public class BusinessException extends RuntimeException{

    /**
     * 발생한 예외의 종류를 명확하게 식별하기 위한 ErrorCode.
     * 이 필드 덕분에 GlobalExceptionHandler에서 어떤 종류의 에러인지 파악하고
     * 그에 맞는 HTTP 상태 코드와 메시지로 응답을 생성.
     * final 키워드를 사용하여 한 번 할당된 후에는 변경될 수 없도록 함.
     */
    private final ErrorCode errorCode;

    /**
     * ErrorCode에 정의된 기본 메시지 외에, 동적인 데이터를 포함하는
     * 커스텀 메시지를 함께 전달하고 싶을 때 사용하는 생성자.
     *
     * @param message 상세한 에러 메시지 (예: "User not found with id: 123")
     * @param errorCode 발생한 에러의 종류를 나타내는 ErrorCode (예: ErrorCode.USER_NOT_FOUND)
     */
    public BusinessException(String message, ErrorCode errorCode) {
        // 부모 클래스인 RuntimeException의 생성자를 호출하여 예외 메시지를 설정.
        super(message);
        // 이 예외의 ErrorCode를 설정.
        this.errorCode = errorCode;
    }

    /**
     * ErrorCode에 정의된 기본 메시지만으로 충분할 때 사용하는 가장 일반적인 생성자.
     *
     * @param errorCode 발생한 에러의 종류를 나타내는 ErrorCode (예: ErrorCode.INVALID_INPUT_VALUE)
     */
    public BusinessException(ErrorCode errorCode) {
        // ErrorCode Enum에 정의된 기본 메시지를 가져와 부모 클래스의 생성자에 전달.
        super(errorCode.getMessage());
        // 이 예외의 ErrorCode를 설정.
        this.errorCode = errorCode;
    }
}
