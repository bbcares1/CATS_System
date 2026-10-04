package group6.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import group6.project.model.User;
import group6.project.service.StaffService;
import jakarta.servlet.http.HttpSession;

@RestController
@Controller
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }
    
    @GetMapping("/home")
    public String staffHome(HttpSession session, Model model) {
        User staff = (User) session.getAttribute("user");
        if (staff == null) {
            return "redirect:/staff/login";
        }
        model.addAttribute("currentUser", staff);
        return "staff-home";
    }
}
