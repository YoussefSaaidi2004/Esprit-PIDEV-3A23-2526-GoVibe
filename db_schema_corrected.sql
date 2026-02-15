-- Schéma GoVibe - VERSION CORRIGÉE
-- Corrections apportées:
-- 1. total_prix: INT -> DECIMAL(10,2) pour les prix avec décimales
-- 2. user_id au lieu de id_user pour cohérence
-- 3. Suppression de total_seats (non présent dans le vol original)

CREATE DATABASE IF NOT EXISTS GoVibe_Project
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE GoVibe_Project;

-- =====================================================
-- TABLE: personne (Utilisateurs)
-- =====================================================
CREATE TABLE personne (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('admin','user') NOT NULL DEFAULT 'user',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- =====================================================
-- TABLE: password_reset
-- =====================================================
CREATE TABLE password_reset (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    token VARCHAR(255) NOT NULL,
    expiration_date DATETIME NOT NULL,
    FOREIGN KEY (email) REFERENCES personne(email) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================
-- TABLE: vol (Vols)
-- =====================================================
CREATE TABLE vol (
    flight_id VARCHAR(50) PRIMARY KEY,
    departure_airport VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    departure_time TIME NOT NULL,
    arrival_time TIME NOT NULL,
    classe_chaise VARCHAR(255) NOT NULL,
    airline VARCHAR(100) NOT NULL,
    prix INT NOT NULL,
    available_seats INT NOT NULL,
    description LONGTEXT,
    CONSTRAINT chk_available_seats CHECK (available_seats >= 0)
) ENGINE=InnoDB;

-- =====================================================
-- TABLE: checkout (Réservations Vol)
-- CORRECTION: user_id au lieu de id_user
-- CORRECTION: total_prix DECIMAL(10,2) au lieu de INT
-- =====================================================
CREATE TABLE checkout (
    checkout_id INT AUTO_INCREMENT PRIMARY KEY,
    flight_id VARCHAR(50) NOT NULL,
    user_id INT NOT NULL,
    reservation_date DATETIME NOT NULL,
    passenger_nbr INT NOT NULL,
    status_reservation VARCHAR(255) NOT NULL,
    total_prix DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (flight_id) REFERENCES vol(flight_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES personne(id) ON DELETE CASCADE,
    CONSTRAINT chk_passenger_nbr CHECK (passenger_nbr > 0),
    CONSTRAINT chk_total_prix CHECK (total_prix >= 0)
) ENGINE=InnoDB;

-- =====================================================
-- TABLE: voiture (Locations de voitures)
-- =====================================================
CREATE TABLE voiture (
    id_voiture INT AUTO_INCREMENT PRIMARY KEY,
    matricule VARCHAR(30) NOT NULL UNIQUE,
    marque VARCHAR(50) NOT NULL,
    modele VARCHAR(50) NOT NULL,
    annee INT NOT NULL,
    type_carburant ENUM('Essence', 'Diesel', 'Hybride', 'Electrique') NOT NULL,
    prix_jour DECIMAL(10,2) NOT NULL,
    statut ENUM('DISPONIBLE', 'LOUEE', 'MAINTENANCE') DEFAULT 'DISPONIBLE',
    adresse_agence VARCHAR(255) NOT NULL,
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    description TEXT,
    image_url VARCHAR(255),
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- =====================================================
-- TABLE: location (Contrats de location)
-- =====================================================
CREATE TABLE location (
    id_location INT AUTO_INCREMENT PRIMARY KEY,
    reference VARCHAR(50) NOT NULL UNIQUE,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    nb_jours INT NOT NULL,
    montant_total DECIMAL(10,2) NOT NULL,
    contrat_pdf VARCHAR(255),
    qr_code VARCHAR(255),
    statut ENUM('EN_ATTENTE','CONFIRMEE','ANNULEE') DEFAULT 'EN_ATTENTE',
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_voiture INT NOT NULL,
    user_id INT NOT NULL,
    FOREIGN KEY (id_voiture) REFERENCES voiture(id_voiture) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES personne(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================
-- TABLE: hotel
-- =====================================================
CREATE TABLE hotel (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100),
    adresse VARCHAR(150),
    ville VARCHAR(100),
    nombre_etoiles INT,
    budget DOUBLE,
    description TEXT,
    photo_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- =====================================================
-- TABLE: chambre
-- =====================================================
CREATE TABLE chambre (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(100),
    capacite INT,
    equipements VARCHAR(255),
    hotel_id INT,
    prix_standard DOUBLE,
    prix_haute_saison DOUBLE,
    prix_basse_saison DOUBLE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (hotel_id) REFERENCES hotel(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================
-- TABLE: reservation
-- =====================================================
CREATE TABLE reservation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    chambre_id INT NOT NULL,
    hotel_id INT NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    prix_total DOUBLE NOT NULL,
    statut ENUM('EN_ATTENTE','CONFIRMEE','ANNULEE') DEFAULT 'EN_ATTENTE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES personne(id) ON DELETE CASCADE,
    FOREIGN KEY (chambre_id) REFERENCES chambre(id) ON DELETE CASCADE,
    FOREIGN KEY (hotel_id) REFERENCES hotel(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================
-- TABLE: activite
-- =====================================================
CREATE TABLE activite (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL,
    localisation VARCHAR(150) NOT NULL,
    prix DECIMAL(10,2) DEFAULT 0.00
) ENGINE=InnoDB;

-- =====================================================
-- TABLE: sessions
-- =====================================================
CREATE TABLE sessions (
    id_session INT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL,
    heure TIME NOT NULL,
    capacite INT NOT NULL,
    nbr_places_restant INT NOT NULL,
    activite_id INT NOT NULL,
    FOREIGN KEY (activite_id) REFERENCES activite(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================
-- TABLE: reservation_session
-- =====================================================
CREATE TABLE reservation_session (
    id_reservation INT AUTO_INCREMENT PRIMARY KEY,
    session_id INT NOT NULL,
    user_id INT NOT NULL,
    nb_places INT DEFAULT 1,
    reserved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES sessions(id_session) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES personne(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================
-- TABLE: forum
-- =====================================================
CREATE TABLE forum (
    forum_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    image VARCHAR(255),
    created_by INT NOT NULL,
    description TEXT,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_private BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (created_by) REFERENCES personne(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================
-- TABLE: membre_forum
-- =====================================================
CREATE TABLE membre_forum (
    forum_id INT NOT NULL,
    user_id INT NOT NULL,
    date_adhesion DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (forum_id, user_id),
    FOREIGN KEY (forum_id) REFERENCES forum(forum_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES personne(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================
-- TABLE: poste
-- =====================================================
CREATE TABLE poste (
    post_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    forum_id INT DEFAULT NULL,
    likes INT DEFAULT 0,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    date_modification DATETIME DEFAULT CURRENT_TIMESTAMP 
        ON UPDATE CURRENT_TIMESTAMP,
    url VARCHAR(255),
    type VARCHAR(50),
    contenu TEXT,
    FOREIGN KEY (user_id) REFERENCES personne(id) ON DELETE CASCADE,
    FOREIGN KEY (forum_id) REFERENCES forum(forum_id) 
        ON DELETE SET NULL
) ENGINE=InnoDB;

-- =====================================================
-- INDEXES pour optimisation des performances
-- =====================================================
CREATE INDEX idx_checkout_user_id ON checkout(user_id);
CREATE INDEX idx_checkout_flight_id ON checkout(flight_id);
CREATE INDEX idx_checkout_status ON checkout(status_reservation);
CREATE INDEX idx_location_user_id ON location(user_id);
CREATE INDEX idx_location_voiture_id ON location(id_voiture);
