package group6.project.service;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import group6.project.model.Staff;
import group6.project.model.User;
import group6.project.repo.StaffRepo;
import group6.project.repo.UserRepo;
import jakarta.servlet.http.HttpSession;

@Service
public class CurrentStaffService {
    private final StaffRepo staffRepo;
    private final UserRepo userRepo;

    public CurrentStaffService(StaffRepo staffRepo, UserRepo userRepo) {
        this.staffRepo = staffRepo;
        this.userRepo = userRepo;
    }

    public Staff requireStaff(Principal principal, HttpSession session) {
        if (principal != null) {
            return staffRepo.findByUserName(principal.getName())
                    .orElseThrow(() -> unauthorized("The signed-in user is not an employee."));
        }

        Object sessionUser = session == null ? null
                : (session.getAttribute("currentUser") != null
                        ? session.getAttribute("currentUser")
                        : session.getAttribute("loggedInUser"));
        if (sessionUser instanceof Staff staff) {
            return staff;
        }
        if (sessionUser instanceof User user) {
            if (user instanceof Staff staff) {
                return staff;
            }
            return staffRepo.findById(user.getUserId())
                    .orElseThrow(() -> unauthorized("The signed-in user is not an employee."));
        }
        if (sessionUser instanceof String username) {
            return userRepo.findByUserName(username)
                    .filter(Staff.class::isInstance)
                    .map(Staff.class::cast)
                    .orElseThrow(() -> unauthorized("The signed-in user is not an employee."));
        }
        throw unauthorized("Please sign in as an employee before using course applications.");
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
