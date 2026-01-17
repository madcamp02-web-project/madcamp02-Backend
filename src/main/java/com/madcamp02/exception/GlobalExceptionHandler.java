package com.madcamp02.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기 (Global Exception Handler)
 * 
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                     예외 처리 흐름                               │
 * │                                                                 │
 * │  [Controller] ─── 예외 발생 ───► [GlobalExceptionHandler]       │
 * │                                         │                       │
 * │                                         ▼                       │
 * │                                  [ErrorResponse 생성]           │
 * │                                         │                       │
 * │                                         ▼                       │
 * │                                  [클라이언트 응답]               │
 * │                                                                 │
 * │  모든 컨트롤러에서 발생하는 예외를 한 곳에서 일관되게 처리합니다.     │
 * │  try-catch를 컨트롤러마다 작성할 필요 없이, 여기서 모두 잡습니다.    │
 * └─────────────────────────────────────────────────────────────────┘
 * 
 * @RestControllerAdvice란?
 * - @ControllerAdvice + @ResponseBody 조합
 * - 모든 컨트롤러에 공통으로 적용되는 예외 처리 로직을 정의
 * - 반환값을 자동으로 JSON으로 변환해줌 (@ResponseBody 덕분)
 * 
 * 예외 처리 우선순위:
 * 1. 가장 구체적인 예외 클래스 (AuthException, TradeException 등)
 * 2. 부모 클래스 (BusinessException)
 * 3. 가장 일반적인 예외 (Exception)
 * 
 * 스프링은 자동으로 가장 구체적인 핸들러를 먼저 매칭합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ========== 비즈니스 예외 처리 (커스텀 예외) ==========
    
    /**
     * 비즈니스 예외 처리 (최상위 커스텀 예외)
     * 
     * BusinessException을 상속받는 모든 예외의 기본 핸들러입니다.
     * 더 구체적인 핸들러(AuthException 등)가 없을 때 이 핸들러가 동작합니다.
     * 
     * @param e 비즈니스 예외
     * @return ErrorResponse (JSON 형태로 클라이언트에 전달)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        
        // ErrorResponse.of() 팩토리 메서드로 응답 생성
        // 기존 Map<String, Object> 방식보다 타입 안전하고 간결함
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ErrorResponse.of(e));
    }

    /**
     * 인증 예외 처리
     * 
     * 발생 상황:
     * - 토큰 만료 (AUTH_001)
     * - 유효하지 않은 토큰 (AUTH_002)
     * - 접근 권한 없음 (AUTH_003)
     * - 사용자 없음 (AUTH_004)
     * - Google 토큰 검증 실패 (AUTH_005)
     * 
     * 프론트엔드 처리 가이드:
     * - AUTH_001, AUTH_002: 로그인 페이지로 리다이렉트
     * - AUTH_003: 권한 없음 메시지 표시
     * 
     * @param e 인증 예외
     * @return ErrorResponse
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException e) {
        log.warn("AuthException: {}", e.getMessage());
        
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ErrorResponse.of(e));
    }

    /**
     * 거래 예외 처리
     * 
     * 발생 상황:
     * - 잔고 부족 (TRADE_001)
     * - 보유 수량 부족 (TRADE_002)
     * - 거래 시간 외 (TRADE_003)
     * - 유효하지 않은 종목 (TRADE_004)
     * 
     * 프론트엔드 처리 가이드:
     * - TRADE_001: "잔고가 부족합니다" 토스트 메시지
     * - TRADE_002: "보유 수량이 부족합니다" 토스트 메시지
     * - TRADE_003: 거래 불가 안내 모달
     * 
     * @param e 거래 예외
     * @return ErrorResponse
     */
    @ExceptionHandler(TradeException.class)
    public ResponseEntity<ErrorResponse> handleTradeException(TradeException e) {
        log.warn("TradeException: {}", e.getMessage());
        
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ErrorResponse.of(e));
    }

    /**
     * 게임 예외 처리
     * 
     * 발생 상황:
     * - 코인 부족 (GAME_001)
     * - 이미 보유한 아이템 (GAME_002)
     * - 아이템 없음 (GAME_003)
     * 
     * 프론트엔드 처리 가이드:
     * - GAME_001: "코인이 부족합니다" + 코인 충전 유도
     * - GAME_002: 중복 아이템 안내 (이미 보유 중)
     * 
     * @param e 게임 예외
     * @return ErrorResponse
     */
    @ExceptionHandler(GameException.class)
    public ResponseEntity<ErrorResponse> handleGameException(GameException e) {
        log.warn("GameException: {}", e.getMessage());
        
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ErrorResponse.of(e));
    }

    /**
     * 사용자 예외 처리
     * 
     * 발생 상황:
     * - 사용자 없음 (USER_001)
     * - 이미 존재하는 사용자 (USER_002)
     * 
     * 프론트엔드 처리 가이드:
     * - USER_001: 404 페이지 또는 "사용자를 찾을 수 없습니다" 표시
     * - USER_002: "이미 가입된 계정입니다" 안내
     * 
     * @param e 사용자 예외
     * @return ErrorResponse
     */
    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorResponse> handleUserException(UserException e) {
        log.warn("UserException: {}", e.getMessage());
        
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ErrorResponse.of(e));
    }

    // ========== 스프링 프레임워크 예외 처리 ==========
    
    /**
     * Validation 예외 처리 (@Valid 검증 실패)
     * 
     * 발생 상황:
     * - @Valid 어노테이션이 붙은 DTO의 필드 검증 실패
     * - 예: @NotBlank, @Size, @Email 등 조건 미충족
     * 
     * 예시:
     * - nickname이 빈 문자열일 때: "nickname: 필수 입력값입니다."
     * - email 형식이 틀렸을 때: "email: 올바른 이메일 형식이 아닙니다."
     * 
     * @param e 검증 예외
     * @return ErrorResponse (첫 번째 에러 메시지만 반환)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("ValidationException: {}", e.getMessage());

        // 첫 번째 에러 메시지 추출
        // 여러 필드가 동시에 실패했을 경우, 첫 번째 에러만 반환 (UX 고려)
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message));
    }

    /**
     * Bind 예외 처리 (요청 데이터 바인딩 실패)
     * 
     * 발생 상황:
     * - 쿼리 파라미터나 폼 데이터를 객체에 바인딩할 때 실패
     * - 예: 숫자 필드에 문자열이 들어왔을 때
     * 
     * @param e 바인드 예외
     * @return ErrorResponse
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        log.warn("BindException: {}", e.getMessage());

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "BIND_ERROR", "요청 데이터 바인딩에 실패했습니다."));
    }

    // ========== 기타 모든 예외 처리 (Fallback) ==========
    
    /**
     * 기타 모든 예외 처리 (최후의 보루)
     * 
     * 위의 모든 핸들러에서 잡지 못한 예외가 여기로 옵니다.
     * 예상치 못한 예외이므로, 사용자에게는 일반적인 에러 메시지를 보여주고
     * 서버 로그에는 상세 스택 트레이스를 기록합니다.
     * 
     * 주의:
     * - 이 핸들러가 자주 호출된다면, 명시적인 예외 처리가 누락된 것
     * - 로그를 확인하여 적절한 커스텀 예외로 전환 필요
     * 
     * @param e 모든 예외
     * @return ErrorResponse (SERVER_001)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        // error 레벨 로깅 + 스택 트레이스 전체 기록 (디버깅용)
        log.error("UnhandledException: ", e);

        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR, 
                        "SERVER_001", 
                        "내부 서버 오류가 발생했습니다."
                ));
    }
}
