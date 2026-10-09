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
import group6.project.model.AccountForm;
import group6.project.model.ApprovalHierarchy;
import group6.project.model.CourseCategory;
import group6.project.model.CourseBatch;
import group6.project.model.CourseDetail;
import group6.project.model.Roles;
import group6.project.model.Staff;
import group6.project.service.AdminService;
import group6.project.service.CourseCategoryService;
import group6.project.service.ExcludedDaysService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import group6.project.service.CourseScheduleService;
import group6.project.service.CourseBatchService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final String CALENDAR_BATCH_ID = "adminCalendarBatchId";
    private static final String CALENDAR_START_DATE = "adminCalendarStartDate";
    private static final String CALENDAR_WEEKEND_DATES = "adminCalendarWeekendDates";
    private static final String CALENDAR_MONTH = "adminCalendarMonth";

    private final AdminService adminService;
    private final CourseCategoryService courseCategoryService;
    private final ExcludedDaysService excludedDaysService;
    private final CourseScheduleService courseScheduleService;
    private final CourseBatchService courseBatchService;

    public AdminController(
            AdminService adminService,
            CourseCategoryService courseCategoryService,
            ExcludedDaysService excludedDaysService,
            CourseScheduleService courseScheduleService,
            CourseBatchService courseBatchService) {
       this.adminService = adminService;
       this.courseCategoryService = courseCategoryService;
       this.excludedDaysService = excludedDaysService;
       this.courseScheduleService = courseScheduleService;
       this.courseBatchService = courseBatchService;
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
            model.addAttribute("accountRoles", List.of(Roles.STAFF, Roles.MANAGER));
            model.addAttribute("managerList", adminService.getManagerList());
            return "StaffForm";
        }
    }

    @GetMapping("/staffs")
    public String showStaffList(Model model) {
        List<Staff> staffs = adminService.getAllStaff();
        model.addAttribute("staffs", staffs);
        model.addAttribute("admins", adminService.getAllAdmins());
        return "StaffList";
    }

    @GetMapping("/budgets")
    public String showBudgetList(Model model) {
        model.addAttribute("staffs", adminService.getAllStaff());
        return "BudgetList";
    }

    @GetMapping("/budgets/edit/{id}")
    public String editBudget(@PathVariable("id") Integer id, Model model) {
        Optional<Staff> selectedStaff = adminService.getIdStaff(id);
        if (selectedStaff.isEmpty()) {
            return "redirect:/admin/budgets";
        }
        model.addAttribute("staff", selectedStaff.get());
        return "ChangeBudget";
    }

    @PostMapping("/budgets/save")
    public String saveBudget(
            @ModelAttribute("staff") Staff staff,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        Optional<Staff> selectedStaff = staff.getUserId() == null
                ? Optional.empty()
                : adminService.getIdStaff(staff.getUserId());
        if (selectedStaff.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Staff member could not be found");
            return "redirect:/admin/budgets";
        }
        if (staff.getTrainingBudget() == null || staff.getTrainingBudget() < 0) {
            result.rejectValue("trainingBudget", "invalid", "Budget must be zero or greater");
        }
        if (staff.getTrainingDays() == null || staff.getTrainingDays() < 0) {
            result.rejectValue("trainingDays", "invalid", "Training days must be zero or greater");
        }
        if (result.hasErrors()) {
            staff.setName(selectedStaff.get().getName());
            staff.setStaffId(selectedStaff.get().getStaffId());
            staff.setRole(selectedStaff.get().getRole());
            model.addAttribute("staff", staff);
            return "ChangeBudget";
        }

        adminService.updateStaffBudget(
                staff.getUserId(), staff.getTrainingBudget(), staff.getTrainingDays());
        redirectAttributes.addFlashAttribute("success", "Training entitlement updated successfully");
        return "redirect:/admin/budgets";
    }

    @GetMapping("/staffs/add")
    public String createNewStaff(Model model) {
        model.addAttribute("accountForm", new AccountForm());
        model.addAttribute("accountRoles", Roles.values());
        model.addAttribute("managerList", adminService.getManagerList());

        return "StaffForm";
    }

    @PostMapping("/accounts/create")
    public String createAccount(
            @Valid @ModelAttribute("accountForm") AccountForm accountForm,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("accountRoles", Roles.values());
            model.addAttribute("managerList", adminService.getManagerList());
            return "StaffForm";
        }

        try {
            adminService.createAccount(accountForm);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("accountRoles", Roles.values());
            model.addAttribute("managerList", adminService.getManagerList());
            return "StaffForm";
        }

        redirectAttributes.addFlashAttribute(
                "success", "Account created successfully");
        return "redirect:/admin/staffs";
    }

    @PostMapping("/staffs/save")
    public String saveStaff(
            @Valid @ModelAttribute("staff") Staff staff,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("managerList", adminService.getManagerList());
            return "StaffForm";
        }
        adminService.saveStaff(staff);
        redirectAttributes.addFlashAttribute(
                "success", "Staff details saved successfully");
        return "redirect:/admin/staffs";
    }

    @PostMapping("/staffs/delete/{id}")
    public String deleteById(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {
        adminService.deleteStaffById(id);
        redirectAttributes.addFlashAttribute("success", "Staff account deleted successfully");
        return "redirect:/admin/staffs";
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
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate requestedStartDate,
            @RequestParam(required = false) String weekendTrainingDates,
            Model model,
            HttpSession session) {
        if (batchId == null && requestedStartDate == null
                && (weekendTrainingDates == null || weekendTrainingDates.isBlank())) {
            batchId = (Long) session.getAttribute(CALENDAR_BATCH_ID);
            requestedStartDate = (LocalDate) session.getAttribute(CALENDAR_START_DATE);
            weekendTrainingDates = (String) session.getAttribute(CALENDAR_WEEKEND_DATES);
        }
        if (month == null || month.isBlank()) {
            month = (String) session.getAttribute(CALENDAR_MONTH);
        }
        return renderCourseCalendar(
                month, batchId, requestedStartDate, weekendTrainingDates, model, session);
    }

    @PostMapping("/calendar")
    public String calculateCourseCalendar(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate requestedStartDate,
            @RequestParam(required = false) String weekendTrainingDates,
            Model model,
            HttpSession session) {
        return renderCourseCalendar(
                month, batchId, requestedStartDate, weekendTrainingDates, model, session);
    }

    private String renderCourseCalendar(
            String month,
            Long batchId,
            LocalDate requestedStartDate,
            String weekendTrainingDates,
            Model model,
            HttpSession session) {
       model.addAttribute("batches", courseBatchService.getAllBatches());
      // Generate the current month's calendar
       YearMonth selectedMonth = (month == null || month.isBlank())
        ? YearMonth.now()
        : YearMonth.parse(month);
       session.setAttribute(CALENDAR_MONTH, selectedMonth.toString());
       model.addAttribute("currentMonth", selectedMonth.toString());
        LocalDate firstDay = selectedMonth.atDay(1);
        LocalDate lastDay = selectedMonth.atEndOfMonth();
        // Generate calendar
        model.addAttribute(
            "calendars",
            courseScheduleService.generateCalendars(
                    firstDay,lastDay));
       // Load holidays configured by Admin
       List<LocalDate> holidayDates = excludedDaysService.getAllExcludedDays()
                    .stream()
                    .map(day -> day.getDate())
                    .toList();
       model.addAttribute("holidayDates", holidayDates);
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
        boolean selectedExcludedDay = selectedWeekends.stream()
                .anyMatch(excludedDaysService::isExcludedDay);
        if (selectedExcludedDay) {
            model.addAttribute(
                    "scheduleError",
                    "Excluded days cannot be selected for weekend training.");
        }
        model.addAttribute(
            "weekendTrainingDates",
            weekendTrainingDates == null ? "" : weekendTrainingDates);
        if (batchId != null) {
            CourseBatch batch = courseBatchService.getBatchById(batchId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Course batch was not found."));
            if (requestedStartDate == null) {
                requestedStartDate = batch.getCourseStartDate();
            }
            model.addAttribute("selectedBatch", batch);
            model.addAttribute("selectedBatchId", batchId);
            model.addAttribute("requestedStartDate", requestedStartDate);
            if (requestedStartDate != null && batch.getTrainingDays() != null) {
                CourseScheduleService.Schedule schedule =
                        courseScheduleService.calculateSchedule(
                                requestedStartDate,
                                batch.getTrainingDays(),
                                selectedWeekends);
                model.addAttribute("schedule", schedule);
                model.addAttribute(
                        "trainingDates",
                        courseScheduleService.getTrainingDates(schedule, selectedWeekends));
                session.setAttribute(CALENDAR_BATCH_ID, batchId);
                session.setAttribute(CALENDAR_START_DATE, requestedStartDate);
                session.setAttribute(
                        CALENDAR_WEEKEND_DATES,
                        weekendTrainingDates == null ? "" : weekendTrainingDates);
            }
        }
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
    public String saveCourse(
            @Valid @ModelAttribute("course") CourseDetail course,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        Integer categoryId = course.getCourseCategory() == null
                ? null
                : course.getCourseCategory().getCategoryId();
        Optional<CourseCategory> selectedCategory = categoryId == null
                ? Optional.empty()
                : courseCategoryService.getCategoryById(categoryId);
        if (selectedCategory.isEmpty()) {
            result.rejectValue(
                    "courseCategory",
                    "invalid",
                    "Select an existing course category");
        }
        if (result.hasErrors()) {
            model.addAttribute("courseList", adminService.getAllCourseDetails());
            model.addAttribute("categories", courseCategoryService.getAllCategories());
            return "CourseList";
        }
        CourseCategory category = selectedCategory.orElseThrow();
        course.setCourseCategory(category);
        if ("Internal Training".equalsIgnoreCase(category.getCategoryName())) {
            course.setCourseFee(0.0);
        }
        adminService.saveCourse(course);
        redirectAttributes.addFlashAttribute("success", "Course saved successfully");
        return "redirect:/admin/courses";
    }

    @PostMapping("/courses/delete/{id}")
    public String deleteCourse(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {
        adminService.deleteCourseById(id);
        redirectAttributes.addFlashAttribute("success", "Course deleted successfully");
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
    public String saveHierarchy(
            @Valid @ModelAttribute("hierarchy") ApprovalHierarchy hierarchy,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("hierarchies", adminService.getAllApprovalHierarchy());
            model.addAttribute("roles", Roles.values());
            return "HierarchyList";
        }
        adminService.saveHierarchy(hierarchy);
        redirectAttributes.addFlashAttribute("success", "Approval level saved successfully");
        return "redirect:/admin/hierarchy";
    }

    @PostMapping("/hierarchy/delete/{id}")
    public String deleteHierarchy(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {
        adminService.deleteHierarchyById(id);
        redirectAttributes.addFlashAttribute("success", "Approval level deleted successfully");
        return "redirect:/admin/hierarchy";
    }

}