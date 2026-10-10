// Handles claim submission, review, document downloads and payment recording.
package group6.project.controller;

import group6.project.form.ClaimForm;
import group6.project.form.DecisionForm;
import group6.project.model.*;
import group6.project.service.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
public class ClaimController {
    private final NotificationService notifications;
    private final CourseFeeApplicationService claims;
    private final TrainingEntitlementService entitlements;
    private final ApprovalRoutingService routing;

    public ClaimController(
            CourseFeeApplicationService claims,
            TrainingEntitlementService entitlements,
            ApprovalRoutingService routing,
            NotificationService notifications) {
        this.notifications = notifications;
        this.claims = claims;
        this.entitlements = entitlements;
        this.routing = routing;
    }

    // Personal claims include rejected and paid records without loading their attachments.
    @GetMapping("/staff/fee")
    public String personal(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        return personalPage((User) session.getAttribute("user"), page, size, model);
    }

    // Invalid uploads stay on the claim page; browsers require the files to be selected again.
    @PostMapping("/staff/fee")
    public String submit(
            @Valid @ModelAttribute("form") ClaimForm form,
            BindingResult binding,
            HttpSession session,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect) {
        User employee = (User) session.getAttribute("user");
        if (!binding.hasErrors()) {
            try {
                CourseFeeApplication saved =
                        claims.submit(
                                form.getCourseId(),
                                form.isPaidPersonally(),
                                form.getReceipt(),
                                form.getCertificate(),
                                employee,
                                form.getReviewerId());
                if (!notifications.claimSubmitted(saved))
                    redirect.addFlashAttribute(
                            "warning",
                            "Claim saved. Email was not sent; it is available in the Manager"
                                    + " queue.");
                redirect.addFlashAttribute("success", "Claim submitted for review.");
                return "redirect:/staff/fee";
            } catch (ResponseStatusException error) {
                if (error.getStatusCode().value() != 400) throw error;
                binding.reject("claim", error.getReason());
            }
        }
        response.setStatus(400);
        model.addAttribute(
                "error",
                binding.getAllErrors().getFirst().getDefaultMessage()
                        + " Please select both files again.");
        return personalPage(employee, 0, 10, model);
    }

