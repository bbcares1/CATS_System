package group6.project.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import group6.project.model.CourseApplication;
import group6.project.model.CourseCategoryType;
import group6.project.model.CourseFeeApplication;
import group6.project.model.Staff;
import group6.project.service.StaffService;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    // Login: login controller saves employee details
    private Staff getCurrentStaff(HttpSession session) {
        Object user = session.getAttribute("user");
        if (!(user instanceof Staff)) {
            return null;
        }
        Staff staff = (Staff) user;
        return staffService.getStaff(staff.getUserId());
    }

    // The Staff controller shows the personal dashboard; applications have their own controller.
    @GetMapping({"", "/home", "/dashboard"})
    public String staffHome(HttpSession session, Model model) {
        Staff staff = getCurrentStaff(session);
        if (staff == null) return "redirect:/employee/login";
        model.addAttribute("currentUser", staff);
        model.addAttribute("summary", staffService.summary(staff, LocalDate.now().getYear()));
        return "staff-home";
    }

    // Fee claims: Retrieve claims and eligible courses.
    @GetMapping("/fee")
    public String showFeeClaims(HttpSession session, Model model) {
        Staff staff = getCurrentStaff(session);
        if (staff == null) {
            return "redirect:/employee/login";
        }
        model.addAttribute("claims", staffService.getClaims(staff));
        model.addAttribute("eligible", staffService.getClaimableCourses(staff));
        model.addAttribute("summary", staffService.summary(staff, LocalDate.now().getYear()));
        model.addAttribute("reimbursed", staffService.getReimbursedFees(staff, LocalDate.now().getYear()));
        return "staff-claims";
    }

    // Fee claims: Require personal payment, completed course and both documents submitted
    @PostMapping("/fee")
    public String submitFeeClaim(@RequestParam Integer courseId,
            @RequestParam(defaultValue = "false") boolean paidPersonally,
            @RequestParam MultipartFile receipt, @RequestParam MultipartFile certificate,
            HttpSession session, RedirectAttributes redirect) {
        Staff staff = getCurrentStaff(session);
        if (staff == null) {
            return "redirect:/employee/login";
        }
        try {
            staffService.submitClaim(courseId, paidPersonally, receipt, certificate, staff);
            redirect.addFlashAttribute("success", "Claim submitted.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/fee";
    }

    @GetMapping("/claims/{id}")
    public String showFeeClaimDetails(@PathVariable Integer id, HttpSession session, Model model, RedirectAttributes redirect) {
        Staff staff = getCurrentStaff(session);
        if (staff == null) {
            return "redirect:/employee/login";
        }
        CourseFeeApplication claim = staffService.getClaim(id);
        if (claim == null) {
            redirect.addFlashAttribute("error", "Claim not found.");
            return "redirect:/staff/home";
        }
        model.addAttribute("claim", claim);
        return "staff-claim-detail";
    }

    @GetMapping("/claims/{id}/{document}")
    public ResponseEntity<byte[]> downloadClaimDocument(@PathVariable Integer id,
            @PathVariable String document, HttpSession session) {
        Staff staff = getCurrentStaff(session);
        if (staff == null) {
            return ResponseEntity.status(401).build();
        }
        CourseFeeApplication claim = staffService.getClaim(id);
        if (claim == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] file;
        String fileName;
        if (document.equals("receipt")) {
            file = claim.getReceipt();
            fileName = claim.getReceiptFileName();
        } else if (document.equals("certificate")) {
            file = claim.getCertificate();
            fileName = claim.getCertificateFileName();
        } else {
            return ResponseEntity.notFound().build();
        }
        if (fileName == null || fileName.isBlank()) {
            fileName = document;
        }
        // Document download: Send the stored file as an attachment with its uploaded name.
        String attachment = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8).build().toString();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", attachment)
                .body(file);
    }
}
