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

    // Keep HTTP forms here and catalogue rules in the service.
    public CourseDetailController(CourseDetailService service, CourseCategoryService categories, CourseProviderService providers) {
        this.service = service;
        this.categories = categories;
        this.providers = providers;
    }

    // Admin can include archived rows when reviewing catalogue data.
    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, Model model) {
        var result = PageSupport.page(service.all(), page, size);
        model.addAttribute("rows", result.getContent());
        model.addAttribute("pageData", result);
        return "admin-course-list";
    }

    // A new form has no database ID supplied by the browser.
    @GetMapping("/new")
    public String create(Model model) {
        return render(null, new CourseForm(), model);
    }

    // Load editable values and the version that was shown to the user.
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Integer id, Model model) {
        return render(id, service.form(id), model);
    }

    // Keep the submitted values when validation or a business rule rejects the form.
    @PostMapping({"/new", "/{id}/edit"})
    public String save(@PathVariable(required = false) Integer id,
            @Valid @ModelAttribute("form") CourseForm form, BindingResult binding,
            Model model, RedirectAttributes redirect) {
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

    // The service decides whether references require archiving or prevent deletion.
    @PostMapping("/{id}/delete")
    public String remove(@PathVariable Integer id, @RequestParam Long version, RedirectAttributes redirect) {
        try {
            service.remove(id, version);
            redirect.addFlashAttribute("success", "Course removed from active use.");
        } catch (ResponseStatusException error) {
            if (error.getStatusCode().value() != 400) throw error;
            redirect.addFlashAttribute("error", error.getReason());
        }
        return "redirect:/admin/courses";
    }

    // GET and invalid POST requests use the same choices and form action.
    private String render(Integer id, CourseForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("editId", id);
        model.addAttribute("formAction", id == null ? "/admin/courses/new" : "/admin/courses/" + id + "/edit");
        model.addAttribute("categories", categories.getAllCategories());
        model.addAttribute("providers", providers.all());
        return "admin-course-form";
    }
}
