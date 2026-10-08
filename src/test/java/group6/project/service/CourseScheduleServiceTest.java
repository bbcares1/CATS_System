package group6.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import group6.project.repo.CourseApplicationRepo;

class CourseScheduleServiceTest {

    @Mock
    private ExcludedDaysService excludedDaysService;

    @Mock
    private CourseApplicationRepo courseApplicationRepo;

    private CourseScheduleService courseScheduleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(excludedDaysService.isExcludedDay(any(LocalDate.class)))
                .thenReturn(false);

        courseScheduleService =
                new CourseScheduleService(
                        excludedDaysService,
                        courseApplicationRepo);
    }

    @Test
    void shouldSkipWeekendWhenCalculatingSchedule() {
        CourseScheduleService.Schedule schedule =
                courseScheduleService.calculateSchedule(
                        LocalDate.of(2026, 10, 8),
                        5.0);

        assertEquals(LocalDate.of(2026, 10, 8), schedule.actualStartDate());
        assertEquals(LocalDate.of(2026, 10, 14), schedule.actualEndDate());
    }

    @Test
    void shouldMoveSaturdayStartToMonday() {
        CourseScheduleService.Schedule schedule =
                courseScheduleService.calculateSchedule(
                        LocalDate.of(2026, 10, 10),
                        3.0);

        assertEquals(LocalDate.of(2026, 10, 12), schedule.actualStartDate());
        assertEquals(LocalDate.of(2026, 10, 14), schedule.actualEndDate());
    }

    @Test
    void shouldMoveHolidayStartToNextWorkingDay() {
        LocalDate holiday = LocalDate.of(2026, 10, 12);
        when(excludedDaysService.isExcludedDay(holiday)).thenReturn(true);

        CourseScheduleService.Schedule schedule =
                courseScheduleService.calculateSchedule(holiday, 3.0);

        assertEquals(LocalDate.of(2026, 10, 13), schedule.actualStartDate());
        assertEquals(LocalDate.of(2026, 10, 15), schedule.actualEndDate());
    }

    @Test
    void shouldSkipHolidayDuringCourse() {
        LocalDate holiday = LocalDate.of(2026, 10, 14);
        when(excludedDaysService.isExcludedDay(holiday)).thenReturn(true);

        CourseScheduleService.Schedule schedule =
                courseScheduleService.calculateSchedule(
                        LocalDate.of(2026, 10, 12),
                        4.0);

        assertEquals(LocalDate.of(2026, 10, 12), schedule.actualStartDate());
        assertEquals(LocalDate.of(2026, 10, 16), schedule.actualEndDate());
    }

    @Test
    void shouldAllowSelectedWeekendTraining() {
        LocalDate saturday = LocalDate.of(2026, 10, 10);

        CourseScheduleService.Schedule schedule =
                courseScheduleService.calculateSchedule(
                        saturday,
                        1.0,
                        Set.of(saturday));

        assertEquals(saturday, schedule.actualStartDate());
        assertEquals(saturday, schedule.actualEndDate());
        assertEquals(
                java.util.List.of(saturday),
                courseScheduleService.getTrainingDates(
                        schedule, Set.of(saturday)));
    }

    @Test
    void excludedDayTakesPrecedenceOverSelectedWeekendTraining() {
        LocalDate saturday = LocalDate.of(2026, 10, 10);
        when(excludedDaysService.isExcludedDay(saturday)).thenReturn(true);

        CourseScheduleService.Schedule schedule =
                courseScheduleService.calculateSchedule(
                        saturday,
                        1.0,
                        Set.of(saturday));

        assertEquals(LocalDate.of(2026, 10, 12), schedule.actualStartDate());
        assertEquals(LocalDate.of(2026, 10, 12), schedule.actualEndDate());
        assertEquals(
                java.util.List.of(LocalDate.of(2026, 10, 12)),
                courseScheduleService.getTrainingDates(
                        schedule, Set.of(saturday)));
    }

    @Test
    void shouldCalculateHalfDaySchedule() {
        CourseScheduleService.Schedule schedule =
                courseScheduleService.calculateSchedule(
                        LocalDate.of(2026, 10, 12),
                        2.5);

        assertEquals(LocalDate.of(2026, 10, 12), schedule.actualStartDate());
        assertEquals(LocalDate.of(2026, 10, 14), schedule.actualEndDate());
        assertEquals(2.5, schedule.trainingDays());
    }

    @Test
    void shouldCalculateScheduleAcrossMonths() {
        CourseScheduleService.Schedule schedule =
                courseScheduleService.calculateSchedule(
                        LocalDate.of(2026, 10, 29),
                        5.0);

        assertEquals(LocalDate.of(2026, 10, 29), schedule.actualStartDate());
        assertEquals(LocalDate.of(2026, 11, 4), schedule.actualEndDate());

        var calendars = courseScheduleService.generateCalendars(
                schedule.actualStartDate(),
                schedule.actualEndDate());

        assertEquals(2, calendars.size());
        assertEquals("OCTOBER", calendars.get(0).month());
        assertEquals(2026, calendars.get(0).year());
        assertEquals("NOVEMBER", calendars.get(1).month());
        assertEquals(2026, calendars.get(1).year());
    }
}
