-- ============================================================
-- GoVibe Hotel Features Patch
-- Features: QR Code, Promo Codes, Loyalty Program, Waitlist
-- ============================================================

-- 1. CODE PROMO TABLE
CREATE TABLE IF NOT EXISTS code_promo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    type ENUM('POURCENTAGE', 'MONTANT_FIXE') DEFAULT 'POURCENTAGE',
    valeur DOUBLE NOT NULL,
    date_expiration DATE,
    actif BOOLEAN DEFAULT TRUE,
    utilisations_max INT DEFAULT 100,
    utilisations_actuelles INT DEFAULT 0,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Default promo codes
INSERT IGNORE INTO code_promo (code, description, type, valeur, date_expiration, actif) VALUES
    ('HOTEL10', 'Réduction 10% sur réservation hôtel', 'POURCENTAGE', 10, '2026-12-31', TRUE),
    ('SUMMER2026', 'Offre été 2026 - 15% de réduction', 'POURCENTAGE', 15, '2026-09-30', TRUE),
    ('WELCOME50', 'Réduction 50 DT bienvenue', 'MONTANT_FIXE', 50, '2026-12-31', TRUE);

-- 2. PROGRAMME FIDELITE TABLE
CREATE TABLE IF NOT EXISTS programme_fidelite (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    points INT DEFAULT 0,
    statut ENUM('BRONZE', 'SILVER', 'GOLD', 'PLATINUM') DEFAULT 'BRONZE',
    total_reservations INT DEFAULT 0,
    total_depenses DOUBLE DEFAULT 0.0,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_mise_a_jour TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user (user_id)
);

-- 3. LISTE D'ATTENTE TABLE
CREATE TABLE IF NOT EXISTS liste_attente (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    hotel_id INT NOT NULL,
    capacite_souhaitee INT DEFAULT 1,
    budget_max DOUBLE,
    date_souhaitee_debut DATE NOT NULL,
    date_souhaitee_fin DATE NOT NULL,
    statut ENUM('EN_ATTENTE', 'NOTIFIE', 'CONFIRME', 'EXPIRE') DEFAULT 'EN_ATTENTE',
    chambre_proposee_id INT DEFAULT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_notification TIMESTAMP NULL
);

-- 4. Add QR code column to reservation
ALTER TABLE reservation ADD COLUMN IF NOT EXISTS qr_code_data TEXT DEFAULT NULL;

-- 5. Add promo code column to reservation  
ALTER TABLE reservation ADD COLUMN IF NOT EXISTS code_promo_utilise VARCHAR(50) DEFAULT NULL;
ALTER TABLE reservation ADD COLUMN IF NOT EXISTS remise_appliquee DOUBLE DEFAULT 0.0;

SELECT 'GoVibe Hotel Features Patch applied successfully!' AS status;
