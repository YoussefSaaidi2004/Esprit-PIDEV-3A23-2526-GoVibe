-- ========================================================================
-- GOVIBE PROJECT - CONSOLIDATED DATABASE FIX (ALL MISSING COLUMNS)
-- ========================================================================
-- This script adds ALL missing columns to the 'personne' table to support:
-- 1. Face ID Integration
-- 2. OAuth2 (Google) Support
-- 3. Adaptive MFA & Account Security
-- ========================================================================

-- Ensure we are using the correct database
-- (Please select your database in phpMyAdmin/Workbench before running)

-- 1. SUPPORT FOR FACE ID
ALTER TABLE personne ADD COLUMN IF NOT EXISTS face_encoding JSON NULL;

-- 2. SUPPORT FOR OAUTH2 (GOOGLE)
-- Make password nullable for Google users
ALTER TABLE personne MODIFY COLUMN password VARCHAR(255) NULL;
ALTER TABLE personne ADD COLUMN IF NOT EXISTS provider VARCHAR(50) DEFAULT 'local';
ALTER TABLE personne ADD COLUMN IF NOT EXISTS provider_id VARCHAR(255) NULL;
ALTER TABLE personne ADD COLUMN IF NOT EXISTS photo_url VARCHAR(500) NULL;
ALTER TABLE personne ADD INDEX IF NOT EXISTS idx_provider_id (provider_id);

-- 3. SUPPORT FOR MFA & SECURITY
ALTER TABLE personne ADD COLUMN IF NOT EXISTS is_account_locked BOOLEAN DEFAULT FALSE;
ALTER TABLE personne ADD COLUMN IF NOT EXISTS lockout_until DATETIME DEFAULT NULL;
ALTER TABLE personne ADD COLUMN IF NOT EXISTS preferred_mfa VARCHAR(20) DEFAULT 'NONE';

-- 4. CREATE SUPPORTING TABLES FOR MFA (Required by ServicePersonne)
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
    INDEX idx_user_id (user_id)
);

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
    INDEX idx_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS otp_codes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    code VARCHAR(10) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    INDEX idx_user_id (user_id)
);

-- 5. LOG THE MIGRATION
CREATE TABLE IF NOT EXISTS InitialData (
    id INT PRIMARY KEY AUTO_INCREMENT,
    table_name VARCHAR(100),
    migration_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(255)
);

INSERT INTO InitialData (table_name, description) VALUES
('personne', 'Consolidated patch: FaceID, OAuth2, and MFA columns added.');

SELECT 'Migration completed successfully!' AS status;
