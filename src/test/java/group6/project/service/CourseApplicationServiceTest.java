package group6.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import group6.project.model.CourseApplication;
import group6.project.model.CourseCategoryType;
import group6.project.model.Staff;
import group6.project.repo.CourseApplicationRepo;
import group6.project.repo.ExcludedDaysRepo;
import group6.project.repo.TrainingEntitlementRepo;

@ExtendWith(MockitoExtension.class)
class CourseApplicationServiceTest {
    @Mock CourseApplicationRepo applicationRepo;
    @Mock TrainingEntitlementRepo entitlementRepo;
    @Mock ExcludedDaysRepo excludedDaysRepo;

    private CourseApplicationService service;
    private Staff staff;

    @BeforeEach
    void setUp() {
        service = new CourseApplicationService(applicationRepo, entitlementRepo, excludedDaysRepo);
        staff = new Staff();
        staff.setUserId(7);
        staff.setTrainingDays(5);
        staff.setTrainingBudget(1000d);
        lenient().when(entitlementRepo.findByStaff_UserIdAndYear(any(), any())).thenReturn(Optional.empty());
        lenient().when(applicationRepo.findByApplicant_UserIdAndStatusIn(any(), any())).thenReturn(java.util.List.of());
        lenient().when(excludedDaysRepo.existsByDate(any())).thenReturn(false);
        lenient().when(applicationRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void internalHalfDayIsFreeAndConsumesHalfDay() {
        CourseApplication application = valid(CourseCategoryType.INTERNAL_TRAINING);
        application.setHalfDayPeriod("AM");

        CourseApplication saved = service.create(application, staff);

        assertEquals(0, saved.getCourseFee());
        assertEquals(0.5, saved.getTrainingDays());
    }

    @Test
    void weekendAndHolidayAreNotWorkingStartDates() {
        CourseApplication application = valid(CourseCategoryType.EXTERNAL_COURSE);
        application.setCourseStartDate(nextWorkingDay());
        application.setCourseEndDate(application.getCourseStartDate());
        when(excludedDaysRepo.existsByDate(application.getCourseStartDate())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.create(application, staff));
    }

    @Test
    void negativeFeesAreRejected() {
        CourseApplication application = valid(CourseCategoryType.EXTERNAL_COURSE);
        application.setCourseFee(-1);

        assertThrows(IllegalArgumentException.class, () -> service.create(application, staff));
    }

    @Test
    void sameApplicationIsExcludedWhenUpdating() {
        CourseApplication existing = valid(CourseCategoryType.EXTERNAL_COURSE);
        existing.setCourseId(10);
        existing.setApplicant(staff);
        existing.setStatus(group6.project.model.ApplicationStatus.UPDATED);
        when(applicationRepo.findById(10)).thenReturn(Optional.of(existing));

        CourseApplication edit = valid(CourseCategoryType.EXTERNAL_COURSE);
        edit.setCourseFee(100);
        assertEquals(group6.project.model.ApplicationStatus.UPDATED,
                service.update(10, edit, staff).getStatus());
    }

    @Test
    void managerCanApproveDirectReportAndRecordsReviewer() {
        var manager = new group6.project.model.Manager();
        manager.setUserId(20);
        staff.setManager(manager);
        CourseApplication application = valid(CourseCategoryType.EXTERNAL_COURSE);
        application.setApplicant(staff);
        when(applicationRepo.findById(10)).thenReturn(Optional.of(application));

        var saved = service.review(10, manager, true, "Approved");
        assertEquals(group6.project.model.ApplicationStatus.APPROVED, saved.getStatus());
        assertEquals(manager, saved.getReviewedBy());
        org.junit.jupiter.api.Assertions.assertNotNull(saved.getReviewedAt());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.review(10, manager, true, null));
    }

    @Test
    void otherManagerCannotReviewOrReadEmployeeHistory() {
        var assigned = new group6.project.model.Manager();
        assigned.setUserId(20);
        staff.setManager(assigned);
        var other = new group6.project.model.Manager();
        other.setUserId(21);
        CourseApplication application = valid(CourseCategoryType.EXTERNAL_COURSE);
        application.setApplicant(staff);
        when(applicationRepo.findById(10)).thenReturn(Optional.of(application));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.review(10, other, true, null));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.employeeCourseHistory(staff, other));
        org.mockito.Mockito.verify(applicationRepo, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void rejectionRequiresReasonAndRecordsDecision() {
        var manager = new group6.project.model.Manager();
        manager.setUserId(20);
        staff.setManager(manager);
        CourseApplication application = valid(CourseCategoryType.EXTERNAL_COURSE);
        application.setApplicant(staff);
        when(applicationRepo.findById(10)).thenReturn(Optional.of(application));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.review(10, manager, false, " "));
        var saved = service.review(10, manager, false, " Budget unavailable ");
        assertEquals(group6.project.model.ApplicationStatus.REJECTED, saved.getStatus());
        assertEquals("Budget unavailable", saved.getDecisionReason());
    }

