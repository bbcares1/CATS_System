-- V2 gave the three business categories stable IDs; their editable labels do not define rules.
-- Repair renamed labels without changing the already published V6 migration.
UPDATE course_category SET kind=NULL;
UPDATE course_category SET kind=CASE category_id
    WHEN 1 THEN 'INTERNAL_TRAINING'
    WHEN 2 THEN 'EXTERNAL_COURSE'
    WHEN 3 THEN 'PROFESSIONAL_CERTIFICATION'
    ELSE NULL END;
