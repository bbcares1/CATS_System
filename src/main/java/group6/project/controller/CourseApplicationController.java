package group6.project.controller;

import java.security.Principal;
import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import group6.project.model.CourseApplication;
import group6.project.model.CourseCategoryType;
import group6.project.model.Staff;
import group6.project.service.CourseApplicationService;
import group6.project.service.CurrentStaffService;
import jakarta.servlet.http.HttpSession;

@Controller
public class CourseApplicationController {
    private final CourseApplicationService courseApplicationService;
    private final CurrentStaffService currentStaffService;

    public CourseApplicationController(CourseApplicationService courseApplicationService,
            CurrentStaffService currentStaffService) {
        this.courseApplicationService = courseApplicationService;
        this.currentStaffService = currentStaffService;
    }

    @GetMapping({"/staff/course-applications", "/course-applications"})
    public String history(Model model, Principal principal, HttpSession session) {
        Staff staff = currentStaffService.requireStaff(principal, session);
        int year = LocalDate.now().getYear();
        model.addAttribute("applications", courseApplicationService.findForStaffAndYear(staff, year));
        model.addAttribute("summary", courseApplicationService.summary(new CourseApplication(), staff, null));
        model.addAttribute("year", year);
        return "course-applications-list";
    }

    @GetMapping({"/staff/course-applications/new", "/course-applications/new"})
    public String newApplication(Model model) {
        addFormModel(model, new CourseApplication(), null);
        return "course-application-form";
    }

    @GetMapping({"/staff/course-applications/{id}", "/course-applications/{id}"})
    public String detail(@PathVariable Integer id, Model model, Principal principal, HttpSession session) {
        Staff staff = currentStaffService.requireStaff(principal, session);
        model.addAttribute("application", courseApplicationService.getOwned(id, staff));
        return "course-application-detail";
    }

    @GetMapping({"/staff/course-applications/{id}/edit", "/course-applications/{id}/edit"})
    public String edit(@PathVariable Integer id, Model model, Principal principal, HttpSession session) {
        Staff staff = currentStaffService.requireStaff(principal, session);
        addFormModel(model, courseApplicationService.getOwned(id, staff), id);
        return "course-application-form";
    }

    @PostMapping({"/staff/course-applications", "/course-applications"})
    public String create(@ModelAttribute("application") CourseApplication form,
            Principal principal, HttpSession session, Model model, RedirectAttributes redirect) {
        Staff staff = currentStaffService.requireStaff(principal, session);
        try {
            courseApplicationService.create(form, staff);
            redirect.addFlashAttribute("success", "Course application submitted.");
            return "redirect:/staff/course-applications";
        } catch (IllegalArgumentException | IllegalStateException error) {
            addFormModel(model, form, null);
            model.addAttribute("error", error.getMessage());
            return "course-application-form";
        }
    }

    @PostMapping({"/staff/course-applications/{id}", "/course-applications/{id}"})
    public String update(@PathVariable Integer id, @ModelAttribute("application") CourseApplication form,
            Principal principal, HttpSession session, Model model, RedirectAttributes redirect) {
        Staff staff = currentStaffService.requireStaff(principal, session);
        try {
            courseApplicationService.update(id, form, staff);
            redirect.addFlashAttribute("success", "Course application updated.");
            return "redirect:/staff/course-applications/" + id;
        } catch (IllegalArgumentException | IllegalStateException error) {
            addFormModel(model, form, id);
            model.addAttribute("error", error.getMessage());
            return "course-application-form";
        }
    }

    @PostMapping({"/staff/course-applications/{id}/delete", "/course-applications/{id}/delete"})
    public String delete(@PathVariable Integer id, Principal principal, HttpSession session,
            RedirectAttributes redirect) {
        courseApplicationService.delete(id, currentStaffService.requireStaff(principal, session));
        redirect.addFlashAttribute("success", "Course application deleted.");
        return "redirect:/staff/course-applications";
    }

    @PostMapping({"/staff/course-applications/{id}/cancel", "/course-applications/{id}/cancel"})
    public String cancel(@PathVariable Integer id, Principal principal, HttpSession session,
            RedirectAttributes redirect) {
        courseApplicationService.cancel(id, currentStaffService.requireStaff(principal, session));
        redirect.addFlashAttribute("success", "Course application cancelled.");
        return "redirect:/staff/course-applications/" + id;
    }

    @PostMapping({"/staff/course-applications/{id}/complete", "/course-applications/{id}/complete"})
    public String complete(@PathVariable Integer id, @RequestParam String experienceComments,
            Principal principal, HttpSession session, RedirectAttributes redirect) {
        courseApplicationService.complete(id, experienceComments,
                currentStaffService.requireStaff(principal, session));
        redirect.addFlashAttribute("success", "Course marked as completed.");
        return "redirect:/staff/course-applications/" + id;
    }

    @PostMapping({"/staff/course-applications/summary", "/course-applications/summary"})
    @ResponseBody
    public CourseApplicationService.Summary summary(@ModelAttribute CourseApplication form,
            @RequestParam(required = false) Integer applicationId,
            Principal principal, HttpSession session) {
        Staff staff = currentStaffService.requireStaff(principal, session);
        return courseApplicationService.summary(form, staff, applicationId);
    }

    private void addFormModel(Model model, CourseApplication application, Integer editId) {
        model.addAttribute("application", application);
        model.addAttribute("categories", CourseCategoryType.values());
        model.addAttribute("editId", editId);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("formAction", editId == null
                ? "/staff/course-applications"
                : "/staff/course-applications/" + editId);
    }
}
