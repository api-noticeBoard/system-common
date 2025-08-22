package com.portfolio.common.system.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * [Exception Handling - Step 4: The Core]
 * @RestControllerAdvice 어노테이션을 사용하여 애플리케이션의 모든 @RestController에서 발생하는
 * 예외를 전역적으로 가로채서 처리하는 클래스.
 *
 * 이 클래스의 핵심적인 역할.
 * 1. 컨트롤러 코드와 예외 처리 로직을 완벽하게 분리하여 코드의 가독성과 유지보수성을 높임.
 * 2. 발생하는 모든 예외에 대해 일관된 형식(ErrorResponse)의 응답을 클라이언트에게 반환하도록 보장.
 * 3. 예외 발생 시, 적절한 로그를 남겨 운영 중 문제 추적을 용이.
 */
@Slf4j                  // 로깅을 위한 Lombok 어노테이션. (log.error(), log.warn() 등 사용 가능)
@RestControllerAdvice   // @ControllerAdvice + @ResponseBody. 예외 처리 결과를 JSON 형태로 반환.
public class GlobalExceptionHandler {

    /**
     * [Exception Handler for @Valid]
     * @ExceptionHandler 어노테이션은 특정 예외 클래스를 지정하여, 해당 예외가 발생했을 때 이 메서드가 호출.
     * 이 메서드는 컨트롤러의 DTO 파라미터에 @Valid 어노테이션을 사용했을 때, 유효성 검증에 실패하면
     * 발생하는 `MethodArgumentNotValidException`을 전문적으로 처리.
     *
     * @param e 발생한 MethodArgumentNotValidException 객체. 어떤 필드가 왜 실패했는지에 대한 모든 정보를 담음.
     * @return 클라이언트에게 반환될, 필드 에러 정보가 포함된 ResponseEntity<ErrorResponse> 객체
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        // 어떤 유효성 검증에서 실패했는지 경고(WARN) 레벨로 로그를 남김. (서버 오류는 아니므로 ERROR 레벨은 아님.)
        log.warn("handleMethodArgumentNotValidException: {}", e.getMessage());

        // ErrorResponse의 정적 팩토리 메서드를 호출하여 표준 에러 응답 DTO를 생성.
        // e.getBindingResult()를 전달하여 상세한 필드 에러 정보를 ErrorResponse에 포함.
        final ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult());

        // 생성된 ErrorResponse 객체와 적절한 HTTP 상태 코드를 담아 ResponseEntity를 반환.
        return new ResponseEntity<>(response, ErrorCode.INVALID_INPUT_VALUE.getStatus());
    }

    /**
     * [Exception Handler for Business Logic Errors]
     * 우리가 직접 정의한 `BusinessException` (및 그 자식 클래스들)을 처리.
     * 서비스 로직에서 `throw new BusinessException(...)` 코드가 실행되면 이 메서드가 호출.
     *
     * @param e 발생한 BusinessException 객체. 어떤 종류의 비즈니스 에러인지 ErrorCode를 통해 앎.
     * @return 클라이언트에게 반환될 ResponseEntity<ErrorResponse> 객체
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(final BusinessException e) {
        // 비즈니스 로직 에러는 원인 파악이 중요하므로 에러(ERROR) 레벨로 로그를 남김.
        // 마지막 인자로 예외 객체(e)를 전달하면 스택 트레이스(Stack Trace)도 함께 로깅.
        log.error("handleBusinessException: {} - {}", e.getErrorCode(), e.getMessage(), e);

        // 발생한 예외로부터 ErrorCode를 가져 옴.
        final ErrorCode errorCode = e.getErrorCode();

        // ErrorCode를 사용하여 표준 에러 응답 DTO를 생성.
        final ErrorResponse response = ErrorResponse.of(errorCode);

        // 생성된 ErrorResponse 객체와 ErrorCode에 정의된 HTTP 상태 코드를 담아 ResponseEntity를 반환.
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    /**
     * [Fallback Exception Handler]
     * 위에서 지정하지 않은 모든 종류의 예외(예: NullPointerException, IOException 등)를 처리하는
     * 최후의 보루 역할을 하는 핸들러.
     *
     * @param e 발생한 예외 객체
     * @return 500 Internal Server Error 상태를 담은 ResponseEntity<ErrorResponse> 객체
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        // 예측하지 못한 예외는 심각한 문제일 수 있으므로 에러(ERROR) 레벨로 전체 스택 트레이스와 함께 로깅.
        log.error("Unhandled Exception: ", e);

        // 클라이언트에게는 상세한 내부 오류 내용을 노출하지 않고, 표준적인 서버 내부 오류 응답을 보냄.
        final ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);

        return new ResponseEntity<>(response, ErrorCode.INTERNAL_SERVER_ERROR.getStatus());
    }
}
