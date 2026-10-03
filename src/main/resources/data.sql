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

-- 2. 插入课程目录 (CourseDetail) 测试数据
-- 注意：列名为 course_category，且枚举字符串为 InternalTraining / ExternalCourse / ProfessionalCertification
INSERT INTO course_detail (title, course_category, course_fee, course_description) 
VALUES ('Company Orientation & Agile', 'InternalTraining', 0.0, '公司内部新员工敏捷流程培训');

INSERT INTO course_detail (title, course_category, course_fee, course_description) 
VALUES ('Spring Boot & Cloud Masterclass', 'ExternalCourse', 850.0, '外部 Spring Boot 实战与微服务进阶课程');

INSERT INTO course_detail (title, course_category, course_fee, course_description) 
VALUES ('AWS Certified Solutions Architect', 'ProfessionalCertification', 1200.0, 'AWS 架构师专业认证考试与考前辅导');

-- 3. 插入员工 (Staff) 测试数据
INSERT INTO staff (staff_id, training_budget, training_days) VALUES ('S1001', 2000.0, 10);
INSERT INTO staff (staff_id, training_budget, training_days) VALUES ('S1002', 1500.0, 7);
INSERT INTO staff (staff_id, training_budget, training_days) VALUES ('S1003', 3000.0, 14);