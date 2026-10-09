package group6.project.service;

import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import group6.project.model.Staff;
import group6.project.repo.StaffRepo;
import jakarta.servlet.http.HttpSession;

@Service
public class CurrentStaffService {
    private final StaffRepo staffRepo;

    // Use the same employee identity source as the Staff pages.
    public CurrentStaffService(StaffRepo staffRepo) {
        this.staffRepo = staffRepo;
    }

    // Reload the employee so a session does not keep stale account details.
    public Staff requireStaff(Principal principal, HttpSession session) {
        if (principal != null) {
            return staffRepo.findByUserName(principal.getName())
                    .orElseThrow(CurrentStaffService::unauthorized);
        }
        Object user = session == null ? null : session.getAttribute("user");
        if (user instanceof Staff staff && staff.getUserId() != null) {
            return staffRepo.findById(staff.getUserId())
                    .orElseThrow(CurrentStaffService::unauthorized);
        }
        throw unauthorized();
    }

    // A missing or non-employee session cannot fall back to another user's records.
    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in as an employee.");
    }
}
