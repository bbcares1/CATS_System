// We check application decisions and invalid state changes.
package group6.project.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import group6.project.form.CourseApplicationForm;
import group6.project.model.*;
import group6.project.repo.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CourseApplicationServiceTest {
    @Mock CourseApplicationRepo applications;
    @Mock TrainingEntitlementService entitlements;
    @Mock UserRepo users;
    @Mock ExcludedDaysRepo holidays;
    @Mock TrainingCalendarPolicyRepo calendar;
    @Mock CourseDetailRepo courses;
    @Mock CourseBatchRepo batches;
    @Mock ApprovalRoutingService routing;
    @InjectMocks CourseApplicationService service;
    Staff staff;
    Manager manager;

    @BeforeEach
    void setup() {
        staff = new Staff();
        staff.setUserId(7);
        manager = new Manager();
        manager.setUserId(1);
        staff.setManager(manager);
        when(users.reportingManagerId(7)).thenReturn(Optional.of(1));
        when(users.lockParticipants(List.of(1, 7))).thenReturn(List.of(manager, staff));
        when(calendar.readCalendar()).thenReturn(Optional.of(new TrainingCalendarPolicy()));
    }

    @Test
    void internalHalfDayIsFreeAndConsumesHalfDay() {
        CourseApplicationForm form = valid(CourseCategoryType.INTERNAL_TRAINING);
        form.setHalfDayPeriod("AM");
        when(routing.resolveReviewer(staff, null)).thenReturn(manager);
        allowance();
        when(applications.save(any())).thenAnswer(call -> call.getArgument(0));
        var saved = service.createOther(form, staff);
        assertEquals(0, saved.getCourseFee().signum());
        assertEquals(0.5, saved.getTrainingDays());
        assertEquals(manager, saved.getApprovalManager());
    }

    @ParameterizedTest
    @EnumSource(
            value = DayOfWeek.class,
            names = {"SATURDAY", "SUNDAY"})
    void weekendStartDatesAreRejected(DayOfWeek weekendDay) {
        var form = valid(CourseCategoryType.EXTERNAL_COURSE);
        form.setCourseStartDate(LocalDate.now().with(TemporalAdjusters.next(weekendDay)));
        form.setCourseEndDate(form.getCourseStartDate());
        when(routing.resolveReviewer(staff, null)).thenReturn(manager);
        var error =
                assertThrows(ResponseStatusException.class, () -> service.createOther(form, staff));
        assertEquals("Start and end dates must be working days.", error.getReason());
        verify(applications, never()).save(any());
    }

    @Test
    void holidayStartDatesAreRejected() {
        var form = valid(CourseCategoryType.EXTERNAL_COURSE);
        var holiday = new ExcludedDays();
        holiday.setDate(form.getCourseStartDate());
        when(holidays.findAll()).thenReturn(List.of(holiday));
        when(routing.resolveReviewer(staff, null)).thenReturn(manager);
        var error =
                assertThrows(ResponseStatusException.class, () -> service.createOther(form, staff));
        assertEquals("Start and end dates must be working days.", error.getReason());
        verify(applications, never()).save(any());
    }

    @Test
    void negativeFeesAreRejected() {
        var form = valid(CourseCategoryType.EXTERNAL_COURSE);
        form.setCourseFee(new BigDecimal("-1"));
        when(routing.resolveReviewer(staff, null)).thenReturn(manager);
        assertThrows(ResponseStatusException.class, () -> service.createOther(form, staff));
        verify(applications, never()).save(any());
    }

    @Test
    void sameApplicationIsExcludedWhenUpdating() {
        var saved = new CourseApplication();
        saved.setCourseId(10);
        saved.setVersion(0L);
        saved.setApplicant(staff);
        when(applications.applicantId(10)).thenReturn(Optional.of(7));
        when(applications.lockById(10)).thenReturn(Optional.of(saved));
        allowance();
        var form = valid(CourseCategoryType.EXTERNAL_COURSE);
        form.setVersion(0L);
        assertEquals(ApplicationStatus.UPDATED, service.updateOther(10, form, staff).getStatus());
        verify(entitlements).summary(staff, form.getCourseStartDate().getYear(), 10);
    }

    private void allowance() {
        when(entitlements.summary(eq(staff), anyInt(), nullable(Integer.class)))
                .thenReturn(
                        new TrainingEntitlementService.AnnualSummary(
                                5, new BigDecimal("1000"), 0, BigDecimal.ZERO, 0, BigDecimal.ZERO));
    }

    private CourseApplicationForm valid(CourseCategoryType category) {
        var form = new CourseApplicationForm();
        form.setCourseTitle("Effective Java");
        form.setTrainingProvider("Demo provider");
        form.setCourseCategory(category);
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek().getValue() > 5) date = date.plusDays(1);
        form.setCourseStartDate(date);
        form.setCourseEndDate(date);
        form.setCourseFee(new BigDecimal("100"));
        form.setJustification("Improve delivery quality");
        return form;
    }
}
