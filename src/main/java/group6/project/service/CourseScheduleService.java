package group6.project.service;

import group6.project.model.CourseCategoryType;
import group6.project.model.ExcludedDays;
import group6.project.repo.ExcludedDaysRepo;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CourseScheduleService {
    private final ExcludedDaysRepo holidays;

    public CourseScheduleService(ExcludedDaysRepo holidays) {
        this.holidays = holidays;
    }

    // Suggest working dates without saving a batch or counting weekends as training days.
    public Schedule calculate(
            CourseCategoryType category, LocalDate requestedStart, double days, String halfDay) {
        if (requestedStart == null || !requestedStart.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Choose a start date after today.");
        }
        if (!Double.isFinite(days)
                || days <= 0
                || days > 260
                || (days != 0.5 && days != Math.floor(days))) {
            throw new IllegalArgumentException(
                    "Use whole days, or 0.5 for one Internal Training session.");
        }
        boolean half = halfDay != null && !halfDay.isBlank();
        if ((days == 0.5) != half) {
            throw new IllegalArgumentException(
                    "Choose AM or PM for 0.5 days; leave it blank for whole days.");
        }
        Set<LocalDate> excluded =
                holidays.findAll().stream().map(ExcludedDays::getDate).collect(Collectors.toSet());
        List<LocalDate> dates = new ArrayList<>();
        LocalDate day = requestedStart;
        int needed = (int) Math.ceil(days);
        while (dates.size() < needed) {
            if (day.getYear() != requestedStart.getYear()) {
                throw new IllegalArgumentException(
                        "The schedule must fit within one calendar year.");
            }
            if (TrainingDayCalculator.isWorkingDay(day, excluded)) dates.add(day);
            day = day.plusDays(1);
        }
        LocalDate start = dates.getFirst();
        LocalDate end = dates.getLast();
        TrainingDayCalculator.count(category, start, end, halfDay, excluded, true);
        return new Schedule(start, end, days, dates);
    }

    public record Schedule(LocalDate start, LocalDate end, double days, List<LocalDate> dates) {}
}
