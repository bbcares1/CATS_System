package group6.project.service;

import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpSession;
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
        return userRepo.findByUserName(userName.trim().toLowerCase(java.util.Locale.ROOT))
                .filter(user -> user.isActive() && password.equals(user.getPassword()))
                .orElse(null);
    }

    // Reload the saved identity so deleted accounts and later role changes affect the next request.
    public User currentUser(HttpSession session) {
        Object value = session == null ? null : session.getAttribute("user");
        if (!(value instanceof User user) || user.getUserId() == null) return null;
        User current = userRepo.findById(user.getUserId()).orElse(null);
        if (current == null || !current.isActive() || !java.util.Objects.equals(user.getVersion(), current.getVersion())) {
            session.removeAttribute("user");
            return null;
        }
        else session.setAttribute("user", current);
        return current;
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
