package group6.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import group6.project.repo.CourseApplicationRepo;

public class CourseScheduleServiceTest {

    @Mock
    private ExcludedDaysService excludedDaysService;

    @Mock
    private CourseApplicationRepo courseApplicationRepo;

    private CourseScheduleService courseScheduleService;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        courseScheduleService =
                new CourseScheduleService(
                        excludedDaysService,
                        courseApplicationRepo);
    }

    @Test
    void shouldSkipWeekendWhenCalculatingSchedule() {

        when(excludedDaysService.isWorkingDay(any(LocalDate.class)))
                .thenAnswer(invocation -> {

                    LocalDate date = invocation.getArgument(0);

                    return date.getDayOfWeek().getValue() <= 5;
                });

        LocalDate requestedStartDate =
                LocalDate.of(2026, 10, 8);

        CourseScheduleService.Schedule schedule =
                courseScheduleService.calculateSchedule(
                        requestedStartDate,
                        5.0);

        assertEquals(
                LocalDate.of(2026, 10, 8),
                schedule.actualStartDate());

        assertEquals(
                LocalDate.of(2026, 10, 14),
                schedule.actualEndDate());
    }
    @Test
void shouldMoveSaturdayStartToMonday() {

    when(excludedDaysService.isWorkingDay(any(LocalDate.class)))
            .thenAnswer(invocation -> {

                LocalDate date = invocation.getArgument(0);

                return date.getDayOfWeek().getValue() <= 5;
            });

    // 2026-10-10 is Saturday
    LocalDate requestedStartDate =
            LocalDate.of(2026, 10, 10);

    CourseScheduleService.Schedule schedule =
            courseScheduleService.calculateSchedule(
                    requestedStartDate,
                    3.0);

    // Requested Saturday should move to Monday
    assertEquals(
            LocalDate.of(2026, 10, 12),
            schedule.actualStartDate());

    // Mon = Day 1, Tue = Day 2, Wed = Day 3
    assertEquals(
            LocalDate.of(2026, 10, 14),
            schedule.actualEndDate());
}

@Test
void shouldMoveHolidayStartToNextWorkingDay() {

    LocalDate holiday =
            LocalDate.of(2026, 10, 12);

    when(excludedDaysService.isWorkingDay(any(LocalDate.class)))
            .thenAnswer(invocation -> {

                LocalDate date = invocation.getArgument(0);

                boolean isWeekend =
                        date.getDayOfWeek().getValue() > 5;

                boolean isHoliday =
                        date.equals(holiday);

                return !isWeekend && !isHoliday;
            });

    LocalDate requestedStartDate =
            LocalDate.of(2026, 10, 12);

    CourseScheduleService.Schedule schedule =
            courseScheduleService.calculateSchedule(
                    requestedStartDate,
                    3.0);

    assertEquals(
            LocalDate.of(2026, 10, 13),
            schedule.actualStartDate());

    assertEquals(
            LocalDate.of(2026, 10, 15),
            schedule.actualEndDate());
}

@Test
void shouldSkipHolidayDuringCourse() {

    LocalDate holiday =
            LocalDate.of(2026, 10, 14);

    when(excludedDaysService.isWorkingDay(any(LocalDate.class)))
            .thenAnswer(invocation -> {

                LocalDate date = invocation.getArgument(0);

                boolean isWeekend =
                        date.getDayOfWeek().getValue() > 5;

                boolean isHoliday =
                        date.equals(holiday);

                return !isWeekend && !isHoliday;
            });

    LocalDate requestedStartDate =
            LocalDate.of(2026, 10, 12);

    CourseScheduleService.Schedule schedule =
            courseScheduleService.calculateSchedule(
                    requestedStartDate,
                    4.0);

    assertEquals(
            LocalDate.of(2026, 10, 12),
            schedule.actualStartDate());

    assertEquals(
            LocalDate.of(2026, 10, 16),
            schedule.actualEndDate());
}
@Test
void shouldCalculateHalfDaySchedule() {

    when(excludedDaysService.isWorkingDay(any(LocalDate.class)))
            .thenAnswer(invocation -> {

                LocalDate date = invocation.getArgument(0);

                return date.getDayOfWeek().getValue() <= 5;
            });

    LocalDate requestedStartDate =
            LocalDate.of(2026, 10, 12);

    CourseScheduleService.Schedule schedule =
            courseScheduleService.calculateSchedule(
                    requestedStartDate,
                    2.5);

    assertEquals(
            LocalDate.of(2026, 10, 12),
            schedule.actualStartDate());

    assertEquals(
            LocalDate.of(2026, 10, 14),
            schedule.actualEndDate());

    assertEquals(
            2.5,
            schedule.trainingDays());
}

@Test
void shouldCalculateScheduleAcrossMonths() {

    when(excludedDaysService.isWorkingDay(any(LocalDate.class)))
            .thenAnswer(invocation -> {

                LocalDate date = invocation.getArgument(0);

                return date.getDayOfWeek().getValue() <= 5;
            });

    LocalDate requestedStartDate =
            LocalDate.of(2026, 10, 29);

    CourseScheduleService.Schedule schedule =
            courseScheduleService.calculateSchedule(
                    requestedStartDate,
                    5.0);

    assertEquals(
            LocalDate.of(2026, 10, 29),
            schedule.actualStartDate());

    assertEquals(
            LocalDate.of(2026, 11, 4),
            schedule.actualEndDate());

    var calendars =
            courseScheduleService.generateCalendars(
                    schedule.actualStartDate(),
                    schedule.actualEndDate());

    assertEquals(2, calendars.size());

    assertEquals("OCTOBER", calendars.get(0).month());
    assertEquals(2026, calendars.get(0).year());

    assertEquals("NOVEMBER", calendars.get(1).month());
    assertEquals(2026, calendars.get(1).year());
}
}