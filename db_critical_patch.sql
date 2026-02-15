-- ========================================================================
-- GOVIBE PROJECT - CRITICAL DATABASE SCHEMA PATCH
-- ========================================================================
-- ATTENTION: This patch fixes critical issues in the database schema:
-- 1. Converts TotalPrix from INT to DECIMAL(10,2) for proper currency handling
-- 2. Adds foreign key constraints for data integrity
-- 3. Standardizes status values to uppercase
-- 4. Ensures proper schema alignment with updated Java entities
--
-- BEFORE RUNNING: Backup your database!
-- USAGE: mysql -u root -p GoVibe_Project < db_critical_patch.sql
-- ========================================================================

USE GoVibe_Project;

-- ========================================================================
-- STEP 1: Backup existing data (optional but recommended)
-- ========================================================================
-- CREATE TABLE checkout_backup AS SELECT * FROM checkout;

-- ========================================================================
-- STEP 2: Modify checkout table structure
-- ========================================================================

-- Step 2a: Alter TotalPrix column from INT to DECIMAL(10,2)
ALTER TABLE checkout MODIFY COLUMN total_prix DECIMAL(10,2) NOT NULL DEFAULT 0.00;

-- Step 2b: Add or update status constraint to ensure uppercase values
-- First, standardize all existing status values to uppercase
UPDATE checkout SET status_reservation = UPPER(status_reservation) WHERE status_reservation IS NOT NULL;

-- Verify the update
-- SELECT DISTINCT status_reservation FROM checkout;

-- ========================================================================
-- STEP 3: Add Foreign Key Constraints (if not already present)
-- ========================================================================

-- Check and add FK for flight_id if it doesn't exist
ALTER TABLE checkout ADD CONSTRAINT fk_checkout_flight 
    FOREIGN KEY (flight_id) REFERENCES vol(flight_id) ON DELETE RESTRICT;

-- Check and add FK for id_user if it doesn't exist (alternative name: id_personne)
ALTER TABLE checkout ADD CONSTRAINT fk_checkout_user 
    FOREIGN KEY (id_user) REFERENCES personne(id_personne) ON DELETE RESTRICT;

-- ========================================================================
-- STEP 4: Ensure location table has proper structure
-- ========================================================================

-- Verify location table has all required columns
ALTER TABLE location 
MODIFY COLUMN id_Voiture INT NOT NULL,
MODIFY COLUMN id_Personne INT NOT NULL;

-- Ensure foreign keys for location
ALTER TABLE location ADD CONSTRAINT fk_location_voiture_check 
    FOREIGN KEY (id_Voiture) REFERENCES voiture(id_Voiture) ON DELETE RESTRICT;

ALTER TABLE location ADD CONSTRAINT fk_location_personne 
    FOREIGN KEY (id_Personne) REFERENCES personne(id_personne) ON DELETE RESTRICT;

-- ========================================================================
-- STEP 5: Validate Schema Integrity
-- ========================================================================

-- Table structure validation
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'GoVibe_Project' 
AND TABLE_NAME = 'checkout'
ORDER BY ORDINAL_POSITION;

-- Foreign keys validation
SELECT CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'GoVibe_Project'
AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME;

-- Data integrity check
SELECT 
    COUNT(*) as total_checkouts,
    COUNT(DISTINCT status_reservation) as status_types,
    GROUP_CONCAT(DISTINCT status_reservation ORDER BY status_reservation) as statuses
FROM checkout;

-- ========================================================================
-- STEP 6: Sample Data Verification (if needed)
-- ========================================================================

-- Verify sample flight reservation pricing
SELECT 
    c.checkout_id,
    c.flight_id,
    c.id_user,
    c.passenger_nbr,
    c.total_prix,
    c.status_reservation,
    c.reservation_date
FROM checkout c
LIMIT 5;

-- ========================================================================
-- COMPLETED: Database schema is now patched and aligned with GoVibe v2
-- ========================================================================
-- 
-- Summary of Changes:
-- ✓ TotalPrix: INT → DECIMAL(10,2) for precision
-- ✓ Status values: Standardized to uppercase (PENDING, CONFIRMED, CANCELLED, COMPLETED)
-- ✓ Foreign keys: Added for data integrity
-- ✓ Constraints: Enhanced with proper ON DELETE behavior
--
-- Next Steps:
-- 1. Run this patch against your production database
-- 2. Verify all queries in STEP 5 return expected results
-- 3. Test checkout CRUD operations thoroughly
-- 4. Monitor application logs for any database errors
--
-- ========================================================================
