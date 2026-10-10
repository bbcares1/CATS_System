// We group approved employee attendance by day for the monthly calendar.
package group6.project.service;

import group6.project.model.*;
import group6.project.repo.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class TrainingCalendarService {
    private final CourseApplicationRepo applications;
    private final ExcludedDaysRepo holidays;

    public TrainingCalendarService(CourseApplicationRepo applications, ExcludedDaysRepo holidays) {
        this.applications = applications;
        this.holidays = holidays;
    }

    // All signed-in roles see attendance, without private justifications, fees or claim evidence.
    @Transactional(readOnly = true)
    public List<List<Day>> month(YearMonth month, CourseCategoryType category) {
        if (month.getYear() < 2000 || month.getYear() > 2100)
            throw new IllegalArgumentException("Choose a month between 2000 and 2100.");
        Map<LocalDate, String> labels = new HashMap<>();
        for (ExcludedDays day : holidays.findAll()) labels.put(day.getDate(), day.getDescription());
        Map<LocalDate, List<Attendance>> byDate = new HashMap<>();
        for (CourseApplication course :
                applications
                        .findByStatusAndCourseStartDateLessThanEqualAndCourseEndDateGreaterThanEqual(
                                ApplicationStatus.APPROVED, month.atEndOfMonth(), month.atDay(1))) {
            if (category != null && course.getCourseCategory() != category) continue;
            LocalDate first =
                    course.getCourseStartDate().isBefore(month.atDay(1))
                            ? month.atDay(1)
                            : course.getCourseStartDate();
            LocalDate last =
                    course.getCourseEndDate().isAfter(month.atEndOfMonth())
                            ? month.atEndOfMonth()
                            : course.getCourseEndDate();
            for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
                if (date.getDayOfWeek().getValue() > 5 || labels.containsKey(date)) continue;
                byDate.computeIfAbsent(date, key -> new ArrayList<>())
                        .add(
                                new Attendance(
                                        course.getApplicant().getName(),
                                        course.getApplicant().getStaffId(),
                                        course.getCourseTitle(),
                                        course.getCourseCategory(),
                                        course.getHalfDayPeriod()));
            }
        }
        LocalDate firstCell =
                month.atDay(1).minusDays(month.atDay(1).getDayOfWeek().getValue() - 1);
        LocalDate lastCell =
                month.atEndOfMonth().plusDays(7 - month.atEndOfMonth().getDayOfWeek().getValue());
        List<List<Day>> weeks = new ArrayList<>();
        for (LocalDate start = firstCell; !start.isAfter(lastCell); start = start.plusDays(7)) {
            List<Day> week = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                LocalDate date = start.plusDays(i);
                var entries =
                        byDate.getOrDefault(date, List.of()).stream()
                                .sorted(
                                        Comparator.comparing(Attendance::employee)
                                                .thenComparing(Attendance::title))
                                .toList();
                week.add(
                        new Day(
                                date,
                                YearMonth.from(date).equals(month),
                                date.getDayOfWeek().getValue() > 5,
                                labels.get(date),
                                entries));
            }
            weeks.add(List.copyOf(week));
        }
        return List.copyOf(weeks);
    }

    public record Attendance(
            String employee,
            String staffId,
            String title,
            CourseCategoryType category,
            String halfDay) {}

    public record Day(
            LocalDate date,
            boolean currentMonth,
            boolean weekend,
            String holiday,
            List<Attendance> attendance) {}
}
