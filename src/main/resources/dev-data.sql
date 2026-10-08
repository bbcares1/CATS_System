-- User subclasses share "user", identified by dtype.
INSERT INTO "user" (dtype, name, user_name, staff_no, designation, role) VALUES
('Admin', 'Alice Wong', 'admin_alice', 'A001', 'System Administrator', 'Admin'),
('Manager', 'Bob Tan', 'mgr_bob', 'M001', 'Department Manager', 'Manager');

INSERT INTO "user" (dtype, name, user_name, staff_id, training_budget, training_days, manager_id, role) VALUES
('Staff', 'Charlie Lee', 'staff_charlie', 'S1001', 3000, 10, (SELECT user_id FROM "user" WHERE user_name = 'mgr_bob'), 'Staff'),
('Staff', 'Diana Chen', 'staff_diana', 'S1002', 2500, 8, (SELECT user_id FROM "user" WHERE user_name = 'mgr_bob'), 'Staff'),
('Staff', 'Evan Lim', 'staff_evan', 'S1003', 4500, 12, (SELECT user_id FROM "user" WHERE user_name = 'mgr_bob'), 'Staff'),
('Staff', 'Fiona Wu', 'staff_fiona', 'S1004', 1800, 5, (SELECT user_id FROM "user" WHERE user_name = 'mgr_bob'), 'Staff'),
('Staff', 'Grace Tan', 'staff_grace', 'S1005', 5000, 15, (SELECT user_id FROM "user" WHERE user_name = 'mgr_bob'), 'Staff');

INSERT INTO excluded_days (date, description) VALUES
('2026-10-01', 'Public Holiday'), ('2026-10-02', 'Public Holiday'),
('2026-12-25', 'Christmas Day');

-- 1. Insert Course Categories
INSERT INTO course_category (category_name) VALUES ( 'Internal Training');
INSERT INTO course_category (category_name) VALUES ( 'External Course');
INSERT INTO course_category (category_name) VALUES ( 'Professional Certification');

-- 2. Insert Course Details
INSERT INTO course_detail (title, course_fee, course_description, category_id) 
VALUES ('Hands-on Java Spring Boot', 500.00, 'In-depth guide to Spring Boot framework and practical application development.', 1);

INSERT INTO course_detail (title, course_fee, course_description, category_id) 
VALUES ('AWS Solutions Architect Certification', 1200.00, 'Official AWS training for Cloud Solutions Architect certification exam preparation.', 3);

INSERT INTO course_detail (title, course_fee, course_description, category_id) 
VALUES ('Agile Project Management Workshop', 850.00, 'Interactive external workshop covering Scrum practices and Agile methodologies.', 2);

