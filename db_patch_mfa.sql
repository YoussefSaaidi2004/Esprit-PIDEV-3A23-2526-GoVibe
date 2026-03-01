-- ========================================================================
-- GOVIBE PROJECT - MFA & AI RISK SCORING PATCH
-- ========================================================================
-- Adds login_attempts, user_sessions, otp_codes tables
-- and new columns to personne for Adaptive MFA support.
-- ========================================================================

-- ========================================================================
-- TABLE: login_attempts
-- ========================================================================
CREATE TABLE IF NOT EXISTS login_attempts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    ip_address VARCHAR(45),
    device VARCHAR(255),
    country VARCHAR(100),
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN DEFAULT FALSE,
    risk_score DOUBLE DEFAULT 0.0,
    auth_level VARCHAR(20) DEFAULT 'LOW',
    INDEX idx_user_id (user_id),
    INDEX idx_login_time (login_time),
    INDEX idx_success (success)
);

-- ========================================================================
-- TABLE: user_sessions
-- ========================================================================
CREATE TABLE IF NOT EXISTS user_sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id INT NOT NULL,
    ip_address VARCHAR(45),
    device_name VARCHAR(255),
    country VARCHAR(100),
    city VARCHAR(100),
    login_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_activity DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_user_id (user_id),
    INDEX idx_is_active (is_active)
);

-- ========================================================================
-- TABLE: otp_codes
-- ========================================================================
CREATE TABLE IF NOT EXISTS otp_codes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    code VARCHAR(10) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    INDEX idx_user_id (user_id),
    INDEX idx_code (code)
);

-- ========================================================================
-- ALTER personne: add MFA columns (safe idempotent approach)
-- ========================================================================
-- Add is_account_locked
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'personne' AND COLUMN_NAME = 'is_account_locked');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE personne ADD COLUMN is_account_locked BOOLEAN DEFAULT FALSE', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add lockout_until
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'personne' AND COLUMN_NAME = 'lockout_until');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE personne ADD COLUMN lockout_until DATETIME DEFAULT NULL', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add preferred_mfa
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'personne' AND COLUMN_NAME = 'preferred_mfa');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE personne ADD COLUMN preferred_mfa VARCHAR(20) DEFAULT ''NONE''', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'MFA schema patch applied successfully!' AS status;
