package group6.project.controller;

import java.util.List;
import java.util.Optional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import group6.project.model.Admin;
import group6.project.model.ApprovalHierarchy;
import group6.project.model.CourseDetail;
import group6.project.model.ExcludedDays;
import group6.project.model.Roles;
import group6.project.model.Staff;
import group6.project.repo.StaffRepo;
import group6.project.model.User;
import group6.project.service.AdminService;
import group6.project.service.CourseCategoryService;
import group6.project.service.ExcludedDaysService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;
import group6.project.service.CourseScheduleService;
import group6.project.model.CourseApplication;
import group6.project.model.User;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final CourseCategoryService courseCategoryService;
    private final ExcludedDaysService excludedDaysService;
    private final CourseScheduleService courseScheduleService;

    public AdminController(
            AdminService adminService,
            CourseCategoryService courseCategoryService,
            ExcludedDaysService excludedDaysService,CourseScheduleService courseScheduleService) {
       this.adminService = adminService;
       this.courseCategoryService = courseCategoryService;
       this.excludedDaysService = excludedDaysService;
       this.courseScheduleService = courseScheduleService;
    }


    @GetMapping("/home")
    public String adminHome(HttpSession session, Model model) {

        if (!(session.getAttribute("user") instanceof Admin admin)) {
            return "redirect:/admin/login";
        }

        model.addAttribute("currentUser", admin);
        return "admin-home";
    }

    
  // ------- this part below is about budgetmanagement
  // -----------------------------------------------

    @GetMapping("/staffs/update/{Id}")
    public String update_budget(Model model, @PathVariable("Id") Integer Id) {
        Optional<Staff> selectedStaff = adminService.getIdStaff(Id);
        if (selectedStaff.isEmpty()) {
            throw new RuntimeException("can not find ID as " + Id + " staff");
        } else {
            Staff staff = selectedStaff.get();
            model.addAttribute("staff", staff);
            model.addAttribute("role", Roles.values());
            model.addAttribute("managerList", adminService.getManagerList());
            return "StaffForm";
        }
    }

    @GetMapping("/staffs")
    public String showStaffList(Model model) {
        List<Staff> staffs = adminService.getAllStaff();
        model.addAttribute("staffs", staffs);
        return "StaffList";
    }

    @GetMapping("/staffs/add")
    public String createNewStaff(Model model) {
        model.addAttribute("staff", new Staff());
        model.addAttribute("role", Roles.values());
        model.addAttribute("managerList", adminService.getManagerList());

        return "StaffForm";
    }


    @PostMapping("/staffs/save")
    public String saveStaff(Staff staff) {
        adminService.saveStaff(staff);
        return "redirect:/admin/staffs";
    }

    @GetMapping("/staffs/delete/{id}")
    public String deleteById(@PathVariable("id") Integer id) {
        adminService.deleteStaffById(id);
        return "redirect:/admin/staffs";
    }

    // this part below is about excludeddays
    // -------------------------------------

    @GetMapping("/excludedDays")
    public String showExcludedDays(Model model) {
        List<ExcludedDays> dayList = excludedDaysService.getAllExcludedDays();
        model.addAttribute("excludedDaysList", dayList);
        model.addAttribute("newExcludedDay", new ExcludedDays());
        return "ExcludedDaysList";
    }

    @PostMapping("/excludedDays/add")
    public String addExcludedDays(@Valid @ModelAttribute("newExcludedDay") ExcludedDays excludedDays,
            BindingResult result, Model model) {
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
            @PathVariable("id") Integer id, Model model) {
        ExcludedDays excludedDay = excludedDaysService.getExcludedDayById(id);
        model.addAttribute("excludedDay", excludedDay);
        return "ExcludedDaysEdit";
    }

    @PostMapping("/excludedDays/edit/{id}")
    public String updateExcludedDay(
            @PathVariable("id") Integer id,
            @Valid @ModelAttribute("excludedDay") ExcludedDays excludedDay, BindingResult result) {
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
    // this part below is about course schedule calendar
    // -------------------------------------------------
    private Set<LocalDate> parseWeekendTrainingDates(String dates) {
        if (dates == null || dates.isBlank()) {
            return Set.of();
        }
        Set<LocalDate> selectedDates = Arrays.stream(dates.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(LocalDate::parse)
                .collect(Collectors.toSet());
        for (LocalDate date : selectedDates) {
            DayOfWeek day = date.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY
                    && day != DayOfWeek.SUNDAY) {
                throw new IllegalArgumentException(
                        "Weekend training dates must be Saturday or Sunday: "
                        + date);
        }
     }
    return selectedDates;
    }
    @GetMapping("/calendar")
    public String showCourseCalendar(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate requestedStartDate,
            @RequestParam(required = false) String weekendTrainingDates,
            Model model) {
       // Load available courses
       model.addAttribute("courses",courseScheduleService.getAllCourses());
      // Generate the current month's calendar
       YearMonth selectedMonth = (month == null || month.isBlank())
        ? YearMonth.now()
        : YearMonth.parse(month);
        LocalDate firstDay = selectedMonth.atDay(1);
        LocalDate lastDay = selectedMonth.atEndOfMonth();
        // Generate calendar
        model.addAttribute(
            "calendars",
            courseScheduleService.generateCalendars(
                    firstDay,lastDay));
       // Load holidays configured by Admin
       model.addAttribute(
            "holidayDates",
            excludedDaysService.getAllExcludedDays()
                    .stream()
                    .map(ExcludedDays::getDate)
                    .toList());
        model.addAttribute(
              "excludedDays",
              excludedDaysService.getAllExcludedDays());
        // Previous and next month
        model.addAttribute(
            "previousMonth",
            selectedMonth.minusMonths(1).toString());
        model.addAttribute(
            "nextMonth",
            selectedMonth.plusMonths(1).toString());
        // Keep the selected course when switching months
        Set<LocalDate> selectedWeekends =
                parseWeekendTrainingDates(weekendTrainingDates);
        model.addAttribute(
            "weekendTrainingDates",
            weekendTrainingDates == null ? "" : weekendTrainingDates);
        if (courseId != null && requestedStartDate != null) {
            CourseApplication course =courseScheduleService.getCourse(courseId);
        if (course.getTrainingDays() != null && course.getTrainingDays() > 0) {
            CourseScheduleService.Schedule schedule =
                    courseScheduleService.calculateSchedule(
                            requestedStartDate,
                            course.getTrainingDays(),
                            selectedWeekends);
        model.addAttribute("selectedCourse", course);
        model.addAttribute("schedule", schedule);
        model.addAttribute("trainingDates",
                     courseScheduleService.getTrainingDates(schedule,selectedWeekends));
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("requestedStartDate", requestedStartDate);
        }
      }
        return "CourseCalendar";
    }
    @PostMapping("/calendar")
    public String calculateCourseSchedule(
            @RequestParam Integer courseId,@RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate requestedStartDate,
            @RequestParam(required = false) String weekendTrainingDates,
            Model model) {
       CourseApplication course =courseScheduleService.getCourse(courseId);
       Set<LocalDate> selectedWeekends =
               parseWeekendTrainingDates(weekendTrainingDates);
       if (course.getTrainingDays() == null || course.getTrainingDays() <= 0) {
           throw new IllegalArgumentException(
                "Training days are not available for this course.");
       } 
        CourseScheduleService.Schedule schedule =courseScheduleService.calculateSchedule(
                requestedStartDate,
                course.getTrainingDays(),
                selectedWeekends);
        model.addAttribute("courses",courseScheduleService.getAllCourses());
        model.addAttribute("selectedCourse", course);
        model.addAttribute("schedule", schedule);
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("requestedStartDate", requestedStartDate);
        YearMonth selectedMonth =YearMonth.from(schedule.actualStartDate());
        model.addAttribute("calendars",
                 courseScheduleService.generateCalendars(
                         selectedMonth.atDay(1),
                         selectedMonth.atEndOfMonth()));
        model.addAttribute("previousMonth",
                 selectedMonth.minusMonths(1).toString());
        model.addAttribute("nextMonth",
                 selectedMonth.plusMonths(1).toString());
        model.addAttribute("holidayDates",
                 excludedDaysService.getAllExcludedDays().stream().map    (ExcludedDays::getDate).toList());
        model.addAttribute(
              "excludedDays",
              excludedDaysService.getAllExcludedDays());
        model.addAttribute("trainingDates",
                 courseScheduleService.getTrainingDates(schedule,selectedWeekends));
        model.addAttribute("weekendTrainingDates",
        weekendTrainingDates == null ? "" : weekendTrainingDates);
        return "CourseCalendar";
    }
    // -------------------------------------------------

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

    // this part below is about Approval Hierarchy
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

    // CreateAccount
    @PostMapping("/accounts/create")
    public String createAccount(
            @RequestParam String userName,
            @RequestParam String name,
            @RequestParam(required = false)
                String designation,
            @RequestParam String accountType,
            @RequestParam(required = false)
                String staffNo,
            @RequestParam String password,
            RedirectAttributes redirectAttributes) {

        try {
            adminService.createAccount(
                userName,
                name,
                designation,
                accountType,
                staffNo,
                password);

            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Account created successfully");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                "errorMessage", e.getMessage());
        }

        return "redirect:/admin/showBudgetList";
    }

    // DeleteAccount
    @PostMapping("/accounts/delete/{id}")
    public String deleteAccount(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            adminService.deleteAccount(id);

            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Account deleted successfully");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                "errorMessage", e.getMessage());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Account is referenced by other records");
        }

        return "redirect:/admin/showBudgetList";
    }

    // updateAccount
    @PostMapping("/accounts/update/{id}")
    public String updateAccount(
            @PathVariable("id") Integer id,
            @RequestParam String userName,
            @RequestParam String name,
            @RequestParam(required = false)
                String designation,
            RedirectAttributes redirectAttributes) {

        try {
            adminService.updateAccount(
                id, userName, name, designation);

            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Account updated successfully");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                "errorMessage", e.getMessage());
        }

        return "redirect:/admin/showBudgetList";
    }

}