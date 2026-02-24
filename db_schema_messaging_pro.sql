-- =====================================================
-- SCHÉMA PROFESSIONNEL: Messagerie Conversationnelle
-- Tables: conversation + message (approche moderne)
-- =====================================================

-- Table des conversations (un fil par client)
CREATE TABLE IF NOT EXISTS conversation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    client_id INT NOT NULL,
    sujet VARCHAR(255),
    status ENUM('ACTIF', 'ARCHIVE', 'FERME') DEFAULT 'ACTIF',
    last_message_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES personne(id) ON DELETE CASCADE,
    INDEX idx_client (client_id),
    INDEX idx_status (status),
    INDEX idx_last_msg (last_message_at DESC)
) ENGINE=InnoDB;

-- Table des messages
CREATE TABLE IF NOT EXISTS message (
    id INT AUTO_INCREMENT PRIMARY KEY,
    conversation_id INT NOT NULL,
    sender_id INT NOT NULL,
    sender_type ENUM('CLIENT', 'ADMIN') NOT NULL,
    content TEXT NOT NULL,
    type ENUM('TEXTE', 'IMAGE', 'FICHIER') DEFAULT 'TEXTE',
    status ENUM('ENVOYE', 'RECU', 'LU') DEFAULT 'ENVOYE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    lu_at TIMESTAMP NULL,
    FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES personne(id) ON DELETE CASCADE,
    INDEX idx_conversation (conversation_id),
    INDEX idx_sender (sender_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB;

-- Vue pour statistiques admin
CREATE VIEW IF NOT EXISTS v_conversations_actives AS
SELECT 
    c.id,
    c.client_id,
    p.prenom AS client_prenom,
    p.nom AS client_nom,
    p.email AS client_email,
    c.sujet,
    c.status,
    c.last_message_at,
    (SELECT COUNT(*) FROM message m WHERE m.conversation_id = c.id AND m.sender_type = 'CLIENT' AND m.status != 'LU') AS messages_non_lus
FROM conversation c
JOIN personne p ON c.client_id = p.id
WHERE c.status = 'ACTIF';
