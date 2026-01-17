package com.madcamp02.exception;

/**
 * 게임 관련 예외
 * 가챠, 인벤토리, 랭킹 관련 실패 시 발생
 */
public class GameException extends BusinessException {

    public GameException(ErrorCode errorCode) {
        super(errorCode);
    }

    public GameException(String message) {
        super(ErrorCode.GAME_INSUFFICIENT_COIN, message);
    }

    public GameException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
