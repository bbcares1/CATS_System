// We handle the separate Employee and Admin login pages and shared logout.
package group6.project.controller;

import group6.project.model.Admin;
import group6.project.model.Manager;
import group6.project.model.Staff;
import group6.project.model.User;
import group6.project.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    // Employees and administrators have separate entry pages.
    @GetMapping({"/", "/login"})
    public String indexLogin() {
        return "login-portal";
    }

    // A signed-in employee returns to their own workspace.
    @GetMapping("/employee/login")
    public String employeeLoginPage(HttpSession session) {
        User user = users.currentUser(session);
        if (user instanceof Manager) return "redirect:/manager/home";
        if (user instanceof Staff) return "redirect:/staff/home";
        return "employee-login";
    }

    // The saved account decides the role; users do not need to choose it twice.
    @PostMapping("/employee/login")
    public String employeeLogin(
            @RequestParam String userName,
            @RequestParam String password,
            HttpServletRequest request,
            Model model) {
        User user = users.authenticate(userName, password);
        if (user instanceof Staff) {
            signIn(request, user);
            return user instanceof Manager ? "redirect:/manager/home" : "redirect:/staff/home";
        }
        model.addAttribute("userName", userName);
        model.addAttribute("error", "Wrong username or password.");
        return "employee-login";
    }

    // Admin accounts keep a separate login page.
    @GetMapping("/admin/login")
    public String adminLoginPage(HttpSession session) {
        return users.currentUser(session) instanceof Admin ? "redirect:/admin/home" : "admin-login";
    }

    // Employee credentials cannot open an administrator session.
    @PostMapping("/admin/login")
    public String adminLogin(
            @RequestParam String userName,
            @RequestParam String password,
            HttpServletRequest request,
            Model model) {
        User user = users.authenticate(userName, password);
        if (user instanceof Admin) {
            signIn(request, user);
            return "redirect:/admin/home";
        }
        model.addAttribute("userName", userName);
        model.addAttribute("error", "Wrong username or password.");
        return "admin-login";
    }

    // Renew the session ID after credentials are checked.
    private void signIn(HttpServletRequest request, User user) {
        request.getSession();
        request.changeSessionId();
        request.getSession().setAttribute("user", user);
        request.getSession().setAttribute("csrfToken", UUID.randomUUID().toString());
    }

    // Clear the saved identity when the user leaves the application.
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
