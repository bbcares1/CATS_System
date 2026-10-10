package group6.project;

import static org.junit.jupiter.api.Assertions.*;

import group6.project.model.*;
import group6.project.repo.*;
import group6.project.service.TrainingEntitlementService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootTest
@Transactional
class AnnualAllowanceIntegrationTest {
    @Autowired StaffRepo employees;
    @Autowired CourseApplicationRepo applications;
    @Autowired TrainingEntitlementService allowances;
    Staff employee;
    int year = LocalDate.now().getYear();

    // This employee has different allocations in two years.
    @BeforeEach
    void prepare() {
        employee = new Staff();
        employee.setUserName("annual_employee");
        employee.setName("Annual employee");
        employee.setPassword("demo123");
        employee.setDesignation("Professional");
        employee = employees.saveAndFlush(employee);
        allowances.saveLimits(employee.getUserId(), year, 10, new BigDecimal("2000.00"));
        allowances.saveLimits(employee.getUserId(), year + 1, 5, new BigDecimal("500.00"));
    }

    // Pending reserves funds, completed still consumes them, and inactive statuses release them.
    @Test
    void reservationAndUsageHaveOneAnnualTotal() {
        course(ApplicationStatus.APPLIED, "0.10", 1, year);
        course(ApplicationStatus.UPDATED, "0.20", 0.5, year);
        course(ApplicationStatus.APPROVED, "100.00", 1, year);
        course(ApplicationStatus.COMPLETED, "200.00", 2, year);
        course(ApplicationStatus.REJECTED, "999.00", 20, year);
        course(ApplicationStatus.DELETED, "999.00", 20, year);
        course(ApplicationStatus.CANCELLED, "999.00", 20, year);
        var summary = allowances.summary(employee, year, null);
        assertEquals(1.5, summary.reservedDays());
        assertEquals(new BigDecimal("0.30"), summary.reservedBudget());
        assertEquals(3, summary.usedDays());
        assertEquals(new BigDecimal("300.00"), summary.usedBudget());
        assertEquals(5.5, summary.remainingDays());
        assertEquals(new BigDecimal("1699.70"), summary.remainingBudget());
    }

    // Excluding an edited row does not exclude another year or change its limits.
    @Test
    void editingAndYearsRemainIndependent() {
        CourseApplication saved = course(ApplicationStatus.APPLIED, "100.00", 1, year);
        course(ApplicationStatus.APPROVED, "200.00", 2, year + 1);
        assertEquals(10, allowances.summary(employee, year, saved.getCourseId()).remainingDays());
        assertEquals(3, allowances.summary(employee, year + 1, null).remainingDays());
        assertEquals(0, allowances.summary(employee, year + 2, null).remainingDays());
        assertEquals(0, allowances.summary(employee, year + 2, null).remainingBudget().signum());
    }

    // An Admin cannot lower an allocation underneath pending or used amounts.
    @Test
    void limitReductionsRespectSavedWork() {
        course(ApplicationStatus.APPLIED, "500.00", 4, year);
        var daysError =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                allowances.saveLimits(
                                        employee.getUserId(), year, 3, new BigDecimal("2000.00")));
        assertEquals(400, daysError.getStatusCode().value());
        assertThrows(
                ResponseStatusException.class,
                () ->
                        allowances.saveLimits(
                                employee.getUserId(), year, 10, new BigDecimal("499.99")));
        assertThrows(
                ResponseStatusException.class,
                () ->
                        allowances.saveLimits(
                                employee.getUserId(), year, 1.25, new BigDecimal("2000.00")));
        assertEquals(10, allowances.summary(employee, year, null).dayLimit());
    }

    // Designation defaults are visible suggestions, not silent changes to historical allocations.
    @Test
    void designationDefaultsAreSimpleAndExplicit() {
        assertEquals(5, allowances.suggestedDays("Administrative"));
        assertEquals(10, allowances.suggestedDays("Professional"));
        assertEquals(0, allowances.suggestedDays("Unassigned"));
        assertEquals(10, allowances.summary(employee, year, null).dayLimit());
    }

    // Saved fixtures cover status accounting without calling submission date validation.
    private CourseApplication course(
            ApplicationStatus status, String fee, double days, int courseYear) {
        CourseApplication course = new CourseApplication();
        course.setApplicant(employee);
        course.setStatus(status);
        course.setCourseTitle("Annual accounting example");
        course.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE);
        course.setCourseStartDate(LocalDate.of(courseYear, 3, 1));
        course.setCourseEndDate(LocalDate.of(courseYear, 3, 1));
        course.setCourseFee(new BigDecimal(fee));
        course.setTrainingDays(days);
        return applications.saveAndFlush(course);
    }
}
