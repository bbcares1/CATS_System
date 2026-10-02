package group6.project.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import group6.project.model.Staff;
import group6.project.service.AdminService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;



@Controller 
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService){
         this.adminService = adminService;
    }

    @GetMapping("/update/{Id}")
    public void update_budget(Model model, @PathVariable("Id") Integer Id) {
       adminService.updateStaffBudget(Id);


    }

    @GetMapping("/showBudgetlist")
    public String showBudgetList(Model model){
        List<Staff> staffs = adminService.getAllStaff();
        model.addAttribute("staffs",staffs);
        return "BudgetList";
    }
    
    

}
