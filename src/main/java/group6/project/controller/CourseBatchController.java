package group6.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import group6.project.model.form.CatalogueBatchForm;
import group6.project.service.CourseBatchService;
import group6.project.service.CourseDetailService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/batches")
public class CourseBatchController {
    private final CourseBatchService batches;
    private final CourseDetailService courses;

    // Session maintenance stays separate from the employee application workflow.
    public CourseBatchController(CourseBatchService batches, CourseDetailService courses) { this.batches = batches; this.courses = courses; }

    // Include old and archived sessions so Admin can inspect booking history.
    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size, Model model) { var result=PageSupport.page(batches.getAllBatches(),page,size); model.addAttribute("batches",result.getContent()); model.addAttribute("pageData",result); return "course-batch-list"; }

    // Existing detail bookmarks now open the same useful maintenance form.
    @GetMapping({"/{id}", "/edit/{id}"})
    public String edit(@PathVariable Long id, Model model) { return form(id, batches.form(batches.get(id)), model); }

    // A course may have several scheduled sessions and may also permit custom dates.
    @GetMapping("/new")
    public String create(@RequestParam(required = false) Integer courseId, Model model) {
        CatalogueBatchForm form = new CatalogueBatchForm(); form.setCourseId(courseId);
        return form(null, form, model);
    }

    // DTO binding and the service protect capacity, dates and original booking references.
    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id, @Valid @ModelAttribute("form") CatalogueBatchForm form,
            BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return form(id, form, model);
        try {
            batches.save(id, form); redirect.addFlashAttribute("success", "Scheduled session saved."); return "redirect:/admin/batches";
        } catch (IllegalArgumentException e) { model.addAttribute("error", e.getMessage()); return form(id, form, model); }
    }

    // Archive instead of deleting a session which may already have applicants.
    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes redirect) {
        batches.archive(id); redirect.addFlashAttribute("success", "Scheduled session archived. Existing applications are unchanged.");
        return "redirect:/admin/batches";
    }

    // Keep the same course selected after validation errors.
    private String form(Long id, CatalogueBatchForm form, Model model) {
        model.addAttribute("batchId", id); model.addAttribute("form", form); model.addAttribute("courses", courses.all());
        return "course-batch-form";
    }
}
