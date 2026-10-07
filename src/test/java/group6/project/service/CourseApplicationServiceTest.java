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
        application.setCourseStartDate(LocalDate.now().plusDays(1));
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
