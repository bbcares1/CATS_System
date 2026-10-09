-- Preserve mandatory category meaning even when Admin changes the display label.
ALTER TABLE course_category ADD COLUMN kind VARCHAR(40);
UPDATE course_category SET kind = CASE category_name
    WHEN 'Internal Training' THEN 'INTERNAL_TRAINING'
    WHEN 'External Course' THEN 'EXTERNAL_COURSE'
    WHEN 'Professional Certification' THEN 'PROFESSIONAL_CERTIFICATION'
    ELSE NULL END;
ALTER TABLE course_category ADD CONSTRAINT uk_category_kind UNIQUE (kind);
-- Existing offers need provider information before publication; no application snapshot is changed.
ALTER TABLE course_detail ADD COLUMN training_provider VARCHAR(255);
ALTER TABLE course_detail ADD COLUMN custom_dates_allowed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE course_detail ADD COLUMN active BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE course_detail ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE course_detail MODIFY COLUMN course_description VARCHAR(2000);
ALTER TABLE course_batch ADD COLUMN half_day_period VARCHAR(255);
ALTER TABLE course_batch ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE course_batch ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE course_application ADD COLUMN catalogue_course_id INT;
ALTER TABLE course_application ADD COLUMN catalogue_batch_id BIGINT;
ALTER TABLE course_application ADD CONSTRAINT fk_application_catalogue FOREIGN KEY (catalogue_course_id) REFERENCES course_detail(course_id);
ALTER TABLE course_application ADD CONSTRAINT fk_application_batch FOREIGN KEY (catalogue_batch_id) REFERENCES course_batch(batch_id);
