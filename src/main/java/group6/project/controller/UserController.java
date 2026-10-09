package group6.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import group6.project.model.Admin;
import group6.project.model.Manager;
import group6.project.model.Staff;
import group6.project.model.User;
import group6.project.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class UserController {
    private final UserService users;

    // All login forms use the same account store and session contract.
    public UserController(UserService users) { this.users = users; }

    // Keep separate employee and administration entry points.
    @GetMapping({"/", "/login"})
    public String indexLogin() { return "login-portal"; }

    // A signed-in employee can return directly to their own workspace.
    @GetMapping("/employee/login")
    public String employeeLoginPage(HttpSession session) {
        User user = users.currentUser(session);
        return user instanceof Staff ? employeeHome(user) : "employee-login";
    }

    // Role comes from the saved account, so a browser cannot choose Manager privileges.
    @PostMapping("/employee/login")
    public String employeeLogin(@RequestParam String userName, @RequestParam String password,
            HttpSession session, HttpServletRequest request, Model model) {
        User user = users.authenticate(userName, password);
        if (user instanceof Staff) {
            signIn(session, request, user);
            return employeeHome(user);
        }
        model.addAttribute("error", "Incorrect employee username or password.");
        return "employee-login";
    }

    // Administration stays separate from the employee login.
    @GetMapping("/admin/login")
    public String adminLoginPage(HttpSession session) {
        return users.currentUser(session) instanceof Admin ? "redirect:/admin/home" : "admin-login";
    }

    // Only an actual Admin account can enter the administration workspace.
    @PostMapping("/admin/login")
    public String adminLogin(@RequestParam String userName, @RequestParam String password,
            HttpSession session, HttpServletRequest request, Model model) {
        User user = users.authenticate(userName, password);
        if (user instanceof Admin) {
            signIn(session, request, user);
            return "redirect:/admin/home";
        }
        model.addAttribute("error", "Incorrect Admin username or password.");
        return "admin-login";
    }

    // Clear the authenticated identity when leaving the workspace.
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // An old GET bookmark can open login, but cannot sign a browser out.
    @GetMapping("/logout")
    public String logoutBookmark() { return "redirect:/login"; }

    // Rotate the anonymous session ID before assigning authenticated identity.
    private void signIn(HttpSession session, HttpServletRequest request, User user) {
        request.changeSessionId();
        group6.project.config.CsrfProtection.rotate(session);
        session.setAttribute("user", user);
    }

    // Manager inherits Staff capability but starts in the team workspace.
    private String employeeHome(User user) {
        return user instanceof Manager ? "redirect:/manager/home" : "redirect:/staff/home";
    }
}
