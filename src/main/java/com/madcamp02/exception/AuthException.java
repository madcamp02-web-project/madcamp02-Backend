package com.madcamp02.exception;

/**
 * 인증 관련 예외
 * 로그인, 토큰 검증, 권한 체크 실패 시 발생
 */
public class AuthException extends BusinessException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthException(String message) {
        super(ErrorCode.AUTH_INVALID_TOKEN, message);
    }

    public AuthException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
