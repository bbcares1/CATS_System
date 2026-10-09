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

    // Application form: Accept the employee edit fields
    @InitBinder("course")
    public void bindApplication(WebDataBinder binder) {
        binder.setAllowedFields("courseTitle", "courseCategory", "trainingProvider", "courseStartDate",
                "courseEndDate", "courseFee", "justification", "workDissemination", "halfDayPeriod");
    }

    // Dashboard: Show  employee and remaining training allowance
    @GetMapping({"", "/home", "/dashboard"})
    public String staffHome(HttpSession session, Model model) {
        Staff staff = getCurrentStaff(session);
        if (staff == null) {
            return "redirect:/employee/login";
        }
        model.addAttribute("currentUser", staff);
        model.addAttribute("summary", staffService.summary(new CourseApplication(), staff, null));
        return "staff-home";
    }

    // View personal course history - Show the employee's applications for the current year.
    @GetMapping({"/applications", "/personal"})
    public String showCourseHistory(HttpSession session, Model model,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Staff staff = getCurrentStaff(session);
        if (staff == null) {
            return "redirect:/employee/login";
        }
        int year = LocalDate.now().getYear();
        // Pagination: Number of results per page that reader can select
        if (size != 10 && size != 20 && size != 25) {
            size = 10;
        }
        List<CourseApplication> applications = staffService.getCourseHistory(staff, year);
        // Pagination - Show results until last page is reached
        int lastPage = 0;
        if (!applications.isEmpty()) {
            lastPage = (applications.size() - 1) / size;
        }
        if (page < 0) {
            page = 0;
        }
        if (page > lastPage) {
            page = lastPage;
        }
        int start = page * size;
        int end = start + size;
        if (end > applications.size()) {
            end = applications.size();
        }
        model.addAttribute("applications", applications.subList(start, end));
        model.addAttribute("summary", staffService.summary(new CourseApplication(), staff, null));
        model.addAttribute("year", year);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("lastPage", lastPage);
        return "staff-history";
    }

    // Course application - Retrieve application details.
    private String showApplicationForm(Model model, CourseApplication application, Integer id) {
        model.addAttribute("course", application);
        model.addAttribute("categories", CourseCategoryType.values());
        model.addAttribute("editId", id);
        model.addAttribute("today", LocalDate.now().plusDays(1));
        model.addAttribute("formAction", "/staff/applications/save");
        return "staff-application-form";
    }

    @GetMapping({"/applications/new", "/apply"})
    public String showNewApplicationForm(HttpSession session, Model model) {
        if (getCurrentStaff(session) == null) {
            return "redirect:/employee/login";
        }
        return showApplicationForm(model, new CourseApplication(), null);
    }

    // Only applied or updated applications can be edited.
    @GetMapping("/applications/{id}/edit")
    public String showEditApplicationForm(@PathVariable Integer id, HttpSession session, Model model, RedirectAttributes redirect) {
        Staff staff = getCurrentStaff(session);
        if (staff == null) {
            return "redirect:/employee/login";
        }
        try {
            CourseApplication application = staffService.getCourseApplication(id, staff);
            if (!staffService.isPending(application)) {
                redirect.addFlashAttribute("error", "Only pending applications can be edited.");
                return "redirect:/staff/home";
            }
            return showApplicationForm(model, application, id);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/home";
        }
    }

    // Show required field, date, allowance, budget and overlap errors.
    @PostMapping("/applications/save")
    public String saveApplication(@ModelAttribute("course") CourseApplication application,
            BindingResult binding,
            @RequestParam(required = false) Integer applicationId,
            HttpSession session, Model model, RedirectAttributes redirect) {
        Staff staff = getCurrentStaff(session);
        if (staff == null) {
            return "redirect:/employee/login";
        }
        if (binding.hasErrors()) {
            model.addAttribute("error", "Please check the dates, category and fee.");
            return showApplicationForm(model, application, applicationId);
        }

        try {
            CourseApplication saved = staffService.saveApplication(applicationId, application, staff);
            redirect.addFlashAttribute("success", "Application saved.");
            return "redirect:/staff/applications/" + saved.getCourseId();
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            return showApplicationForm(model, application, applicationId);
        }
    }

    // Application details: Handle lookup errors from the unchanged shared service.
    @GetMapping("/applications/{id}")
    public String showApplicationDetails(@PathVariable Integer id, HttpSession session, Model model, RedirectAttributes redirect) {
        Staff staff = getCurrentStaff(session);
        if (staff == null) {
            return "redirect:/employee/login";
        }
        try {
            model.addAttribute("course", staffService.getCourseApplication(id, staff));
            model.addAttribute("today", LocalDate.now());
            return "staff-application-detail";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/home";
        }
    }

    // Show errors when deletion, cancellation or completion is not allowed.
    @PostMapping("/applications/{id}/{action}")
    public String updateApplicationStatus(@PathVariable Integer id, @PathVariable String action,
            @RequestParam(required = false) String experienceComments,
            HttpSession session, RedirectAttributes redirect) {
        Staff staff = getCurrentStaff(session);
        if (staff == null) {
            return "redirect:/employee/login";
        }
        try {
            switch (action) {
                case "delete":
                    staffService.deleteApplication(id, staff);
                    redirect.addFlashAttribute("success", "Application deleted.");
                    break;
                case "cancel":
                    staffService.cancelApplication(id, staff);
                    redirect.addFlashAttribute("success", "Application cancelled.");
                    break;
                case "complete":
                    staffService.completeApplication(id, experienceComments, staff);
                    redirect.addFlashAttribute("success", "Course marked completed.");
                    break;
                default:
                    redirect.addFlashAttribute("error", "Unknown action.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/applications/" + id;
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
        model.addAttribute("summary", staffService.summary(new CourseApplication(), staff, null));
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
