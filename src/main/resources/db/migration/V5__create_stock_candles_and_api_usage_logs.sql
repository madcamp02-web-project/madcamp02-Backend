-- V5__create_stock_candles_and_api_usage_logs.sql
-- Phase 3.5 데이터 전략: EODHD + DB Caching 및 Quota Management

-- 1. stock_candles 테이블: Historical OHLCV 데이터 저장
-- EODHD API 응답을 저장하여 API 호출을 최소화
CREATE TABLE stock_candles (
    symbol VARCHAR(20) NOT NULL,
    date DATE NOT NULL,
    open DECIMAL(19, 4),
    high DECIMAL(19, 4),
    low DECIMAL(19, 4),
    close DECIMAL(19, 4),
    volume BIGINT,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_stock_candles PRIMARY KEY (symbol, date)
);

CREATE INDEX idx_stock_candles_symbol_date ON stock_candles(symbol, date DESC);

-- 2. api_usage_logs 테이블: API 호출 횟수 추적
-- EODHD의 엄격한 일일 제한(20회)을 관리하기 위함
CREATE TABLE api_usage_logs (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(50) NOT NULL, -- 예: 'EODHD'
    call_date DATE NOT NULL,
    call_count INT DEFAULT 0,
    CONSTRAINT uq_api_usage_logs_provider_date UNIQUE (provider, call_date)
);

COMMENT ON TABLE stock_candles IS '주식 캔들(OHLCV) 데이터 캐싱 테이블';
COMMENT ON TABLE api_usage_logs IS '외부 API 일일 호출 사용량 로그';
