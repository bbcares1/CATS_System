-- A version protects decisions made from a page opened before an employee's edit.
ALTER TABLE course_application ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE course_application ADD COLUMN reviewer_id INT;
ALTER TABLE course_application ADD CONSTRAINT fk_application_reviewer FOREIGN KEY (reviewer_id) REFERENCES manager(user_id);
