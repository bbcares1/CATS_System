-- Preserve the existing limits as annual records before removing the shared defaults.
ALTER TABLE training_entitlement ADD COLUMN day_limit DOUBLE NOT NULL DEFAULT 0;
ALTER TABLE training_entitlement ADD COLUMN budget DECIMAL(12,2) NOT NULL DEFAULT 0;
UPDATE training_entitlement SET
    day_limit = (SELECT COALESCE(s.training_days,0) FROM staff s WHERE s.user_id = training_entitlement.staff_id),
    budget = (SELECT COALESCE(s.training_budget,0) FROM staff s WHERE s.user_id = training_entitlement.staff_id);
INSERT INTO training_entitlement (`year`,staff_id,day_limit,budget)
SELECT EXTRACT(YEAR FROM CURRENT_DATE),s.user_id,COALESCE(s.training_days,0),COALESCE(s.training_budget,0)
FROM staff s WHERE NOT EXISTS (
    SELECT 1 FROM training_entitlement e WHERE e.staff_id=s.user_id AND e.`year`=EXTRACT(YEAR FROM CURRENT_DATE));
ALTER TABLE training_entitlement MODIFY COLUMN staff_id INT NOT NULL;
ALTER TABLE staff DROP COLUMN training_budget;
ALTER TABLE staff DROP COLUMN training_days;
ALTER TABLE course_application MODIFY COLUMN course_fee DECIMAL(12,2) NOT NULL;
UPDATE course_detail SET course_fee=0 WHERE course_fee IS NULL;
ALTER TABLE course_detail MODIFY COLUMN course_fee DECIMAL(12,2) NOT NULL;
ALTER TABLE course_application MODIFY COLUMN justification VARCHAR(2000);
ALTER TABLE course_application MODIFY COLUMN work_dissemination VARCHAR(2000);
ALTER TABLE course_application MODIFY COLUMN experience_comments VARCHAR(2000);
ALTER TABLE course_application MODIFY COLUMN decision_reason VARCHAR(2000);
