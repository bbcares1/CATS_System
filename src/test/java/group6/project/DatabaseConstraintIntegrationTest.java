package group6.project;

import static org.junit.jupiter.api.Assertions.*;

import group6.project.model.*;
import group6.project.repo.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@SpringBootTest
@Transactional
class DatabaseConstraintIntegrationTest {
    @Autowired JdbcTemplate database;
    @Autowired UserRepo users;
    @Autowired TrainingEntitlementRepo allowances;
    @Autowired CourseDetailRepo courses;
    @Autowired CourseApplicationRepo applications;

    // Direct database writes cannot bypass the annual half-day, year and money limits.
    @Test
    void annualValuesCannotBeNegativeOrFractionalOutsideHalfDays() {
        var staff = employee("constraint_staff");
        var row = new TrainingEntitlement(2026);
        row.setStaff(staff);
        row.setDayLimit(5d);
        row.setBudget(new BigDecimal("1000"));
        row = allowances.saveAndFlush(row);
        Integer id = row.getId();
        for (String value :
                java.util.List.of("day_limit=-1", "day_limit=0.25", "budget=-1", "year=1999")) {
            String constraint =
                    value.startsWith("day_limit")
                            ? "ck_entitlement_days"
                            : value.startsWith("budget")
                                    ? "ck_entitlement_budget"
                                    : "ck_entitlement_year";
            rejected(
                    () ->
                            database.update(
                                    "update training_entitlement set "
                                            + value.replace("year=", "`year`=")
                                            + " where id=?",
                                    id),
                    constraint);
        }
        assertEquals(
                5d,
                database.queryForObject(
                        "select day_limit from training_entitlement where id=?", Double.class, id));
    }

    // Both money checks and existing foreign keys protect catalogue data.
    @Test
    void catalogueMoneyAndForeignKeysAreEnforced() {
        var course = new CourseDetail();
        course.setTitle("Constraint fixture");
        course.setCourseFee(new BigDecimal("100"));
        course = courses.saveAndFlush(course);
        Integer id = course.getCourseId();
        rejected(
                () ->
                        database.update(
                                "update course_detail set course_fee=-1 where course_id=?", id),
                "ck_catalogue_fee");
        rejected(
                () ->
                        database.update(
                                "update course_detail set category_id=2147483647 where course_id=?",
                                id),
                "fk_catalogue_category");
    }

    // Persisted application snapshots retain valid periods and half-day values.
    @Test
    void applicationDatesAndTrainingDaysAreEnforced() {
        var staff = employee("snapshot_constraint");
        var course = new CourseApplication();
        course.setApplicant(staff);
        course.setCourseTitle("Snapshot fixture");
        course.setCourseStartDate(java.time.LocalDate.of(2026, 11, 2));
        course.setCourseEndDate(course.getCourseStartDate());
        course.setTrainingDays(1d);
        course = applications.saveAndFlush(course);
        Integer id = course.getCourseId();
        rejected(
                () ->
                        database.update(
                                "update course_application set training_days=0.25 where"
                                    + " course_id=?",
                                id),
                "ck_application_days");
        rejected(
                () ->
                        database.update(
                                "update course_application set course_end_date='2026-11-01' where"
                                    + " course_id=?",
                                id),
                "ck_application_period");
        rejected(
                () ->
                        database.update(
                                "update course_application set course_end_date='2027-01-01' where"
                                    + " course_id=?",
                                id),
                "ck_application_period");
    }

    // H2 and MySQL translate CHECK errors differently; both must name the expected constraint.
    private void rejected(Runnable write, String constraint) {
        var failure = assertThrows(org.springframework.dao.DataAccessException.class, write::run);
        assertTrue(
                failure.getMessage().toLowerCase(java.util.Locale.ROOT).contains(constraint),
                failure.getMessage());
    }

    // Isolated fixtures do not depend on or change development samples.
    private Staff employee(String name) {
        var staff = new Staff();
        staff.setUserName(name);
        staff.setStaffId(name);
        staff.setName(name);
        staff.setPassword("test");
        return users.saveAndFlush(staff);
    }
}
