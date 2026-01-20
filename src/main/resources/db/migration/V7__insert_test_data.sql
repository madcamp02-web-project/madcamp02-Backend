-- ============================================
-- MadCamp02 Backend Test Data Seed
-- Version: 7
-- Description:
--   - 통합 시나리오 테스트용 더미 데이터 삽입
--   - Users / Wallet / Portfolio / Trade Logs / Items / Inventory
--   - Watchlist / Stock Candles / Chat History 등
-- ============================================

-- 주의사항:
-- - 본 데이터는 개발/테스트 데이터베이스용입니다.
-- - 운영 환경에서는 적용하지 않는 것을 권장합니다.

-- --------------------------------------------
-- 1. 아이템 마스터 (items)
-- --------------------------------------------
INSERT INTO items (name, description, category, rarity, probability, image_url)
VALUES
    ('Basic Nameplate', '기본 이름표 테두리', 'NAMEPLATE', 'COMMON', 0.5, 'https://example.com/items/nameplate_basic.png'),
    ('Silver Nameplate', '실버 등급 이름표', 'NAMEPLATE', 'RARE', 0.3, 'https://example.com/items/nameplate_silver.png'),
    ('Golden Nameplate', '골드 등급 이름표', 'NAMEPLATE', 'EPIC', 0.15, 'https://example.com/items/nameplate_gold.png'),
    ('Default Avatar', '기본 아바타 외형', 'AVATAR', 'COMMON', 0.4, 'https://example.com/items/avatar_default.png'),
    ('Trader Avatar', '트레이더 스타일 아바타', 'AVATAR', 'RARE', 0.25, 'https://example.com/items/avatar_trader.png'),
    ('Wizard Avatar', '도사 스타일 아바타', 'AVATAR', 'EPIC', 0.1, 'https://example.com/items/avatar_wizard.png'),
    ('Dark Theme', '다크 테마', 'THEME', 'RARE', 0.15, 'https://example.com/items/theme_dark.png'),
    ('Light Theme', '라이트 테마', 'THEME', 'COMMON', 0.35, 'https://example.com/items/theme_light.png');

-- --------------------------------------------
-- 2. 사용자 (users) - 10명 샘플
-- --------------------------------------------
INSERT INTO users (email, password, nickname, provider, birth_date, saju_element, zodiac_sign, avatar_url,
                   is_public, is_ranking_joined, birth_time, gender, calendar_type)
