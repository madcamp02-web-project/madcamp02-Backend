-- ============================================
-- MadCamp02 Backend Database Schema
-- Version: 1.0
-- Description: 초기 데이터베이스 스키마 생성
-- ============================================

-- ============================================
-- 1. USERS 테이블 (사용자)
-- ============================================

-- ============================================
-- 수정 내역: 일반 로그인 지원을 위한 스키마 수정
-- ============================================
CREATE TABLE users (
       user_id BIGSERIAL PRIMARY KEY,
       email VARCHAR(255) NOT NULL UNIQUE,
       password VARCHAR(255),                    -- 추가
       nickname VARCHAR(50) NOT NULL,
       provider VARCHAR(20) DEFAULT 'LOCAL',     -- GOOGLE → LOCAL
       birth_date DATE,                          -- NOT NULL 제거
       saju_element VARCHAR(10),
       zodiac_sign VARCHAR(20),
       avatar_url TEXT,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 2. WALLET 테이블 (지갑)
-- ============================================
CREATE TABLE wallet (
    wallet_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    cash_balance NUMERIC(19,4) DEFAULT 10000.0000 NOT NULL,
    realized_profit NUMERIC(19,4) DEFAULT 0.0000 NOT NULL,
    total_assets NUMERIC(19,4) DEFAULT 10000.0000 NOT NULL,
    game_coin INT DEFAULT 0 NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ============================================
-- 3. PORTFOLIO 테이블 (포트폴리오)
-- ============================================
CREATE TABLE portfolio (
    pf_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ticker VARCHAR(10) NOT NULL,
    quantity INT NOT NULL CHECK (quantity >= 0),
    avg_price NUMERIC(19,4) NOT NULL,
    CONSTRAINT fk_portfolio_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT uq_portfolio_user_ticker UNIQUE (user_id, ticker)
);

-- ============================================
-- 4. TRADE_LOGS 테이블 (거래 기록)
-- ============================================
CREATE TABLE trade_logs (
    log_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ticker VARCHAR(10) NOT NULL,
    trade_type VARCHAR(4) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')),
    price NUMERIC(19,4) NOT NULL,
    quantity INT NOT NULL,
    total_amount NUMERIC(19,4) NOT NULL,
    fee NUMERIC(19,4) DEFAULT 0.0000,
    realized_pnl NUMERIC(19,4),
    trade_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trade_logs_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ============================================
-- 5. ITEMS 테이블 (아이템)
-- ============================================
CREATE TABLE items (
    item_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(20) NOT NULL,
    rarity VARCHAR(20) NOT NULL,
    probability FLOAT NOT NULL,
    image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 6. INVENTORY 테이블 (인벤토리)
-- ============================================
CREATE TABLE inventory (
    inv_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    is_equipped BOOLEAN DEFAULT FALSE NOT NULL,
    acquired_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_item FOREIGN KEY (item_id) REFERENCES items(item_id) ON DELETE CASCADE,
    CONSTRAINT uq_inventory_user_item UNIQUE (user_id, item_id)
);

-- ============================================
-- 7. CHAT_HISTORY 테이블 (채팅 기록)
-- ============================================
CREATE TABLE chat_history (
    chat_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id UUID NOT NULL,
    messages JSONB NOT NULL,
    sentiment_score FLOAT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_history_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ============================================
-- 8. WATCHLIST 테이블 (관심종목)
-- ============================================
CREATE TABLE watchlist (
    watchlist_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ticker VARCHAR(10) NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_watchlist_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT uq_watchlist_user_ticker UNIQUE (user_id, ticker)
);

-- ============================================
-- 9. NOTIFICATIONS 테이블 (알림)
-- ============================================
CREATE TABLE notifications (
    notif_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT,
    is_read BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ============================================
-- 10. 인덱스 생성
-- ============================================

-- Users 인덱스
CREATE INDEX idx_users_email ON users(email);

-- Wallet 인덱스
CREATE INDEX idx_wallet_user ON wallet(user_id);

-- Portfolio 인덱스
CREATE INDEX idx_portfolio_user ON portfolio(user_id);
CREATE INDEX idx_portfolio_ticker ON portfolio(ticker);

-- Trade Logs 인덱스
CREATE INDEX idx_trade_user ON trade_logs(user_id);
CREATE INDEX idx_trade_date ON trade_logs(trade_date);
CREATE INDEX idx_trade_ticker ON trade_logs(ticker);

-- Inventory 인덱스
CREATE INDEX idx_inventory_user ON inventory(user_id);
CREATE INDEX idx_inventory_item ON inventory(item_id);

-- Chat History 인덱스
CREATE INDEX idx_chat_user ON chat_history(user_id);
CREATE INDEX idx_chat_session ON chat_history(session_id);
CREATE INDEX idx_chat_gin ON chat_history USING GIN (messages);

-- Watchlist 인덱스
CREATE INDEX idx_watchlist_user ON watchlist(user_id);
CREATE INDEX idx_watchlist_ticker ON watchlist(ticker);

-- Notifications 인덱스
CREATE INDEX idx_notif_user ON notifications(user_id);
CREATE INDEX idx_notif_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notif_created ON notifications(created_at DESC);

-- Items 테이블 인덱스
CREATE INDEX idx_items_rarity ON items(rarity);
CREATE INDEX idx_items_category ON items(category);

-- Inventory 테이블 인덱스
CREATE INDEX idx_inventory_equipped ON inventory(user_id, is_equipped);

-- ============================================
-- 11. 트리거 생성 (updated_at 자동 업데이트)
-- ============================================

-- Users 테이블 updated_at 자동 업데이트
CREATE OR REPLACE FUNCTION update_users_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_users_updated_at();

-- Wallet 테이블 updated_at 자동 업데이트
CREATE OR REPLACE FUNCTION update_wallet_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_wallet_updated_at
    BEFORE UPDATE ON wallet
    FOR EACH ROW
    EXECUTE FUNCTION update_wallet_updated_at();

-- ============================================
-- 스키마 생성 완료
-- ============================================
