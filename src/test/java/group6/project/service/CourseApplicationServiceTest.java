package group6.project.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
    @Mock group6.project.repo.TrainingCalendarPolicyRepo calendar;
    @Mock ApprovalRoutingService routing;
    @Mock CourseApplicationRepo applicationRepo;
    @Mock TrainingEntitlementRepo entitlementRepo;
    @Mock ExcludedDaysRepo excludedDaysRepo;
    @Mock group6.project.repo.StaffRepo employees;

    private CourseApplicationService service;
    private Staff staff;

    @BeforeEach
    void setUp() {
        service = new CourseApplicationService(applicationRepo, entitlementRepo, excludedDaysRepo, employees, routing, calendar);
        lenient().when(calendar.readCalendar()).thenReturn(Optional.of(new group6.project.model.TrainingCalendarPolicy()));
        staff = new Staff();
        staff.setUserId(7);
        var allowance = new group6.project.model.TrainingEntitlement(LocalDate.now().getYear());
        allowance.setDayLimit(5d);
        allowance.setBudget(new java.math.BigDecimal("1000"));
        lenient().when(employees.lockById(any())).thenReturn(Optional.of(staff));
        lenient().when(entitlementRepo.findByStaff_UserIdAndYear(any(), any())).thenReturn(Optional.of(allowance));
        lenient().when(applicationRepo.findByApplicant_UserIdAndStatusIn(any(), any())).thenReturn(java.util.List.of());
        lenient().when(excludedDaysRepo.existsByDate(any())).thenReturn(false);
        lenient().when(applicationRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // The write-time identity check also covers an account disabled after the route's session check.
    @Test
    void disabledOrChangedAccountCannotSubmitAnApplication() {
        staff.setActive(false);
        assertThrows(IllegalArgumentException.class, () -> service.create(valid(CourseCategoryType.EXTERNAL_COURSE), staff));
        staff.setActive(true); staff.setVersion(0L);
        Staff current = new Staff(); current.setUserId(7); current.setVersion(1L);
        when(employees.lockById(7)).thenReturn(Optional.of(current));
        assertThrows(IllegalArgumentException.class, () -> service.create(valid(CourseCategoryType.EXTERNAL_COURSE), staff));
        verify(applicationRepo, never()).save(any());
    }

    @Test
    void internalHalfDayIsFreeAndConsumesHalfDay() {
        CourseApplication application = valid(CourseCategoryType.INTERNAL_TRAINING);
        application.setHalfDayPeriod("AM");

        CourseApplication saved = service.create(application, staff);

        assertEquals(0, saved.getCourseFee().signum());
        assertEquals(0.5, saved.getTrainingDays());
    }

    @ParameterizedTest
    @EnumSource(value = DayOfWeek.class, names = {"SATURDAY", "SUNDAY"})
    void weekendStartDatesAreRejected(DayOfWeek weekendDay) {
        CourseApplication application = valid(CourseCategoryType.EXTERNAL_COURSE);
        LocalDate date = LocalDate.now().with(TemporalAdjusters.next(weekendDay));
        application.setCourseStartDate(date);
        application.setCourseEndDate(date);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.create(application, staff));

        assertEquals("Start and end dates must be working days.", error.getMessage());
        verify(applicationRepo, never()).save(any());
    }

    @Test
    void holidayStartDatesAreRejected() {
        CourseApplication application = valid(CourseCategoryType.EXTERNAL_COURSE);
        when(excludedDaysRepo.existsByDate(application.getCourseStartDate())).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.create(application, staff));

        assertEquals("Start and end dates must be working days.", error.getMessage());
        verify(applicationRepo, never()).save(any());
    }

    @Test
    void negativeFeesAreRejected() {
        CourseApplication application = valid(CourseCategoryType.EXTERNAL_COURSE);
        application.setCourseFee(new java.math.BigDecimal("-1"));

        assertThrows(IllegalArgumentException.class, () -> service.create(application, staff));
    }

    @Test
    void sameApplicationIsExcludedWhenUpdating() {
        CourseApplication existing = valid(CourseCategoryType.EXTERNAL_COURSE);
        existing.setCourseId(10);
        existing.setVersion(0L);
        existing.setApplicant(staff);
        existing.setStatus(group6.project.model.ApplicationStatus.UPDATED);
        when(applicationRepo.findById(10)).thenReturn(Optional.of(existing));

        CourseApplication edit = valid(CourseCategoryType.EXTERNAL_COURSE);
        edit.setCourseFee(new java.math.BigDecimal("100"));
        edit.setVersion(existing.getVersion());
        assertEquals(group6.project.model.ApplicationStatus.UPDATED,
                service.update(10, edit, staff).getStatus());
    }

    // Completed reservations must be included before accepting another course.
    @Test
    void completedCoursesUseAllowanceAndDecimalFeesAreExact() {
        CourseApplication first = valid(CourseCategoryType.EXTERNAL_COURSE);
        first.setCourseId(11); first.setStatus(group6.project.model.ApplicationStatus.COMPLETED);
        first.setTrainingDays(2d); first.setCourseFee(new java.math.BigDecimal("0.10"));
        CourseApplication second = valid(CourseCategoryType.EXTERNAL_COURSE);
        second.setCourseId(12); second.setStatus(group6.project.model.ApplicationStatus.COMPLETED);
        second.setTrainingDays(3d); second.setCourseFee(new java.math.BigDecimal("0.20"));
        when(applicationRepo.findByApplicant_UserIdAndStatusIn(any(), any())).thenReturn(java.util.List.of(first, second));
        var summary = service.summaryForYear(staff, first.getCourseStartDate().getYear(), null);
        assertEquals(0d, summary.remainingDays());
        assertEquals(new java.math.BigDecimal("0.30"), summary.usedBudget());
        assertThrows(IllegalArgumentException.class, () -> service.create(valid(CourseCategoryType.EXTERNAL_COURSE), staff));
        verify(applicationRepo, never()).save(any());
    }

    // A missing annual allocation cannot inherit another year's allowance.
    @Test
    void missingYearHasNoAllowance() {
        when(entitlementRepo.findByStaff_UserIdAndYear(any(), any())).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.create(valid(CourseCategoryType.EXTERNAL_COURSE), staff));
        verify(applicationRepo, never()).save(any());
    }

    // Half-day periods apply to one date, avoiding an ambiguous multi-day duration.
    @Test
    void multiDayHalfSessionIsRejected() {
        CourseApplication form = valid(CourseCategoryType.INTERNAL_TRAINING);
        form.setHalfDayPeriod("AM");
        form.setCourseEndDate(form.getCourseStartDate().plusWeeks(1));
        assertThrows(IllegalArgumentException.class, () -> service.create(form, staff));
        verify(applicationRepo, never()).save(any());
    }

    // Cross-year periods must be split so every request belongs to one allowance.
    @Test
    void crossYearDatesAreRejected() {
        CourseApplication form = valid(CourseCategoryType.EXTERNAL_COURSE);
        int year = LocalDate.now().getYear()+1;
        form.setCourseStartDate(LocalDate.of(year,12,30));
        form.setCourseEndDate(LocalDate.of(year+1,1,2));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.create(form, staff));
        assertEquals("A course must be within one calendar year.", error.getMessage());
    }

    // Bound entity fields are never trusted to set identity, status or an existing review.
    @Test
    void createCopiesOnlyEditableDetails() {
        CourseApplication form = valid(CourseCategoryType.EXTERNAL_COURSE);
        form.setCourseId(900); form.setDecisionReason("Forged approval");
        form.setStatus(group6.project.model.ApplicationStatus.APPROVED);
        CourseApplication saved = service.create(form, staff);
        assertNull(saved.getCourseId());
        assertNull(saved.getDecisionReason());
        assertEquals(group6.project.model.ApplicationStatus.APPLIED, saved.getStatus());
        assertSame(staff, saved.getApplicant());
    }

    // Completion on the end date is too early, even through the shared service.
    @Test
    void completionWaitsUntilTheDayAfterEndDate() {
        CourseApplication course = valid(CourseCategoryType.EXTERNAL_COURSE);
        course.setApplicant(staff); course.setStatus(group6.project.model.ApplicationStatus.APPROVED);
        course.setCourseEndDate(LocalDate.now());
        when(applicationRepo.findById(11)).thenReturn(Optional.of(course));
        assertThrows(IllegalStateException.class, () -> service.complete(11, "Useful course", staff));
        verify(applicationRepo, never()).save(any());
    }

    private CourseApplication valid(CourseCategoryType category) {
        CourseApplication application = new CourseApplication();
        application.setCourseTitle("Effective Java");
        application.setTrainingProvider("NUS-ISS");
        application.setCourseCategory(category);
        application.setCourseStartDate(nextWorkingDay());
        application.setCourseEndDate(application.getCourseStartDate());
        application.setCourseFee(new java.math.BigDecimal("100"));
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
