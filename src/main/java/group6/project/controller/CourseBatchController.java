package group6.project.controller;

import group6.project.form.CourseBatchForm;
import group6.project.service.*;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/batches")
public class CourseBatchController {
    private final CourseBatchService service;
    private final CourseDetailService courses;

    // Keep HTTP forms here and catalogue rules in the service.
    public CourseBatchController(CourseBatchService service, CourseDetailService courses) {
        this.service = service;
        this.courses = courses;
    }

    // Admin can include archived rows when reviewing catalogue data.
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        var result = PageSupport.page(service.getAllBatches(), page, size);
        model.addAttribute("rows", result.getContent());
        model.addAttribute("pageData", result);
        return "course-batch-list";
    }

    // A new form has no database ID supplied by the browser.
    @GetMapping("/new")
    public String create(Model model) {
        return render(null, new CourseBatchForm(), model);
    }

    // Load editable values and the version that was shown to the user.
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        return render(id, service.form(id), model);
    }

    // Keep the submitted values when validation or a business rule rejects the form.
    @PostMapping({"/new", "/{id}/edit"})
    public String save(
            @PathVariable(required = false) Long id,
            @Valid @ModelAttribute("form") CourseBatchForm form,
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
        redirect.addFlashAttribute("success", "Schedule saved.");
        return "redirect:/admin/batches";
    }

    // The service decides whether references require archiving or prevent deletion.
    @PostMapping("/{id}/delete")
    public String remove(
            @PathVariable Long id, @RequestParam Long version, RedirectAttributes redirect) {
        try {
            service.remove(id, version);
            redirect.addFlashAttribute("success", "Schedule removed from active use.");
        } catch (ResponseStatusException error) {
            if (error.getStatusCode().value() != 400) throw error;
            redirect.addFlashAttribute("error", error.getReason());
        }
        return "redirect:/admin/batches";
    }

    // GET and invalid POST requests use the same choices and form action.
    private String render(Long id, CourseBatchForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("editId", id);
        model.addAttribute(
                "formAction", id == null ? "/admin/batches/new" : "/admin/batches/" + id + "/edit");
        model.addAttribute("courses", courses.all());
        return "course-batch-form";
    }
}
