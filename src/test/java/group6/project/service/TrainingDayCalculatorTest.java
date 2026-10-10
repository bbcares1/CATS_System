// We check working-day counts and the allowed half-day cases.
package group6.project.service;

import static org.junit.jupiter.api.Assertions.*;

import group6.project.model.CourseCategoryType;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

class TrainingDayCalculatorTest {
    private final LocalDate monday = LocalDate.of(2027, 1, 4);

    // Dates in the middle can be holidays; neither boundary can be one.
    @Test
    void excludesWeekendsAndHolidaysAndChecksBoundaries() {
        Set<LocalDate> holidays = Set.of(monday.plusDays(2));
        assertEquals(
                5,
                TrainingDayCalculator.count(
                        CourseCategoryType.EXTERNAL_COURSE,
                        monday,
                        monday.plusDays(7),
                        null,
                        holidays,
                        false));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        TrainingDayCalculator.count(
                                CourseCategoryType.EXTERNAL_COURSE,
                                monday.plusDays(2),
                                monday.plusDays(3),
                                null,
                                holidays,
                                false));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        TrainingDayCalculator.count(
                                CourseCategoryType.EXTERNAL_COURSE,
                                monday,
                                monday.plusDays(5),
                                null,
                                holidays,
                                false));
    }

    // Only a single-day Internal AM or PM can consume half a training day.
    @Test
    void halfDaysHaveOneUnambiguousMeaning() {
        assertEquals(
                0.5,
                TrainingDayCalculator.count(
                        CourseCategoryType.INTERNAL_TRAINING,
                        monday,
                        monday,
                        "AM",
                        Set.of(),
                        false));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        TrainingDayCalculator.count(
                                CourseCategoryType.INTERNAL_TRAINING,
                                monday,
                                monday.plusDays(1),
                                "AM",
                                Set.of(),
                                false));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        TrainingDayCalculator.count(
                                CourseCategoryType.PROFESSIONAL_CERTIFICATION,
                                monday,
                                monday,
                                "PM",
                                Set.of(),
                                false));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        TrainingDayCalculator.count(
                                CourseCategoryType.INTERNAL_TRAINING,
                                monday,
                                monday,
                                "HALF_DAY",
                                Set.of(),
                                false));
    }

    // Submission requires a future start; every course must stay within one calendar year.
    @Test
    void rejectsPastReversedAndCrossYearDates() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        TrainingDayCalculator.count(
                                CourseCategoryType.EXTERNAL_COURSE,
                                LocalDate.now(),
                                LocalDate.now(),
                                null,
                                Set.of(),
                                true));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        TrainingDayCalculator.count(
                                CourseCategoryType.EXTERNAL_COURSE,
                                monday.plusDays(1),
                                monday,
                                null,
                                Set.of(),
                                false));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        TrainingDayCalculator.count(
                                CourseCategoryType.EXTERNAL_COURSE,
                                monday,
                                monday.plusYears(1),
                                null,
                                Set.of(),
                                false));
    }
}
