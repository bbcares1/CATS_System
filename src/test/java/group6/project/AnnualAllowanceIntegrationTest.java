package group6.project;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import group6.project.model.*;
import group6.project.repo.*;
import group6.project.service.*;

@SpringBootTest
@Transactional
class AnnualAllowanceIntegrationTest {
    @Autowired StaffRepo employees;
    @Autowired CourseApplicationRepo applications;
    @Autowired TrainingEntitlementRepo entitlements;
    @Autowired CourseApplicationService policy;
    @Autowired TrainingEntitlementService allowances;
    Staff staff;
    int year;
    LocalDate start;

    // Use real annual records and repository status filters, with transaction rollback after each test.
    @BeforeEach
    void prepare() {
        staff = new Staff();
        staff.setUserName("annual_fixture"); staff.setPassword("test"); staff.setName("Annual fixture");
        staff.setStaffId("ANNUAL"); staff.setRole(Roles.STAFF);
        Manager reportingManager = new Manager();
        reportingManager.setUserName("review_" + java.util.UUID.randomUUID()); reportingManager.setStaffId("REVIEW_" + java.util.UUID.randomUUID());
        reportingManager.setName("Review fixture"); reportingManager.setPassword("test"); reportingManager.setRole(Roles.MANAGER);
        reportingManager = employees.saveAndFlush(reportingManager);
        staff.setManager(reportingManager);
        staff = employees.saveAndFlush(staff);
        start = LocalDate.now().plusDays(7);
        while (start.getDayOfWeek().getValue()>5) start=start.plusDays(1);
        year=start.getYear();
        allowances.saveLimits(staff.getUserId(), year, 5, new BigDecimal("1000.00"));
    }

    // Preserve completed use, release withdrawn requests and exclude another year's records.
    @Test
    void statusFiltersAndCalendarYearsUseOneTotal() {
        for (ApplicationStatus status : ApplicationStatus.values()) {
            CourseApplication course = form();
            course.setApplicant(staff); course.setStatus(status); course.setTrainingDays(0.5);
            course.setCourseFee(new BigDecimal("0.10"));
            applications.saveAndFlush(course);
        }
        var total=policy.summaryForYear(staff,year,null);
        assertEquals(2d,total.usedDays());
        assertEquals(new BigDecimal("0.40"),total.usedBudget());
        assertEquals(0d,policy.summaryForYear(staff,year+1,null).usedDays());
        assertEquals(0d,policy.summaryForYear(staff,year+1,null).dayLimit());
    }

    // Updating a future year's limits must not change any previous year.
    @Test
    void yearlyLimitsAreIndependentAndBelowUseChangesAreRejected() {
        allowances.saveLimits(staff.getUserId(), year+1, 10, new BigDecimal("2000.00"));
        CourseApplication course = form(); course.setApplicant(staff);
        course.setStatus(ApplicationStatus.COMPLETED); course.setTrainingDays(2d);
        applications.saveAndFlush(course);
        assertThrows(IllegalArgumentException.class,
                () -> allowances.saveLimits(staff.getUserId(),year,1,new BigDecimal("1000.00")));
        assertEquals(5d,policy.summaryForYear(staff,year,null).dayLimit());
        assertEquals(10d,policy.summaryForYear(staff,year+1,null).dayLimit());
        assertEquals(2,entitlements.findByStaff_UserId(staff.getUserId()).size());
    }

    // Both legacy routes and Staff pages use the shared lifecycle policy.
    @Test
    void deletingAndCancellingReleaseReservationsWithoutRemovingHistory() {
        CourseApplication created=policy.create(form(),staff);
        assertEquals(1d,policy.summaryForYear(staff,year,null).usedDays());
        policy.delete(created.getCourseId(),staff);
        assertEquals(0d,policy.summaryForYear(staff,year,null).usedDays());
        CourseApplication approved=form(); approved.setApplicant(staff);
        approved.setStatus(ApplicationStatus.APPROVED); approved.setTrainingDays(1d);
        applications.saveAndFlush(approved);
        policy.cancel(approved.getCourseId(),staff);
        assertEquals(0d,policy.summaryForYear(staff,year,null).usedDays());
        assertEquals(2,policy.findForStaffAndYear(staff,year).size());
        assertThrows(IllegalStateException.class,()->policy.cancel(approved.getCourseId(),staff));
    }

    // The form describes only the requested course; the service supplies identity and state.
    private CourseApplication form() {
        CourseApplication course = new CourseApplication();
        course.setCourseTitle("Decimal course"); course.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE);
        course.setTrainingProvider("Training centre"); course.setJustification("Improve skills");
        course.setCourseStartDate(start); course.setCourseEndDate(start);
        course.setCourseFee(new BigDecimal("100.10"));
        return course;
    }
}
