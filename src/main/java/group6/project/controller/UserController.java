package group6.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import group6.project.model.Admin;
import group6.project.model.Manager;
import group6.project.model.Staff;
import group6.project.model.User;
import group6.project.service.UserService;
import jakarta.servlet.http.HttpSession;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

   
    @GetMapping({"/", "/login"})
    public String indexLogin() {
        return "login-portal";
    }

    
    @GetMapping("/employee/login")
    public String employeeLoginPage(HttpSession session) {

        Object user = session.getAttribute("user");

        if (user instanceof Manager) {
            return "redirect:/manager/home";
        }
        if (user instanceof Staff) {
            return "redirect:/staff/home";
        }
        return "employee-login";
    }

    @PostMapping("/employee/login")
    public String handleStaffLogin(@RequestParam("userName") String userName,
                                   @RequestParam("password") String password,
                                   @RequestParam("designation") String designation,
                                   HttpSession session,
                                   Model model) {

        User user = userService.authenticate(userName, password);

        if (user instanceof Manager manager && "Manager".equalsIgnoreCase(designation)) {
            session.setAttribute("user", manager);
            return "redirect:/manager/home";
        }

        if (user instanceof Staff staff && !(user instanceof Manager) && "Staff".equalsIgnoreCase(designation)) {
            session.setAttribute("user", staff);
            return "redirect:/staff/home";
        }

        model.addAttribute("error", "Incorrect username or password for the Employee!");
        return "employee-login";
    }

    
    @GetMapping("/admin/login")
    public String adminLoginPage(HttpSession session) {

        Object user = session.getAttribute("user");

        if (user instanceof Admin) {
            return "redirect:/admin/home";
        }

        return "admin-login";
    }

    @PostMapping("/admin/login")
    public String handleAdminLogin(@RequestParam("userName") String userName,
                                   @RequestParam("password") String password,
                                    HttpSession session,
                                    Model model) {

        User user = userService.authenticate(userName, password);

        if (user instanceof Admin admin) {
            session.setAttribute("user", admin);
            return "redirect:/admin/home";
        }

        model.addAttribute("error", "Incorrect username or password for the Admin!");
        return "admin-login";
    }

    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
