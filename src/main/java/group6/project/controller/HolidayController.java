package group6.project.controller;

import group6.project.model.form.HolidayForm;
import group6.project.service.ExcludedDaysService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/excludedDays")
public class HolidayController {
    private final ExcludedDaysService holidays;

    // Admin maintains the dates used by every course workflow.
    public HolidayController(ExcludedDaysService holidays) {
        this.holidays = holidays;
    }

    // Show the existing calendar and a short new-holiday form.
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        var result = PageSupport.page(holidays.getAllExcludedDays(), page, size);
        model.addAttribute("holidays", result.getContent());
        model.addAttribute("pageData", result);
        model.addAttribute("form", new HolidayForm());
        return "holiday-list";
    }

    // Edit only date/label/version rather than binding a persisted entity from the browser.
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        model.addAttribute("form", holidays.form(id));
        model.addAttribute("id", id);
        return "holiday-edit";
    }

    // Invalid date bindings and business conflicts stay on the form with the entered values.
    @PostMapping({"/add", "/edit/{id}"})
    public String save(
            @PathVariable(required = false) Integer id,
            @ModelAttribute("form") HolidayForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect) {
        try {
            if (binding.hasErrors())
                throw new IllegalArgumentException("Enter a valid holiday date.");
            holidays.save(id, form);
            redirect.addFlashAttribute("success", "Holiday saved.");
            return "redirect:/admin/excludedDays";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("id", id);
            if (id == null) {
                model.addAttribute("holidays", holidays.getAllExcludedDays());
                return "holiday-list";
            }
            return "holiday-edit";
        }
    }

    // Destructive actions require POST and the current row version.
    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Integer id, @RequestParam Long version, RedirectAttributes redirect) {
        try {
            holidays.delete(id, version);
            redirect.addFlashAttribute("success", "Holiday removed.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/excludedDays";
    }
}
