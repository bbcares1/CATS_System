package group6.project.controller;

import group6.project.form.DecisionForm;
import group6.project.model.ApplicationStatus;
import group6.project.model.CourseApplication;
import group6.project.model.Manager;
import group6.project.service.CourseApplicationService;
import group6.project.service.ManagerService;
import group6.project.service.NotificationService;
import group6.project.service.TrainingEntitlementService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/manager")
public class ManagerController {
    private final NotificationService notifications;
    private final ManagerService managers;
    private final CourseApplicationService applications;
    private final TrainingEntitlementService entitlements;

    // Manager pages query through ManagerService; decisions use the application service.
    public ManagerController(
            ManagerService managers,
            CourseApplicationService applications,
            TrainingEntitlementService entitlements,
            NotificationService notifications) {
        this.notifications = notifications;
        this.managers = managers;
        this.applications = applications;
        this.entitlements = entitlements;
    }

    // A Manager keeps the same personal Staff workspace as every other employee.
    @GetMapping({"", "/home"})
    public String home(HttpSession session, Model model) {
        Manager manager = (Manager) session.getAttribute("user");
        model.addAttribute("currentUser", manager);
        model.addAttribute(
                "pendingCount",
                managers.getPendingApplicationGroups(manager.getUserId()).stream()
                        .mapToInt(group -> group.applications().size())
                        .sum());
        return "manager-home";
    }

    // Review requests are grouped by employee, including assigned peer-Manager requests.
    @GetMapping("/approvals")
    public String pending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        Manager manager = (Manager) session.getAttribute("user");
        var groups = managers.getPendingApplicationGroups(manager.getUserId());
        List<CourseApplication> rows = new ArrayList<>();
        for (var group : groups) rows.addAll(group.applications());
        var result = PageSupport.page(rows, page, size);
        List<ManagerService.ApplicationGroup> visible = new ArrayList<>();
        for (var group : groups) {
            List<CourseApplication> courses = new ArrayList<>();
            for (CourseApplication course : group.applications()) {
                if (result.getContent().contains(course)) courses.add(course);
            }
            if (!courses.isEmpty())
                visible.add(
                        new ManagerService.ApplicationGroup(
                                group.employeeId(),
                                group.employeeName(),
                                group.staffId(),
                                courses));
        }
        model.addAttribute("groups", visible);
        model.addAttribute("pageData", result);
        model.addAttribute(
                "applicationCount",
                groups.stream().mapToInt(group -> group.applications().size()).sum());
        return "manager-approvals";
    }

    // The detail page includes annual totals and other approved absences before deciding.
    @GetMapping("/applications/{id}")
    public String details(@PathVariable Integer id, HttpSession session, Model model) {
        CourseApplication course =
                managers.getApplicationForManager(
                        ((Manager) session.getAttribute("user")).getUserId(), id);
        DecisionForm form = new DecisionForm();
        form.setVersion(course.getVersion());
        model.addAttribute("decision", form);
        return detailModel(course, session, model);
    }

    // Invalid reasons remain on screen; stale or unauthorised decisions keep their HTTP errors.
    @PostMapping("/applications/{id}/decision")
    public String decide(
            @PathVariable Integer id,
            @Valid @ModelAttribute("decision") DecisionForm form,
            BindingResult binding,
            HttpSession session,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect) {
        Manager manager = (Manager) session.getAttribute("user");
        CourseApplication course = managers.getApplicationForManager(manager.getUserId(), id);
        if (!binding.hasErrors()) {
            try {
                CourseApplication saved = applications.decide(id, form, manager);
                if (!notifications.decided(saved))
                    redirect.addFlashAttribute(
                            "warning",
                            "Decision saved. Email was not sent; the employee can read it in"
                                    + " CATS.");
                redirect.addFlashAttribute(
                        "success",
                        form.getApproved() ? "Application approved." : "Application rejected.");
                return "redirect:/manager/applications/" + id;
            } catch (ResponseStatusException error) {
                if (error.getStatusCode().value() != 400) throw error;
                binding.reject("decision", error.getReason());
            }
        }
        response.setStatus(400);
        return detailModel(course, session, model);
    }

    // A selected employee must belong to this Manager's actual reporting team.
    @GetMapping("/history")
    public String history(
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        Integer managerId = ((Manager) session.getAttribute("user")).getUserId();
        var rows =
                employeeId == null
                        ? List.<CourseApplication>of()
                        : managers.history(managerId, employeeId);
        var result = PageSupport.page(rows, page, size);
        model.addAttribute("team", managers.team(managerId));
        model.addAttribute("employeeId", employeeId);
        model.addAttribute("applications", result.getContent());
        model.addAttribute("pageData", result);
        model.addAttribute("year", LocalDate.now().getYear());
        return "manager-history";
    }

    // Reading a team member's history does not grant permission to decide someone else's request.
    private String detailModel(CourseApplication course, HttpSession session, Model model) {
        Manager manager = (Manager) session.getAttribute("user");
        model.addAttribute("course", course);
        model.addAttribute(
                "summary",
                entitlements.summary(
                        course.getApplicant(), course.getCourseStartDate().getYear(), null));
        model.addAttribute("overlaps", managers.overlaps(manager.getUserId(), course));
        model.addAttribute(
                "canDecide",
                course.getApprovalManager() != null
                        && course.getApprovalManager().getUserId().equals(manager.getUserId())
                        && (course.getStatus() == ApplicationStatus.APPLIED
                                || course.getStatus() == ApplicationStatus.UPDATED));
        return "manager-application-detail";
    }
}
