package group6.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
        User user = (User) session.getAttribute("user");
        if (userService.isManager(user)) {
            return "redirect:/manager/home";
        }
        if (userService.isStaff(user)) {
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

       
        if (user != null && userService.isStaffOrManager(user)) {
            if (user != null && userService.isManager(user) && designation.equalsIgnoreCase("Manager")) {
                session.setAttribute("user", user);
                return "redirect:/manager/home";
            }
            if (user != null && userService.isStaff(user) && designation.equalsIgnoreCase("Staff")) {
                session.setAttribute("user", user);
                return "redirect:/staff/home";
            }
        }

        model.addAttribute("error", "Incorrect username or password for the Employee!");
        return "employee-login";
    }

    
    @GetMapping("/admin/login")
    public String adminLoginPage(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null && userService.isAdmin(user)) {
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

        
        if (user != null && userService.isAdmin(user)) {
            session.setAttribute("user", user);
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
