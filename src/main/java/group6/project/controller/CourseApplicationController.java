// Handles course application forms and personal history.
package group6.project.controller;

import group6.project.form.CatalogueApplicationForm;
import group6.project.form.CourseApplicationForm;
import group6.project.model.CourseApplication;
import group6.project.model.CourseCategoryType;
import group6.project.model.User;
import group6.project.service.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class CourseApplicationController {
    private final NotificationService notifications;
    private final CourseApplicationService applications;
    private final CourseCatalogueService catalogue;
    private final TrainingEntitlementService entitlements;
    private final ApprovalRoutingService routing;

    public CourseApplicationController(
            CourseApplicationService applications,
            CourseCatalogueService catalogue,
            TrainingEntitlementService entitlements,
            ApprovalRoutingService routing,
            NotificationService notifications) {
        this.notifications = notifications;
        this.applications = applications;
        this.catalogue = catalogue;
        this.entitlements = entitlements;
        this.routing = routing;
    }

    // Existing Staff links now lead to the catalogue as the default starting point.
    @GetMapping({
        "/staff/apply",
        "/staff/applications/new",
        "/staff/course-applications/new",
        "/course-applications/new"
    })
    public String start() {
        return "redirect:/staff/courses";
    }

    // Current-year history is the default; employees can also find a future or past request.
    @GetMapping({
        "/staff/personal",
        "/staff/applications",
        "/staff/course-applications",
        "/course-applications"
    })
    public String history(
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        int selectedYear = year == null ? LocalDate.now().getYear() : year;
        User employee = (User) session.getAttribute("user");
        var result =
                PageSupport.page(
                        applications.findForStaffAndYear(employee, selectedYear), page, size);
        model.addAttribute("applications", result.getContent());
        model.addAttribute("pageData", result);
        model.addAttribute("year", selectedYear);
        model.addAttribute("summary", entitlements.summary(employee, selectedYear, null));
        return "staff-history";
    }

    // An external offer can be requested without waiting for Admin to add it to the catalogue.
    @GetMapping("/staff/apply/other")
    public String other(HttpSession session, Model model) {
        return otherForm(null, new CourseApplicationForm(), session, model);
    }

    // Show the offered price and schedules before collecting the employee's reasons.
    @GetMapping("/staff/courses/{courseId}/apply")
    public String catalogue(
            @PathVariable Integer courseId,
            @RequestParam(required = false) Long batchId,
            HttpSession session,
            Model model) {
        CatalogueApplicationForm form = new CatalogueApplicationForm();
        form.setCourseVersion(catalogue.offer(courseId).getVersion());
        for (var choice : catalogue.schedules(courseId)) {
            if (choice.batch().getBatchId().equals(batchId)) {
                form.setBatchId(batchId);
                form.setBatchVersion(choice.batch().getVersion());
            }
        }
        return catalogueForm(courseId, null, form, session, model);
    }

    // Copy the editable fields into a small form, never bind the persisted application itself.
    @GetMapping({
        "/staff/applications/{id}/edit",
        "/staff/course-applications/{id}/edit",
        "/course-applications/{id}/edit"
    })
    public String edit(@PathVariable Integer id, HttpSession session, Model model) {
        CourseApplication course = applications.getOwned(id, (User) session.getAttribute("user"));
        applications.requirePending(course);
        if (course.getCatalogueCourse() != null) {
            CatalogueApplicationForm form = new CatalogueApplicationForm();
            form.setVersion(course.getVersion());
            form.setCourseStartDate(course.getCourseStartDate());
            form.setCourseEndDate(course.getCourseEndDate());
            form.setHalfDayPeriod(course.getHalfDayPeriod());
            form.setJustification(course.getJustification());
            form.setWorkDissemination(course.getWorkDissemination());
            return catalogueForm(
                    course.getCatalogueCourse().getCourseId(), id, form, session, model);
        }
        CourseApplicationForm form = new CourseApplicationForm();
        form.setVersion(course.getVersion());
        form.setCourseTitle(course.getCourseTitle());
        form.setTrainingProvider(course.getTrainingProvider());
        form.setCourseCategory(course.getCourseCategory());
        form.setCourseFee(course.getCourseFee());
        form.setCourseStartDate(course.getCourseStartDate());
        form.setCourseEndDate(course.getCourseEndDate());
        form.setHalfDayPeriod(course.getHalfDayPeriod());
        form.setJustification(course.getJustification());
        form.setWorkDissemination(course.getWorkDissemination());
        return otherForm(id, form, session, model);
    }

    // Bean errors and business errors both keep the employee's input on the page.
    @PostMapping({"/staff/apply/other", "/staff/applications/{id}/edit"})
    public String saveOther(
            @PathVariable(required = false) Integer id,
            @Valid @ModelAttribute("form") CourseApplicationForm form,
            BindingResult binding,
            HttpSession session,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect) {
        if (!binding.hasErrors()) {
            try {
                User employee = (User) session.getAttribute("user");
                CourseApplication saved =
                        id == null
                                ? applications.createOther(form, employee)
                                : applications.updateOther(id, form, employee);
                if (!notifications.submitted(saved))
                    redirect.addFlashAttribute(
                            "warning",
                            "Application saved. Email was not sent; the Manager can review it in"
                                    + " CATS.");
                redirect.addFlashAttribute("success", "Application saved.");
                return "redirect:/staff/applications/" + saved.getCourseId();
            } catch (ResponseStatusException error) {
                if (error.getStatusCode().value() != 400) throw error;
                binding.reject("application", error.getReason());
            }
        }
        response.setStatus(400);
        return otherForm(id, form, session, model);
    }

    // The browser cannot change the catalogue price, provider, applicant or decision.
    @PostMapping({"/staff/courses/{courseId}/apply", "/staff/applications/{id}/catalogue-edit"})
    public String saveCatalogue(
            @PathVariable(required = false) Integer courseId,
            @PathVariable(required = false) Integer id,
            @Valid @ModelAttribute("form") CatalogueApplicationForm form,
            BindingResult binding,
            HttpSession session,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect) {
        User employee = (User) session.getAttribute("user");
        if (id != null) {
            CourseApplication saved = applications.getOwned(id, employee);
            if (saved.getCatalogueCourse() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Use the other-course form.");
            }
            courseId = saved.getCatalogueCourse().getCourseId();
        }
        if (!binding.hasErrors()) {
            try {
                CourseApplication saved =
                        id == null
                                ? applications.createFromCatalogue(courseId, form, employee)
                                : applications.updateCatalogue(id, form, employee);
                if (!notifications.submitted(saved))
                    redirect.addFlashAttribute(
                            "warning",
                            "Application saved. Email was not sent; the Manager can review it in"
                                    + " CATS.");
                redirect.addFlashAttribute("success", "Application saved.");
                return "redirect:/staff/applications/" + saved.getCourseId();
            } catch (ResponseStatusException error) {
                if (error.getStatusCode().value() != 400) throw error;
                binding.reject("application", error.getReason());
            }
        }
        response.setStatus(400);
        return catalogueForm(courseId, id, form, session, model);
    }

    // Show the saved snapshot and decision rather than the current catalogue entry.
    @GetMapping({
        "/staff/applications/{id}",
        "/staff/course-applications/{id}",
        "/course-applications/{id}"
    })
    public String details(@PathVariable Integer id, HttpSession session, Model model) {
        model.addAttribute(
                "course", applications.getOwned(id, (User) session.getAttribute("user")));
        model.addAttribute("today", LocalDate.now());
        return "staff-application-detail";
    }

    // These actions keep history and carry a version from the page being submitted.
    @PostMapping("/staff/applications/{id}/{action:delete|cancel|complete}")
    public String changeStatus(
            @PathVariable Integer id,
            @PathVariable String action,
            @RequestParam Long version,
            @RequestParam(required = false) String experienceComments,
            HttpSession session,
            RedirectAttributes redirect) {
        User employee = (User) session.getAttribute("user");
        try {
            switch (action) {
                case "delete" -> applications.delete(id, version, employee);
                case "cancel" -> applications.cancel(id, version, employee);
                case "complete" -> applications.complete(id, version, experienceComments, employee);
            }
            redirect.addFlashAttribute("success", "Application updated.");
        } catch (ResponseStatusException error) {
            if (error.getStatusCode().value() != 400) throw error;
            redirect.addFlashAttribute("error", error.getReason());
            redirect.addFlashAttribute("experienceComments", experienceComments);
        }
        return "redirect:/staff/applications/" + id;
    }

    // Both GET and invalid POST requests use the same choices and annual summary.
    private String otherForm(
            Integer id, CourseApplicationForm form, HttpSession session, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("editId", id);
        model.addAttribute(
                "formAction",
                id == null ? "/staff/apply/other" : "/staff/applications/" + id + "/edit");
        model.addAttribute("categories", CourseCategoryType.values());
        formContext(id, form.getCourseStartDate(), session, model);
        return "staff-application-form";
    }

    // Existing applications remain editable even when the catalogue offer has been archived.
    private String catalogueForm(
            Integer courseId,
            Integer id,
            CatalogueApplicationForm form,
            HttpSession session,
            Model model) {
        CourseApplication existing =
                id == null ? null : applications.getOwned(id, (User) session.getAttribute("user"));
        model.addAttribute("form", form);
        model.addAttribute("editId", id);
        model.addAttribute(
                "offer",
                existing == null ? catalogue.offer(courseId) : existing.getCatalogueCourse());
        model.addAttribute("existing", existing);
        model.addAttribute(
                "schedules", existing == null ? catalogue.schedules(courseId) : List.of());
        model.addAttribute(
                "formAction",
                id == null
                        ? "/staff/courses/" + courseId + "/apply"
                        : "/staff/applications/" + id + "/catalogue-edit");
        formContext(id, form.getCourseStartDate(), session, model);
        return "catalogue-application-form";
    }

    // The page offers a reviewer choice only to a Manager without a reporting manager.
    private void formContext(Integer id, LocalDate start, HttpSession session, Model model) {
        User employee = (User) session.getAttribute("user");
        int year = start == null ? LocalDate.now().getYear() : start.getYear();
        if (year < 2000 || year > 2100) year = LocalDate.now().getYear();
        model.addAttribute("employee", employee);
        model.addAttribute("reviewers", id == null ? routing.choices(employee) : List.of());
        model.addAttribute("summary", entitlements.summary(employee, year, id));
        model.addAttribute("year", year);
        model.addAttribute("today", LocalDate.now().plusDays(1));
    }
}
