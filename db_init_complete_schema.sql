-- ========================================================================
-- GOVIBE PROJECT - COMPLETE DATABASE SCHEMA INITIALIZATION
-- ========================================================================
-- This script creates all necessary tables with proper data types,
-- constraints, and relationships for the GoVibe flight management system.
-- ========================================================================

-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS GoVibe_Project;
USE GoVibe_Project;

-- ========================================================================
-- TABLE: Personne (Users/Personnel)
-- ========================================================================
CREATE TABLE IF NOT EXISTS personne (
    id_personne INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    telephone VARCHAR(20),
    role VARCHAR(50) DEFAULT 'USER',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_role (role)
);

-- ========================================================================
-- TABLE: Location (Rentals/Bookings)
-- ========================================================================
CREATE TABLE IF NOT EXISTS location (
    id_location INT PRIMARY KEY AUTO_INCREMENT,
    reference VARCHAR(50) UNIQUE NOT NULL,
    id_Voiture INT NOT NULL,
    id_Personne INT NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    montant_total DECIMAL(10,2) NOT NULL,
    nb_jours INT GENERATED ALWAYS AS (DATEDIFF(date_fin, date_debut)) STORED,
    statut VARCHAR(50) DEFAULT 'EN_ATTENTE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_Personne) REFERENCES personne(id_personne) ON DELETE RESTRICT,
    INDEX idx_reference (reference),
    INDEX idx_statut (statut),
    INDEX idx_dates (date_debut, date_fin)
);

-- ========================================================================
-- TABLE: Voiture (Vehicles)
-- ========================================================================
CREATE TABLE IF NOT EXISTS voiture (
    id_Voiture INT PRIMARY KEY AUTO_INCREMENT,
    marque VARCHAR(100) NOT NULL,
    modele VARCHAR(100) NOT NULL,
    matricule VARCHAR(50) UNIQUE NOT NULL,
    annee INT,
    nb_places INT DEFAULT 4,
    prix_location_jour DECIMAL(10,2) NOT NULL,
    couleur VARCHAR(50),
    statut VARCHAR(50) DEFAULT 'AVAILABLE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_marque (marque),
    INDEX idx_matricule (matricule),
    INDEX idx_statut (statut)
);

-- Add foreign key for location to voiture
ALTER TABLE location ADD CONSTRAINT fk_location_voiture 
    FOREIGN KEY (id_Voiture) REFERENCES voiture(id_Voiture) ON DELETE RESTRICT;

-- ========================================================================
-- TABLE: Aeroport (Airports)
-- ========================================================================
CREATE TABLE IF NOT EXISTS aeroport (
    id_aeroport INT PRIMARY KEY AUTO_INCREMENT,
    code_aeroport VARCHAR(10) UNIQUE NOT NULL,
    nom_aeroport VARCHAR(200) NOT NULL,
    ville VARCHAR(100) NOT NULL,
    pays VARCHAR(100) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_code (code_aeroport),
    INDEX idx_ville (ville)
);

-- ========================================================================
-- TABLE: Vol (Flights)
-- ========================================================================
CREATE TABLE IF NOT EXISTS vol (
    flight_id VARCHAR(50) PRIMARY KEY,
    airline VARCHAR(100) NOT NULL,
    departure_airport VARCHAR(10) NOT NULL,
    arrival_airport VARCHAR(10) NOT NULL,
    departure_time DATETIME NOT NULL,
    arrival_time DATETIME NOT NULL,
    duration INT COMMENT 'Duration in minutes',
    price DECIMAL(10,2) NOT NULL,
    available_seats INT DEFAULT 0,
    total_seats INT DEFAULT 180,
    aircraft_type VARCHAR(100),
    status VARCHAR(50) DEFAULT 'SCHEDULED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (departure_airport) REFERENCES aeroport(code_aeroport) ON DELETE RESTRICT,
    FOREIGN KEY (arrival_airport) REFERENCES aeroport(code_aeroport) ON DELETE RESTRICT,
    INDEX idx_airline (airline),
    INDEX idx_dates (departure_time, arrival_time),
    INDEX idx_status (status)
);

