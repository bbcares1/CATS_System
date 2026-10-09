-- Keep legacy nullable snapshots for explicit import review, while rejecting impossible values.
ALTER TABLE training_entitlement ADD CONSTRAINT ck_entitlement_year CHECK (`year` BETWEEN 2000 AND 2100);
ALTER TABLE training_entitlement ADD CONSTRAINT ck_entitlement_days CHECK (day_limit BETWEEN 0 AND 366 AND day_limit * 2 = FLOOR(day_limit * 2));
ALTER TABLE training_entitlement ADD CONSTRAINT ck_entitlement_budget CHECK (budget >= 0);
ALTER TABLE course_detail ADD CONSTRAINT ck_catalogue_fee CHECK (course_fee >= 0);
ALTER TABLE course_application ADD CONSTRAINT ck_application_fee CHECK (course_fee >= 0);
ALTER TABLE course_application ADD CONSTRAINT ck_application_days CHECK (training_days IS NULL OR (training_days > 0 AND training_days <= 366 AND training_days * 2 = FLOOR(training_days * 2)));
ALTER TABLE course_application ADD CONSTRAINT ck_application_period CHECK (course_start_date IS NULL OR course_end_date IS NULL OR (course_end_date >= course_start_date AND EXTRACT(YEAR FROM course_start_date) = EXTRACT(YEAR FROM course_end_date)));
ALTER TABLE course_batch ADD CONSTRAINT ck_batch_capacity CHECK (capacity IS NULL OR capacity >= 0);
