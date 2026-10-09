ALTER TABLE course_application ADD COLUMN approval_manager_id INT;
UPDATE course_application a SET approval_manager_id = (SELECT u.manager_id FROM users u WHERE u.user_id=a.staff_id);
ALTER TABLE course_application ADD CONSTRAINT fk_application_approval_manager FOREIGN KEY (approval_manager_id) REFERENCES users(user_id);
ALTER TABLE course_application ADD CONSTRAINT ck_application_not_self_review CHECK (approval_manager_id IS NULL OR approval_manager_id <> staff_id);

ALTER TABLE course_fee_application ADD COLUMN approval_manager_id INT;
ALTER TABLE course_fee_application ADD COLUMN reviewer_id INT;
ALTER TABLE course_fee_application ADD COLUMN reimbursed_by_id INT;
ALTER TABLE course_fee_application ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE course_fee_application ADD COLUMN amount DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE course_fee_application ADD COLUMN payment_reference VARCHAR(255);
ALTER TABLE course_fee_application MODIFY decision_reason VARCHAR(2000);
UPDATE course_fee_application c SET approval_manager_id = (SELECT u.manager_id FROM users u WHERE u.user_id=c.staff_id);
UPDATE course_fee_application c SET amount = COALESCE((SELECT a.course_fee FROM course_application a WHERE a.course_id=c.course_application_id),0);
ALTER TABLE course_fee_application ADD CONSTRAINT fk_claim_approval_manager FOREIGN KEY (approval_manager_id) REFERENCES users(user_id);
ALTER TABLE course_fee_application ADD CONSTRAINT fk_claim_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(user_id);
ALTER TABLE course_fee_application ADD CONSTRAINT fk_claim_payment_admin FOREIGN KEY (reimbursed_by_id) REFERENCES users(user_id);
ALTER TABLE course_fee_application ADD CONSTRAINT ck_claim_not_self_review CHECK (approval_manager_id IS NULL OR approval_manager_id <> staff_id);
ALTER TABLE course_fee_application ADD CONSTRAINT ck_claim_amount CHECK (amount >= 0);
