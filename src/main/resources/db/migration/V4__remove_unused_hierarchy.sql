-- Actual approvals use users.manager_id and each application's assigned reviewer.
DROP TABLE approvalhierarchy;
ALTER TABLE excluded_days ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE course_fee_application DROP FOREIGN KEY fk_claim_batch;
ALTER TABLE course_fee_application DROP COLUMN batch_id;
