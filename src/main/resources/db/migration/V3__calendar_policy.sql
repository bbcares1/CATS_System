-- One shared row prevents holiday edits during application and schedule validation.
CREATE TABLE training_calendar_policy (id INT PRIMARY KEY);
INSERT INTO training_calendar_policy(id) VALUES (1);
