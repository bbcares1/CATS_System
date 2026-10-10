package group6.project.service;

import group6.project.model.CourseCategoryType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public final class TrainingDayCalculator {
    // This calculation needs dates and holidays only, not a repository or a session.
    private TrainingDayCalculator() {}

    // All categories use whole working days; only single-day Internal Training can use AM or PM.
    public static double count(CourseCategoryType category, LocalDate start, LocalDate end,
            String halfDay, Set<LocalDate> holidays, boolean futureRequired) {
        if (category == null || start == null || end == null) {
            throw new IllegalArgumentException("Category and both dates are required.");
        }
        if (futureRequired && !start.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("The course start date must be after today.");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("The end date cannot be before the start date.");
        }
        if (start.getYear() != end.getYear()) {
            throw new IllegalArgumentException("A course must be within one calendar year.");
        }
        if (!isWorkingDay(start, holidays) || !isWorkingDay(end, holidays)) {
            throw new IllegalArgumentException("Start and end dates must be working days.");
        }
        if (halfDay != null && !halfDay.isBlank()) {
            if (category != CourseCategoryType.INTERNAL_TRAINING) {
                throw new IllegalArgumentException("Only Internal Training supports half-day sessions.");
            }
            if (!start.equals(end) || !Set.of("AM", "PM").contains(halfDay)) {
                throw new IllegalArgumentException("AM/PM half-day sessions must be on one date.");
            }
            return 0.5;
        }
        double days = 0;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            if (isWorkingDay(day, holidays)) days++;
        }
        return days;
    }

    // The start, end and counted days follow the same working calendar.
    public static boolean isWorkingDay(LocalDate day, Set<LocalDate> holidays) {
        return day.getDayOfWeek() != DayOfWeek.SATURDAY
                && day.getDayOfWeek() != DayOfWeek.SUNDAY && !holidays.contains(day);
    }
}
