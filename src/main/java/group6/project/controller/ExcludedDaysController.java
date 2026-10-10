package group6.project.controller;

import group6.project.form.HolidayForm;
import group6.project.model.ExcludedDays;
import group6.project.service.ExcludedDaysService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/excluded-days")
public class ExcludedDaysController {
    private final ExcludedDaysService holidays;

    // Holiday rules are shared with application dates and course schedules.
    public ExcludedDaysController(ExcludedDaysService holidays) {
        this.holidays = holidays;
    }

    // Paginate configured dates and keep the small add form on the same page.
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        var result = PageSupport.page(holidays.getAllExcludedDays(), page, size);
        model.addAttribute("excludedDaysList", result.getContent());
        model.addAttribute("pageData", result);
        if (!model.containsAttribute("newExcludedDay"))
            model.addAttribute("newExcludedDay", new HolidayForm());
        return "ExcludedDaysList";
    }

    // Only date and description come from the form, never an existing entity ID.
    @PostMapping("/add")
    public String add(
            @Valid @ModelAttribute("newExcludedDay") HolidayForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect) {
        if (!binding.hasErrors()) {
            try {
                holidays.addExcludedDay(values(form));
            } catch (ResponseStatusException error) {
                if (error.getStatusCode().value() != 400 && error.getStatusCode().value() != 409)
                    throw error;
                binding.reject("holiday", error.getReason());
            }
        }
        if (binding.hasErrors()) return list(0, 10, model);
        redirect.addFlashAttribute("success", "Public holiday added.");
        return "redirect:/excluded-days";
    }

    // Put the saved version in the form to detect another administrator's edit.
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        ExcludedDays holiday = holidays.getExcludedDayById(id);
        HolidayForm form = new HolidayForm();
        form.setDate(holiday.getDate());
        form.setDescription(holiday.getDescription());
        form.setVersion(holiday.getVersion());
        model.addAttribute("excludedDay", form);
        model.addAttribute("holidayId", id);
        return "ExcludedDaysEdit";
    }

    // Keep entered values when a date is already in use or a field is missing.
    @PostMapping("/edit/{id}")
    public String update(
            @PathVariable Integer id,
            @Valid @ModelAttribute("excludedDay") HolidayForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect) {
        if (!binding.hasErrors()) {
            try {
                holidays.updateExcludedDay(id, values(form));
            } catch (ResponseStatusException error) {
                if (error.getStatusCode().value() != 400) throw error;
                binding.reject("holiday", error.getReason());
            }
        }
        if (binding.hasErrors()) {
            model.addAttribute("holidayId", id);
            return "ExcludedDaysEdit";
        }
        redirect.addFlashAttribute("success", "Public holiday updated.");
        return "redirect:/excluded-days";
    }

    // A referenced holiday cannot silently change already calculated training days.
    @PostMapping("/delete/{id}")
    public String delete(
            @PathVariable Integer id, @RequestParam Long version, RedirectAttributes redirect) {
        holidays.deleteExcludedDay(id, version);
        redirect.addFlashAttribute("success", "Public holiday deleted.");
        return "redirect:/excluded-days";
    }

    // Copy the three permitted form values, leaving identity under service control.
    private ExcludedDays values(HolidayForm form) {
        ExcludedDays day = new ExcludedDays();
        day.setDate(form.getDate());
        day.setDescription(form.getDescription().trim());
        day.setVersion(form.getVersion());
        return day;
    }
}
