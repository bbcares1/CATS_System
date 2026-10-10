// Shows approved employee attendance for a selected month.
package group6.project.controller;

import group6.project.model.Admin;
import group6.project.model.CourseCategoryType;
import group6.project.model.Manager;
import group6.project.service.*;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Controller
public class TrainingCalendarController {
    private final TrainingCalendarService calendar;
    private final UserService users;

    public TrainingCalendarController(TrainingCalendarService calendar, UserService users) {
        this.calendar = calendar;
        this.users = users;
    }

    // Month/category filters, previous/next links and direct month selection stay in normal MVC.
    @GetMapping("/training/calendar")
    public String month(
            @RequestParam(defaultValue = "") String month,
            @RequestParam(required = false) CourseCategoryType category,
            HttpSession session,
            Model model) {
        var actor = users.currentUser(session);
        if (actor == null) return "redirect:/login";
        try {
            YearMonth selected = month.isBlank() ? YearMonth.now() : YearMonth.parse(month);
            model.addAttribute("weeks", calendar.month(selected, category));
            model.addAttribute("month", selected);
            List<YearMonth> months = new ArrayList<>();
            for (int number = 1; number <= 12; number++)
                months.add(YearMonth.of(selected.getYear(), number));
            model.addAttribute("months", months);
            model.addAttribute("previous", selected.minusMonths(1));
            model.addAttribute("next", selected.plusMonths(1));
            model.addAttribute("category", category);
            model.addAttribute("categories", CourseCategoryType.values());
            model.addAttribute(
                    "workspace",
                    actor instanceof Admin
                            ? "Admin"
                            : actor instanceof Manager ? "Manager" : "Staff");
            return "training-calendar";
        } catch (java.time.DateTimeException | IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Choose a valid month between 2000 and 2100.");
        }
    }
}
