CREATE TABLE training_calendar_policy (id INT PRIMARY KEY, CONSTRAINT ck_calendar_policy CHECK(id=1));
INSERT INTO training_calendar_policy(id) VALUES(1);
ALTER TABLE excluded_days ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE INDEX ix_application_schedule ON course_application(status,course_start_date,course_end_date);
CREATE INDEX ix_application_reviewer_status ON course_application(approval_manager_id,status);
CREATE INDEX ix_claim_reviewer_status ON course_fee_application(approval_manager_id,application_status);
