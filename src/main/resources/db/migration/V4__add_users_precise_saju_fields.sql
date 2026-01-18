-- ============================================
-- MadCamp02 Backend Database Migration
-- Version: 4
-- Description:
--   - Add users.birth_time, users.gender, users.calendar_type
--   - 정밀 사주 계산을 위한 필드 추가
--   - 성별/양력음력/시간까지 포함한 사주 계산
-- ============================================

-- 1) birth_time (생년월일시)
-- TIME 타입으로 저장 (예: 13:05:00)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS birth_time TIME;

-- 기존 데이터는 00:00:00으로 기본값 설정
UPDATE users
SET birth_time = '00:00:00'
WHERE birth_time IS NULL;

-- 2) gender (성별)
-- MALE | FEMALE | OTHER (향후 확장 가능)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS gender VARCHAR(10);

-- 3) calendar_type (양력/음력 구분)
-- SOLAR (양력) | LUNAR (음력) | LUNAR_LEAP (음력윤달)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS calendar_type VARCHAR(20);

-- 기본값: 기존 데이터는 양력(SOLAR)으로 설정
UPDATE users
SET calendar_type = 'SOLAR'
WHERE calendar_type IS NULL;
