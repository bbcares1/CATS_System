-- 插入测试员工额度数据 (Staff 表)
INSERT INTO staff (staff_id, training_budget, training_days) VALUES 
('S1001', 3000.00, 10),
('S1002', 2500.00, 8),
('S1003', 4500.00, 12),
('S1004', 1800.00, 5),
('S1005', 5000.00, 15);

-- 插入系统用户数据 (User 表 - 包含 Admin/Manager/Staff 账号)
INSERT INTO user (dtype, name, user_name, staff_no, designation) VALUES 
('Admin', 'Alice Wong', 'admin_alice', 'A001', 'System Administrator'),
('Manager', 'Bob Tan', 'mgr_bob', 'M001', 'Department Manager'),
('User', 'Charlie Lee', 'staff_charlie', 'S1001', 'Software Engineer'),
('User', 'Diana Chen', 'staff_diana', 'S1002', 'QA Analyst');


--- 1. 插入排除日期 (ExcludedDays) 测试数据
INSERT INTO excluded_days (date) VALUES ('2026-10-01');
INSERT INTO excluded_days (date) VALUES ('2026-10-02');
INSERT INTO excluded_days (date) VALUES ('2026-12-25');

-- 1. Insert Course Categories
INSERT INTO course_category (category_id, category_name) VALUES (1, 'Internal Training');
INSERT INTO course_category (category_id, category_name) VALUES (2, 'External Course');
INSERT INTO course_category (category_id, category_name) VALUES (3, 'Professional Certification');

-- 2. Insert Course Details
INSERT INTO course_detail (title, course_fee, course_description, category_id) 
VALUES ('Hands-on Java Spring Boot', 500.00, 'In-depth guide to Spring Boot framework and practical application development.', 1);

INSERT INTO course_detail (title, course_fee, course_description, category_id) 
VALUES ('AWS Solutions Architect Certification', 1200.00, 'Official AWS training for Cloud Solutions Architect certification exam preparation.', 3);

INSERT INTO course_detail (title, course_fee, course_description, category_id) 
VALUES ('Agile Project Management Workshop', 850.00, 'Interactive external workshop covering Scrum practices and Agile methodologies.', 2);

-- 3. 插入员工 (Staff) 测试数据
INSERT INTO staff (staff_id, training_budget, training_days) VALUES ('S1001', 2000.0, 10);
INSERT INTO staff (staff_id, training_budget, training_days) VALUES ('S1002', 1500.0, 7);
INSERT INTO staff (staff_id, training_budget, training_days) VALUES ('S1003', 3000.0, 14);