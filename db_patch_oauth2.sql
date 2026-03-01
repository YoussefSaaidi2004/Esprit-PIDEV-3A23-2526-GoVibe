-- ========================================================================
-- GOVIBE PROJECT - PATCH : Support OAuth2 Google
-- ========================================================================
-- Ce script ajoute les colonnes nécessaires pour l'authentification
-- via Google OAuth2 à la table 'personne' existante.
--
-- Exécutez ce script UNE SEULE FOIS sur votre base de données 'govibe'.
-- ========================================================================

USE govibe;

-- Rendre le mot de passe nullable (car les utilisateurs Google n'en ont pas)
ALTER TABLE personne MODIFY COLUMN password VARCHAR(255) NULL;

-- Ajouter les colonnes OAuth2
ALTER TABLE personne ADD COLUMN IF NOT EXISTS provider VARCHAR(50) DEFAULT 'local';
ALTER TABLE personne ADD COLUMN IF NOT EXISTS provider_id VARCHAR(255) NULL;
ALTER TABLE personne ADD COLUMN IF NOT EXISTS photo_url VARCHAR(500) NULL;

-- Index pour recherche rapide par provider_id
ALTER TABLE personne ADD INDEX IF NOT EXISTS idx_provider_id (provider_id);

-- ========================================================================
-- Vérification
-- ========================================================================
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'govibe_project' AND TABLE_NAME = 'personne'
ORDER BY ORDINAL_POSITION;
