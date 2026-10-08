package group6.project.service;

import java.util.List;
import java.security.Principal;
import group6.project.model.User;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import group6.project.model.Staff;
import group6.project.repo.StaffRepo;

@Service
public class StaffService {

    private final StaffRepo staffRepo;

    public StaffService(StaffRepo staffRepo) {
        this.staffRepo = staffRepo;
    }

    public List<Staff> getAllStaff() {
        return staffRepo.findAll();
    }

    public Staff getStaff(Integer id) {
        return staffRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Staff not found with id: " + id));
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
        if (sessionUser instanceof User user && user.getUserId() != null) {
            return staffRepo.findById(user.getUserId())
                    .orElseThrow(() -> unauthorized("The signed-in user is not an employee."));
        }
        if (sessionUser instanceof String username) {
            return staffRepo.findByUserName(username)
                    .orElseThrow(() -> unauthorized("The signed-in user is not an employee."));
        }
        throw unauthorized("Please sign in as an employee before using course applications.");
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
