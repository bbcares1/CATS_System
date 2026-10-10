package group6.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @Mock group6.project.repo.StaffRepo employees;
    @Mock CourseApplicationRepo applicationRepo;
    @Mock TrainingEntitlementRepo entitlementRepo;
    @Mock ExcludedDaysRepo excludedDaysRepo;

    private CourseApplicationService service;
    private Staff staff;

    @BeforeEach
    void setUp() {
        service = new CourseApplicationService(applicationRepo,
                new TrainingEntitlementService(entitlementRepo, employees, applicationRepo), excludedDaysRepo, employees);
        staff = new Staff();
        staff.setUserId(7);
        var allowance = new group6.project.model.TrainingEntitlement(LocalDate.now().getYear());
        allowance.setStaff(staff); allowance.setDayLimit(5d); allowance.setBudget(new java.math.BigDecimal("1000"));
        lenient().when(employees.lockById(7)).thenReturn(Optional.of(staff));
        lenient().when(entitlementRepo.findByStaff_UserIdAndYear(any(), any())).thenReturn(Optional.of(allowance));
        lenient().when(applicationRepo.findByApplicant_UserIdAndStatusIn(any(), any())).thenReturn(java.util.List.of());
        lenient().when(excludedDaysRepo.existsByDate(any())).thenReturn(false);
        lenient().when(applicationRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
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
        var holiday = new group6.project.model.ExcludedDays(); holiday.setDate(application.getCourseStartDate());
        when(excludedDaysRepo.findAll()).thenReturn(java.util.List.of(holiday));

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
        existing.setApplicant(staff);
        existing.setStatus(group6.project.model.ApplicationStatus.UPDATED);
        when(applicationRepo.findById(10)).thenReturn(Optional.of(existing));

        CourseApplication edit = valid(CourseCategoryType.EXTERNAL_COURSE);
        edit.setCourseFee(new java.math.BigDecimal("100"));
        assertEquals(group6.project.model.ApplicationStatus.UPDATED,
                service.update(10, edit, staff).getStatus());
    }

    private CourseApplication valid(CourseCategoryType category) {
        CourseApplication application = new CourseApplication();
        application.setCourseTitle("Effective Java");
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
