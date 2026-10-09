package group6.project.service;

import java.util.*;
import org.springframework.stereotype.Service;
import group6.project.model.*;
import group6.project.repo.UserRepo;

@Service
public class ApprovalRoutingService {
    private final UserRepo users;

    // Reporting assignments and request reviewers are separate: old requests keep their reviewer.
    public ApprovalRoutingService(UserRepo users) { this.users = users; }

    // Only a Manager without a reporting manager chooses a peer; Staff cannot bypass their boss.
    public User forSubmission(User employee, Integer selectedId) {
        Integer reportingId = users.reportingManagerId(employee.getUserId()).orElse(null);
        Integer reviewerId = reportingId == null ? selectedId : reportingId;
        if (reviewerId == null) throw new IllegalArgumentException("Ask Admin to assign a manager, or choose a reviewer if you are a Manager.");
        var locked = lockParticipants(employee.getUserId(), reviewerId);
        User current = locked.get(employee.getUserId());
        if (!(current instanceof Staff) || !current.isActive() || employee.getVersion() != null
                && !Objects.equals(employee.getVersion(), current.getVersion())) {
            throw new IllegalArgumentException("Your account changed. Sign in again before continuing.");
        }
        Integer currentBoss = current.getManager() == null ? null : current.getManager().getUserId();
        if (!Objects.equals(currentBoss, reportingId)) throw new IllegalArgumentException("Your reporting assignment changed. Reload the form.");
        if (currentBoss == null && !(current instanceof Manager)) throw new IllegalArgumentException("Ask Admin to assign your reporting manager first.");
        if (selectedId != null && currentBoss != null && !selectedId.equals(currentBoss)) throw new IllegalArgumentException("Your reporting manager reviews your requests.");
        User reviewer = locked.get(reviewerId);
        if (employee.getUserId().equals(reviewerId) || !(reviewer instanceof Manager) || !reviewer.isActive()) {
            throw new IllegalArgumentException("Choose another active Manager. You cannot review your own request.");
        }
        return reviewer;
    }

    // Lock identities in ID order, including peer requests, to avoid opposite-order lock waits.
    public Map<Integer, User> lockParticipants(Integer employeeId, Integer reviewerId) {
        List<Integer> ids = java.util.stream.Stream.of(employeeId, reviewerId).filter(Objects::nonNull).distinct().sorted().toList();
        Map<Integer, User> result = new HashMap<>();
        for (User user : users.lockParticipants(ids)) result.put(user.getUserId(), user);
        if (result.size() != ids.size()) throw new IllegalArgumentException("An account no longer exists.");
        return result;
    }

    // Peer choices contain no password or unrelated employee details.
    public List<ReviewerChoice> choices(User employee) {
        if (!(employee instanceof Manager) || employee.getManager() != null) return List.of();
        return users.findAll().stream().filter(u -> u instanceof Manager && u.isActive() && !u.getUserId().equals(employee.getUserId()))
                .sorted(Comparator.comparing(User::getName)).map(u -> new ReviewerChoice(u.getUserId(), u.getName(), u.getStaffId())).toList();
    }

    public record ReviewerChoice(Integer id, String name, String staffId) {}
}
