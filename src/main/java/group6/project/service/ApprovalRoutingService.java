// Selects a reporting Manager or an eligible peer Manager for review.
package group6.project.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import group6.project.model.Manager;
import group6.project.model.User;
import group6.project.repo.ManagerRepo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ApprovalRoutingService {
    private final ManagerRepo managers;

    public ApprovalRoutingService(ManagerRepo managers) {
        this.managers = managers;
    }

    // Only a Manager without a reporting manager may choose another Manager.
    public List<Manager> choices(User applicant) {
        if (!(applicant instanceof Manager) || applicant.getManager() != null) return List.of();
        return managers.findAll().stream()
                .filter(
                        manager ->
                                manager.isActive()
                                        && !manager.getUserId().equals(applicant.getUserId()))
                .toList();
    }

    // The caller locks the participating accounts before resolving a new assignment.
    public User resolveReviewer(User applicant, Integer selectedId) {
        User reviewer = applicant.getManager();
        if (reviewer == null && applicant instanceof Manager && selectedId != null) {
            reviewer = managers.findById(selectedId).orElse(null);
        } else if (reviewer != null
                && selectedId != null
                && !selectedId.equals(reviewer.getUserId())) {
            throw new ResponseStatusException(BAD_REQUEST, "Use your assigned reporting manager.");
        }
        if (!(reviewer instanceof Manager)
                || !reviewer.isActive()
                || reviewer.getUserId().equals(applicant.getUserId())) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "An active, different Manager must review this request.");
        }
        return reviewer;
    }
}
