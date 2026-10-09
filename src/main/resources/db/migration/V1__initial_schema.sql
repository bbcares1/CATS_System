-- One database identity is shared by the JOINED account tables.
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    designation VARCHAR(255),
    role VARCHAR(20) NOT NULL
);
CREATE TABLE staff (
    user_id INT PRIMARY KEY,
    staff_id VARCHAR(255) UNIQUE,
    training_budget DOUBLE,
    training_days INT,
    manager_id INT,
    CONSTRAINT fk_staff_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);
CREATE TABLE manager (
    user_id INT PRIMARY KEY,
    CONSTRAINT fk_manager_staff FOREIGN KEY (user_id) REFERENCES staff(user_id)
);
ALTER TABLE staff ADD CONSTRAINT fk_staff_manager FOREIGN KEY (manager_id) REFERENCES manager(user_id);
CREATE TABLE admin (
    user_id INT PRIMARY KEY,
    staff_no VARCHAR(255),
    CONSTRAINT fk_admin_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);
CREATE TABLE training_entitlement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    `year` INT NOT NULL,
    staff_id INT,
    CONSTRAINT uk_entitlement_staff_year UNIQUE (staff_id, `year`),
    CONSTRAINT fk_entitlement_staff FOREIGN KEY (staff_id) REFERENCES staff(user_id)
);
CREATE TABLE excluded_days (
    id INT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
);
CREATE TABLE approvalhierarchy (
    hierarchy_id INT AUTO_INCREMENT PRIMARY KEY,
    `level` INT,
    approval_role VARCHAR(20)
);
CREATE TABLE course_category (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(255) UNIQUE
);
CREATE TABLE course_detail (
    course_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    course_fee DOUBLE,
    course_description VARCHAR(255),
    category_id INT,
    CONSTRAINT fk_catalogue_category FOREIGN KEY (category_id) REFERENCES course_category(category_id)
);
CREATE TABLE course_batch (
    batch_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id INT,
    course_start_date DATE,
    course_end_date DATE,
    capacity INT,
    CONSTRAINT fk_batch_course FOREIGN KEY (course_id) REFERENCES course_detail(course_id)
);
CREATE TABLE course_application (
    course_id INT AUTO_INCREMENT PRIMARY KEY,
    staff_id INT NOT NULL,
    course_title VARCHAR(255),
    course_category VARCHAR(40),
    training_provider VARCHAR(255),
    course_start_date DATE,
    course_end_date DATE,
    course_fee DOUBLE NOT NULL,
    justification VARCHAR(255),
    work_dissemination VARCHAR(255),
    training_days DOUBLE,
    half_day_period VARCHAR(255),
    status VARCHAR(20),
    submitted_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    reviewed_at TIMESTAMP(6),
    decision_reason VARCHAR(255),
    experience_comments VARCHAR(255),
    CONSTRAINT fk_application_staff FOREIGN KEY (staff_id) REFERENCES staff(user_id)
);
CREATE INDEX ix_application_staff_status ON course_application(staff_id, status);
CREATE INDEX ix_application_dates ON course_application(course_start_date, course_end_date);
CREATE TABLE course_fee_application (
    application_id INT AUTO_INCREMENT PRIMARY KEY,
    staff_id INT,
    batch_id BIGINT,
    course_application_id INT UNIQUE,
    application_status VARCHAR(20) NOT NULL,
    receipt MEDIUMBLOB,
    certificate MEDIUMBLOB,
    receipt_file_name VARCHAR(255),
    receipt_content_type VARCHAR(255),
    certificate_file_name VARCHAR(255),
    certificate_content_type VARCHAR(255),
    submitted_at TIMESTAMP(6),
    reviewed_at TIMESTAMP(6),
    reimbursed_at TIMESTAMP(6),
    decision_reason VARCHAR(255),
    CONSTRAINT fk_claim_staff FOREIGN KEY (staff_id) REFERENCES staff(user_id),
    CONSTRAINT fk_claim_batch FOREIGN KEY (batch_id) REFERENCES course_batch(batch_id),
    CONSTRAINT fk_claim_application FOREIGN KEY (course_application_id) REFERENCES course_application(course_id)
);
