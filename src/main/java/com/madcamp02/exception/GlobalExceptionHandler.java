package com.madcamp02.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리기
 * 모든 컨트롤러에서 발생하는 예외를 한 곳에서 처리
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", e.getErrorCode().getHttpStatus().value());
        response.put("error", e.getErrorCode().getCode());
        response.put("message", e.getMessage());

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(response);
    }

    /**
     * 인증 예외 처리
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuthException(AuthException e) {
        log.warn("AuthException: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", e.getErrorCode().getHttpStatus().value());
        response.put("error", e.getErrorCode().getCode());
        response.put("message", e.getMessage());

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(response);
    }

    /**
     * 거래 예외 처리
     */
    @ExceptionHandler(TradeException.class)
    public ResponseEntity<Map<String, Object>> handleTradeException(TradeException e) {
        log.warn("TradeException: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", e.getErrorCode().getHttpStatus().value());
        response.put("error", e.getErrorCode().getCode());
        response.put("message", e.getMessage());

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(response);
    }

    /**
     * 게임 예외 처리
     */
    @ExceptionHandler(GameException.class)
    public ResponseEntity<Map<String, Object>> handleGameException(GameException e) {
        log.warn("GameException: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", e.getErrorCode().getHttpStatus().value());
        response.put("error", e.getErrorCode().getCode());
        response.put("message", e.getMessage());

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(response);
    }

    /**
     * 사용자 예외 처리
     */
    @ExceptionHandler(UserException.class)
    public ResponseEntity<Map<String, Object>> handleUserException(UserException e) {
        log.warn("UserException: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", e.getErrorCode().getHttpStatus().value());
        response.put("error", e.getErrorCode().getCode());
        response.put("message", e.getMessage());

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(response);
    }

    /**
     * Validation 예외 처리 (@Valid 실패)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("ValidationException: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "VALIDATION_ERROR");
        
        // 첫 번째 에러 메시지 추출
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");
        response.put("message", message);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Bind 예외 처리
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(BindException e) {
        log.warn("BindException: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "BIND_ERROR");
        response.put("message", "요청 데이터 바인딩에 실패했습니다.");

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("UnhandledException: ", e);

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "SERVER_001");
        response.put("message", "내부 서버 오류가 발생했습니다.");

        return ResponseEntity.internalServerError().body(response);
    }
}
