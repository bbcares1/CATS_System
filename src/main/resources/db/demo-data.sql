-- Fixed fictitious data for the 11-12 October 2026 classroom demo.
-- MySQL loads this only when creating an empty development volume.
-- Ordinary restarts preserve changes; reset-demo explicitly restores this starting point.

-- All passwords are demo123. Root Managers review each other rather than themselves.
INSERT INTO users(user_id, user_name, password, name, staff_id, email, role, designation, manager_id) VALUES
(1, 'admin', 'demo123', 'Alex Admin', 'A001', 'admin@example.test', 'ADMIN', NULL, NULL),
(2, 'manager', 'demo123', 'Morgan Manager', 'M001', 'manager@example.test', 'MANAGER', 'Professional', NULL),
(3, 'manager2', 'demo123', 'Casey Manager', 'M002', 'manager2@example.test', 'MANAGER', 'Professional', NULL),
(4, 'staff', 'demo123', 'Sam Staff', 'S001', 'staff@example.test', 'STAFF', 'Professional', 2),
(5, 'staff2', 'demo123', 'Taylor Staff', 'S002', 'staff2@example.test', 'STAFF', 'Administrative', 2),
(6, 'staff3', 'demo123', 'Jordan Staff', 'S003', 'staff3@example.test', 'STAFF', 'Professional', 3);

-- Annual allowances stay fixed regardless of the installation date.
INSERT INTO training_entitlement(staff_id, `year`, day_limit, budget) VALUES
(2, 2025, 10, 2000),
(3, 2025, 10, 2000),
(4, 2025, 10, 2000),
(5, 2025, 5, 2000),
(6, 2025, 10, 2000),
(2, 2026, 10, 2000),
(3, 2026, 10, 2000),
(4, 2026, 10, 2000),
(5, 2026, 5, 2000),
(6, 2026, 10, 2000),
(2, 2027, 10, 2000),
(3, 2027, 10, 2000),
(4, 2027, 10, 2000),
(5, 2027, 5, 2000),
(6, 2027, 10, 2000);

-- Fictitious providers; no real employee email addresses are used.
INSERT INTO course_provider(provider_id, name, email) VALUES
(1, 'CATS Learning', 'learning@example.test'),
(2, 'SkillWorks Demo', 'training@example.test');

-- Includes the useful three-day course created during local testing.
INSERT INTO course_detail(course_id, title, course_description, course_fee, category_id, provider_id, custom_dates_allowed) VALUES
(1, 'Demo - Java teamwork workshop', 'Practise reviewing a small Java application.', 0, 1, 1, FALSE),
(2, 'Demo - Cloud fundamentals', 'Learn deployment, databases and monitoring.', 400, 2, 2, TRUE),
(3, 'Demo - Professional Java certification', 'Prepare for a Java assessment.', 300, 3, 2, TRUE),
(4, 'Demo - External skills workshop', 'Practise teamwork over three working days.', 500, 2, 1, FALSE);

-- Future dates support both demo days; the November course skips a weekend and public holiday.
INSERT INTO course_batch(batch_id, course_id, course_start_date, course_end_date, training_days, capacity, half_day_period) VALUES
(1, 1, '2026-10-19', '2026-10-19', 0.5, 10, 'AM'),
(2, 1, '2026-10-19', '2026-10-19', 0.5, 10, 'PM'),
(3, 2, '2026-10-26', '2026-10-27', 2, 15, NULL),
(4, 4, '2026-10-26', '2026-10-28', 3, 10, NULL),
(5, 1, '2026-10-26', '2026-10-30', 5, 10, NULL),
(6, 2, '2026-11-06', '2026-11-10', 2, 15, NULL);

