-- V8__add_period_to_stock_candles.sql
-- EODHD 차트 데이터 배치 최적화: period 컬럼 추가 및 PK 변경
-- resolution(d/w/m)별로 데이터를 구분하여 저장하여 API 호출 최소화

-- 1. 기존 데이터에 period='d' 추가 (임시 컬럼)
ALTER TABLE stock_candles ADD COLUMN period_temp VARCHAR(1);

-- 2. 기존 데이터 모두 'd'로 설정
UPDATE stock_candles SET period_temp = 'd';

-- 3. period_temp를 NOT NULL로 변경
ALTER TABLE stock_candles ALTER COLUMN period_temp SET NOT NULL;

-- 4. 기존 PK 제약조건 제거
ALTER TABLE stock_candles DROP CONSTRAINT pk_stock_candles;

-- 5. 기존 인덱스 제거
DROP INDEX IF EXISTS idx_stock_candles_symbol_date;

-- 6. period_temp를 period로 이름 변경
ALTER TABLE stock_candles RENAME COLUMN period_temp TO period;

-- 7. 새로운 복합 PK 추가 (symbol, date, period)
ALTER TABLE stock_candles ADD CONSTRAINT pk_stock_candles PRIMARY KEY (symbol, date, period);

-- 8. 새로운 인덱스 추가 (조회 성능 향상)
CREATE INDEX idx_stock_candles_symbol_period_date ON stock_candles(symbol, period, date DESC);

COMMENT ON COLUMN stock_candles.period IS '시간 간격: d (daily), w (weekly), m (monthly)';
