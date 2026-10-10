-- Schema and reference categories for a fresh CATS installation.
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    designation VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    email VARCHAR(255) UNIQUE,
    staff_id VARCHAR(255) UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    manager_id INT,
    CONSTRAINT fk_user_manager FOREIGN KEY (manager_id) REFERENCES users(user_id)
);
CREATE TABLE training_entitlement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    `year` INT NOT NULL,
    staff_id INT NOT NULL,
    day_limit DOUBLE NOT NULL DEFAULT 0,
    budget DECIMAL(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT uk_entitlement_staff_year UNIQUE (staff_id, `year`),
    CONSTRAINT fk_entitlement_staff FOREIGN KEY (staff_id) REFERENCES users(user_id)
);
CREATE TABLE excluded_days (
    id INT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE course_category (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(255) UNIQUE,
    kind VARCHAR(40) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE course_provider (
    provider_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    website VARCHAR(255),
    email VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE course_detail (
    course_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    course_fee DECIMAL(12,2) NOT NULL,
    course_description VARCHAR(2000),
    category_id INT NOT NULL,
    provider_id INT NOT NULL,
    custom_dates_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_course_provider FOREIGN KEY (provider_id) REFERENCES course_provider(provider_id),
    CONSTRAINT fk_catalogue_category FOREIGN KEY (category_id) REFERENCES course_category(category_id)
);
CREATE TABLE course_batch (
    batch_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id INT,
    course_start_date DATE,
    course_end_date DATE,
    training_days DOUBLE,
    capacity INT,
    half_day_period VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_batch_course FOREIGN KEY (course_id) REFERENCES course_detail(course_id)
);
CREATE TABLE course_application (
    course_id INT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    reviewer_id INT,
    approval_manager_id INT,
    staff_id INT NOT NULL,
    course_title VARCHAR(255),
    course_category VARCHAR(40),
    training_provider VARCHAR(255),
    course_start_date DATE,
    course_end_date DATE,
    course_fee DECIMAL(12,2) NOT NULL,
    justification VARCHAR(2000),
    work_dissemination VARCHAR(2000),
    training_days DOUBLE,
    half_day_period VARCHAR(255),
    status VARCHAR(20),
    submitted_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    reviewed_at TIMESTAMP(6),
    decision_reason VARCHAR(2000),
    experience_comments VARCHAR(2000),
    catalogue_course_id INT,
    catalogue_batch_id BIGINT,
    CONSTRAINT fk_application_course FOREIGN KEY (catalogue_course_id) REFERENCES course_detail(course_id),
    CONSTRAINT fk_application_batch FOREIGN KEY (catalogue_batch_id) REFERENCES course_batch(batch_id),
    CONSTRAINT fk_application_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(user_id),
    CONSTRAINT fk_application_manager FOREIGN KEY (approval_manager_id) REFERENCES users(user_id),
    CONSTRAINT fk_application_staff FOREIGN KEY (staff_id) REFERENCES users(user_id)
);
CREATE INDEX ix_application_staff_status ON course_application(staff_id, status);
CREATE INDEX ix_application_dates ON course_application(course_start_date, course_end_date);
CREATE TABLE course_fee_application (
    application_id INT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    approval_manager_id INT,
    reviewer_id INT,
    reimbursed_by_id INT,
    amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    payment_reference VARCHAR(255),
    staff_id INT,
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
    decision_reason VARCHAR(2000),
    CONSTRAINT fk_claim_manager FOREIGN KEY (approval_manager_id) REFERENCES users(user_id),
    CONSTRAINT fk_claim_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(user_id),
    CONSTRAINT fk_claim_payer FOREIGN KEY (reimbursed_by_id) REFERENCES users(user_id),
    CONSTRAINT fk_claim_staff FOREIGN KEY (staff_id) REFERENCES users(user_id),
    CONSTRAINT fk_claim_application FOREIGN KEY (course_application_id) REFERENCES course_application(course_id)
);

-- Shared row used when validating course dates and public holidays.
CREATE TABLE training_calendar_policy (id INT PRIMARY KEY);
INSERT INTO training_calendar_policy(id) VALUES (1);

-- Reference categories are present in every environment; these are not demo users.
INSERT INTO course_category(category_id, category_name, kind) VALUES
(1, 'Internal Training', 'INTERNAL_TRAINING'),
(2, 'External Course', 'EXTERNAL_COURSE'),
(3, 'Professional Certification', 'PROFESSIONAL_CERTIFICATION');
