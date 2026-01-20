-- ============================================
-- MadCamp02 Backend Database Migration
-- Version: 9
-- Description:
--   - Create exchange_rates table for FX rates
--   - Source: Korea Eximbank (AP01 - 현재 환율)
-- ============================================

CREATE TABLE IF NOT EXISTS exchange_rates (
    id           BIGSERIAL PRIMARY KEY,
    as_of_date   DATE        NOT NULL,
    cur_unit     VARCHAR(20) NOT NULL,
    cur_nm       VARCHAR(100),
    deal_bas_r   NUMERIC(18, 6),
    ttb          NUMERIC(18, 6),
    tts          NUMERIC(18, 6),
    bkpr         NUMERIC(18, 6),
    kftc_deal_bas_r NUMERIC(18, 6),
    kftc_bkpr   NUMERIC(18, 6),
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- (as_of_date, cur_unit) 조합은 한 번만 존재해야 함
CREATE UNIQUE INDEX IF NOT EXISTS ux_exchange_rates_asof_curunit
    ON exchange_rates (as_of_date, cur_unit);

