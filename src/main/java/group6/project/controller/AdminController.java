package group6.project.controller;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;

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
import group6.project.model.ExcludedDays;
import group6.project.service.TrainingEntitlementService;
import group6.project.service.ExcludedDaysService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final TrainingEntitlementService entitlements;

    public AdminController(
            TrainingEntitlementService entitlements) {
       this.entitlements = entitlements;
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

    // Reporting managers now use actual employee identities, rather than unused role-level rows.
    @GetMapping({"/hierarchy", "/staffs"})
    public String accountsEntry() { return "redirect:/admin/accounts"; }
}
