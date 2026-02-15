-- ========================================
-- GoVibe Travel - Database Schema
-- ========================================

-- Drop existing tables if they exist (for clean setup)
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS chambre;
DROP TABLE IF EXISTS hotel;

-- ========================================
-- Create Hotel Table
-- ========================================
CREATE TABLE hotel (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(100) NOT NULL,
    adresse VARCHAR(150),
    ville VARCHAR(100),
    nombre_etoiles INT,
    budget DOUBLE,
    description TEXT,
    photo_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ========================================
-- Create Chambre Table
-- ========================================
CREATE TABLE chambre (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(100) NOT NULL,
    capacite INT NOT NULL,
    equipements VARCHAR(255),
    hotel_id INT NOT NULL,
    prix_standard DOUBLE NOT NULL,
    prix_haute_saison DOUBLE NOT NULL,
    prix_basse_saison DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (hotel_id) REFERENCES hotel(id) ON DELETE CASCADE
);

-- ========================================
-- Create Reservation Table (NEW)
-- ========================================
CREATE TABLE reservation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    client_nom VARCHAR(100) NOT NULL,
    client_email VARCHAR(100) NOT NULL,
    client_telephone VARCHAR(20),
    chambre_id INT NOT NULL,
    hotel_id INT NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    prix_total DOUBLE NOT NULL,
    statut VARCHAR(50) DEFAULT 'EN_ATTENTE', -- EN_ATTENTE, CONFIRMEE, ANNULEE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (chambre_id) REFERENCES chambre(id) ON DELETE CASCADE,
    FOREIGN KEY (hotel_id) REFERENCES hotel(id) ON DELETE CASCADE
);

-- ========================================
-- Insert Sample Data
-- ========================================

-- Sample Hotels
INSERT INTO hotel (nom, adresse, ville, nombre_etoiles, budget, description, photo_url) VALUES
('Hôtel Royal Palace', '123 Avenue Habib Bourguiba', 'Tunis', 5, 250.00, 'Hôtel de luxe au cœur de la capitale tunisienne avec vue panoramique', 'https://example.com/royal.jpg'),
('Beach Resort Hammamet', 'Zone Touristique Yasmine', 'Hammamet', 4, 180.00, 'Resort en bord de mer avec piscines et spa', 'https://example.com/beach.jpg'),
('Sousse Palace', 'Boulevard de la Corniche', 'Sousse', 4, 150.00, 'Hôtel moderne avec accès direct à la plage', 'https://example.com/sousse.jpg'),
('Oasis Tozeur', 'Avenue Abou El Kacem Chebbi', 'Tozeur', 3, 100.00, 'Hôtel traditionnel près des palmeraies', 'https://example.com/oasis.jpg');

-- Sample Chambres
INSERT INTO chambre (type, capacite, equipements, hotel_id, prix_standard, prix_haute_saison, prix_basse_saison) VALUES
('Suite Présidentielle', 4, 'WiFi, TV LED, Jacuzzi, Vue mer, Minibar', 1, 400.00, 550.00, 350.00),
('Chambre Double Deluxe', 2, 'WiFi, TV, Climatisation, Balcon', 1, 180.00, 250.00, 150.00),
('Suite Familiale', 6, 'WiFi, TV, Kitchenette, 2 Chambres', 2, 300.00, 400.00, 250.00),
('Chambre Standard', 2, 'WiFi, TV, Climatisation', 2, 120.00, 170.00, 100.00),
('Chambre Triple', 3, 'WiFi, TV, Climatisation, Vue piscine', 3, 150.00, 200.00, 120.00),
('Bungalow', 4, 'WiFi, TV, Jardin privé, Climatisation', 4, 200.00, 280.00, 160.00);

-- Sample Reservations
INSERT INTO reservation (client_nom, client_email, client_telephone, chambre_id, hotel_id, date_debut, date_fin, prix_total, statut) VALUES
('Ahmed Ben Salem', 'ahmed.salem@email.com', '+216 20 123 456', 1, 1, '2026-03-15', '2026-03-20', 2000.00, 'CONFIRMEE'),
('Fatma Trabelsi', 'fatma.trabelsi@email.com', '+216 21 234 567', 3, 2, '2026-04-01', '2026-04-07', 1800.00, 'EN_ATTENTE'),
('Mohamed Gharbi', 'mohamed.gharbi@email.com', '+216 22 345 678', 5, 3, '2026-03-25', '2026-03-28', 450.00, 'CONFIRMEE'),
('Sarah Mansour', 'sarah.mansour@email.com', '+216 23 456 789', 2, 1, '2026-05-10', '2026-05-15', 900.00, 'EN_ATTENTE');

-- ========================================
-- Indexes for Performance
-- ========================================
CREATE INDEX idx_chambre_hotel ON chambre(hotel_id);
CREATE INDEX idx_reservation_chambre ON reservation(chambre_id);
CREATE INDEX idx_reservation_hotel ON reservation(hotel_id);
CREATE INDEX idx_reservation_dates ON reservation(date_debut, date_fin);
CREATE INDEX idx_reservation_statut ON reservation(statut);

-- ========================================
-- End of Schema
-- ========================================

