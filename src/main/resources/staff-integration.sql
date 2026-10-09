-- Staff integration: Run once on the existing group6 tables in MySQL Workbench.
-- Manual setup: Skip existing columns. This file does not run automatically.
USE group6;

-- Reporting manager: Link each staff member to a manager.
ALTER TABLE staff
    ADD COLUMN manager_id INT NULL,
    ADD CONSTRAINT fk_staff_reporting_manager FOREIGN KEY (manager_id)
        REFERENCES manager (user_id);

-- Manager assignment: Use actual staff and manager user IDs, for example:
-- UPDATE staff SET manager_id = 12 WHERE user_id = 25;
-- Manager assignment: The manager ID must exist in the manager table.
-- Manager assignment: NULL means no manager has been assigned yet.
-- Fee claims: Link claims to completed courses. Existing batch claims may keep NULL.
ALTER TABLE course_fee_application
    ADD COLUMN course_application_id INT NULL,
    ADD COLUMN reimbursed_at DATETIME(6) NULL,
    ADD CONSTRAINT uk_staff_claim_course UNIQUE (course_application_id),
    ADD CONSTRAINT fk_staff_claim_course FOREIGN KEY (course_application_id)
        REFERENCES course_application (course_id);

-- Claim documents: Keep existing documents and allow PDFs up to 5 MB.
ALTER TABLE course_fee_application
    MODIFY COLUMN receipt MEDIUMBLOB,
    MODIFY COLUMN certificate MEDIUMBLOB;
