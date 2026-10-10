package group6.project.service;

import group6.project.form.AccountForm;
import group6.project.model.*;
import group6.project.repo.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class AccountAdminService {
    private final UserRepo users;
    private final CourseApplicationRepo applications;
    private final CourseFeeApplicationRepo claims;
    private final TrainingEntitlementRepo entitlements;

    // Account forms are separate from entities; annual limits stay in their existing yearly
    // workflow.
    public AccountAdminService(
            UserRepo users,
            CourseApplicationRepo applications,
            CourseFeeApplicationRepo claims,
            TrainingEntitlementRepo entitlements) {
        this.users = users;
        this.applications = applications;
        this.claims = claims;
        this.entitlements = entitlements;
    }

    // Include disabled accounts so Admin can restore access without recreating their history.
    public List<User> all() {
        return users.findAll().stream()
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    // Missing edit links give a normal 404.
    public User get(Integer id) {
        return users.findById(id)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Account not found."));
    }

    // Only active Manager accounts can receive reporting assignments.
    public List<User> managers() {
        return all().stream().filter(u -> u instanceof Manager && u.isActive()).toList();
    }

    // Keep password blank on GET; an empty edit password leaves the saved credential unchanged.
    public AccountForm form(User user) {
        AccountForm form = new AccountForm();
        form.setVersion(user.getVersion());
        form.setName(user.getName());
        form.setUserName(user.getUserName());
        form.setStaffId(user.getStaffId());
        form.setEmail(user.getEmail());
        form.setDesignation(user.getDesignation());
        form.setRole(user.getRole());
        form.setActive(user.isActive());
        form.setManagerId(user.getManager() == null ? null : user.getManager().getUserId());
        return form;
    }

    // Serialize assignment/role edits, preserve identity, and expire old sessions through the
    // account version.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public User save(Integer id, AccountForm form, Integer actorId) {
        List<User> accounts = users.lockAccounts();
        requireAdmin(accounts, actorId);
        User target = id == null ? subtype(form.getRole()) : find(accounts, id);
        if (id != null && !Objects.equals(form.getVersion(), target.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This account changed. Reload it before saving.");
        }
        if (form.getRole() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an account role.");
        if (id != null
                && id.equals(actorId)
                && (form.getRole() != Roles.ADMIN || !form.isActive())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "You cannot remove your own Admin access.");
        }
        if (id != null && (target.getRole() != form.getRole() || !form.isActive())) {
            requireAnotherAdmin(accounts, target);
            if (target instanceof Manager && (form.getRole() != Roles.MANAGER || !form.isActive()))
                requireNoReports(accounts, id);
            if (!(target instanceof Admin) && (form.getRole() == Roles.ADMIN || !form.isActive()))
                requireNoPendingRequests(id);
        }
        String username = text(form.getUserName(), "Username", 100).toLowerCase(Locale.ROOT);
        if (!username.matches("[a-z0-9_.-]{3,100}"))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Use 3–100 letters, digits, dots, underscores or hyphens for the username.");
        String staffId = text(form.getStaffId(), "Staff ID", 255);
        if (form.getEmail() == null || form.getEmail().isBlank())
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Email is required for application notifications.");
        String email =
                form.getEmail() == null || form.getEmail().isBlank()
                        ? null
                        : form.getEmail().trim().toLowerCase(Locale.ROOT);
        Integer excluded = id == null ? -1 : id;
        if (users.existsByUserNameIgnoreCaseAndUserIdNot(username, excluded))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists.");
        if (users.existsByStaffIdIgnoreCaseAndUserIdNot(staffId, excluded))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff ID already exists.");
        if (email != null && users.existsByEmailIgnoreCaseAndUserIdNot(email, excluded))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Email already belongs to another account.");
        if (email != null
                && (email.length() > 255
                        || email.chars().anyMatch(Character::isWhitespace)
                        || !email.matches("[^ @]+@[^ @]+[.][^ @]+")))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Enter a valid email address.");
        User manager =
                form.getRole() == Roles.ADMIN || form.getManagerId() == null
                        ? null
                        : find(accounts, form.getManagerId());
        if (id != null
                && manager == null
                && target.getManager() != null
                && form.getRole() != Roles.ADMIN) {
            requireNoPendingRequests(id);
        }
        if (manager != null) {
            if (!(manager instanceof Manager) || !manager.isActive())
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Choose an active manager.");
            requireNoCycle(id, manager);
        }
        target.setName(text(form.getName(), "Name", 255));
        target.setUserName(username);
        target.setStaffId(staffId);
        target.setEmail(email);
        target.setDesignation(optionalText(form.getDesignation(), 255));
        target.setManager(manager);
        target.setActive(form.isActive());
        if (id == null || form.getPassword() != null && !form.getPassword().isBlank()) {
            target.setPassword(password(form.getPassword()));
        }
        Roles oldRole = target.getRole();
        target = users.saveAndFlush(target);
        if (id != null && oldRole != form.getRole()) {
            users.changeRole(id, form.getRole().name());
            return get(id);
        }
        return target;
    }

    // Delete only unused mistakes; accounts with history are disabled instead, without losing
    // records.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(Integer id, Long version, Integer actorId) {
        List<User> accounts = users.lockAccounts();
        requireAdmin(accounts, actorId);
        User target = find(accounts, id);
        if (id.equals(actorId))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "You cannot delete your own account.");
        if (!Objects.equals(version, target.getVersion()))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This account changed. Reload it before deleting.");
        requireAnotherAdmin(accounts, target);
        requireNoReports(accounts, id);
        if (applications.existsByApplicant_UserIdOrReviewer_UserId(id, id)
                || applications.existsByApprovalManager_UserId(id)
                || claims
                        .existsByApplicant_UserIdOrReviewer_UserIdOrApprovalManager_UserIdOrReimbursedBy_UserId(
                                id, id, id, id)
                || !entitlements.findByStaff_UserId(id).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This account has history or annual allowances. Disable it instead.");
        }
        users.delete(target);
        users.flush();
    }

    // Select the actual JPA subtype for a new account; a browser role cannot change an existing
    // entity type.
    private User subtype(Roles role) {
        if (role == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an account role.");
        User user =
                switch (role) {
                    case ADMIN -> new Admin();
                    case MANAGER -> new Manager();
                    case STAFF -> new Staff();
                };
        user.setRole(role);
        return user;
    }

    // Recheck the actor inside the write transaction rather than trusting an earlier session
    // lookup.
    private User requireAdmin(List<User> accounts, Integer id) {
        User actor = find(accounts, id);
        if (!(actor instanceof Admin) || !actor.isActive())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return actor;
    }

    // Locked identities are reused throughout the same account maintenance operation.
    private User find(List<User> accounts, Integer id) {
        return accounts.stream()
                .filter(u -> u.getUserId().equals(id))
                .findFirst()
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Account not found."));
    }

    // Keep at least one active administrator, even when two maintenance requests arrive together.
    private void requireAnotherAdmin(List<User> accounts, User target) {
        if (target instanceof Admin
                && target.isActive()
                && accounts.stream()
                        .noneMatch(
                                u ->
                                        u instanceof Admin
                                                && u.isActive()
                                                && !u.getUserId().equals(target.getUserId()))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Keep at least one active Admin account.");
        }
    }

    // Reassign all direct reports before demoting, disabling or deleting their manager.
    private void requireNoReports(List<User> accounts, Integer id) {
        if (applications.existsByApprovalManager_UserIdAndStatusIn(
                        id, List.of(ApplicationStatus.APPLIED, ApplicationStatus.UPDATED))
                || claims.existsByApprovalManager_UserIdAndApplicationStatus(
                        id, ApplicationStatus.APPLIED)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Resolve this manager's assigned pending reviews first.");
        }
        if (accounts.stream()
                .anyMatch(u -> u.getManager() != null && id.equals(u.getManager().getUserId()))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Reassign this manager's direct reports first.");
        }
    }

    // Open employee requests must be decided or withdrawn before employee access is removed.
    private void requireNoPendingRequests(Integer id) {
        if (applications.existsByApplicant_UserIdAndStatusIn(
                        id,
                        List.of(
                                ApplicationStatus.APPLIED,
                                ApplicationStatus.UPDATED,
                                ApplicationStatus.APPROVED))
                || claims.existsByApplicant_UserIdAndApplicationStatus(
                        id, ApplicationStatus.APPLIED)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Resolve pending applications/claims and complete or cancel approved courses"
                            + " first.");
        }
    }

    // Managers can report upward, but self-reporting and longer cycles are not a valid hierarchy.
    private void requireNoCycle(Integer employeeId, User manager) {
        Set<Integer> visited = new HashSet<>();
        for (User current = manager; current != null; current = current.getManager()) {
            if (current.getUserId().equals(employeeId) || !visited.add(current.getUserId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Reporting assignments cannot contain a cycle.");
            }
        }
    }

    // Required profile text is trimmed and kept within database limits.
    private String text(String value, String label, int limit) {
        if (value == null || value.isBlank() || value.trim().length() > limit)
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    label + " is required and cannot exceed " + limit + " characters.");
        return value.trim();
    }

    // Blank optional profile fields are stored as null rather than empty unique values.
    private String optionalText(String value, int limit) {
        if (value == null || value.isBlank()) return null;
        if (value.trim().length() > limit)
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Profile text cannot exceed " + limit + " characters.");
        return value.trim();
    }

    // Keep classroom credentials simple; the blank edit field means no password change.
    private String password(String value) {
        if (value == null || value.isBlank() || value.length() > 255)
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A new account needs a password of at most 255 characters.");
        return value;
    }
}
