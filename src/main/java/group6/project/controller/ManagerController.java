package group6.project.controller;

import group6.project.service.UserService;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import group6.project.model.Manager;
import group6.project.model.User;
import group6.project.service.ManagerService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/managers")
@RequestMapping("/manager")
public class ManagerController {

    private final UserService userService;
    private final ManagerService managerService;

    public ManagerController(ManagerService managerService, UserService userService) {
        this.managerService = managerService;
        this.userService = userService;
    }

    @GetMapping
    public List<Manager> getAllManagers() {
        return managerService.getAllManagers();
    }

    @GetMapping("/{id}")
    public Manager getManager(@PathVariable Integer id) {
        return managerService.getManager(id);
    }

    @GetMapping("/staff-id/{staffId}")
    public Manager getManagerByStaffId(@PathVariable String staffId) {
        return managerService.getManagerByStaffId(staffId);
    }

    @GetMapping({"/home"})
    public String managerDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");

        if (user == null || !userService.isManager(user)) {
            return "redirect:/employee/login";
        }
        if (!(user instanceof Manager)) {
            return "redirect:/employee/login";
        }
        
        model.addAttribute("currentUser", user);
        return "manager-home";
    }
}
