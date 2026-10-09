package group6.project.service;

import org.springframework.stereotype.Service;
import group6.project.model.Admin;
import group6.project.model.Manager;
import group6.project.model.Staff;
import group6.project.model.User;
import group6.project.repo.UserRepo;

@Service
public class UserService {
    private final UserRepo userRepo;

    // Use the shared account repository for both login forms.
    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    // Check the classroom login credentials before establishing a session.
    public User authenticate(String userName, String password) {
        if (userName == null || password == null) {
            return null;
        }
        return userRepo.findByUserName(userName)
                .filter(user -> password.equals(user.getPassword()))
                .orElse(null);
    }

    // Admin accounts use the separate administration login.
    public boolean isAdmin(User user) {
        return user instanceof Admin;
    }

    // Managers have both their team dashboard and inherited Staff capabilities.
    public boolean isManager(User user) {
        return user instanceof Manager;
    }

    // Select the Staff login role, rather than all Staff capabilities.
    public boolean isStaff(User user) {
        return user instanceof Staff && !(user instanceof Manager);
    }

    // Both employee types may use course applications and personal history.
    public boolean isStaffOrManager(User user) {
        return user instanceof Staff;
    }
}
