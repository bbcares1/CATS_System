-- Fictional accounts for local development only. Every password is demo123.
INSERT INTO users (user_id, user_name, password, name, designation, role) VALUES
(1, 'admin_alice', 'demo123', 'Alice Wong', 'Administrator', 'ADMIN'),
(2, 'mgr_bob', 'demo123', 'Bob Tan', 'Department manager', 'MANAGER'),
(3, 'mgr_chris', 'demo123', 'Chris Lee', 'Department manager', 'MANAGER'),
(4, 'staff_alex', 'demo123', 'Alex Tan', 'Professional', 'STAFF'),
(5, 'staff_sam', 'demo123', 'Sam Lim', 'Administrative', 'STAFF'),
(6, 'staff_pat', 'demo123', 'Pat Wong', 'Professional', 'STAFF');
INSERT INTO admin (user_id, staff_no) VALUES (1, 'A001');
INSERT INTO staff (user_id, staff_id, training_budget, training_days) VALUES
(2, 'M001', 4000, 15), (3, 'M002', 4000, 15),
(4, 'S001', 2000, 10), (5, 'S002', 1500, 5), (6, 'S003', 2000, 10);
INSERT INTO manager (user_id) VALUES (2), (3);
-- Assign managers after both parent and subtype records exist.
UPDATE staff SET manager_id = 2 WHERE user_id IN (4, 5);
UPDATE staff SET manager_id = 3 WHERE user_id = 6;
INSERT INTO training_entitlement (`year`, staff_id) VALUES
(2026, 2), (2026, 3), (2026, 4), (2026, 5), (2026, 6);
INSERT INTO excluded_days (date, description) VALUES ('2026-12-25', 'Christmas Day');
INSERT INTO course_detail (course_id, title, course_fee, course_description, category_id) VALUES
(1, 'Java web applications', 600, 'Build and test a Spring MVC application.', 2),
(2, 'Workplace presentation skills', 0, 'A half-day internal workshop.', 1),
(3, 'Cloud professional certification', 900, 'Prepare for professional certification.', 3);
INSERT INTO course_batch (course_id, course_start_date, course_end_date, capacity) VALUES
(1, '2026-11-02', '2026-11-03', 20),
(2, '2026-11-09', '2026-11-09', 15),
(3, '2026-11-16', '2026-11-17', 12);
INSERT INTO course_application
(staff_id, course_title, course_category, training_provider, course_start_date, course_end_date,
 course_fee, training_days, half_day_period, status, justification, work_dissemination,
 decision_reason, experience_comments) VALUES
(4, 'Java web applications', 'EXTERNAL_COURSE', 'NUS-ISS', '2026-11-02', '2026-11-03', 600, 2, NULL,
 'APPLIED', 'Improve the applications our team maintains.', 'Sam will cover urgent requests.', NULL, NULL),
(5, 'Workplace presentation skills', 'INTERNAL_TRAINING', 'ISS', '2026-11-09', '2026-11-09', 0, 0.5, 'AM',
 'UPDATED', 'Present project results more clearly.', NULL, NULL, NULL),
(6, 'Cloud professional certification', 'PROFESSIONAL_CERTIFICATION', 'Cloud Academy', '2026-11-16', '2026-11-17', 900, 2, NULL,
 'APPLIED', 'Support the upcoming cloud project.', NULL, NULL, NULL),
(4, 'Software testing', 'EXTERNAL_COURSE', 'NUS-ISS', '2026-11-23', '2026-11-23', 300, 1, NULL,
 'APPROVED', 'Improve our automated tests.', NULL, 'Share the exercises with the team.', NULL),
(2, 'Team leadership', 'EXTERNAL_COURSE', 'NUS-ISS', '2026-09-18', '2026-09-18', 300, 1, NULL,
 'COMPLETED', 'Improve feedback and planning.', NULL, 'Relevant to the manager role.', 'Useful feedback exercises.'),
(5, 'Advanced certification', 'PROFESSIONAL_CERTIFICATION', 'Training Centre', '2026-09-21', '2026-09-21', 1200, 1, NULL,
 'REJECTED', 'Learn a new platform.', NULL, 'This is outside the current role.', NULL),
(4, 'Conference workshop', 'EXTERNAL_COURSE', 'Training Centre', '2026-09-22', '2026-09-22', 200, 1, NULL,
 'CANCELLED', 'Review industry practices.', NULL, 'Share the notes after attending.', NULL),
(5, 'Internal introduction', 'INTERNAL_TRAINING', 'ISS', '2026-09-23', '2026-09-23', 0, 1, NULL,
 'DELETED', 'Understand the new team process.', NULL, NULL, NULL);
