// We show the employee dashboard using the shared annual allowance calculation.
package group6.project.controller;

import group6.project.model.Staff;
import group6.project.service.TrainingEntitlementService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class StaffController {
    private final TrainingEntitlementService entitlements;

    public StaffController(TrainingEntitlementService entitlements) {
        this.entitlements = entitlements;
    }

    // Managers use this same page when working on their own training.
    @GetMapping({"/staff", "/staff/home", "/staff/dashboard"})
    public String home(HttpSession session, Model model) {
        Staff employee = (Staff) session.getAttribute("user");
        model.addAttribute("currentUser", employee);
        model.addAttribute(
                "summary", entitlements.summary(employee, LocalDate.now().getYear(), null));
        return "staff-home";
    }
}
