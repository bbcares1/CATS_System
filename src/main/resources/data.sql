-- =========================================================
-- data.sql
-- =========================================================


-- =========================================================
-- 1. USERS
-- User 是父类
--
-- User
-- ├── Admin
-- └── Staff
--      └── Manager
--
-- user_id:
-- 1 = Alice   (Admin)
-- 2 = Bob     (Manager)
-- 3 = Charlie (Staff)
-- 4 = Diana   (Staff)
-- =========================================================

INSERT INTO users
    (user_id, user_name, password, name, designation, role)
VALUES
    (1, 'admin_alice', 'password', 'Alice Wong',
     'System Administrator', 'ADMIN'),

    (2, 'mgr_bob', 'password', 'Bob Tan',
     'Department Manager', 'MANAGER'),

    (3, 'staff_charlie', 'password', 'Charlie Lee',
     'Software Engineer', 'STAFF'),

    (4, 'staff_diana', 'password', 'Diana Chen',
     'QA Analyst', 'STAFF');


-- =========================================================
-- 2. STAFF
--
-- Staff extends User
--
-- Bob 是 Manager，但是 Manager extends Staff，
-- 所以 Bob 也必须存在于 staff table。
--
-- Alice 是 Admin extends User，
-- 所以 Alice 不属于 staff table。
-- =========================================================

INSERT INTO staff
    (user_id, staff_id, training_budget, training_days)
VALUES
    (2, 'M001', 4500.00, 12),
    (3, 'S1001', 3000.00, 10),
    (4, 'S1002', 2500.00, 8);


-- =========================================================
-- 3. MANAGER
--
-- Manager extends Staff
--
-- Bob:
-- users.user_id = 2
-- staff.user_id = 2
-- manager.user_id = 2
-- =========================================================

INSERT INTO manager (user_id)
VALUES (2);


-- =========================================================
-- 4. ADMIN
--
-- Admin extends User
--
-- Alice:
-- users.user_id = 1
-- admin.user_id = 1
-- =========================================================

INSERT INTO admin
    (user_id, staff_no)
VALUES
    (1, 'A001');


-- =========================================================
-- 5. EXCLUDED DAYS
-- =========================================================

INSERT INTO excluded_days (date, description)
VALUES ('2026-10-01', 'Public Holiday');

INSERT INTO excluded_days (date, description)
VALUES ('2026-10-02', 'Public Holiday');

INSERT INTO excluded_days (date, description)
VALUES ('2026-12-25', 'Christmas Day');


-- =========================================================
-- 6. COURSE CATEGORIES
-- =========================================================

INSERT INTO course_category
    (category_id, category_name)
VALUES
    (1, 'Internal Training');

INSERT INTO course_category
    (category_id, category_name)
VALUES
    (2, 'External Course');

INSERT INTO course_category
    (category_id, category_name)
VALUES
    (3, 'Professional Certification');


-- =========================================================
-- 7. COURSE DETAILS
-- =========================================================

INSERT INTO course_detail
    (title, course_fee, course_description, category_id)
VALUES
    (
        'Hands-on Java Spring Boot',
        500.00,
        'In-depth guide to Spring Boot framework and practical application development.',
        1
    );

INSERT INTO course_detail
    (title, course_fee, course_description, category_id)
VALUES
    (
        'AWS Solutions Architect Certification',
        1200.00,
        'Official AWS training for Cloud Solutions Architect certification exam preparation.',
        3
    );

INSERT INTO course_detail
    (title, course_fee, course_description, category_id)
VALUES
    (
        'Agile Project Management Workshop',
        850.00,
        'Interactive external workshop covering Scrum practices and Agile methodologies.',
        2
    );