    // Pending claims are assigned to this Manager, including valid peer-Manager claims.
    @GetMapping("/manager/claims")
    public String queue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        var result = claims.pending(((User) session.getAttribute("user")).getUserId(), page, size);
        model.addAttribute("claims", result.getContent());
        model.addAttribute("claimPage", result);
        model.addAttribute("claimRoute", "/manager/claims");
        return "manager-claims";
    }

    // The applicant, assigned Manager and Admin may read evidence; other users receive 404.
    @GetMapping("/{workspace:staff|manager|admin}/claims/{id}")
    public String details(
            @PathVariable String workspace,
            @PathVariable Integer id,
            HttpSession session,
            Model model) {
        CourseFeeApplication claim = claims.accessible(id, (User) session.getAttribute("user"));
        DecisionForm form = new DecisionForm();
        form.setVersion(claim.getVersion());
        model.addAttribute("decision", form);
        return detailPage(workspace, claim, (User) session.getAttribute("user"), model);
    }

    // A decision never records payment; both approve and reject require a reason.
    @PostMapping("/manager/claims/{id}/decision")
    public String decide(
            @PathVariable Integer id,
            @Valid @ModelAttribute("decision") DecisionForm form,
            BindingResult binding,
            HttpSession session,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirect) {
        User actor = (User) session.getAttribute("user");
        CourseFeeApplication claim = claims.accessible(id, actor);
        if (!binding.hasErrors()) {
            try {
                claims.decide(
                        id,
                        actor.getUserId(),
                        form.getApproved() ? "approve" : "reject",
                        form.getReason(),
                        form.getVersion());
                if (!notifications.claimDecided(claims.accessible(id, actor)))
                    redirect.addFlashAttribute(
                            "warning",
                            "Decision saved. Email was not sent; it is available in the employee"
                                    + " history.");
                redirect.addFlashAttribute("success", "Claim decision recorded.");
                return "redirect:/manager/claims/" + id;
            } catch (ResponseStatusException error) {
                if (error.getStatusCode().value() != 400) throw error;
                binding.reject("decision", error.getReason());
            }
        }
        response.setStatus(400);
        return detailPage("manager", claim, actor, model);
    }

    // Downloads use an attachment response even for PDF and images, so uploads cannot execute
    // inline.
    @GetMapping("/{workspace:staff|manager|admin}/claims/{id}/{document:receipt|certificate}")
    public ResponseEntity<byte[]> document(
            @PathVariable Integer id, @PathVariable String document, HttpSession session) {
        CourseFeeApplication claim = claims.accessible(id, (User) session.getAttribute("user"));
        boolean receipt = "receipt".equals(document);
        String name = receipt ? claim.getReceiptFileName() : claim.getCertificateFileName();
        byte[] bytes = receipt ? claim.getReceipt() : claim.getCertificate();
        if (bytes == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("X-Content-Type-Options", "nosniff")
                .header(
                        "Content-Disposition",
                        ContentDisposition.attachment()
                                .filename(name == null ? document : name, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(bytes);
    }

    // Keep paid claims in the list so the team can trace actual reimbursements.
    @GetMapping("/admin/payments")
    public String payments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        var result = claims.approved(page, size);
        model.addAttribute("claims", result.getContent());
        model.addAttribute("claimPage", result);
        model.addAttribute("claimRoute", "/admin/payments");
        return "admin-payments";
    }

    // Admin records a payment reference only after paying an approved claim.
    @PostMapping("/admin/payments/{id}")
    public String pay(
            @PathVariable Integer id,
            @RequestParam Long version,
            @RequestParam String reference,
            HttpSession session,
            RedirectAttributes redirect) {
        try {
            claims.reimburse(
                    id, ((User) session.getAttribute("user")).getUserId(), reference, version);
            redirect.addFlashAttribute("success", "Payment recorded.");
        } catch (ResponseStatusException error) {
            if (error.getStatusCode().value() != 400) throw error;
            redirect.addFlashAttribute("error", error.getReason());
        }
        return "redirect:/admin/payments";
    }

    // One detail template is used by all three workspaces with a strictly scoped decision form.
    private String detailPage(
            String workspace, CourseFeeApplication claim, User actor, Model model) {
        model.addAttribute("claim", claim);
        model.addAttribute(
                "workspace", workspace.substring(0, 1).toUpperCase() + workspace.substring(1));
        model.addAttribute("base", "/" + workspace + "/claims");
        model.addAttribute(
                "back",
                "staff".equals(workspace)
                        ? "/staff/fee"
                        : "admin".equals(workspace) ? "/admin/payments" : "/manager/claims");
        model.addAttribute(
                "canDecide",
                "manager".equals(workspace)
                        && claim.getApplicationStatus() == ApplicationStatus.APPLIED
                        && claim.getApprovalManager() != null
                        && claim.getApprovalManager().getUserId().equals(actor.getUserId())
                        && !claim.getApplicant().getUserId().equals(actor.getUserId()));
        return "claim-detail";
    }

    // Reimbursements and annual course allowance are separate amounts.
    private String personalPage(User employee, int page, int size, Model model) {
        if (!model.containsAttribute("form")) model.addAttribute("form", new ClaimForm());
        int year = LocalDate.now().getYear();
        var result = claims.personal(employee.getUserId(), page, size);
        model.addAttribute("claims", result.getContent());
        model.addAttribute("claimPage", result);
        model.addAttribute("claimRoute", "/staff/fee");
        model.addAttribute("eligible", claims.eligible(employee));
        model.addAttribute("reviewers", routing.choices(employee));
        model.addAttribute("summary", entitlements.summary(employee, year, null));
        model.addAttribute("reimbursed", claims.reimbursed(employee, year));
        return "staff-claims";
    }
}
