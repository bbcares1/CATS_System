-- Admin user
INSERT INTO users (id, username, password, name, designation, role, manager_id) 
VALUES (1, 'ryan', 'admin123', 'Ryan', 'Administrator', 'ADMIN', NULL);

-- Manager user
INSERT INTO users (id, username, password, name, designation, role, manager_id) 
VALUES (2, 'michael', 'pass123', 'Michael', 'Team Leader', 'MANAGER', NULL);

-- Staff users
INSERT INTO users (id, username, password, name, designation, role, manager_id) 
VALUES (3, 'junie', 'pass123', 'Junie', 'Professional', 'STAFF', 2);

INSERT INTO users (id, username, password, name, designation, role, manager_id) 
VALUES (4, 'fan', 'pass123', 'Fan', 'Professional', 'STAFF', 2);

INSERT INTO users (id, username, password, name, designation, role, manager_id) 
VALUES (5, 'martin', 'pass123', 'Martin', 'Professional', 'STAFF', 2);

INSERT INTO users (id, username, password, name, designation, role, manager_id) 
VALUES (6, 'imran', 'pass123', 'Imran', 'Professional', 'STAFF', 2);

INSERT INTO users (id, username, password, name, designation, role, manager_id) 
VALUES (7, 'owen', 'pass123', 'OWen', 'Professional', 'STAFF', 2);

INSERT INTO users (id, username, password, name, designation, role, manager_id) 
VALUES (8, 'jialu', 'pass123', 'Jialu', 'Professional', 'STAFF', 2);