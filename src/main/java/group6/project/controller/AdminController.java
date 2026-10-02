package group6.project.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import group6.project.model.Staff;
import group6.project.repo.StaffRepo;
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
    public String update_budget(Model model, @PathVariable("Id") Integer Id) {
        Optional<Staff> selectedStaff = adminService.getIdStaff(Id);
        if(selectedStaff.isEmpty()){
            throw new RuntimeException("未找到 ID 为 " + Id + " 的员工");
        }
        else{
            Staff Staff = selectedStaff.get();
            model.addAttribute(Staff);
            adminService.save(Staff);
             return "/ChangeBudget";
            
        }

       


    }

    @GetMapping("/showBudgetList")
    public String showBudgetList(Model model){
        List<Staff> staffs = adminService.getAllStaff();
        model.addAttribute("staffs",staffs);
        return "BudgetList";
    }
    
    

}
