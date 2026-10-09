-- Older claims referenced a batch rather than an application; use the available catalogue fee and verify it before recording payment.
UPDATE course_fee_application c SET amount = COALESCE(
    (SELECT d.course_fee FROM course_batch b JOIN course_detail d ON d.course_id=b.course_id WHERE b.batch_id=c.batch_id),0)
WHERE c.course_application_id IS NULL AND c.batch_id IS NOT NULL AND c.amount=0;
