package group6.project.controller;

import group6.project.model.Admin;
import group6.project.service.*;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/reviews")
public class ReviewAssignmentController {
    private final ReviewAssignmentService assignments;
    private final AccountAdminService accounts;
    private final UserService users;

    // The repair page is separate from normal Manager review and never changes a decision.
    public ReviewAssignmentController(
            ReviewAssignmentService assignments, AccountAdminService accounts, UserService users) {
        this.assignments = assignments;
        this.accounts = accounts;
        this.users = users;
    }

    // Show pre-upgrade requests that otherwise have no approval queue.
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int applicationPage,
            @RequestParam(defaultValue = "0") int claimPage,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        model.addAttribute("applications", assignments.applications(applicationPage, size));
        model.addAttribute("claims", assignments.claims(claimPage, size));
        model.addAttribute("managers", accounts.managers());
        return "review-assignments";
    }

    // Admin explicitly selects a reviewer with the version currently displayed in the list.
    @PostMapping("/{type}/{id}")
    public String assign(
            @PathVariable String type,
            @PathVariable Integer id,
            @RequestParam Integer reviewerId,
            @RequestParam Long version,
            HttpSession session,
            RedirectAttributes redirect) {
        if (!(users.currentUser(session) instanceof Admin admin))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        try {
            assignments.assign(type, id, reviewerId, version, admin.getUserId());
            redirect.addFlashAttribute("success", "Reviewer assigned.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/reviews";
    }
}
