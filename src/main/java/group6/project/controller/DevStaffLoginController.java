package group6.project.controller;

import group6.project.repo.StaffRepo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** Test account selection, deliberately unavailable outside the dev profile. */
@Controller
@Profile("dev")
public class DevStaffLoginController {
    private final StaffRepo staffRepo;

    public DevStaffLoginController(StaffRepo staffRepo) {
        this.staffRepo = staffRepo;
    }

    @GetMapping("/dev/staff-login")
    public String loginPage(Model model) {
        model.addAttribute("staffAccounts", staffRepo.findAll());
        return "dev-staff-login";
    }

    @PostMapping("/dev/staff-login")
    public String selectStaff(@RequestParam String username, HttpServletRequest request) {
        var staff = staffRepo.findByUserName(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown employee test account."));
        var previous = request.getSession(false);
        if (previous != null) previous.invalidate();
        request.getSession(true).setAttribute("currentUser", staff.getUserName());
        return "redirect:/staff/course-applications";
    }
}
