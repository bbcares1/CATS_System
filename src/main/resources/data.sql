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