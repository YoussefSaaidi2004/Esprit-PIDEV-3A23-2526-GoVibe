-- Disable foreign key checks temporarily to clear tables
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE reservation_session;
TRUNCATE TABLE sessions;
TRUNCATE TABLE activite;
SET FOREIGN_KEY_CHECKS = 1;

-- Insert 6 Activities
INSERT INTO activite (id, name, description, type, localisation, prix, status) VALUES
(1, 'Fo5arCity', 'Découvrir les traditions de nabeul', 'Artisanat • Créatif', 'Nabeul', 65.50, 'Confirmed'),
(2, 'Médina Tour', 'Visite guidée immersive au cœur de la vieille ville de Tunis.', 'Culture', 'Tunis (Médina)', 45.00, 'Confirmed'),
(3, 'Blue & White Escapade', 'Une balade inoubliable dans les ruelles bleues et blanches.', 'Tourisme', 'Sidi Bou Said', 30.00, 'Confirmed'),
(4, 'Sunset Kayak', 'Une aventure sportive en kayak pour admirer le coucher du soleil.', 'Sport', 'Ghar El Melh', 75.00, 'Confirmed'),
(5, 'Hiking Cap Bon', 'Une randonnée spectaculaire entre montagnes et mer méditerranée.', 'Sport', 'Cap Bon', 55.00, 'Confirmed'),
(6, 'Gusto d\'Oro', 'Dégustation d\'huiles d\'olive artisanales et produits du terroir.', 'Gourmand', 'Dégustation Huile d\'Olive', 40.00, 'Confirmed');

-- Insert 6 Sessions (one for each activity)
INSERT INTO sessions (id_session, date, heure, capacite, nbr_places_restant, activite_id) VALUES
(1, '2026-03-01', '10:00:00', 20, 19, 1),
(2, '2026-03-02', '14:00:00', 15, 14, 2),
(3, '2026-03-05', '09:00:00', 25, 24, 3),
(4, '2026-03-10', '16:00:00', 10, 9, 4),
(5, '2026-03-15', '08:30:00', 12, 11, 5),
(6, '2026-03-20', '11:00:00', 30, 29, 6);

-- Insert 6 Reservations (one for each session)
INSERT INTO reservation_session (session_id, nb_places, user_ref) VALUES
(1, 1, 'admin'),
(2, 1, 'admin'),
(3, 1, 'admin'),
(4, 1, 'admin'),
(5, 1, 'admin'),
(6, 1, 'admin');