VALUES
    ('local_user1@example.com',  '$2a$10$abcdefghijklmnopqrstuv', 'LocalUser1',  'LOCAL',
     '1995-01-10', 'FIRE',  '쥐', 'https://example.com/avatars/user1.png',  TRUE, TRUE, '08:30:00', 'MALE',   'SOLAR'),
    ('local_user2@example.com',  '$2a$10$abcdefghijklmnopqrstuv', 'LocalUser2',  'LOCAL',
     '1993-03-21', 'WATER', '소',  'https://example.com/avatars/user2.png',  TRUE, TRUE, '14:15:00', 'FEMALE', 'SOLAR'),
    ('google_user1@example.com', NULL,                               'GUser1',      'GOOGLE',
     '1990-07-15', 'WOOD',  '호랑이', 'https://example.com/avatars/user3.png',  TRUE, TRUE, '12:00:00', 'MALE',   'SOLAR'),
    ('google_user2@example.com', NULL,                               'GUser2',      'GOOGLE',
     '1988-11-02', 'GOLD',  '토끼',   'https://example.com/avatars/user4.png',  TRUE, TRUE, '09:45:00', 'FEMALE', 'SOLAR'),
    ('kakao_user1@example.com',  NULL,                               'KUser1',      'KAKAO',
     '1999-05-30', 'EARTH', '용',    'https://example.com/avatars/user5.png',  TRUE, TRUE, '18:00:00', 'MALE',   'SOLAR'),
    ('kakao_user2@example.com',  NULL,                               'KUser2',      'KAKAO',
     '2001-09-09', 'FIRE',  '말',    'https://example.com/avatars/user6.png',  TRUE, TRUE, '07:20:00', 'FEMALE', 'SOLAR'),
    ('local_user3@example.com',  '$2a$10$abcdefghijklmnopqrstuv', 'LocalUser3',  'LOCAL',
     NULL,          NULL,   NULL,    NULL,                                   TRUE, TRUE, '00:00:00', NULL,    NULL),
    ('local_user4@example.com',  '$2a$10$abcdefghijklmnopqrstuv', 'LocalUser4',  'LOCAL',
     NULL,          NULL,   NULL,    NULL,                                   TRUE, FALSE,'00:00:00', NULL,    NULL),
    ('local_user5@example.com',  '$2a$10$abcdefghijklmnopqrstuv', 'LocalUser5',  'LOCAL',
     NULL,          NULL,   NULL,    NULL,                                   FALSE,TRUE,'00:00:00', NULL,    NULL),
    ('demo_user@example.com',    '$2a$10$abcdefghijklmnopqrstuv', 'DemoUser',    'LOCAL',
     '1997-12-25', 'WATER', '돼지',  'https://example.com/avatars/demo.png', TRUE, TRUE, '10:10:00', 'OTHER',  'SOLAR'),
    -- 추가 더미 사용자 (간단 프로필)
    ('extra_user1@example.com',  '$2a$10$abcdefghijklmnopqrstuv', 'ExtraUser1',  'LOCAL',
     NULL,          NULL,   NULL,    NULL,                                   TRUE, TRUE, '00:00:00', NULL,    NULL),
    ('extra_user2@example.com',  '$2a$10$abcdefghijklmnopqrstuv', 'ExtraUser2',  'LOCAL',
     NULL,          NULL,   NULL,    NULL,                                   TRUE, TRUE, '00:00:00', NULL,    NULL),
    ('extra_user3@example.com',  '$2a$10$abcdefghijklmnopqrstuv', 'ExtraUser3',  'LOCAL',
     NULL,          NULL,   NULL,    NULL,                                   TRUE, TRUE, '00:00:00', NULL,    NULL),
    ('extra_user4@example.com',  '$2a$10$abcdefghijklmnopqrstuv', 'ExtraUser4',  'LOCAL',
     NULL,          NULL,   NULL,    NULL,                                   TRUE, TRUE, '00:00:00', NULL,    NULL),
    ('extra_user5@example.com',  '$2a$10$abcdefghijklmnopqrstuv', 'ExtraUser5',  'LOCAL',
     NULL,          NULL,   NULL,    NULL,                                   TRUE, TRUE, '00:00:00', NULL,    NULL),
    ('extra_user6@example.com',  '$2a$10$abcdefghijklmnopqrstuv', 'ExtraUser6',  'LOCAL',
     NULL,          NULL,   NULL,    NULL,                                   TRUE, TRUE, '00:00:00', NULL,    NULL),
    ('extra_user7@example.com',  '$2a$10$abcdefghijklmnopqrstuv', 'ExtraUser7',  'LOCAL',
     NULL,          NULL,   NULL,    NULL,                                   TRUE, TRUE, '00:00:00', NULL,    NULL),
    ('extra_user8@example.com',  '$2a$10$abcdefghijklmnopqrstuv', 'ExtraUser8',  'LOCAL',
     NULL,          NULL,   NULL,    NULL,                                   TRUE, TRUE, '00:00:00', NULL,    NULL),
    ('extra_user9@example.com',  '$2a$10$abcdefghijklmnopqrstuv', 'ExtraUser9',  'LOCAL',
     NULL,          NULL,   NULL,    NULL,                                   TRUE, TRUE, '00:00:00', NULL,    NULL),
    ('extra_user10@example.com', '$2a$10$abcdefghijklmnopqrstuv', 'ExtraUser10', 'LOCAL',
     NULL,          NULL,   NULL,    NULL,                                   TRUE, TRUE, '00:00:00', NULL,    NULL)
ON CONFLICT (email) DO NOTHING;

