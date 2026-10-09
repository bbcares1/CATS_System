-- CATS System - LOCAL TEST DATA ONLY
-- Use with a disposable local database and Hibernate create-drop.
-- Do not commit test credentials or this seed file to the shared main branch.

-- 1. Excluded days: simulated dates for calendar testing
INSERT INTO excluded_days (date, description) VALUES
('2026-10-01', 'Public Holiday'),
('2026-10-02', 'Public Holiday'),
('2026-10-14', 'Company Holiday'),
('2026-10-22', 'Training Centre Closed'),
('2026-10-23', 'Company Holiday'),
('2026-11-05', 'Company Event'),
('2026-11-16', 'Company Holiday'),
('2026-11-17', 'Training Centre Closed'),
('2026-12-24', 'Company Holiday'),
('2026-12-25', 'Christmas Day'),
('2026-12-31', 'Year-End Closure'),
('2027-01-01', 'New Year Closure');

-- 2. Course categories
INSERT INTO course_category (category_id, category_name) VALUES
(1, 'Internal Training'),
(2, 'External Course'),
(3, 'Professional Certification');

-- 3. Course catalogue
INSERT INTO course_detail (title, course_fee, course_description, category_id) VALUES
('Hands-on Java Spring Boot', 500.00, 'In-depth guide to Spring Boot framework and practical application development.', 1),
('AWS Solutions Architect Certification', 1200.00, 'Official AWS training for Cloud Solutions Architect certification exam preparation.', 3),
('Agile Project Management Workshop', 850.00, 'Interactive external workshop covering Scrum practices and Agile methodologies.', 2);

-- 4. Parent user (JOINED inheritance). Non-login test account only.
INSERT INTO users (user_id, user_name, password, name, designation, role, email) VALUES
(1001, 'staff_calendar_test', 'NOT_FOR_LOGIN', 'Calendar Test Staff', 'Software Engineer', 'STAFF', 'calendar-test@example.invalid');

-- 5. Child staff; user_id references users.user_id
INSERT INTO staff (user_id, staff_id, training_budget, training_days) VALUES
(1001, 'S1001', 3000.00, 20);

-- 6. Course applications for Calendar dropdown.
-- Dates below are seed metadata; the calendar recomputes its own dates
-- from the selected requested start date, training_days and excluded days.
INSERT INTO course_application
(course_title, training_days, course_fee, course_start_date, course_end_date, staff_id, status)
VALUES
('Java Training', 15.0, 500.00, '2026-10-12', '2026-11-02', 1001, 'APPLIED'),
('Python Programming', 20.0, 350.00, '2026-10-19', '2026-11-13', 1001, 'APPLIED'),
('AWS Cloud Computing', 10.0, 1200.00, '2026-10-15', '2026-10-28', 1001, 'APPLIED'),
('Spring Boot Development', 12.5, 600.00, '2026-10-20', '2026-11-05', 1001, 'APPLIED'),
('Data Analytics with SQL', 5.0, 450.00, '2026-11-03', '2026-11-09', 1001, 'APPLIED'),
('Cybersecurity Fundamentals', 20.0, 900.00, '2026-10-28', '2026-11-24', 1001, 'APPLIED'),
('Agile Project Management', 7.5, 300.00, '2026-12-21', '2026-12-30', 1001, 'APPLIED');