-- Singapore 2026 holidays and Sunday replacement days: mom.gov.sg/employment-practices/public-holidays.
INSERT INTO excluded_days(date, description) VALUES
('2026-01-01', 'New Year''s Day'),
('2026-02-17', 'Chinese New Year'),
('2026-02-18', 'Chinese New Year'),
('2026-03-21', 'Hari Raya Puasa'),
('2026-04-03', 'Good Friday'),
('2026-05-01', 'Labour Day'),
('2026-05-27', 'Hari Raya Haji'),
('2026-05-31', 'Vesak Day'),
('2026-06-01', 'Vesak Day observed'),
('2026-08-09', 'National Day'),
('2026-08-10', 'National Day observed'),
('2026-11-08', 'Deepavali'),
('2026-11-09', 'Deepavali observed'),
('2026-12-25', 'Christmas Day');

-- Sam has every status. The only Approved course ended on 9 October, with no comments or claim.
INSERT INTO course_application(course_id, staff_id, approval_manager_id, reviewer_id, course_title, course_category, training_provider, course_start_date, course_end_date, course_fee, training_days, half_day_period, status, justification, work_dissemination, decision_reason, experience_comments, submitted_at, updated_at, reviewed_at) VALUES
(101, 4, 2, NULL, 'Demo - Java testing workshop', 'EXTERNAL_COURSE', 'SkillWorks Demo', '2026-10-20', '2026-10-20', 100, 1, NULL, 'APPLIED', 'Write reliable project tests.', 'Taylor covers urgent requests.', NULL, NULL, '2026-10-01 09:00:00', NULL, NULL),
(102, 4, 2, 2, 'Demo - Cloud deployment basics', 'EXTERNAL_COURSE', 'SkillWorks Demo', '2026-10-09', '2026-10-09', 200, 1, NULL, 'APPROVED', 'Prepare our deployment.', 'Share support with the team.', 'Share deployment notes after attending.', NULL, '2026-10-01 10:00:00', NULL, '2026-10-02 10:00:00'),
(103, 4, 2, 2, 'Demo - Java certification preparation', 'PROFESSIONAL_CERTIFICATION', 'SkillWorks Demo', '2026-10-06', '2026-10-06', 300, 1, NULL, 'COMPLETED', 'Improve Java knowledge.', 'No handover needed.', 'Relevant to our project.', 'I attended and shared the Java concepts.', '2026-09-28 09:00:00', '2026-10-06 18:00:00', '2026-10-01 11:00:00'),
(104, 4, 2, NULL, 'Demo - Internal code review', 'INTERNAL_TRAINING', 'CATS Learning', '2026-10-22', '2026-10-22', 0, 0.5, 'AM', 'UPDATED', 'Practise code review.', 'Available after lunch.', NULL, NULL, '2026-10-02 09:00:00', '2026-10-05 09:00:00', NULL),
(105, 4, 2, 2, 'Demo - Industry seminar', 'EXTERNAL_COURSE', 'SkillWorks Demo', '2026-10-23', '2026-10-23', 250, 1, NULL, 'REJECTED', 'Explore another field.', 'No handover.', 'Choose training relevant to your current role.', NULL, '2026-10-01 12:00:00', NULL, '2026-10-02 12:00:00'),
(106, 4, 2, NULL, 'Demo - Presentation workshop', 'EXTERNAL_COURSE', 'SkillWorks Demo', '2026-10-23', '2026-10-23', 50, 1, NULL, 'DELETED', 'Improve presentations.', 'No handover.', NULL, NULL, '2026-10-03 09:00:00', '2026-10-05 10:00:00', NULL),
(107, 4, 2, 2, 'Demo - Communication workshop', 'EXTERNAL_COURSE', 'SkillWorks Demo', '2026-10-26', '2026-10-26', 50, 1, NULL, 'CANCELLED', 'Improve communication.', 'No handover.', 'Approved before the employee cancelled.', NULL, '2026-10-01 13:00:00', '2026-10-08 09:00:00', '2026-10-02 13:00:00'),
(108, 5, 2, NULL, 'Demo - Team reporting', 'EXTERNAL_COURSE', 'SkillWorks Demo', '2026-10-20', '2026-10-20', 100, 1, NULL, 'APPLIED', 'Improve reporting.', 'Sam covers the inbox.', NULL, NULL, '2026-10-02 14:00:00', NULL, NULL),
(109, 2, 3, NULL, 'Demo - Manager mentoring', 'INTERNAL_TRAINING', 'CATS Learning', '2026-10-23', '2026-10-23', 0, 1, NULL, 'APPLIED', 'Improve mentoring.', 'Casey covers urgent decisions.', NULL, NULL, '2026-10-02 15:00:00', NULL, NULL),
(110, 5, 2, 2, 'Demo - Spreadsheet reporting', 'EXTERNAL_COURSE', 'SkillWorks Demo', '2026-10-05', '2026-10-05', 150, 1, NULL, 'COMPLETED', 'Improve spreadsheets.', 'No handover.', 'Approved for reporting work.', 'I attended and shared a reporting guide.', '2026-09-25 09:00:00', '2026-10-05 18:00:00', '2026-09-28 09:00:00'),
(111, 6, 3, 3, 'Demo - Daily work skills', 'EXTERNAL_COURSE', 'SkillWorks Demo', '2026-10-07', '2026-10-07', 100, 1, NULL, 'COMPLETED', 'Improve daily work.', 'No handover.', 'Share a short summary after attending.', 'I attended and shared a daily work checklist.', '2026-09-29 09:00:00', '2026-10-07 18:00:00', '2026-10-01 09:00:00');