-- --------------------------------------------
-- 3. 지갑 (wallet) - 각 사용자 1개씩
-- --------------------------------------------
INSERT INTO wallet (user_id, cash_balance, realized_profit, total_assets, game_coin)
SELECT user_id,
       CASE email
           WHEN 'local_user1@example.com'  THEN 15000.0000
           WHEN 'local_user2@example.com'  THEN  8000.0000
           WHEN 'google_user1@example.com' THEN 12000.0000
           WHEN 'google_user2@example.com' THEN  9500.0000
           WHEN 'kakao_user1@example.com'  THEN 11000.0000
           WHEN 'kakao_user2@example.com'  THEN 13000.0000
           WHEN 'local_user3@example.com'  THEN 10000.0000
           WHEN 'local_user4@example.com'  THEN 10000.0000
           WHEN 'local_user5@example.com'  THEN  9000.0000
           WHEN 'demo_user@example.com'    THEN 20000.0000
           ELSE 10000.0000
       END AS cash_balance,
       0.0000 AS realized_profit,
       CASE email
           WHEN 'demo_user@example.com' THEN 22000.0000
           ELSE 10000.0000
       END AS total_assets,
       CASE email
           WHEN 'demo_user@example.com' THEN 500
           ELSE 100
       END AS game_coin
FROM users
WHERE email IN (
    'local_user1@example.com',
    'local_user2@example.com',
    'google_user1@example.com',
    'google_user2@example.com',
    'kakao_user1@example.com',
    'kakao_user2@example.com',
    'local_user3@example.com',
    'local_user4@example.com',
    'local_user5@example.com',
    'demo_user@example.com',
    'extra_user1@example.com',
    'extra_user2@example.com',
    'extra_user3@example.com',
    'extra_user4@example.com',
    'extra_user5@example.com',
    'extra_user6@example.com',
    'extra_user7@example.com',
    'extra_user8@example.com',
    'extra_user9@example.com',
    'extra_user10@example.com'
)
ON CONFLICT (user_id) DO NOTHING;

-- --------------------------------------------
-- 4. 포트폴리오 (portfolio) - 일부 사용자 보유 종목
-- --------------------------------------------
INSERT INTO portfolio (user_id, ticker, quantity, avg_price)
SELECT u.user_id, v.ticker, v.quantity, v.avg_price
FROM users u
JOIN (
    VALUES
        ('local_user1@example.com',  'AAPL', 10, 180.00),
        ('local_user1@example.com',  'MSFT',  5, 320.00),
        ('google_user1@example.com', 'GOOGL', 4, 140.00),
        ('google_user1@example.com', 'NVDA',  3, 900.00),
        ('kakao_user1@example.com',  'TSLA',  6, 220.00),
        ('demo_user@example.com',    'AAPL', 15, 175.00),
        ('demo_user@example.com',    'MSFT', 10, 315.00),
        ('demo_user@example.com',    'GOOGL', 8, 135.00),
        ('demo_user@example.com',    'NVDA',  5, 880.00)
) AS v(email, ticker, quantity, avg_price)
    ON u.email = v.email
ON CONFLICT (user_id, ticker) DO NOTHING;

-- --------------------------------------------
-- 5. 거래 로그 (trade_logs) - 기본 시나리오
-- --------------------------------------------
INSERT INTO trade_logs (user_id, ticker, trade_type, price, quantity, total_amount, fee, realized_pnl, trade_date)
SELECT u.user_id, v.ticker, v.trade_type, v.price, v.quantity,
       v.price * v.quantity AS total_amount,
       0.0000 AS fee,
       v.realized_pnl,
       v.trade_date::timestamp
FROM users u
JOIN (
    VALUES
        ('local_user1@example.com',  'AAPL',  'BUY',  180.00, 10, NULL,              '2026-01-10 10:00:00'),
        ('local_user1@example.com',  'MSFT',  'BUY',  320.00,  5, NULL,              '2026-01-11 11:00:00'),
        ('google_user1@example.com', 'GOOGL', 'BUY',  140.00,  4, NULL,              '2026-01-12 09:30:00'),
        ('kakao_user1@example.com',  'TSLA',  'BUY',  220.00,  6, NULL,              '2026-01-13 14:15:00'),
        ('demo_user@example.com',    'AAPL',  'BUY',  175.00, 15, NULL,              '2026-01-10 10:30:00'),
        ('demo_user@example.com',    'AAPL',  'SELL', 195.00,  5, 100.00,            '2026-01-15 15:00:00'),
        ('demo_user@example.com',    'NVDA',  'BUY',  880.00,  5, NULL,              '2026-01-16 16:00:00')
) AS v(email, ticker, trade_type, price, quantity, realized_pnl, trade_date)
    ON u.email = v.email;

