// Handles course-category maintenance forms.
package group6.project.controller;

import group6.project.form.CourseCategoryForm;
import group6.project.model.CourseCategoryType;
import group6.project.service.*;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class CourseCategoryController {
    private final CourseCategoryService service;

    public CourseCategoryController(CourseCategoryService service) {
        this.service = service;
    }

    // Admin can include archived rows when reviewing catalogue data.
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        var result = PageSupport.page(service.getAllCategories(), page, size);
        model.addAttribute("rows", result.getContent());
        model.addAttribute("pageData", result);
        return "course-category-list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        return render(null, new CourseCategoryForm(), model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Integer id, Model model) {
        return render(id, service.form(id), model);
    }

    // Keep the submitted values when validation or a business rule rejects the form.
    @PostMapping({"/new", "/{id}/edit"})
    public String save(
            @PathVariable(required = false) Integer id,
            @Valid @ModelAttribute("form") CourseCategoryForm form,
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
        redirect.addFlashAttribute("success", "Category saved.");
        return "redirect:/admin/categories";
    }

    // Remove unused records while keeping existing history.
    @PostMapping("/{id}/delete")
    public String remove(
            @PathVariable Integer id, @RequestParam Long version, RedirectAttributes redirect) {
        try {
            service.delete(id, version);
            redirect.addFlashAttribute("success", "Category removed from active use.");
        } catch (ResponseStatusException error) {
            if (error.getStatusCode().value() != 400) throw error;
            redirect.addFlashAttribute("error", error.getReason());
        }
        return "redirect:/admin/categories";
    }

    // Reuse the form choices after a validation error.
    private String render(Integer id, CourseCategoryForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("editId", id);
        model.addAttribute(
                "formAction",
                id == null ? "/admin/categories/new" : "/admin/categories/" + id + "/edit");
        model.addAttribute("kinds", CourseCategoryType.values());
        return "course-category-form";
    }
}
