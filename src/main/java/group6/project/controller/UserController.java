package group6.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private UserService userService;

   
    @GetMapping({"/", "/login"})
    public String indexLogin() {
        return "login-portal";
    }

    
    @GetMapping("/staff/login")
    public String staffLoginPage(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null && userService.isStaffOrManager(user)) {
            return "redirect:/staff/home";
        }
        return "staff-login";
    }

    @PostMapping("/staff/login")
    public String handleStaffLogin(@RequestParam("username") String username,
                                   @RequestParam("password") String password,
                                   HttpSession session,
                                   Model model) {
        User user = userService.authenticate(username, password);

       
        if (user != null && userService.isStaffOrManager(user)) {
            session.setAttribute("user", user);
            return "redirect:/staff/home";
        }

        model.addAttribute("error", "Incorrect username or password for the Staff!");
        return "staff-login";
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
    public String handleAdminLogin(@RequestParam("username") String username,
                                   @RequestParam("password") String password,
                                   HttpSession session,
                                   Model model) {
        User user = userService.authenticate(username, password);

        
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