-- --------------------------------------------
-- 6. 인벤토리 (inventory) - 일부 장착 상태
-- --------------------------------------------
-- 간단히: 첫 번째 4개 아이템을 demo_user와 local_user1에게 부여
INSERT INTO inventory (user_id, item_id, is_equipped)
SELECT u.user_id, i.item_id,
       CASE
           WHEN i.name IN ('Golden Nameplate', 'Wizard Avatar') THEN TRUE
           ELSE FALSE
       END AS is_equipped
FROM users u
JOIN items i ON i.name IN (
    'Basic Nameplate',
    'Silver Nameplate',
    'Golden Nameplate',
    'Default Avatar',
    'Trader Avatar',
    'Dark Theme'
)
WHERE u.email IN ('demo_user@example.com', 'local_user1@example.com')
ON CONFLICT (user_id, item_id) DO NOTHING;

-- --------------------------------------------
-- 7. 관심종목 (watchlist) - 사용자별 대표 종목
-- --------------------------------------------
INSERT INTO watchlist (user_id, ticker)
SELECT u.user_id, v.ticker
FROM users u
JOIN (
    VALUES
        ('local_user1@example.com',  'AAPL'),
        ('local_user1@example.com',  'MSFT'),
        ('local_user1@example.com',  'GOOGL'),
        ('local_user1@example.com',  'AMZN'),
        ('local_user1@example.com',  'NVDA'),
        ('demo_user@example.com',    'AAPL'),
        ('demo_user@example.com',    'MSFT'),
        ('demo_user@example.com',    'GOOGL'),
        ('demo_user@example.com',    'AMZN'),
        ('demo_user@example.com',    'NVDA'),
        ('google_user1@example.com', 'AAPL'),
        ('google_user1@example.com', 'TSLA'),
        ('kakao_user1@example.com',  'NVDA'),
        ('kakao_user2@example.com',  'GOOGL')
) AS v(email, ticker)
    ON u.email = v.email
ON CONFLICT (user_id, ticker) DO NOTHING;

-- --------------------------------------------
-- 8. Stock Candles (stock_candles) - 간단 캔들 데이터
-- --------------------------------------------
INSERT INTO stock_candles (symbol, date, open, high, low, close, volume)
VALUES
    ('AAPL', DATE '2026-01-15', 190.00, 196.00, 189.50, 195.12, 120000000),
    ('AAPL', DATE '2026-01-16', 195.12, 198.00, 194.00, 197.50, 110000000),
    ('MSFT', DATE '2026-01-15', 330.00, 335.00, 328.00, 332.10, 80000000),
    ('GOOGL',DATE '2026-01-15', 140.00, 142.50, 139.00, 141.20, 60000000),
    ('NVDA', DATE '2026-01-15', 900.00, 920.00, 895.00, 910.50, 50000000),
    ('TSLA', DATE '2026-01-15', 220.00, 228.00, 218.00, 225.30, 70000000)
ON CONFLICT (symbol, date) DO NOTHING;

-- --------------------------------------------
-- 9. Chat History (chat_history) - 샘플 세션
-- --------------------------------------------
INSERT INTO chat_history (user_id, session_id, messages, sentiment_score)
SELECT u.user_id,
       gen_random_uuid(),
       '[
          {"role":"user","content":"오늘 포트폴리오 어떻게 보나요?"},
          {"role":"assistant","content":"현재 수익률은 약 5%이며, 기술주 비중이 높습니다."}
        ]'::jsonb,
       0.8
FROM users u
WHERE u.email = 'demo_user@example.com';

