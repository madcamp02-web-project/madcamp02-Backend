package com.madcamp02.exception;

/**
 * 사용자 관련 예외
 * 사용자 조회, 생성 실패 시 발생
 */
public class UserException extends BusinessException {

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UserException(String message) {
        super(ErrorCode.USER_NOT_FOUND, message);
    }

    public UserException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
