package group6.project.controller;

import group6.project.form.ScheduleForm;
import group6.project.model.CourseCategoryType;
import group6.project.service.CourseScheduleService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/schedule")
public class CourseScheduleController {
    private final CourseScheduleService schedules;

    // This is a calculator; saving a batch is a separate, explicit form submission.
    public CourseScheduleController(CourseScheduleService schedules) {
        this.schedules = schedules;
    }

    // Opening the calculator never writes a course schedule.
    @GetMapping
    public String show(Model model) {
        model.addAttribute("form", new ScheduleForm());
        return render(model);
    }

    // Preview the dates and return input errors next to the same form.
    @PostMapping
    public String preview(
            @Valid @ModelAttribute("form") ScheduleForm form, BindingResult binding, Model model) {
        if (!binding.hasErrors()) {
            try {
                model.addAttribute(
                        "schedule",
                        schedules.calculate(
                                form.getCategory(),
                                form.getStartDate(),
                                form.getDays(),
                                form.getHalfDayPeriod()));
            } catch (IllegalArgumentException error) {
                binding.reject("schedule", error.getMessage());
            }
        }
        return render(model);
    }

    // Every response needs category choices, including rejected submissions.
    private String render(Model model) {
        model.addAttribute("categories", CourseCategoryType.values());
        return "course-schedule";
    }
}
