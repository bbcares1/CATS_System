package group6.project.controller;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import group6.project.model.ExcludedDays;
import group6.project.service.ExcludedDaysService;
import group6.project.service.TrainingCalendarService;

@Controller
public class TrainingCalendarController {

    private final TrainingCalendarService trainingCalendarService;
    private final ExcludedDaysService excludedDaysService;

    // Constructor Injection
    public TrainingCalendarController(
            TrainingCalendarService trainingCalendarService,
            ExcludedDaysService excludedDaysService) {

        this.trainingCalendarService = trainingCalendarService;
        this.excludedDaysService = excludedDaysService;
    }

    // Display Training Calendar
    @GetMapping("/training/calendar")
    public String showTrainingCalendar(
            @RequestParam(required = false) String month,
            Model model) {

        YearMonth selectedMonth;

        // Default to current month
        try {
            selectedMonth =
                    (month == null || month.isBlank())
                    ? YearMonth.now()
                    : YearMonth.parse(month);

        } catch (DateTimeParseException e) {
            selectedMonth = YearMonth.now();
        }

        // Generate calendar for selected month
        model.addAttribute(
                "calendar",
                trainingCalendarService.getCalendar(selectedMonth));

        // Load officially saved and approved training dates
        model.addAttribute(
                "trainingByDate",
                trainingCalendarService.getTrainingByDate(selectedMonth));

        // Load Admin excluded days once
        List<ExcludedDays> excludedDays =
                excludedDaysService.getAllExcludedDays();

        // Dates used for conflict detection
        model.addAttribute(
                "excludedDates",
                excludedDays.stream()
                        .map(ExcludedDays::getDate)
                        .toList());

        // Full records used to display holiday descriptions
        model.addAttribute(
                "excludedDays",
                excludedDays);

        // Selected month
        model.addAttribute(
                "selectedMonth",
                selectedMonth.toString());

        // Previous month
        model.addAttribute(
                "previousMonth",
                selectedMonth.minusMonths(1).toString());

        // Next month
        model.addAttribute(
                "nextMonth",
                selectedMonth.plusMonths(1).toString());

        return "TrainingCalendar";
    }
}
