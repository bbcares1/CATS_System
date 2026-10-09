package group6.project.controller;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import group6.project.model.Admin;
import group6.project.model.ApprovalHierarchy;
import group6.project.model.CourseDetail;
import group6.project.model.ExcludedDays;
import group6.project.model.Roles;
import group6.project.model.Staff;
import group6.project.model.User;
import group6.project.service.AdminService;
import group6.project.service.TrainingEntitlementService;
import group6.project.service.CourseCategoryService;
import group6.project.service.ExcludedDaysService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final TrainingEntitlementService entitlements;
    private final AdminService adminService;
    private final CourseCategoryService courseCategoryService;
    private final ExcludedDaysService excludedDaysService;

    public AdminController(
            AdminService adminService,
            TrainingEntitlementService entitlements,
            CourseCategoryService courseCategoryService,
            ExcludedDaysService excludedDaysService) {
       this.adminService = adminService;
       this.entitlements = entitlements;
       this.courseCategoryService = courseCategoryService;
       this.excludedDaysService = excludedDaysService;
}


    @GetMapping("/home")
    public String adminHome(HttpSession session, Model model) {

        if (!(session.getAttribute("user") instanceof Admin admin)) {
            return "redirect:/admin/login";
        }

        model.addAttribute("currentUser", admin);
        return "admin-home";
    }

    
    // Select an employee and year without binding a whole Staff entity from the form.
    @GetMapping({"/update/{id}", "/entitlements/{id}"})
    public String editEntitlement(@PathVariable Integer id,
            @RequestParam(required = false) Integer year, HttpSession session, Model model) {
        if (!(session.getAttribute("user") instanceof Admin)) return "redirect:/admin/login";
        int selectedYear = year == null ? LocalDate.now().getYear() : year;
        model.addAttribute("staff", entitlements.employee(id));
        model.addAttribute("year", selectedYear);
        model.addAttribute("summary", entitlements.summary(id, selectedYear));
        return "ChangeBudget";
    }

    // A single annual list shows the limits alongside the same totals used by Staff and Manager.
    @GetMapping({"/showBudgetList", "/entitlements"})
    public String showBudgetList(@RequestParam(required = false) Integer year, HttpSession session, Model model) {
        if (!(session.getAttribute("user") instanceof Admin)) return "redirect:/admin/login";
        int selectedYear = year == null ? LocalDate.now().getYear() : year;
        model.addAttribute("year", selectedYear);
        model.addAttribute("rows", entitlements.rows(selectedYear));
        return "BudgetList";
    }

    // Keep previous years intact and reject a limit below existing reservations.
    @PostMapping({"/save", "/entitlements/save"})
    public String saveEntitlement(@RequestParam Integer staffId, @RequestParam int year,
            @RequestParam double dayLimit, @RequestParam BigDecimal budget,
            HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        if (!(session.getAttribute("user") instanceof Admin)) return "redirect:/admin/login";
        try {
            entitlements.saveLimits(staffId, year, dayLimit, budget);
            redirect.addFlashAttribute("success", "Annual allowance saved.");
            return "redirect:/admin/entitlements?year=" + year;
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/entitlements/" + staffId + "?year=" + year;
        }
    }

  // this part below is about excludeddays
  // -------------------------------------

    @GetMapping("/excludedDays")
    public String showExcludedDays(Model model) {
        List<ExcludedDays> dayList =excludedDaysService.getAllExcludedDays();
        model.addAttribute("excludedDaysList", dayList);
        model.addAttribute("newExcludedDay", new ExcludedDays());
        return "ExcludedDaysList";
    }

    @PostMapping("/excludedDays/add")
    public String addExcludedDays(@Valid @ModelAttribute("newExcludedDay") ExcludedDays excludedDays,BindingResult result,Model model) {
        if (result.hasErrors()) {
        model.addAttribute(
                "excludedDaysList",
                excludedDaysService.getAllExcludedDays());
        return "ExcludedDaysList";
    }
    excludedDaysService.addExcludedDay(excludedDays);
        return "redirect:/admin/excludedDays";
    }

    @GetMapping("/excludedDays/edit/{id}")
    public String showEditExcludedDay(
            @PathVariable("id") Integer id,Model model) {
       ExcludedDays excludedDay =excludedDaysService.getExcludedDayById(id);
       model.addAttribute("excludedDay", excludedDay);
       return "ExcludedDaysEdit";
    }

    @PostMapping("/excludedDays/edit/{id}")
    public String updateExcludedDay(
            @PathVariable("id") Integer id,
            @Valid @ModelAttribute("excludedDay") ExcludedDays excludedDay,BindingResult result) {
       if (result.hasErrors()) {
           return "ExcludedDaysEdit";
    }
       excludedDaysService.updateExcludedDay(id, excludedDay);
       return "redirect:/admin/excludedDays";
    }

    @GetMapping("/deleteExcludedDays/{id}")
    public String deleteExcludedDays(@PathVariable("id") Integer id) {
        excludedDaysService.deleteExcludedDay(id);
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