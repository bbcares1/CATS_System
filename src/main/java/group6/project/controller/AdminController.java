package group6.project.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import group6.project.model.ApprovalHierarchy;
import group6.project.model.CourseDetail;
import group6.project.model.ExcludedDays;
import group6.project.model.Roles;
import group6.project.model.Staff;
import group6.project.service.AdminService;
import group6.project.service.CourseCategoryService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final CourseCategoryService courseCategoryService;

    public AdminController(AdminService adminService, CourseCategoryService courseCategoryService) {
        this.adminService = adminService;
        this.courseCategoryService = courseCategoryService;
    }

  // ------- this part below is about budgetmanagement
  // -----------------------------------------------

    @GetMapping("/update/{Id}")
    public String update_budget(Model model, @PathVariable("Id") Integer Id) {
        Optional<Staff> selectedStaff = adminService.getIdStaff(Id);
        if (selectedStaff.isEmpty()) {
            throw new RuntimeException("未找到 ID 为 " + Id + " 的员工");
        } else {
            Staff staff = selectedStaff.get();
            model.addAttribute("staff", staff); 
            return "ChangeBudget"; 
        }
    }

    @GetMapping("/showBudgetList")
    public String showBudgetList(Model model) {
        List<Staff> staffs = adminService.getAllStaff();
        model.addAttribute("staffs", staffs);
        return "BudgetList";
    }

    @PostMapping("/save")
    public String postMethodName(Staff staff) {
        adminService.save(staff);
        return "redirect:/admin/showBudgetList";
    }

    @GetMapping("/delete/{id}")
    public String deleteById(@PathVariable("id") Integer id) {
        adminService.deleteById(id);
        return "redirect:/admin/showBudgetList";
    }

  // this part below is about excludeddays
  // -------------------------------------

    @GetMapping("/excludedDays")
    public String showExcludedDays(Model model) {
        List<ExcludedDays> dayList = adminService.getAllExcludedDays();
        model.addAttribute("dayList", dayList);
        model.addAttribute("newExcludedDay", new ExcludedDays());
        return "ExcludedDaysList";
    }

    @PostMapping("/excludedDays/add")
    public String addExcludedDays(@ModelAttribute("newExcludedDay") ExcludedDays excludedDays) {
        adminService.saveExcludedDays(excludedDays);
        return "redirect:/admin/excludedDays";
    }

    @GetMapping("/deleteExcludedDays/{id}")
    public String deleteExcludedDays(@PathVariable("id") Integer id) {
        adminService.deleteExcludedDays(id);
        return "redirect:/admin/excludedDays";
    }

   

    @GetMapping("/courses") 
    public String getCourseList(Model model) {
        List<CourseDetail> courseList = adminService.getAllCourseDetails();
        model.addAttribute("courseList", courseList);
        model.addAttribute("course", new CourseDetail());
        model.addAttribute("categories", courseCategoryService.getAllCategories());
        return "CourseList";
    }

    @GetMapping("/courses/edit/{id}") 
    public String editCourse(@PathVariable("id") Integer id, Model model) {
        Optional<CourseDetail> course = adminService.getByIdCourseDetails(id);
        if (course.isPresent()) {
            model.addAttribute("course", course.get());
            model.addAttribute("courseList", adminService.getAllCourseDetails());
            model.addAttribute("categories", courseCategoryService.getAllCategories());
            return "CourseList";
        } else {
            return "redirect:/admin/courses";
        }
    }

    @PostMapping("/courses/save")
    public String saveCourse(@ModelAttribute("course") CourseDetail course) {
        adminService.saveCourse(course);
        return "redirect:/admin/courses"; 
    }

    @GetMapping("/courses/delete/{id}")
    public String deleteCourse(@PathVariable("id") Integer id) {
        adminService.deleteCourseById(id);
        return "redirect:/admin/courses"; 
    }

    //this part below is about Approval Hierarchy
    // ----------------------------------------

    @GetMapping("/hierarchy")
    public String showAllHierarchy(Model model) {
        model.addAttribute("hierarchies", adminService.getAllApprovalHierarchy());
        model.addAttribute("hierarchy", new ApprovalHierarchy());
        model.addAttribute("roles", Roles.values());
        return "HierarchyList";
    }

    @GetMapping("/hierarchy/edit/{id}")
    public String editHierarchy(@PathVariable("id") Integer id, Model model) {
        Optional<ApprovalHierarchy> hierarchySelected = adminService.getHierarchyById(id);
        if (hierarchySelected.isPresent()) {
            model.addAttribute("hierarchy", hierarchySelected.get());
            model.addAttribute("hierarchies", adminService.getAllApprovalHierarchy());
            model.addAttribute("roles", Roles.values());
            return "HierarchyList";
        }
        return "redirect:/admin/hierarchy";
    }

    @PostMapping("/hierarchy/save")
    public String saveHierarchy(@ModelAttribute("hierarchy") ApprovalHierarchy hierarchy) {
        adminService.saveHierarchy(hierarchy);
        return "redirect:/admin/hierarchy";
    }

    @GetMapping("/hierarchy/delete/{id}")
    public String deleteHierarchy(@PathVariable("id") Integer id) {
        adminService.deleteHierarchyById(id);
        return "redirect:/admin/hierarchy";
    }
}