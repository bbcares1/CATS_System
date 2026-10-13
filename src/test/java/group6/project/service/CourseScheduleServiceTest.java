// We check calculated course dates, holidays and half-day rules.
package group6.project.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import group6.project.model.CourseCategoryType;
import group6.project.model.ExcludedDays;
import group6.project.repo.ExcludedDaysRepo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

class CourseScheduleServiceTest {
    private final ExcludedDaysRepo holidays = mock(ExcludedDaysRepo.class);
    private final CourseScheduleService schedules = new CourseScheduleService(holidays);
    private final LocalDate monday =
            LocalDate.now()
                    .plusYears(1)
                    .withDayOfYear(1)
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

    // Use next year's working week so the test does not expire after the presentation.
    @BeforeEach
    void setup() {
        when(holidays.findAll()).thenReturn(List.of());
    }

    @Test
    void skipsWeekends() {
        var schedule =
                schedules.calculate(
                        CourseCategoryType.EXTERNAL_COURSE, monday.plusDays(4), 2, null);
        assertEquals(monday.plusDays(7), schedule.end());
        assertEquals(List.of(monday.plusDays(4), monday.plusDays(7)), schedule.dates());
    }

    @Test
    void rejectsTodayAndPastStart() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        schedules.calculate(
                                CourseCategoryType.EXTERNAL_COURSE, LocalDate.now(), 1, null));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        schedules.calculate(
                                CourseCategoryType.EXTERNAL_COURSE,
                                LocalDate.now().minusDays(1),
                                1,
                                null));
    }

    @Test
    void movesSaturdayToMonday() {
        var schedule =
                schedules.calculate(
                        CourseCategoryType.EXTERNAL_COURSE, monday.minusDays(2), 3, null);
        assertEquals(monday, schedule.start());
        assertEquals(monday.plusDays(2), schedule.end());
    }

    @Test
    void movesHolidayStartToNextWorkingDay() {
        exclude(monday);
        var schedule = schedules.calculate(CourseCategoryType.EXTERNAL_COURSE, monday, 3, null);
        assertEquals(monday.plusDays(1), schedule.start());
        assertEquals(monday.plusDays(3), schedule.end());
    }

    @Test
    void skipsHolidayDuringCourse() {
        exclude(monday.plusDays(2));
        var schedule = schedules.calculate(CourseCategoryType.EXTERNAL_COURSE, monday, 4, null);
        assertEquals(monday.plusDays(4), schedule.end());
        assertFalse(schedule.dates().contains(monday.plusDays(2)));
    }

    @Test
    void halfDayRequiresInternalAndOneSession() {
        var schedule = schedules.calculate(CourseCategoryType.INTERNAL_TRAINING, monday, 0.5, "AM");
        assertEquals(monday, schedule.end());
        assertEquals(0.5, schedule.days());
        assertThrows(
                IllegalArgumentException.class,
                () -> schedules.calculate(CourseCategoryType.EXTERNAL_COURSE, monday, 0.5, "AM"));
        assertThrows(
                IllegalArgumentException.class,
                () -> schedules.calculate(CourseCategoryType.INTERNAL_TRAINING, monday, 0.5, ""));
    }

    @Test
    void rejectsMultiDayHalfSessionsAndInvalidDurations() {
        for (double days : List.of(0.0, 2.5, Double.NaN, Double.POSITIVE_INFINITY, 261.0)) {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            schedules.calculate(
                                    CourseCategoryType.INTERNAL_TRAINING, monday, days, null));
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> schedules.calculate(CourseCategoryType.INTERNAL_TRAINING, monday, 2, "PM"));
    }

    @Test
    void crossesMonthsButNotYears() {
        LocalDate lastWeek =
                monday.withMonth(11)
                        .withDayOfMonth(27)
                        .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        var schedule = schedules.calculate(CourseCategoryType.EXTERNAL_COURSE, lastWeek, 5, null);
        assertEquals(12, schedule.end().getMonthValue());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        schedules.calculate(
                                CourseCategoryType.EXTERNAL_COURSE,
                                monday.withMonth(12).withDayOfMonth(31),
                                2,
                                null));
    }

    @Test
    void rejectsUnknownCategory() {
        assertThrows(
                IllegalArgumentException.class, () -> schedules.calculate(null, monday, 1, null));
    }

    // A saved excluded day is used both at the start and in the middle of a schedule.
    private void exclude(LocalDate date) {
        ExcludedDays holiday = new ExcludedDays();
        holiday.setDate(date);
        when(holidays.findAll()).thenReturn(List.of(holiday));
    }
}
