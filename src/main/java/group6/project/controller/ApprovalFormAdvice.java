package group6.project.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import group6.project.service.*;
import jakarta.servlet.http.HttpSession;

@ControllerAdvice(assignableTypes = {StaffController.class, CourseCatalogueController.class})
public class ApprovalFormAdvice {
    private final ApprovalRoutingService routing;
    private final UserService users;

    // Both application forms and the claim form show the same reviewer choice.
    public ApprovalFormAdvice(ApprovalRoutingService routing, UserService users) { this.routing = routing; this.users = users; }

    // The choice is offered only when the signed-in Manager has no reporting manager.
    @ModelAttribute
    public void reviewerChoices(HttpSession session, Model model) {
        var user = users.currentUser(session);
        model.addAttribute("approvalChoices", user == null ? java.util.List.of() : routing.choices(user));
        model.addAttribute("needsApprovalChoice", user instanceof group6.project.model.Manager && user.getManager() == null);
        model.addAttribute("reportingManagerName", user == null || user.getManager() == null ? null : user.getManager().getName());
    }
}
