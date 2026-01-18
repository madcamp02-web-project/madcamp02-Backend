-- ============================================
-- MadCamp02 Backend Database Migration
-- Version: 2
-- Description:
--   - Add users.is_public / users.is_ranking_joined
--   - Default TRUE, NOT NULL
-- ============================================

-- 1) is_public
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_public BOOLEAN;

ALTER TABLE users
    ALTER COLUMN is_public SET DEFAULT TRUE;

UPDATE users
SET is_public = TRUE
WHERE is_public IS NULL;

ALTER TABLE users
    ALTER COLUMN is_public SET NOT NULL;

-- 2) is_ranking_joined
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_ranking_joined BOOLEAN;

ALTER TABLE users
    ALTER COLUMN is_ranking_joined SET DEFAULT TRUE;

UPDATE users
SET is_ranking_joined = TRUE
WHERE is_ranking_joined IS NULL;

ALTER TABLE users
    ALTER COLUMN is_ranking_joined SET NOT NULL;