    @Test
    void completedCoursesStillConsumeAnnualAllowance() {
        var completed = valid(CourseCategoryType.EXTERNAL_COURSE);
        completed.setCourseId(1);
        completed.setTrainingDays(5d);
        completed.setCourseFee(1000);
        completed.setStatus(group6.project.model.ApplicationStatus.COMPLETED);
        when(applicationRepo.findByApplicant_UserIdAndStatusIn(any(), any())).thenAnswer(invocation -> {
            java.util.List<group6.project.model.ApplicationStatus> statuses = invocation.getArgument(1);
            return statuses.contains(completed.getStatus()) ? java.util.List.of(completed) : java.util.List.of();
        });
        var summary = service.summary(new CourseApplication(), staff, null);
        // Use the course year, independent of whether the next working day crosses New Year.
        var preview = valid(CourseCategoryType.EXTERNAL_COURSE);
        summary = service.summary(preview, staff, null);
        assertEquals(0, summary.remainingDays());
        assertEquals(0, summary.remainingBudget());
        assertThrows(IllegalArgumentException.class, () -> service.create(preview, staff));
    }

    @Test
    void fullDayCannotOverlapHalfDayEvenWhenFullDayValueIsBlank() {
        var morning = valid(CourseCategoryType.INTERNAL_TRAINING);
        morning.setHalfDayPeriod("AM");
        morning.setCourseId(1);
        morning.setTrainingDays(0.5);
        when(applicationRepo.findByApplicant_UserIdAndStatusIn(any(), any())).thenReturn(java.util.List.of(morning));
        var fullDay = valid(CourseCategoryType.INTERNAL_TRAINING);
        fullDay.setHalfDayPeriod("");
        assertThrows(IllegalArgumentException.class, () -> service.create(fullDay, staff));
    }

    @Test
    void oppositeHalfDaysCanShareOneDate() {
        var morning = valid(CourseCategoryType.INTERNAL_TRAINING);
        morning.setHalfDayPeriod("AM");
        morning.setCourseId(1);
        morning.setTrainingDays(0.5);
        when(applicationRepo.findByApplicant_UserIdAndStatusIn(any(), any())).thenReturn(java.util.List.of(morning));
        var afternoon = valid(CourseCategoryType.INTERNAL_TRAINING);
        afternoon.setHalfDayPeriod("PM");
        assertEquals(0.5, service.create(afternoon, staff).getTrainingDays());
    }

    @Test
    void multiDayHalfSessionsAreRejected() {
        var form = valid(CourseCategoryType.INTERNAL_TRAINING);
        form.setHalfDayPeriod("AM");
        form.setCourseEndDate(form.getCourseStartDate().plusDays(7));
        assertThrows(IllegalArgumentException.class, () -> service.create(form, staff));
    }

    @Test
    void summaryDoesNotRequireTitleOrJustification() {
        var form = valid(CourseCategoryType.EXTERNAL_COURSE);
        form.setCourseTitle(null);
        form.setJustification(null);
        assertEquals(1, service.summary(form, staff, null).requestedDays());
    }

    @Test
    void reversedDatesDoNotProduceNegativePreview() {
        var form = valid(CourseCategoryType.INTERNAL_TRAINING);
        form.setCourseEndDate(form.getCourseStartDate().minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> service.summary(form, staff, null));
    }

    @Test
    void summaryCannotExcludeAnotherEmployeesApplication() {
        var application = valid(CourseCategoryType.EXTERNAL_COURSE);
        var other = new Staff();
        other.setUserId(99);
        application.setApplicant(other);
        when(applicationRepo.findById(1)).thenReturn(Optional.of(application));
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.summary(new CourseApplication(), staff, 1));
        assertEquals(403, error.getStatusCode().value());
    }

    @Test
    void nonFiniteFeesCannotBypassBudgetValidation() {
        for (double fee : new double[] {Double.NaN, Double.POSITIVE_INFINITY}) {
            var form = valid(CourseCategoryType.EXTERNAL_COURSE);
            form.setCourseFee(fee);
            assertThrows(IllegalArgumentException.class, () -> service.create(form, staff));
        }
    }

    @Test
    void invalidHalfDayValuesAreRejectedForExternalCourses() {
        var form = valid(CourseCategoryType.EXTERNAL_COURSE);
        form.setHalfDayPeriod("invalid");
        assertThrows(IllegalArgumentException.class, () -> service.create(form, staff));
    }

    @Test
    void approvalRechecksAllowanceAfterBudgetChanges() {
        var manager = new group6.project.model.Manager();
        manager.setUserId(20);
        staff.setManager(manager);
        staff.setTrainingBudget(50d);
        var application = valid(CourseCategoryType.EXTERNAL_COURSE);
        application.setCourseId(10);
        application.setApplicant(staff);
        when(applicationRepo.findById(10)).thenReturn(Optional.of(application));
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.review(10, manager, true, null));
        assertEquals(409, error.getStatusCode().value());
        assertEquals(group6.project.model.ApplicationStatus.APPLIED, application.getStatus());
    }

    private CourseApplication valid(CourseCategoryType category) {
        CourseApplication application = new CourseApplication();
        application.setCourseTitle("Effective Java");
        application.setCourseCategory(category);
        application.setCourseStartDate(nextWorkingDay());
        application.setCourseEndDate(application.getCourseStartDate());
        application.setCourseFee(100);
        application.setJustification("Improve delivery quality");
        return application;
    }

    private LocalDate nextWorkingDay() {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek().getValue() > 5) {
            date = date.plusDays(1);
        }
        return date;
    }
}
