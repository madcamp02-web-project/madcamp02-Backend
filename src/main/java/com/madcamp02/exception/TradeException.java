package com.madcamp02.exception;

/**
 * 거래 관련 예외
 * 매수/매도 실패 시 발생
 */
public class TradeException extends BusinessException {

    public TradeException(ErrorCode errorCode) {
        super(errorCode);
    }

    public TradeException(String message) {
        super(ErrorCode.TRADE_INSUFFICIENT_BALANCE, message);
    }

    public TradeException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
