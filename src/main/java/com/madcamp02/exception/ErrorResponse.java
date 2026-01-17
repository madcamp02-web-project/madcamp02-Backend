package com.madcamp02.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * 에러 응답 DTO (Data Transfer Object)
 * 
 * 모든 예외 상황에서 클라이언트에게 일관된 형식으로 에러 정보를 전달합니다.
 * 프론트엔드에서 이 구조를 기반으로 에러 처리 로직을 작성할 수 있습니다.
 * 
 * 응답 예시:
 * {
 *   "timestamp": "2026-01-17T12:00:00",
 *   "status": 400,
 *   "error": "TRADE_001",
 *   "message": "잔고가 부족합니다."
 * }
 * 
 * 프론트엔드 연동 가이드:
 * - error 필드의 코드(예: TRADE_001)를 기반으로 사용자 친화적 메시지 매핑 가능
 * - status 필드로 HTTP 상태 코드 확인 가능
 * - message 필드는 디버깅용, 사용자에게 직접 노출 시 주의 필요
 */
@Getter
@Builder
public class ErrorResponse {
    
    /**
     * 에러 발생 시각
     * ISO 8601 형식 (예: "2026-01-17T12:00:00")
     */
    private final String timestamp;
    
    /**
     * HTTP 상태 코드
     * 예: 400 (Bad Request), 401 (Unauthorized), 404 (Not Found), 500 (Internal Server Error)
     */
    private final int status;
    
    /**
     * 에러 코드 (프론트엔드에서 분기 처리용)
     * 명세서 기준:
     * - AUTH_001~005: 인증 관련
     * - TRADE_001~004: 거래 관련
     * - GAME_001~003: 게임 관련
     * - USER_001~002: 사용자 관련
     * - SERVER_001~002: 서버 관련
     * - VALIDATION_ERROR: 입력값 검증 실패
     * - BIND_ERROR: 요청 데이터 바인딩 실패
     */
    private final String error;
    
    /**
     * 에러 메시지 (상세 설명)
     * 디버깅 및 로깅 목적, 사용자에게 직접 노출 시 주의
     */
    private final String message;
    
    // ========== 팩토리 메서드 (Factory Methods) ==========
    // 팩토리 메서드란?
    // 객체 생성을 캡슐화하는 정적 메서드입니다.
    // new 키워드 대신 의미 있는 이름의 메서드로 객체를 생성하여 가독성을 높입니다.
    // 예: ErrorResponse.of(errorCode, message) → "이 에러코드와 메시지로 응답 만들어줘"
    
    /**
     * ErrorCode enum으로부터 ErrorResponse 생성
     * 
     * 사용 예:
     * ErrorResponse.of(ErrorCode.TRADE_INSUFFICIENT_BALANCE, "잔고가 부족합니다.")
     * 
     * @param errorCode 에러 코드 enum (status, code, 기본 message 포함)
     * @param message 커스텀 에러 메시지 (null이면 errorCode의 기본 메시지 사용)
     * @return ErrorResponse 인스턴스
     */
    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(errorCode.getHttpStatus().value())
                .error(errorCode.getCode())
                .message(message != null ? message : errorCode.getMessage())
                .build();
    }
    
    /**
     * ErrorCode enum으로부터 ErrorResponse 생성 (기본 메시지 사용)
     * 
     * 사용 예:
     * ErrorResponse.of(ErrorCode.AUTH_EXPIRED_TOKEN)
     * 
     * @param errorCode 에러 코드 enum
     * @return ErrorResponse 인스턴스
     */
    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage());
    }
    
    /**
     * BusinessException으로부터 ErrorResponse 생성
     * 
     * BusinessException은 모든 커스텀 예외의 부모 클래스이므로,
     * AuthException, TradeException, GameException, UserException 모두 이 메서드로 처리 가능
     * 
     * @param exception 비즈니스 예외 (errorCode와 message 포함)
     * @return ErrorResponse 인스턴스
     */
    public static ErrorResponse of(BusinessException exception) {
        return of(exception.getErrorCode(), exception.getMessage());
    }
    
    /**
     * HttpStatus와 커스텀 정보로 ErrorResponse 생성
     * 
     * Validation, Bind 등 ErrorCode enum에 정의되지 않은 예외 처리용
     * 
     * 사용 예:
     * ErrorResponse.of(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "nickname: 필수 입력값입니다.")
     * 
     * @param httpStatus HTTP 상태
     * @param errorCode 커스텀 에러 코드 문자열
     * @param message 에러 메시지
     * @return ErrorResponse 인스턴스
     */
    public static ErrorResponse of(HttpStatus httpStatus, String errorCode, String message) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(httpStatus.value())
                .error(errorCode)
                .message(message)
                .build();
    }
}
