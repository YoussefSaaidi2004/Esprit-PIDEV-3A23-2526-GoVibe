-- Database Migration: Checkout Enhancement
-- Adds passenger details and booking preferences to checkout table

ALTER TABLE checkout ADD COLUMN IF NOT EXISTS passenger_name VARCHAR(255);
ALTER TABLE checkout ADD COLUMN IF NOT EXISTS passenger_email VARCHAR(255);
ALTER TABLE checkout ADD COLUMN IF NOT EXISTS passenger_phone VARCHAR(50);
ALTER TABLE checkout ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50) DEFAULT 'CREDIT_CARD';
ALTER TABLE checkout ADD COLUMN IF NOT EXISTS seat_preference VARCHAR(20) DEFAULT 'WINDOW';
ALTER TABLE checkout ADD COLUMN IF NOT EXISTS travel_class VARCHAR(20) DEFAULT 'Economy';

-- Update existing records with default values
UPDATE checkout 
SET passenger_name = 'Guest User',
    passenger_email = 'guest@govibe.com',
    passenger_phone = '+216-00-000-000'
WHERE passenger_name IS NULL;
