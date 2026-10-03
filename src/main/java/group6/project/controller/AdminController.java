package group6.project.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import group6.project.model.CourseCategory;
import group6.project.model.CourseDetail;
import group6.project.model.ExcludedDays;
import group6.project.model.Staff;
import group6.project.repo.StaffRepo;
import group6.project.service.AdminService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/update/{Id}")
    public String update_budget(Model model, @PathVariable("Id") Integer Id) {
        Optional<Staff> selectedStaff = adminService.getIdStaff(Id);
        if (selectedStaff.isEmpty()) {
            throw new RuntimeException("未找到 ID 为 " + Id + " 的员工");
        } else {
            Staff Staff = selectedStaff.get();
            model.addAttribute(Staff);
            adminService.save(Staff);
            return "/ChangeBudget";

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

    @GetMapping("/excludedDays")
    public String showExcludedDays(Model model) {
        List<ExcludedDays> dayList = adminService.getAllExcludedDays();
        model.addAttribute("dayList",dayList);
        model.addAttribute("newExcludedDay", new ExcludedDays());
        return "ExcludedDaysList";
    }

    @PostMapping("/excludedDays/add")
    public String getMethodName(@ModelAttribute("newExcludedDays") ExcludedDays excludedDays) {
        adminService.saveExcludedDays(excludedDays);

        return "redirect:/admin/excludedDays";
    }

    @GetMapping("/deleteExcludedDays/{id}")
    public String deleteExcludedDays(@PathVariable("id") Integer id) {

        adminService.deleteExcludedDays(id);

        return "redirect:/admin/excludedDays";
    }

    @GetMapping("/course")
    public String getCourseList(Model model) {
        List<CourseDetail> courseList = adminService.getAllCourseDetails();

        model.addAttribute("courseList", courseList);
        model.addAttribute("course", new CourseDetail());

        model.addAttribute("categories", CourseCategory.values());
        return "CourseList";
    }

    @GetMapping("/course/edit/{id}")
    public String editCourse(@PathVariable("id") Integer id, Model model) {
        Optional<CourseDetail> course = adminService.getByIdCourseDetails(id);
        if (course.isPresent()) {
            model.addAttribute("course",course.get());
            model.addAttribute("courseList", adminService.getAllCourseDetails());
            model.addAttribute("categories", CourseCategory.values());
            return "CourseList";
        } else {
            return "redirect:/admin/course";
        }

    }

    @PostMapping("/courses/save")
    public String saveCourse(@ModelAttribute("course") CourseDetail course) {
        adminService.saveCourse(course);
        return "redirect:/admin/course";
    }

    @GetMapping("/courses/delete/{id}")
    public String deleteCourse(@PathVariable("id") Integer id) {
        adminService.deleteCourseById(id);
        return "redirect:/admin/course";
    }

}
