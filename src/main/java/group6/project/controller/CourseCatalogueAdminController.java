package group6.project.controller;

import group6.project.model.form.CatalogueCourseForm;
import group6.project.service.CourseDetailService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/courses")
public class CourseCatalogueAdminController {
    private final CourseDetailService courses;

    // Admin is checked centrally before any catalogue route runs.
    public CourseCatalogueAdminController(CourseDetailService courses) {
        this.courses = courses;
    }

    // Show published and archived offers with their provider and date options.
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        var result = PageSupport.page(courses.all(), page, size);
        model.addAttribute("courses", result.getContent());
        model.addAttribute("pageData", result);
        return "admin-course-list";
    }

    // New offers start as an empty DTO rather than a bindable JPA entity.
    @GetMapping("/new")
    public String create(Model model) {
        return form(null, new CatalogueCourseForm(), model);
    }

    // Load the saved version so stale edits can be rejected on POST.
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        return form(id, courses.form(courses.get(id)), model);
    }

    // Preserve entered fields and show server validation errors on the same form.
    @PostMapping("/save")
    public String save(
            @RequestParam(required = false) Integer id,
            @Valid @ModelAttribute("form") CatalogueCourseForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect) {
        if (binding.hasErrors()) return form(id, form, model);
        try {
            courses.save(id, form);
            redirect.addFlashAttribute("success", "Course saved.");
            return "redirect:/admin/courses";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return form(id, form, model);
        }
    }

    // Archiving uses POST and keeps application history intact.
    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Integer id, RedirectAttributes redirect) {
        courses.archive(id);
        redirect.addFlashAttribute(
                "success", "Course archived. Existing applications are unchanged.");
        return "redirect:/admin/courses";
    }

    // One form serves create/edit with only supported business categories available.
    private String form(Integer id, CatalogueCourseForm form, Model model) {
        model.addAttribute("courseId", id);
        model.addAttribute("form", form);
        model.addAttribute("categories", courses.categories());
        return "admin-course-form";
    }
}
