
-- Création de la table Personne
CREATE TABLE IF NOT EXISTS personne ( 
    id INT(11) NOT NULL AUTO_INCREMENT,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('admin','user') NOT NULL DEFAULT 'user',
    PRIMARY KEY (id)
);

-- Ajout de la clé étrangère dans la table Poste si elle n'existe pas déjà
-- Note: Assurez-vous que la colonne user_id dans poste est du même type que id dans personne (INT(11))
ALTER TABLE poste
ADD CONSTRAINT fk_poste_personne
FOREIGN KEY (user_id) REFERENCES personne(id)
ON DELETE CASCADE
ON UPDATE CASCADE;

-- Insertion de données de test (Optionnel)
INSERT INTO personne (nom, prenom, email, password, role) VALUES 
('Admin', 'System', 'admin@govibe.tn', 'password123', 'admin'),
('User', 'Test', 'user@govibe.tn', 'user123', 'user');


-- Table de liaison Membre - Forum
CREATE TABLE IF NOT EXISTS membre_forum (
    forum_id INT(11) NOT NULL,
    user_id INT(11) NOT NULL,
    date_adhesion DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (forum_id, user_id),
    CONSTRAINT fk_membre_forum FOREIGN KEY (forum_id) REFERENCES forum(forum_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_membre_user FOREIGN KEY (user_id) REFERENCES personne(id) ON DELETE CASCADE ON UPDATE CASCADE
);
