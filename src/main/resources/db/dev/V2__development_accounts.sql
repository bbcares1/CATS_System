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

-- Fictitious catalogue data gives the team useful pages immediately after startup.
INSERT INTO course_provider(provider_id, name, email) VALUES
(1, 'CATS Learning', 'learning@example.test'), (2, 'SkillWorks Demo', 'training@example.test');
INSERT INTO course_detail(course_id, title, course_description, course_fee, category_id, provider_id, custom_dates_allowed) VALUES
(1, 'Java teamwork workshop', 'Practice reviewing a small Java application with your team.', 0, 1, 1, FALSE),
(2, 'Cloud fundamentals', 'Learn deployment basics, databases and application monitoring.', 400, 2, 2, TRUE),
(3, 'Professional Java certification', 'Prepare for a Java certification assessment.', 300, 3, 2, TRUE);
INSERT INTO course_batch(course_id, course_start_date, course_end_date, training_days, capacity, half_day_period) VALUES
(1, DATE_ADD(CURRENT_DATE, INTERVAL (7 - WEEKDAY(CURRENT_DATE)) DAY), DATE_ADD(CURRENT_DATE, INTERVAL (7 - WEEKDAY(CURRENT_DATE)) DAY), 0.5, 10, 'AM'),
(1, DATE_ADD(CURRENT_DATE, INTERVAL (7 - WEEKDAY(CURRENT_DATE)) DAY), DATE_ADD(CURRENT_DATE, INTERVAL (7 - WEEKDAY(CURRENT_DATE)) DAY), 0.5, 10, 'PM'),
(2, DATE_ADD(CURRENT_DATE, INTERVAL (7 - WEEKDAY(CURRENT_DATE)) DAY), DATE_ADD(CURRENT_DATE, INTERVAL (8 - WEEKDAY(CURRENT_DATE)) DAY), 2, 15, NULL);
