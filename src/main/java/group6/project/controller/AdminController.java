package group6.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import group6.project.model.User;
import group6.project.service.AdminService;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService){
         this.adminService = adminService;
    }
    
    @GetMapping("/home")
    public String adminHome(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("user");
        if (admin == null) {
            return "redirect:/admin/login";
        }
        model.addAttribute("currentUser", admin);
        return "admin-home";
    }
}
