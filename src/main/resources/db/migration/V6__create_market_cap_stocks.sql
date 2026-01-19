-- V6__create_market_cap_stocks.sql
-- Phase 3.5 데이터 전략: Market Movers Top 20 Market Cap DB 관리

-- Market Movers를 위한 Top 20 Market Cap 종목 관리 테이블
CREATE TABLE market_cap_stocks (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL UNIQUE,
    company_name VARCHAR(200),
    market_cap_rank INTEGER NOT NULL, -- 1~20 순위
    is_active BOOLEAN DEFAULT TRUE,    -- 활성화 여부
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_market_cap_stocks_rank ON market_cap_stocks(market_cap_rank);
CREATE INDEX idx_market_cap_stocks_active ON market_cap_stocks(is_active) WHERE is_active = TRUE;

COMMENT ON TABLE market_cap_stocks IS 'Market Movers를 위한 Top 20 Market Cap 종목 관리 테이블';

-- Top 20 Market Cap 종목 초기 데이터 (2026-01-19 기준)
-- 참고: 실제 시가총액 순위는 변동될 수 있으므로, 관리자 API로 업데이트 가능하도록 설계
INSERT INTO market_cap_stocks (symbol, company_name, market_cap_rank, is_active) VALUES
('AAPL', 'Apple Inc.', 1, TRUE),
('MSFT', 'Microsoft Corporation', 2, TRUE),
('GOOGL', 'Alphabet Inc.', 3, TRUE),
('AMZN', 'Amazon.com Inc.', 4, TRUE),
('NVDA', 'NVIDIA Corporation', 5, TRUE),
('META', 'Meta Platforms Inc.', 6, TRUE),
('TSLA', 'Tesla, Inc.', 7, TRUE),
('BRK.B', 'Berkshire Hathaway Inc.', 8, TRUE),
('V', 'Visa Inc.', 9, TRUE),
('UNH', 'UnitedHealth Group Inc.', 10, TRUE),
('JNJ', 'Johnson & Johnson', 11, TRUE),
('WMT', 'Walmart Inc.', 12, TRUE),
('JPM', 'JPMorgan Chase & Co.', 13, TRUE),
('MA', 'Mastercard Incorporated', 14, TRUE),
('PG', 'The Procter & Gamble Company', 15, TRUE),
('HD', 'The Home Depot, Inc.', 16, TRUE),
('DIS', 'The Walt Disney Company', 17, TRUE),
('AVGO', 'Broadcom Inc.', 18, TRUE),
('PEP', 'PepsiCo, Inc.', 19, TRUE),
('COST', 'Costco Wholesale Corporation', 20, TRUE);
