-- Fictitious local-only accounts. This location is never enabled by the production profile.
INSERT INTO users(user_id, user_name, password, name, staff_id, email, role, designation, manager_id) VALUES
(1, 'admin', 'demo123', 'Alex Admin', 'A001', 'admin@example.test', 'ADMIN', NULL, NULL),
(2, 'manager', 'demo123', 'Morgan Manager', 'M001', 'manager@example.test', 'MANAGER', 'Professional', NULL),
(3, 'manager2', 'demo123', 'Casey Manager', 'M002', 'manager2@example.test', 'MANAGER', 'Professional', NULL),
(4, 'staff', 'demo123', 'Sam Staff', 'S001', 'staff@example.test', 'STAFF', 'Professional', 2),
(5, 'staff2', 'demo123', 'Taylor Staff', 'S002', 'staff2@example.test', 'STAFF', 'Administrative', 2),
(6, 'staff3', 'demo123', 'Jordan Staff', 'S003', 'staff3@example.test', 'STAFF', 'Professional', 3);
INSERT INTO training_entitlement(staff_id, `year`, day_limit, budget)
SELECT user_id, YEAR(CURRENT_DATE), CASE WHEN designation='Administrative' THEN 5 ELSE 10 END, 2000
FROM users WHERE role IN ('STAFF', 'MANAGER');
INSERT INTO training_entitlement(staff_id, `year`, day_limit, budget)
SELECT user_id, YEAR(CURRENT_DATE)+1, CASE WHEN designation='Administrative' THEN 5 ELSE 10 END, 2000
FROM users WHERE role IN ('STAFF', 'MANAGER');
