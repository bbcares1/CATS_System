package group6.project.controller;

import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import group6.project.model.*;
import group6.project.model.form.CatalogueApplicationForm;
import group6.project.service.*;
import jakarta.servlet.http.HttpSession;

@Controller
public class CourseCatalogueController {
    private final CourseCatalogueService catalogue;
    private final CourseApplicationService applications;
    private final UserService users;

    // Keep the catalogue workflow alongside the retained other-course form.
    public CourseCatalogueController(CourseCatalogueService catalogue, CourseApplicationService applications, UserService users) {
        this.catalogue = catalogue;
        this.applications = applications;
        this.users = users;
    }

    // Cards show the important fields; filters and the other-course action stay visible.
    @GetMapping({"/staff/courses", "/staff/apply"})
    public String browse(@RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) CourseCategoryType category, @RequestParam(defaultValue = "") String provider,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Model model) {
        var matches = catalogue.search(q, category, provider);
        if (size != 10 && size != 20 && size != 25) size = 10;
        int lastPage = Math.max(0, (matches.size() - 1) / size);
        page = Math.max(0, Math.min(page, lastPage));
        int from = page * size;
        model.addAttribute("courses", matches.subList(from, Math.min(from + size, matches.size())));
        model.addAttribute("total", matches.size());
        model.addAttribute("page", page); model.addAttribute("lastPage", lastPage); model.addAttribute("size", size);
        model.addAttribute("q", q); model.addAttribute("category", category); model.addAttribute("provider", provider);
        model.addAttribute("categories", CourseCategoryType.values());
        model.addAttribute("providers", catalogue.search("", null, "").stream().map(CourseDetail::getTrainingProvider).distinct().sorted().toList());
        return "course-catalogue";
    }

    // The details page explains the course before the employee starts a short application.
    @GetMapping("/staff/courses/{id}")
    public String details(@PathVariable Integer id, Model model) {
        model.addAttribute("course", catalogue.offer(id));
        model.addAttribute("batches", catalogue.schedules(id));
        return "catalogue-course-detail";
    }

    // Catalogue metadata is read-only; only dates, justification and arrangements are entered.
    @GetMapping("/staff/courses/{id}/apply")
    public String apply(@PathVariable Integer id, Model model) {
        var course = catalogue.offer(id);
        var form = new CatalogueApplicationForm(); form.setCourseVersion(course.getVersion());
        return applicationModel(id, form, model);
    }

    // A normal MVC preview shows duration and allowance without saving anything.
    @PostMapping("/staff/courses/{id}/apply")
    public String submit(@PathVariable Integer id, @ModelAttribute("form") CatalogueApplicationForm form,
            BindingResult binding, @RequestParam(defaultValue = "submit") String action,
            HttpSession session, Model model, RedirectAttributes redirect) {
        Staff staff = (Staff) users.currentUser(session);
        if (binding.hasErrors()) {
            model.addAttribute("error", "Please check the selected dates and session.");
            return applicationModel(id, form, model);
        }
        try {
            if ("preview".equals(action)) {
                model.addAttribute("summary", catalogue.preview(id, form, staff));
                return applicationModel(id, form, model);
            }
            if (!"submit".equals(action)) throw new IllegalArgumentException("Choose Check allowance or Submit application.");
            CourseApplication saved = catalogue.submit(id, form, staff);
            redirect.addFlashAttribute("success", "Application submitted.");
            return "redirect:/staff/applications/" + saved.getCourseId();
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return applicationModel(id, form, model);
        }
    }

    // Editing keeps the original offer snapshot, even if Admin has archived or changed it.
    @GetMapping("/staff/applications/{id}/edit-catalogue")
    public String edit(@PathVariable Integer id, HttpSession session, Model model) {
        var course = applications.getOwned(id, (Staff) users.currentUser(session));
        applications.requirePending(course);
        if (course.getCatalogueCourse() == null) return "redirect:/staff/applications/" + id + "/edit";
        var form = new CatalogueApplicationForm(); form.setApplicationVersion(course.getVersion());
        form.setStartDate(course.getCourseStartDate()); form.setEndDate(course.getCourseEndDate());
        form.setHalfDayPeriod(course.getHalfDayPeriod()); form.setJustification(course.getJustification());
        form.setWorkDissemination(course.getWorkDissemination());
        model.addAttribute("course", course); model.addAttribute("form", form); model.addAttribute("today", LocalDate.now().plusDays(1));
        return "catalogue-application-edit";
    }

    // The shared policy still checks pending state, ownership, working dates and annual allowance.
    @PostMapping("/staff/applications/{id}/edit-catalogue")
    public String saveEdit(@PathVariable Integer id, @ModelAttribute("form") CatalogueApplicationForm form,
            BindingResult binding, HttpSession session, Model model, RedirectAttributes redirect) {
        try {
            if (binding.hasErrors()) throw new IllegalArgumentException("Please check the selected dates.");
            catalogue.edit(id, form, (Staff) users.currentUser(session));
            redirect.addFlashAttribute("success", "Application updated.");
            return "redirect:/staff/applications/" + id;
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            var course = applications.getOwned(id, (Staff) users.currentUser(session));
            form.setApplicationVersion(course.getVersion());
            model.addAttribute("course", course); model.addAttribute("form", form); model.addAttribute("today", LocalDate.now().plusDays(1));
            return "catalogue-application-edit";
        }
    }

    // Refresh offer/version information after an error while preserving the employee's entered text.
    private String applicationModel(Integer id, CatalogueApplicationForm form, Model model) {
        var course = catalogue.offer(id); form.setCourseVersion(course.getVersion());
        model.addAttribute("course", course); model.addAttribute("form", form);
        model.addAttribute("batches", catalogue.schedules(id));
        model.addAttribute("today", LocalDate.now().plusDays(1));
        return "catalogue-application-form";
    }
}
