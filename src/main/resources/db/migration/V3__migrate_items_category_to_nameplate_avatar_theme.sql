-- ============================================
-- MadCamp02 Backend Database Migration
-- Version: 3
-- Description:
--   - Migrate items.category legacy values to target taxonomy
--   - Fail fast if unknown values remain
--   - (Optional hardening) Add CHECK constraint to prevent regressions
--
-- Target taxonomy: NAMEPLATE | AVATAR | THEME
-- Legacy taxonomy: COSTUME | ACCESSORY | AURA | BACKGROUND
-- Mapping (docs v2.7.3):
--   - COSTUME     -> AVATAR
--   - ACCESSORY   -> AVATAR
--   - AURA        -> AVATAR
--   - BACKGROUND  -> THEME
-- ============================================

-- 1) Legacy -> target mapping
UPDATE items
SET category = CASE category
    WHEN 'COSTUME' THEN 'AVATAR'
    WHEN 'ACCESSORY' THEN 'AVATAR'
    WHEN 'AURA' THEN 'AVATAR'
    WHEN 'BACKGROUND' THEN 'THEME'
    ELSE category
END
WHERE category IN ('COSTUME', 'ACCESSORY', 'AURA', 'BACKGROUND');

-- 2) Fail fast on unknown values (including unexpected legacy/new strings)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM items
        WHERE category NOT IN ('NAMEPLATE', 'AVATAR', 'THEME')
    ) THEN
        RAISE EXCEPTION 'V3 migration failed: items.category has unknown values';
    END IF;
END $$;

-- 3) Prevent regressions (hard constraint)
ALTER TABLE items
    ADD CONSTRAINT chk_items_category_allowed
    CHECK (category IN ('NAMEPLATE', 'AVATAR', 'THEME'));


