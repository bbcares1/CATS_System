-- Keep the Java hierarchy but store each identity once, so changing role never removes history.
ALTER TABLE users ADD COLUMN staff_id VARCHAR(255);
ALTER TABLE users ADD COLUMN manager_id INT;
ALTER TABLE users ADD COLUMN email VARCHAR(255);
ALTER TABLE users ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
UPDATE users SET staff_id=(SELECT s.staff_id FROM staff s WHERE s.user_id=users.user_id),
    manager_id=(SELECT s.manager_id FROM staff s WHERE s.user_id=users.user_id);
UPDATE users SET staff_id=(SELECT a.staff_no FROM admin a WHERE a.user_id=users.user_id)
    WHERE user_id IN (SELECT user_id FROM admin);
UPDATE users SET role='STAFF' WHERE user_id IN (SELECT user_id FROM staff);
UPDATE users SET role='MANAGER' WHERE user_id IN (SELECT user_id FROM manager);
UPDATE users SET role='ADMIN' WHERE user_id IN (SELECT user_id FROM admin);
ALTER TABLE users ADD CONSTRAINT uk_user_staff_id UNIQUE (staff_id);
ALTER TABLE users ADD CONSTRAINT uk_user_email UNIQUE (email);
ALTER TABLE users ADD CONSTRAINT fk_user_manager FOREIGN KEY (manager_id) REFERENCES users(user_id);
ALTER TABLE users ADD CONSTRAINT ck_user_role CHECK (role IN ('STAFF','MANAGER','ADMIN'));

-- Historical applicant/reviewer links target the identity, regardless of its current subtype.
ALTER TABLE training_entitlement DROP FOREIGN KEY fk_entitlement_staff;
ALTER TABLE training_entitlement ADD CONSTRAINT fk_entitlement_user FOREIGN KEY (staff_id) REFERENCES users(user_id);
ALTER TABLE course_application DROP FOREIGN KEY fk_application_staff;
ALTER TABLE course_application ADD CONSTRAINT fk_application_user FOREIGN KEY (staff_id) REFERENCES users(user_id);
ALTER TABLE course_application DROP FOREIGN KEY fk_application_reviewer;
ALTER TABLE course_application ADD CONSTRAINT fk_application_reviewer_user FOREIGN KEY (reviewer_id) REFERENCES users(user_id);
ALTER TABLE course_fee_application DROP FOREIGN KEY fk_claim_staff;
ALTER TABLE course_fee_application ADD CONSTRAINT fk_claim_user FOREIGN KEY (staff_id) REFERENCES users(user_id);
ALTER TABLE staff DROP FOREIGN KEY fk_staff_manager;
DROP TABLE admin;
DROP TABLE manager;
DROP TABLE staff;
-- Preserve old role-level setup rows for reference; current reporting uses users.manager_id.
ALTER TABLE approvalhierarchy RENAME TO legacy_approvalhierarchy;
