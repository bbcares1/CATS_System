package group6.project.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import group6.project.model.ApprovedTrainingDate;
import group6.project.repo.ApprovedTrainingDateRepo;

@Service
public class TrainingCalendarService {

    private final ApprovedTrainingDateRepo approvedTrainingDateRepo;
    private final CourseScheduleService courseScheduleService;
    private final ExcludedDaysService excludedDaysService;

    public TrainingCalendarService(
            ApprovedTrainingDateRepo approvedTrainingDateRepo,
            CourseScheduleService courseScheduleService,
            ExcludedDaysService excludedDaysService) {

        this.approvedTrainingDateRepo = approvedTrainingDateRepo;
        this.courseScheduleService = courseScheduleService;
        this.excludedDaysService = excludedDaysService;
    }

    // Generate the calendar for the selected month
    public CourseScheduleService.CalendarMonth getCalendar(
            YearMonth selectedMonth) {

        return courseScheduleService.generateCalendars(
                selectedMonth.atDay(1),
                selectedMonth.atEndOfMonth()
        ).get(0);
    }

    // Get approved training dates grouped by date
    public Map<LocalDate, List<ApprovedTrainingDate>> getTrainingByDate(
            YearMonth selectedMonth) {

        LocalDate firstDay = selectedMonth.atDay(1);
        LocalDate lastDay = selectedMonth.atEndOfMonth();

        List<ApprovedTrainingDate> trainingDates =
                approvedTrainingDateRepo.findApprovedTrainingDates(
                        firstDay,
                        lastDay);

        Map<LocalDate, List<ApprovedTrainingDate>> trainingByDate =
                new HashMap<>();

        for (ApprovedTrainingDate training : trainingDates) {

            trainingByDate
                    .computeIfAbsent(
                            training.getTrainingDate(),
                            date -> new ArrayList<>())
                    .add(training);
        }

        return trainingByDate;
    }

    // Check whether a saved training date conflicts with an excluded day
    public boolean hasHolidayConflict(LocalDate date) {

        if (date == null) {
            return false;
        }

        return excludedDaysService.isExcludedDay(date);
    }
}
