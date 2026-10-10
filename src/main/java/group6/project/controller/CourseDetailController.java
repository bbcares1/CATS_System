// Handles course details, offered dates and the date calculator.
package group6.project.controller;

import group6.project.form.CourseForm;
import group6.project.service.*;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/courses")
public class CourseDetailController {
    private final CourseDetailService service;
    private final CourseCategoryService categories;
    private final CourseProviderService providers;
    private final CourseBatchService batches;
    private final CourseScheduleService schedules;

    public CourseDetailController(
            CourseDetailService service,
            CourseCategoryService categories,
            CourseProviderService providers,
            CourseBatchService batches,
            CourseScheduleService schedules) {
        this.service = service;
        this.categories = categories;
        this.providers = providers;
        this.batches = batches;
        this.schedules = schedules;
    }

    // Admin can include archived rows when reviewing catalogue data.
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        var result = PageSupport.page(service.all(), page, size);
        model.addAttribute("rows", result.getContent());
        model.addAttribute("pageData", result);
        return "admin-course-list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        return render(null, new CourseForm(), model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Integer id, Model model) {
        return render(id, service.form(id), model);
    }

    // Keep the submitted values when validation or a business rule rejects the form.
    @PostMapping({"/new", "/{id}/edit"})
    public String save(
            @PathVariable(required = false) Integer id,
            @Valid @ModelAttribute("form") CourseForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect) {
        if (binding.hasErrors()) return render(id, form, model);
        try {
            service.save(id, form);
        } catch (ResponseStatusException error) {
            if (error.getStatusCode().value() != 400) throw error;
            binding.reject("catalogue", error.getReason());
            return render(id, form, model);
        }
        redirect.addFlashAttribute("success", "Course saved.");
        return "redirect:/admin/courses";
    }

    // Calculate within the current form, keeping all entered course details without saving.
    @PostMapping({"/new/calculate", "/{id}/edit/calculate"})
    public String calculate(
            @PathVariable(required = false) Integer id,
            @ModelAttribute("form") CourseForm form,
            BindingResult binding,
            Model model) {
        if (!binding.hasErrors()) {
            try {
                if (form.getCategoryId() == null || form.getDays() == null) {
                    throw new IllegalArgumentException(
                            "Choose a category and enter the training days.");
                }
                var result =
                        schedules.calculate(
                                categories.get(form.getCategoryId()).getKind(),
                                form.getStartDate(),
                                form.getDays(),
                                form.getHalfDayPeriod());
                form.setStartDate(result.start());
                form.setEndDate(result.end());
            } catch (IllegalArgumentException error) {
                binding.reject("schedule", error.getMessage());
            }
        }
        return render(id, form, model);
    }

    // Remove unused records while keeping existing history.
    @PostMapping("/{id}/delete")
    public String remove(
            @PathVariable Integer id, @RequestParam Long version, RedirectAttributes redirect) {
        try {
            service.remove(id, version);
            redirect.addFlashAttribute("success", "Course removed from active use.");
        } catch (ResponseStatusException error) {
            if (error.getStatusCode().value() != 400) throw error;
            redirect.addFlashAttribute("error", error.getReason());
        }
        return "redirect:/admin/courses";
    }

    // Reuse the form choices after a validation error.
    private String render(Integer id, CourseForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("editId", id);
        model.addAttribute(
                "formAction", id == null ? "/admin/courses/new" : "/admin/courses/" + id + "/edit");
        model.addAttribute("categories", categories.getAllCategories());
        model.addAttribute("providers", providers.all());
        model.addAttribute("schedules", id == null ? java.util.List.of() : batches.forCourse(id));
        return "admin-course-form";
    }
}
