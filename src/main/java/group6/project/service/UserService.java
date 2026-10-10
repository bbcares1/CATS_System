// Checks login credentials and reloads the signed-in account.
package group6.project.service;

import group6.project.model.Admin;
import group6.project.model.Manager;
import group6.project.model.Staff;
import group6.project.model.User;
import group6.project.repo.UserRepo;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    // Plain passwords are kept for the classroom demo, as agreed by the team.
    public User authenticate(String userName, String password) {
        if (userName == null || password == null) return null;
        return userRepo.findByUserNameIgnoreCase(userName.trim())
                .filter(user -> user.isActive() && password.equals(user.getPassword()))
                .orElse(null);
    }

    // Reload identity; an account edit, disable or role change requires a fresh login.
    public User currentUser(HttpSession session) {
        Object saved = session == null ? null : session.getAttribute("user");
        if (!(saved instanceof User user) || user.getUserId() == null) return null;
        User current = userRepo.findById(user.getUserId()).orElse(null);
        if (current == null
                || !current.isActive()
                || !Objects.equals(user.getVersion(), current.getVersion())) {
            session.removeAttribute("user");
            return null;
        }
        session.setAttribute("user", current);
        return current;
    }

    public boolean isAdmin(User user) {
        return user instanceof Admin;
    }

    // Managers also inherit every Staff capability.
    public boolean isManager(User user) {
        return user instanceof Manager;
    }

    // This identifies the Staff role, rather than inherited Staff permissions.
    public boolean isStaff(User user) {
        return user instanceof Staff && !(user instanceof Manager);
    }

    // Both employee roles may apply for courses themselves.
    public boolean isStaffOrManager(User user) {
        return user instanceof Staff;
    }
}
