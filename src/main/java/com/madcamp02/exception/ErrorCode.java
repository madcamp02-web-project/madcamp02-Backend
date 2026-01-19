package com.madcamp02.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 * 모든 비즈니스 예외에서 사용하는 에러 코드 enum
 * 
 * 명세서 기준 에러 코드:
 * - AUTH_001~008: 인증 관련 (001~005 기본, 006 카카오, 007 비밀번호, 008 이메일중복)
 * - TRADE_001~004: 거래 관련
 * - GAME_001~003: 게임 관련
 * - USER_001~002: 사용자 관련
 * - SERVER_001~002: 서버 관련
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ========== 인증 관련 (AUTH) ==========
    // AUTH_001: 토큰 만료 (명세서 기준)
    AUTH_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "토큰이 만료되었습니다."),
    // AUTH_002: 유효하지 않은 토큰
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "유효하지 않은 토큰입니다."),
    // AUTH_003: 권한 없음
    AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_003", "접근 권한이 없습니다."),
    // AUTH_004: 사용자 없음 (추가)
    AUTH_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_004", "사용자를 찾을 수 없습니다."),
    // AUTH_005: Google 토큰 검증 실패 (추가)
    AUTH_GOOGLE_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_005", "Google 토큰 검증에 실패했습니다."),
    // AUTH_006: Kakao 토큰 검증 실패 (카카오 OAuth 추가)
    AUTH_KAKAO_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_006", "Kakao 토큰 검증에 실패했습니다."),
    // AUTH_007: 비밀번호 불일치 (일반 로그인 추가)
    AUTH_PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH_007", "비밀번호가 일치하지 않습니다."),
    // AUTH_008: 이메일 중복 (회원가입 시 이미 존재하는 이메일)
    AUTH_EMAIL_DUPLICATION(HttpStatus.CONFLICT, "AUTH_008", "이미 가입된 이메일입니다."),

    // ========== 거래 관련 (TRADE) ==========
    // TRADE_001: 잔고 부족
    TRADE_INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "TRADE_001", "잔고가 부족합니다."),
    // TRADE_002: 보유 수량 부족
    TRADE_INSUFFICIENT_QUANTITY(HttpStatus.BAD_REQUEST, "TRADE_002", "보유 수량이 부족합니다."),
    // TRADE_003: 거래 시간 외 (명세서 순서 기준)
    TRADE_MARKET_CLOSED(HttpStatus.BAD_REQUEST, "TRADE_003", "거래 시간이 아닙니다."),
    // TRADE_004: 유효하지 않은 종목
    TRADE_INVALID_TICKER(HttpStatus.BAD_REQUEST, "TRADE_004", "유효하지 않은 종목입니다."),

    // ========== 게임 관련 (GAME) ==========
    // GAME_001: 코인 부족
    GAME_INSUFFICIENT_COIN(HttpStatus.BAD_REQUEST, "GAME_001", "게임 코인이 부족합니다."),
    // GAME_002: 이미 보유한 아이템
    GAME_ITEM_ALREADY_OWNED(HttpStatus.CONFLICT, "GAME_002", "이미 보유한 아이템입니다."),
    // GAME_003: 아이템 없음 (추가)
    GAME_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "GAME_003", "아이템을 찾을 수 없습니다."),

    // ========== 사용자 관련 (USER) ==========
    // USER_001: 사용자 없음
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    // USER_002: 이미 존재하는 사용자 (추가)
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_002", "이미 존재하는 사용자입니다."),

    // ========== 서버 에러 (SERVER) ==========
    // SERVER_001: 내부 서버 오류
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_001", "내부 서버 오류가 발생했습니다."),
    // SERVER_002: 외부 API 호출 실패 (추가)
    EXTERNAL_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "SERVER_002", "외부 API 호출에 실패했습니다."),
    // SERVER_003: Quota 초과 (Phase 3.5)
    QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "QUOTA_EXCEEDED", "일일 외부 데이터 요청 허용량을 초과했습니다. (매일 00:00 초기화)");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
