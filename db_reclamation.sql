-- Table pour les réclamations
CREATE TABLE IF NOT EXISTS reclamation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    sujet VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    reponse TEXT NULL,
    status VARCHAR(50) DEFAULT 'EN_ATTENTE',
    date_envoi TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_reponse TIMESTAMP NULL,
    created_by_user INT NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_date_envoi (date_envoi),
    CONSTRAINT fk_reclamation_user FOREIGN KEY (user_id) REFERENCES personne(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
