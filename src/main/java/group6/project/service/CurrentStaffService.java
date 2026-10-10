package group6.project.service;

import java.security.Principal;
import group6.project.model.Staff;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentStaffService {
    // Legacy application pages use the same session key until they move to the shared controller.
    public Staff requireStaff(Principal principal, HttpSession session) {
        Object user = session == null ? null : session.getAttribute("user");
        if (user instanceof Staff staff) return staff;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in as an employee.");
    }
}
