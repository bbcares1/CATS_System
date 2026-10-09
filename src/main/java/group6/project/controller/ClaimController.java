package group6.project.controller;

import group6.project.model.*;
import group6.project.service.*;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

@Controller
public class ClaimController {
    private final CourseFeeApplicationService claims;
    private final UserService users;

    // Claim review and actual payment have separate MVC pages and role checks.
    public ClaimController(CourseFeeApplicationService claims, UserService users) {
        this.claims = claims;
        this.users = users;
    }

    // List claims waiting for this manager, including peer requests explicitly assigned to them.
    @GetMapping("/manager/claims")
    public String queue(
            HttpSession session,
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var queue = claims.pending(manager(session).getUserId(), page, size);
        model.addAttribute("claims", queue.getContent());
        model.addAttribute("claimPage", queue);
        model.addAttribute("claimRoute", "/manager/claims");
        return "manager-claims";
    }

    // The manager reads the evidence before approving or rejecting a claim.
    @GetMapping("/manager/claims/{id}")
    public String detail(@PathVariable Integer id, HttpSession session, Model model) {
        model.addAttribute("claim", reviewClaim(id, manager(session)));
        model.addAttribute("managerView", true);
        return "claim-detail";
    }

    // A required reason and the displayed version are submitted through a normal form.
    @PostMapping("/manager/claims/{id}/decision")
    public String decide(
            @PathVariable Integer id,
            @RequestParam String decision,
            @RequestParam(defaultValue = "") String reason,
            @RequestParam Long version,
            HttpSession session,
            RedirectAttributes redirect) {
        try {
            claims.decide(id, manager(session).getUserId(), decision, reason, version);
            redirect.addFlashAttribute(
                    "success", "Claim " + ("approve".equals(decision) ? "approved." : "rejected."));
            return "redirect:/manager/claims";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addFlashAttribute("reason", reason);
            return "redirect:/manager/claims/" + id;
        }
    }

    // Evidence is always downloaded as an attachment, never executed inline in the browser.
    @GetMapping("/manager/claims/{id}/{document}")
    public ResponseEntity<byte[]> document(
            @PathVariable Integer id, @PathVariable String document, HttpSession session) {
        CourseFeeApplication claim = reviewClaim(id, manager(session));
        boolean receipt = "receipt".equals(document);
        if (!receipt && !"certificate".equals(document)) return ResponseEntity.notFound().build();
        byte[] bytes = receipt ? claim.getReceipt() : claim.getCertificate();
        if (bytes == null) return ResponseEntity.notFound().build();
        String filename = receipt ? claim.getReceiptFileName() : claim.getCertificateFileName();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("X-Content-Type-Options", "nosniff")
                .header(
                        "Content-Disposition",
                        ContentDisposition.attachment()
                                .filename(
                                        filename == null ? document : filename,
                                        StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(bytes);
    }

    // Only approved claims appear here; paid entries remain visible.
    @GetMapping("/admin/payments")
    public String payments(
            HttpSession session,
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        admin(session);
        var payments = claims.approved(page, size);
        model.addAttribute("claims", payments.getContent());
        model.addAttribute("claimPage", payments);
        model.addAttribute("claimRoute", "/admin/payments");
        return "admin-payments";
    }

    // Admin records the real payment reference once; this does not approve a claim.
    @PostMapping("/admin/payments/{id}")
    public String reimburse(
            @PathVariable Integer id,
            @RequestParam String reference,
            @RequestParam Long version,
            HttpSession session,
            RedirectAttributes redirect) {
        try {
            claims.reimburse(id, admin(session).getUserId(), reference, version);
            redirect.addFlashAttribute("success", "Reimbursement recorded.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/payments";
    }

    // Owning a claim never grants permission to use its Manager review page.
    private CourseFeeApplication reviewClaim(Integer id, Manager actor) {
        var claim = claims.accessible(id, actor);
        if (claim.getApprovalManager() == null
                || !actor.getUserId().equals(claim.getApprovalManager().getUserId())
                || actor.getUserId().equals(claim.getApplicant().getUserId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return claim;
    }

    // Recheck session type after the route interceptor in case access changed during the request.
    private Manager manager(HttpSession session) {
        if (users.currentUser(session) instanceof Manager manager) return manager;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    // Payment recording belongs to an active Admin session.
    private Admin admin(HttpSession session) {
        if (users.currentUser(session) instanceof Admin admin) return admin;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
}
