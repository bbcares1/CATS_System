-- Match the session service's bounds; missing legacy capacities still require import review.
ALTER TABLE course_batch ADD CONSTRAINT ck_batch_capacity_range CHECK (capacity IS NULL OR capacity BETWEEN 1 AND 100000);
