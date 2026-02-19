-- =====================================================
-- PATCH: Correction colonne reservation_session
-- Remplace user_id par user_ref pour matcher le code Java
-- =====================================================

-- Supprimer la contrainte de clé étrangère si elle existe
ALTER TABLE reservation_session DROP FOREIGN KEY IF EXISTS reservation_session_ibfk_2;

-- Supprimer la colonne user_id
ALTER TABLE reservation_session DROP COLUMN IF EXISTS user_id;

-- Ajouter la colonne user_ref (VARCHAR pour stocker la référence utilisateur)
ALTER TABLE reservation_session ADD COLUMN user_ref VARCHAR(50) NOT NULL DEFAULT 'USER001';
