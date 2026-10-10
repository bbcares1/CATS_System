package group6.project.controller;

import java.util.List;
import java.util.Optional;
import java.util.Comparator;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.mail.MailException;

import group6.project.model.Admin;
import group6.project.model.AdminEmailForm;
import group6.project.model.ApprovalHierarchy;
import group6.project.model.CourseCategory;
import group6.project.model.CourseBatch;
import group6.project.model.CourseDetail;
import group6.project.model.Roles;
import group6.project.model.Staff;
import group6.project.model.User;
import group6.project.service.AdminService;
import group6.project.service.AdminEmailService;
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

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final group6.project.service.TrainingEntitlementService entitlements;
    private final AdminService adminService;
    private final AdminEmailService adminEmailService;

    public AdminController(
            AdminService adminService,
            AdminEmailService adminEmailService,
            group6.project.service.TrainingEntitlementService entitlements) {
       this.entitlements = entitlements;
       this.adminService = adminService;
       this.adminEmailService = adminEmailService;
    }


    @GetMapping("/home")
    public String adminHome(HttpSession session, Model model) {

        if (!(session.getAttribute("user") instanceof Admin admin)) {
            return "redirect:/admin/login";
        }

        model.addAttribute("currentUser", admin);
        return "admin-home";
    }

    @GetMapping("/emails")
    public String showEmailRecipients(
            @RequestParam(required = false) String compose,
            @RequestParam(required = false) String recipientEmail,
            HttpSession session, Model model) {
        if (!(session.getAttribute("user") instanceof Admin)) {
            return "redirect:/admin/login";
        }
        boolean composeMode = "true".equalsIgnoreCase(compose);
        model.addAttribute("composeMode", composeMode);
        if (composeMode) {
            AdminEmailForm form = new AdminEmailForm();
            form.setRecipientEmail(recipientEmail);
            model.addAttribute("emailForm", form);
        } else {
            List<User> recipients = adminService.viewList().stream()
                    .filter(user -> user.getRole() == Roles.MANAGER || user.getRole() == Roles.STAFF)
                    .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            model.addAttribute("recipients", recipients);
        }
        return "admin-email-list";
    }

    @PostMapping("/emails/send")
    public String sendAdminEmail(
            @Valid @ModelAttribute("emailForm") AdminEmailForm form,
            BindingResult result,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!(session.getAttribute("user") instanceof Admin)) {
            return "redirect:/admin/login";
        }
        if (result.hasErrors()) {
            model.addAttribute("composeMode", true);
            return "admin-email-list";
        }

        try {
            adminEmailService.send(form);
        } catch (MailException error) {
            logger.error("Admin email delivery failed. Check the active mail profile and SMTP authentication settings.", error);
            model.addAttribute("composeMode", true);
            model.addAttribute("error",
                    "Email could not be sent. Check the QQ SMTP profile and credentials, then review the application console for diagnostic details.");
            return "admin-email-list";
        } catch (IllegalStateException error) {
            logger.error("Admin email delivery is not configured.", error);
            model.addAttribute("composeMode", true);
            model.addAttribute("error", error.getMessage());
            return "admin-email-list";
        }

        redirectAttributes.addFlashAttribute(
                "success",
                "QQ SMTP accepted the email for delivery. Delivery may take time; check the recipient's spam folder if it does not arrive.");
        return "redirect:/admin/emails";
    }

    // The year selector keeps annual allowances separate from account details.
    @GetMapping({"/budgets", "/entitlements"})
    public String showBudgetList(@RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Model model) {
        int selectedYear = year == null ? LocalDate.now().getYear() : year;
        var result = PageSupport.page(entitlements.rows(selectedYear), page, size);
        model.addAttribute("year", selectedYear);
        model.addAttribute("rows", result.getContent());
        model.addAttribute("pageData", result);
        return "annual-allowance-list";
    }

    // Populate the selected year; a new year starts with the designation's suggested day limit.
    @GetMapping({"/budgets/edit/{id}", "/entitlements/{id}"})
    public String editBudget(@PathVariable Integer id, @RequestParam(required = false) Integer year, Model model) {
        int selectedYear = year == null ? LocalDate.now().getYear() : year;
        Staff staff = entitlements.employee(id);
        var summary = entitlements.summary(staff, selectedYear, null);
        var form = new group6.project.form.AnnualAllowanceForm();
        form.setStaffId(id);
        form.setYear(selectedYear);
        form.setDayLimit(summary.dayLimit());
        form.setBudget(summary.budget());
        return allowanceForm(form, model);
    }

    // Bind only allowance fields; bad input returns to the form with the entered values.
    @PostMapping({"/budgets/save", "/entitlements/save"})
    public String saveBudget(@Valid @ModelAttribute("form") group6.project.form.AnnualAllowanceForm form,
            BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return allowanceForm(form, model);
        try {
            entitlements.saveLimits(form.getStaffId(), form.getYear(), form.getDayLimit(), form.getBudget());
        } catch (org.springframework.web.server.ResponseStatusException error) {
            if (error.getStatusCode().value() != 400) throw error;
            binding.reject("allowance", error.getReason());
            return allowanceForm(form, model);
        }
        redirect.addFlashAttribute("success", "Annual allowance saved.");
        return "redirect:/admin/entitlements?year=" + form.getYear();
    }

    // Show the employee and the designation rule after GETs and rejected form submissions.
    private String allowanceForm(group6.project.form.AnnualAllowanceForm form, Model model) {
        Staff staff = entitlements.employee(form.getStaffId());
        model.addAttribute("staff", staff);
        model.addAttribute("form", form);
        model.addAttribute("suggestedDays", entitlements.suggestedDays(staff.getDesignation()));
        return "annual-allowance-form";
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
