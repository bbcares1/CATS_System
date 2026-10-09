package group6.project.controller;

import java.time.YearMonth;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import group6.project.model.CourseCategoryType;
import group6.project.service.*;
import jakarta.servlet.http.HttpSession;

@Controller
public class TrainingCalendarController {
    private final TrainingCalendarService calendar;
    private final UserService users;

    // Calendar is a shared signed-in page, including the Admin workspace.
    public TrainingCalendarController(TrainingCalendarService calendar,UserService users) {this.calendar=calendar;this.users=users;}

    // Month/category filters, previous/next links and direct month selection stay in normal MVC.
    @GetMapping("/training/calendar")
    public String month(@RequestParam(defaultValue="") String month,@RequestParam(required=false) CourseCategoryType category,HttpSession session,Model model) {
        var actor=users.currentUser(session);if(actor==null) return "redirect:/login";
        try {
            YearMonth selected=month.isBlank()?YearMonth.now():YearMonth.parse(month);
            model.addAttribute("weeks",calendar.month(selected,category));model.addAttribute("month",selected);
            model.addAttribute("previous",selected.minusMonths(1));model.addAttribute("next",selected.plusMonths(1));
            model.addAttribute("category",category);model.addAttribute("categories",CourseCategoryType.values());
            model.addAttribute("workspace",actor instanceof group6.project.model.Admin?"Admin":actor instanceof group6.project.model.Manager?"Manager":"Staff");
            return "training-calendar";
        } catch(java.time.DateTimeException|IllegalArgumentException e) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a valid month between 2000 and 2100.");}
    }
}