-- ========================================================================
-- TABLE: Checkout (Flight Reservations)
-- ========================================================================
CREATE TABLE IF NOT EXISTS checkout (
    checkout_id INT PRIMARY KEY AUTO_INCREMENT,
    flight_id VARCHAR(50) NOT NULL,
    id_user INT NOT NULL,
    reservation_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    passenger_nbr INT NOT NULL CHECK (passenger_nbr > 0),
    status_reservation VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_prix DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    
    -- Passenger details
    passenger_name VARCHAR(255),
    passenger_email VARCHAR(255),
    passenger_phone VARCHAR(50),
    
    -- Booking preferences
    payment_method VARCHAR(50) DEFAULT 'CREDIT_CARD',
    seat_preference VARCHAR(20) DEFAULT 'WINDOW',
    travel_class VARCHAR(20) DEFAULT 'Economy',
    
    -- Metadata
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Constraints
    FOREIGN KEY (flight_id) REFERENCES vol(flight_id) ON DELETE RESTRICT,
    FOREIGN KEY (id_user) REFERENCES personne(id_personne) ON DELETE RESTRICT,
    
    -- Indexes for performance
    INDEX idx_flight_id (flight_id),
    INDEX idx_user_id (id_user),
    INDEX idx_status (status_reservation),
    INDEX idx_dates (reservation_date),
    CHECK (status_reservation IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED'))
);

-- ========================================================================
-- TABLE: InitialData (Track which migrations have been applied)
-- ========================================================================
CREATE TABLE IF NOT EXISTS InitialData (
    id INT PRIMARY KEY AUTO_INCREMENT,
    table_name VARCHAR(100),
    migration_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(255)
);

-- ========================================================================
-- INSERT SAMPLE DATA FOR TESTING
-- ========================================================================

-- Insert sample airports
INSERT IGNORE INTO aeroport (code_aeroport, nom_aeroport, ville, pays) VALUES
('TUN', 'Monastir International Airport', 'Monastir', 'Tunisia'),
('CDG', 'Charles de Gaulle Airport', 'Paris', 'France'),
('LHR', 'London Heathrow', 'London', 'United Kingdom'),
('FCO', 'Leonardo da Vinci', 'Rome', 'Italy');

-- Insert sample users
INSERT IGNORE INTO personne (nom, prenom, email, telephone, role) VALUES
('Dupont', 'Jean', 'jean.dupont@email.com', '+33612345678', 'USER'),
('Smith', 'Sarah', 'sarah.smith@email.com', '+442071838750', 'USER'),
('Rossi', 'Marco', 'marco.rossi@email.com', '+39612345678', 'USER'),
('Admin', 'System', 'admin@govibe.com', '+21625000000', 'ADMIN');

-- Insert sample flights
INSERT IGNORE INTO vol (flight_id, airline, departure_airport, arrival_airport, departure_time, arrival_time, duration, price, available_seats, total_seats, aircraft_type, status) VALUES
('AF102', 'Air France', 'TUN', 'CDG', '2024-02-15 08:00:00', '2024-02-15 11:30:00', 210, 180.00, 100, 180, 'Airbus A320', 'SCHEDULED'),
('BA205', 'British Airways', 'CDG', 'LHR', '2024-02-16 14:00:00', '2024-02-16 15:15:00', 75, 150.00, 50, 180, 'Boeing 737', 'SCHEDULED'),
('ITA301', 'Alitalia', 'LHR', 'FCO', '2024-02-17 10:30:00', '2024-02-17 13:00:00', 150, 95.00, 120, 180, 'Airbus A321', 'SCHEDULED');

-- Insert sample vehicles
INSERT IGNORE INTO voiture (marque, modele, matricule, annee, nb_places, prix_location_jour, couleur, statut) VALUES
('Toyota', 'Corolla', 'TN-2024-001', 2024, 5, 50.00, 'White', 'AVAILABLE'),
('BMW', '3 Series', 'TN-2024-002', 2024, 5, 80.00, 'Black', 'AVAILABLE'),
('Mercedes', 'C-Class', 'TN-2024-003', 2024, 5, 90.00, 'Silver', 'AVAILABLE');

-- ========================================================================
-- INITIALIZATION TRACKING
-- ========================================================================
INSERT INTO InitialData (table_name, description) VALUES
('personne', 'Base users table created'),
('voiture', 'Vehicles table created'),
('location', 'Rental bookings table created'),
('aeroport', 'Airports reference table created'),
('vol', 'Flights table created'),
('checkout', 'Flight reservations table created');

-- ========================================================================
-- VERIFICATION QUERIES
-- ========================================================================
-- Run these to verify schema integrity:
-- SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'GoVibe_Project';
-- SELECT * FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = 'GoVibe_Project' AND CONSTRAINT_NAME LIKE 'fk%';
-- SELECT COUNT(*) FROM personne;
-- SELECT COUNT(*) FROM vol;
-- ========================================================================
