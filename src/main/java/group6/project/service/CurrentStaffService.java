package group6.project.service;

import java.security.Principal;
import group6.project.model.Staff;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

/** Compatibility adapter; new callers should use StaffService. */
@Deprecated
@Service
public class CurrentStaffService {
    private final StaffService staffService;

    public CurrentStaffService(StaffService staffService) {
        this.staffService = staffService;
    }

    public Staff requireStaff(Principal principal, HttpSession session) {
        return staffService.requireStaff(principal, session);
    }
}
