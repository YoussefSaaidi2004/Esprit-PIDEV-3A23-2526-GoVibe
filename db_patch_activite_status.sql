-- =====================================================
-- PATCH: Ajouter colonne status à la table activite
-- =====================================================

-- Ajouter la colonne status avec valeur par défaut 'Confirmed'
ALTER TABLE activite 
ADD COLUMN status VARCHAR(20) DEFAULT 'Confirmed';

-- Mettre à jour les activités existantes pour avoir 'Confirmed' comme status
UPDATE activite 
SET status = 'Confirmed' 
WHERE status IS NULL;
