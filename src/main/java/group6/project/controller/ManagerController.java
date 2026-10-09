package group6.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import group6.project.model.Manager;
import group6.project.service.ManagerService;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/manager")
public class ManagerController {

    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @GetMapping({"", "/home"})
    public String managerHome(HttpSession session, Model model) {
        Manager manager = signedInManager(session);
        if (manager == null) {
            return "redirect:/employee/login";
        }

        model.addAttribute("currentUser", manager);
        return "manager-home";
    }

    @GetMapping("/approvals")
    public String pendingApplications(HttpSession session, Model model) {
        Manager manager = signedInManager(session);
        if (manager == null) {
            return "redirect:/employee/login";
        }
        var groups = managerService.getPendingApplicationGroups(manager.getUserId());
        model.addAttribute("currentUser", manager);
        model.addAttribute("groups", groups);
        model.addAttribute("applicationCount", groups.stream()
                .mapToInt(group -> group.applications().size()).sum());
        return "manager-approvals";
    }

    @GetMapping("/applications/{applicationId}")
    public String applicationDetails(@PathVariable Integer applicationId,
            HttpSession session, Model model) {
        Manager manager = signedInManager(session);
        if (manager == null) {
            return "redirect:/employee/login";
        }
        model.addAttribute("currentUser", manager);
        model.addAttribute("courseApplication",
                managerService.getApplicationForManager(manager.getUserId(), applicationId));
        return "manager-application-detail";
    }

    private Manager signedInManager(HttpSession session) {
        return session.getAttribute("user") instanceof Manager manager
                && manager.getUserId() != null ? manager : null;
    }
}
