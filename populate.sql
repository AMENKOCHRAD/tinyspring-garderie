USE garderie_db;

DELETE FROM affectation;
DELETE FROM groupe;
DELETE FROM classe;
DELETE FROM salle;
DELETE FROM users WHERE role_id = 3;

INSERT INTO salle (climatise, disponible, equipements, nom, surface, type) VALUES
(1, 1, 'Tapis, Jouets, Livres', 'Salle Soleil', 45.5, 'JEUX'),
(1, 1, 'Lits, Couvertures', 'Salle Lune', 35.0, 'SIESTE'),
(1, 1, 'Tables, Tableaux', 'Salle Étoile', 50.0, 'ETUDE');

INSERT INTO classe (age_maximum, age_minimum, annee_scolaire, capacite_max, niveau, nom, salle_id) VALUES
(3, 1, '2023-2024', 20, 'CRECHE', 'Petite Section', (SELECT id FROM salle WHERE nom='Salle Soleil' LIMIT 1)),
(5, 3, '2023-2024', 25, 'MATERNELLE', 'Moyenne Section', (SELECT id FROM salle WHERE nom='Salle Étoile' LIMIT 1));

INSERT INTO users (email, enabled, nom, prenom, telephone, password, role_id) VALUES
('animatrice1@garderie.com', 1, 'Martin', 'Sophie', '0600112233', '$2a$10$wY9C8t4D01r63w51qTqF5OYyS1W3K1sE5hM72oRtzR4m8zD/Q7uS', 3),
('animatrice2@garderie.com', 1, 'Dupont', 'Julie', '0600112244', '$2a$10$wY9C8t4D01r63w51qTqF5OYyS1W3K1sE5hM72oRtzR4m8zD/Q7uS', 3);

INSERT INTO groupe (animatrice_id, capacite, horaire_debut, horaire_fin, langue_principale, nom, classe_id) VALUES
((SELECT id FROM users WHERE email='animatrice1@garderie.com'), 10, '08:00:00', '12:00:00', 'Français', 'Les Poussins', (SELECT id FROM classe WHERE nom='Petite Section' LIMIT 1)),
((SELECT id FROM users WHERE email='animatrice2@garderie.com'), 15, '14:00:00', '18:00:00', 'Anglais', 'The Foxes', (SELECT id FROM classe WHERE nom='Moyenne Section' LIMIT 1));

INSERT INTO affectation (date_debut, date_fin, enfant_id, statut, groupe_id) VALUES
('2023-09-01', '2024-06-30', 101, 'ACTIF', (SELECT id FROM groupe WHERE nom='Les Poussins' LIMIT 1)),
('2023-09-01', '2024-06-30', 102, 'ACTIF', (SELECT id FROM groupe WHERE nom='Les Poussins' LIMIT 1)),
('2023-09-01', '2024-06-30', 103, 'ACTIF', (SELECT id FROM groupe WHERE nom='Les Poussins' LIMIT 1)),
('2023-09-01', '2024-06-30', 104, 'ACTIF', (SELECT id FROM groupe WHERE nom='Les Poussins' LIMIT 1)),
('2023-09-01', '2024-06-30', 105, 'ACTIF', (SELECT id FROM groupe WHERE nom='Les Poussins' LIMIT 1)),
('2023-09-01', '2024-06-30', 106, 'ACTIF', (SELECT id FROM groupe WHERE nom='Les Poussins' LIMIT 1)),
('2023-09-01', '2024-06-30', 107, 'ACTIF', (SELECT id FROM groupe WHERE nom='Les Poussins' LIMIT 1)),
('2023-09-01', '2024-06-30', 108, 'ACTIF', (SELECT id FROM groupe WHERE nom='Les Poussins' LIMIT 1));

INSERT INTO affectation (date_debut, date_fin, enfant_id, statut, groupe_id) VALUES
('2023-09-01', '2024-06-30', 201, 'ACTIF', (SELECT id FROM groupe WHERE nom='The Foxes' LIMIT 1)),
('2023-09-01', '2024-06-30', 202, 'ACTIF', (SELECT id FROM groupe WHERE nom='The Foxes' LIMIT 1)),
('2023-09-01', '2024-06-30', 203, 'ACTIF', (SELECT id FROM groupe WHERE nom='The Foxes' LIMIT 1)),
('2023-09-01', '2024-06-30', 204, 'INACTIF', (SELECT id FROM groupe WHERE nom='The Foxes' LIMIT 1));
