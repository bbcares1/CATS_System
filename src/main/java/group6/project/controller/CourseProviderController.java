// Handles training-provider maintenance forms.
package group6.project.controller;

import group6.project.form.CourseProviderForm;
import group6.project.service.*;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/providers")
public class CourseProviderController {
    private final CourseProviderService service;

    public CourseProviderController(CourseProviderService service) {
        this.service = service;
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
        return "course-provider-list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        return render(null, new CourseProviderForm(), model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Integer id, Model model) {
        return render(id, service.form(id), model);
    }

    // Keep the submitted values when validation or a business rule rejects the form.
    @PostMapping({"/new", "/{id}/edit"})
    public String save(
            @PathVariable(required = false) Integer id,
            @Valid @ModelAttribute("form") CourseProviderForm form,
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
        redirect.addFlashAttribute("success", "Provider saved.");
        return "redirect:/admin/providers";
    }

    // Remove unused records while keeping existing history.
    @PostMapping("/{id}/delete")
    public String remove(
            @PathVariable Integer id, @RequestParam Long version, RedirectAttributes redirect) {
        try {
            service.remove(id, version);
            redirect.addFlashAttribute("success", "Provider removed from active use.");
        } catch (ResponseStatusException error) {
            if (error.getStatusCode().value() != 400) throw error;
            redirect.addFlashAttribute("error", error.getReason());
        }
        return "redirect:/admin/providers";
    }

    // Reuse the form choices after a validation error.
    private String render(Integer id, CourseProviderForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("editId", id);
        model.addAttribute(
                "formAction",
                id == null ? "/admin/providers/new" : "/admin/providers/" + id + "/edit");

        return "course-provider-form";
    }
}
