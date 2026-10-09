package group6.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;

import group6.project.model.Manager;
import group6.project.service.ManagerService;
import group6.project.service.UserService;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/manager")
public class ManagerController {

    private final UserService userService;
    private final ManagerService managerService;

    public ManagerController(ManagerService managerService, UserService userService) {
        this.managerService = managerService;
        this.userService = userService;
    }

    @GetMapping({"", "/home"})
    public String managerHome(HttpSession session, Model model) {
        Manager manager = signedInManager(session);
        if (manager == null) {
            return "redirect:/employee/login";
        }

        model.addAttribute("currentUser", manager);
        model.addAttribute("pendingCount", managerService.getPendingApplicationGroups(manager.getUserId()).stream()
                .mapToInt(group -> group.applications().size()).sum());
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
        model.addAttribute("support", managerService.getDecisionSupport(manager.getUserId(), applicationId));
        return "manager-application-detail";
    }

    // POST decisions use session identity, not a manager ID submitted by the browser.
    @PostMapping("/applications/{applicationId}/decision")
    public String decide(@PathVariable Integer applicationId, @RequestParam String decision,
            @RequestParam(defaultValue = "") String reason, @RequestParam Long version,
            HttpSession session, RedirectAttributes redirect) {
        Manager manager = signedInManager(session);
        if (manager == null) return "redirect:/employee/login";
        try {
            managerService.decide(manager.getUserId(), applicationId, decision, reason, version);
            redirect.addFlashAttribute("success", "approve".equals(decision) ? "Application approved." : "Application rejected.");
            return "redirect:/manager/approvals";
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) throw e;
            redirect.addFlashAttribute("error", e.getReason());
            redirect.addFlashAttribute("decisionReason", reason);
            return "redirect:/manager/applications/" + applicationId;
        }
    }

    // The history selector contains only direct reports and defaults to the current calendar year.
    @GetMapping("/history")
    public String teamHistory(@RequestParam(required = false) Integer employeeId,
            HttpSession session, Model model) {
        Manager manager = signedInManager(session);
        if (manager == null) return "redirect:/employee/login";
        model.addAttribute("currentUser", manager);
        model.addAttribute("employees", managerService.getSubordinates(manager.getUserId()));
        model.addAttribute("employeeId", employeeId);
        model.addAttribute("year", LocalDate.now().getYear());
        model.addAttribute("courses", employeeId == null ? java.util.List.of()
                : managerService.getEmployeeHistory(manager.getUserId(), employeeId));
        return "manager-history";
    }

    // Each Manager route checks the shared session role before accessing team records.
    private Manager signedInManager(HttpSession session) {
        return userService.currentUser(session) instanceof Manager manager
                && manager.getUserId() != null ? manager : null;
    }
}