-- Pending, approved/unpaid and paid claims. Payments do not deduct the budget again.
-- Short classroom TXT samples keep the receipt and certificate contents readable.
INSERT INTO course_fee_application(
    application_id, staff_id, course_application_id, approval_manager_id, reviewer_id,
    reimbursed_by_id, amount, application_status, receipt, certificate,
    receipt_file_name, certificate_file_name, receipt_content_type, certificate_content_type,
    submitted_at, reviewed_at, reimbursed_at, payment_reference, decision_reason
) VALUES
(101, 4, 103, 2, NULL, NULL, 300, 'APPLIED',
 'Classroom sample receipt: Sam Staff (S001), Demo - Java certification preparation, SkillWorks Demo, 6 October 2026, SGD 300 paid personally.',
 'Classroom sample certificate: Sam Staff (S001) completed Demo - Java certification preparation, SkillWorks Demo, 6 October 2026. Simulated attendance.',
 'demo-101-receipt.txt', 'demo-101-certificate.txt', 'text/plain', 'text/plain',
 '2026-10-07 09:00:00', NULL, NULL, NULL, NULL),
(102, 5, 110, 2, 2, 1, 150, 'APPROVED',
 'Classroom sample receipt: Taylor Staff (S002), Demo - Spreadsheet reporting, SkillWorks Demo, 5 October 2026, SGD 150 paid personally.',
 'Classroom sample certificate: Taylor Staff (S002) completed Demo - Spreadsheet reporting, SkillWorks Demo, 5 October 2026. Simulated attendance.',
 'demo-102-receipt.txt', 'demo-102-certificate.txt', 'text/plain', 'text/plain',
 '2026-10-06 09:00:00', '2026-10-07 09:00:00', '2026-10-08 09:00:00', 'DEMO-PAID-001', 'Receipt and attendance checked.'),
(103, 6, 111, 3, 3, NULL, 100, 'APPROVED',
 'Classroom sample receipt: Jordan Staff (S003), Demo - Daily work skills, SkillWorks Demo, 7 October 2026, SGD 100 paid personally.',
 'Classroom sample certificate: Jordan Staff (S003) completed Demo - Daily work skills, SkillWorks Demo, 7 October 2026. Simulated attendance.',
 'demo-103-receipt.txt', 'demo-103-certificate.txt', 'text/plain', 'text/plain',
 '2026-10-08 09:00:00', '2026-10-09 09:00:00', NULL, NULL, 'Receipt and attendance checked.');
