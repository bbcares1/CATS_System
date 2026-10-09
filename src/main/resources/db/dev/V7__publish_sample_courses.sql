-- Publish only the known fictional development offers; preserve any manually added records.
UPDATE course_detail SET training_provider='NUS-ISS',active=TRUE WHERE course_id=1 AND title='Java web applications';
UPDATE course_detail SET training_provider='ISS',active=TRUE,custom_dates_allowed=TRUE WHERE course_id=2 AND title='Workplace presentation skills';
UPDATE course_detail SET training_provider='Cloud Academy',active=TRUE WHERE course_id=3 AND title='Cloud professional certification';
-- Link sample applications without changing their saved title, fee or dates.
UPDATE course_application SET catalogue_course_id=1 WHERE course_title='Java web applications' AND staff_id=4;
UPDATE course_application SET catalogue_course_id=2 WHERE course_title='Workplace presentation skills' AND staff_id=5;
UPDATE course_application SET catalogue_course_id=3 WHERE course_title='Cloud professional certification' AND staff_id=6;
