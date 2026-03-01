-- ========================================================================
-- GOVIBE PROJECT - DATABASE PATCH FOR FACE ID INTEGRATION
-- ========================================================================
-- This patch adds the necessary column to store the 128-point face 
-- encoding for the users in the personne table.
-- ========================================================================

USE govibe;

-- Add face_encoding column to personne table to store the Face ID data
-- Using JSON since the encoding is a list of 128 float values
ALTER TABLE personne ADD COLUMN IF NOT EXISTS face_encoding JSON NULL;

-- Log the migration
INSERT INTO InitialData (table_name, description) VALUES
('personne', 'Added face_encoding column for Face ID authentication');
