-- =====================================================
-- TABLE: contact (Messagerie Client-Admin)
-- =====================================================

CREATE TABLE IF NOT EXISTS contact (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    sujet VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    reponse TEXT,
    status ENUM('EN_ATTENTE', 'LU', 'REPONDU', 'FERME') DEFAULT 'EN_ATTENTE',
    date_envoi TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_reponse TIMESTAMP NULL,
    created_by_user BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES personne(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Index pour optimiser les requêtes
CREATE INDEX idx_contact_user_id ON contact(user_id);
CREATE INDEX idx_contact_status ON contact(status);
CREATE INDEX idx_contact_date ON contact(date_envoi DESC);
