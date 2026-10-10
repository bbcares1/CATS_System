package group6.project.service;

import group6.project.model.ApprovalHierarchy;
import group6.project.model.User;
import group6.project.repo.ApprovalHierarchyRepo;
import group6.project.repo.UserRepo;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AdminService {
    private final UserRepo users;
    private final ApprovalHierarchyRepo hierarchies;

    // Catalogue and account writes have their own services.
    public AdminService(UserRepo users, ApprovalHierarchyRepo hierarchies) {
        this.users = users;
        this.hierarchies = hierarchies;
    }

    // Read the existing hierarchy page while reporting managers are consolidated.
    public List<ApprovalHierarchy> getAllApprovalHierarchy() {
        return hierarchies.findAllByOrderByLevelAsc();
    }

    // Resolve an existing hierarchy entry for its edit form.
    public Optional<ApprovalHierarchy> getHierarchyById(Integer id) {
        return hierarchies.findById(id);
    }

    // Save a hierarchy entry from the current Admin form.
    public void saveHierarchy(ApprovalHierarchy hierarchy) {
        hierarchies.save(hierarchy);
    }

    // Remove an unused hierarchy entry.
    public void deleteHierarchyById(Integer id) {
        hierarchies.deleteById(id);
    }

    // Admin's manual email page chooses recipients from saved accounts.
    public List<User> viewList() {
        return users.findAll();
    }
}
