package group6.project.controller;

import group6.project.model.Staff;
import group6.project.service.StaffService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class StaffController {
    private final StaffService staff;

    // The route interceptor has already checked the saved employee session.
    public StaffController(StaffService staff) {
        this.staff = staff;
    }

    // Managers use this same page when working on their own training.
    @GetMapping({"/staff", "/staff/home", "/staff/dashboard"})
    public String home(HttpSession session, Model model) {
        Staff employee = (Staff) session.getAttribute("user");
        model.addAttribute("currentUser", employee);
        model.addAttribute("summary", staff.summary(employee, LocalDate.now().getYear()));
        return "staff-home";
    }
}
