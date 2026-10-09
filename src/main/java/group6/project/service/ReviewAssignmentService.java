package group6.project.service;

import group6.project.model.*;
import group6.project.repo.*;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class ReviewAssignmentService {
    private final UserRepo users;
    private final CourseApplicationRepo applications;
    private final CourseFeeApplicationRepo claims;

    // Admin can repair pre-upgrade requests without guessing their reviewer during migration.
    public ReviewAssignmentService(
            UserRepo users, CourseApplicationRepo applications, CourseFeeApplicationRepo claims) {
        this.users = users;
        this.applications = applications;
        this.claims = claims;
    }

    // Unassigned applications remain visible until an Admin selects a valid reviewer.
    public Page<CourseApplication> applications(int page, int size) {
        return applications.unassigned(
                List.of(ApplicationStatus.APPLIED, ApplicationStatus.UPDATED), page(page, size));
    }

    // Legacy claim lists use metadata only, just like the regular queue.
    public Page<ClaimSummary> claims(int page, int size) {
        return claims.unassigned(ApplicationStatus.APPLIED, page(page, size));
    }

    // Reviewer repair is limited to pending, unassigned requests and preserves identity/history.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void assign(String type, Integer id, Integer reviewerId, Long version, Integer adminId) {
        if (type == null || !Set.of("application", "claim").contains(type))
            throw new IllegalArgumentException("Choose an application or claim.");
        if (version == null)
            throw new IllegalArgumentException("Reload the request before assigning its reviewer.");
        Integer employeeId =
                ("claim".equals(type) ? claims.applicantId(id) : applications.applicantId(id))
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "This legacy request has no applicant. Correct its"
                                                    + " import data before assigning a reviewer."));
        if (reviewerId == null) throw new IllegalArgumentException("Choose a reviewing Manager.");
        var ids =
                java.util.stream.Stream.of(employeeId, reviewerId, adminId)
                        .distinct()
                        .sorted()
                        .toList();
        var locked =
                users.lockParticipants(ids).stream()
                        .collect(java.util.stream.Collectors.toMap(User::getUserId, u -> u));
        if (!(locked.get(adminId) instanceof Admin admin) || !admin.isActive())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        User reviewer = locked.get(reviewerId);
        if (!(reviewer instanceof Manager) || !reviewer.isActive() || employeeId.equals(reviewerId))
            throw new IllegalArgumentException(
                    "Choose another active Manager. Self-review is not allowed.");
        if ("claim".equals(type)) {
            var claim =
                    claims.lockById(id)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (claim.getApprovalManager() != null
                    || claim.getApplicationStatus() != ApplicationStatus.APPLIED
                    || !Objects.equals(version, claim.getVersion())) {
                throw new IllegalArgumentException(
                        "This claim changed or already has a reviewer. Reload the list.");
            }
            claim.setApprovalManager(reviewer);
            claims.save(claim);
        } else {
            var course =
                    applications
                            .lockById(id)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (course.getApprovalManager() != null
                    || !List.of(ApplicationStatus.APPLIED, ApplicationStatus.UPDATED)
                            .contains(course.getStatus())
                    || !Objects.equals(version, course.getVersion())) {
                throw new IllegalArgumentException(
                        "This application changed or already has a reviewer. Reload the list.");
            }
            course.setApprovalManager(reviewer);
            applications.save(course);
        }
    }

    // Bound list size even when the URL contains an unsupported value.
    private Pageable page(int page, int size) {
        return PageRequest.of(Math.max(0, page), Set.of(10, 20, 25).contains(size) ? size : 10);
    }
}
