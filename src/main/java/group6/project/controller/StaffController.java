package group6.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;


import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import group6.project.model.Staff;
import group6.project.model.User;
import group6.project.service.StaffService;
import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }
    
    @GetMapping("/home")
    public String staffHome(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return "redirect:/employee/login";
        }
        if (!(user instanceof Staff)) {
            return "redirect:/employee/login";
        }

        model.addAttribute("currentUser", user);
        return "staff-home";
    }


    @GetMapping
    public List<Staff> getAllStaff() {
        return staffService.getAllStaff();
    }

    @GetMapping("/{id}")
    public Staff getStaff(@PathVariable Integer id) {
        return staffService.getStaff(id);
    }
}
