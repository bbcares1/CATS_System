// Handles edits to the dates and places offered for a course.
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
    private final CourseScheduleService schedules;

    public CourseBatchController(CourseBatchService service, CourseScheduleService schedules) {
        this.service = service;
        this.schedules = schedules;
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        return render(id, service.form(id), model);
    }

    // Keep the submitted values when validation or a business rule rejects the form.
    @PostMapping("/{id}/edit")
    public String save(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") CourseBatchForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect) {
        form.setCourseId(service.get(id).getCourseDetail().getCourseId());
        if (binding.hasErrors()) return render(id, form, model);
        try {
            service.save(id, form);
        } catch (ResponseStatusException error) {
            if (error.getStatusCode().value() != 400) throw error;
            binding.reject("catalogue", error.getReason());
            return render(id, form, model);
        }
        redirect.addFlashAttribute("success", "Schedule saved.");
        return "redirect:/admin/courses/" + form.getCourseId() + "/edit";
    }

    // The calculator uses this course's category and does not save any changes.
    @PostMapping("/{id}/edit/calculate")
    public String calculate(
            @PathVariable Long id,
            @ModelAttribute("form") CourseBatchForm form,
            BindingResult binding,
            Model model) {
        var batch = service.get(id);
        form.setCourseId(batch.getCourseDetail().getCourseId());
        if (!binding.hasErrors()) {
            try {
                if (form.getDays() == null)
                    throw new IllegalArgumentException("Enter the training days.");
                var result =
                        schedules.calculate(
                                batch.getCourseDetail().getCourseCategory().getKind(),
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
            @PathVariable Long id, @RequestParam Long version, RedirectAttributes redirect) {
        Integer courseId = service.get(id).getCourseDetail().getCourseId();
        try {
            service.remove(id, version);
            redirect.addFlashAttribute("success", "Schedule removed from active use.");
        } catch (ResponseStatusException error) {
            if (error.getStatusCode().value() != 400) throw error;
            redirect.addFlashAttribute("error", error.getReason());
        }
        return "redirect:/admin/courses/" + courseId + "/edit";
    }

    // Reuse the form choices after a validation error.
    private String render(Long id, CourseBatchForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("editId", id);
        model.addAttribute("formAction", "/admin/batches/" + id + "/edit");
        model.addAttribute("course", service.get(id).getCourseDetail());
        return "course-batch-form";
    }
}
