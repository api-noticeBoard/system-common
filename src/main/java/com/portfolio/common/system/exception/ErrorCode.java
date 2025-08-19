package com.portfolio.common.system.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * [Exception Handling - Step 1]
 * 애플리케이션에서 발생할 수 있는 모든 에러의 종류를 표준화하여 Enum으로 정의.
 * - HTTP 상태 코드, 고유 에러 코드, 기본 메시지를 한 곳에서 관리하여 일관성 유지.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 접두사	    영문	한글 의미	        설명
    // C	        Common	공통	            도메인과 상관없이 모든 시스템에서 발생할 수 있는 일반적인 에러입니다. (예: 잘못된 입력, 서버 내부 오류 등)
    // A	        Auth	인증/인가	    로그인, 회원가입, 권한 등 보안과 관련된 에러입니다. (예: 비밀번호 불일치, 토큰 만료, 접근 거부 등)
    // U	        User	사용자	        이미 존재하는 이메일입니다 (U001), 존재하지 않는 사용자입니다 (U002)
    // P	        Post	게시글	        존재하지 않는 게시글입니다 (P001), 게시글 작성 권한이 없습니다 (P002)
    // F	        File	파일	            파일 업로드 용량을 초과했습니다 (F001), 지원하지 않는 파일 형식입니다 (F002)
    // T	        Payment	결제	            잔액이 부족합니다 (T001), 만료된 카드입니다 (T002)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "유효하지 않은 입력 값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", "잘못된 타입의 값입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C005", "접근이 거부되었습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증에 실패하였습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
