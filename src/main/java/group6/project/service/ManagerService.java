package group6.project.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;

import group6.project.model.ApplicationStatus;
import group6.project.model.CourseApplication;
import group6.project.model.CourseCategoryType;
import group6.project.model.Manager;
import group6.project.model.Staff;
import group6.project.repo.CourseApplicationRepo;
import group6.project.repo.ManagerRepo;
import group6.project.repo.StaffRepo;

@Service
public class ManagerService {

    private final CourseApplicationService policy;
    private final StaffRepo staffRepo;
    private final ManagerRepo managerRepo;
    private final CourseApplicationRepo courseApplicationRepo;

    public ManagerService(ManagerRepo managerRepo, CourseApplicationRepo courseApplicationRepo,
            CourseApplicationService policy, StaffRepo staffRepo) {
        this.managerRepo = managerRepo;
        this.policy = policy;
        this.staffRepo = staffRepo;
        this.courseApplicationRepo = courseApplicationRepo;
    }

    public List<Manager> getAllManagers() {
        return managerRepo.findAll();
    }

    public Manager getManager(Integer id) {
        return managerRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Manager not found with id: " + id));
    }

    public Manager getManagerByStaffId(String staffId) {
        return managerRepo.findByStaffId(staffId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Manager not found with staffId: " + staffId));
    }

    @Transactional(readOnly = true)
    public List<ApplicationGroup> getPendingApplicationGroups(Integer managerId) {
        getManager(managerId);
        Map<Integer, List<ApplicationView>> byEmployee = new LinkedHashMap<>();
        for (CourseApplication application : courseApplicationRepo.findPendingForManager(
                managerId, List.of(ApplicationStatus.APPLIED, ApplicationStatus.UPDATED))) {
            ApplicationView view = toView(application);
            byEmployee.computeIfAbsent(view.applicantId(), key -> new ArrayList<>()).add(view);
        }
        return byEmployee.values().stream().map(applications -> {
            ApplicationView first = applications.getFirst();
            return new ApplicationGroup(first.applicantId(), first.applicantName(),
                    first.staffId(), applications);
        }).toList();
    }

    @Transactional(readOnly = true)
    public ApplicationView getApplicationForManager(Integer managerId, Integer applicationId) {
        getManager(managerId);
        return courseApplicationRepo.findForManager(applicationId, managerId)
                .map(ManagerService::toView)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course application not found"));
    }

    private static ApplicationView toView(CourseApplication application) {
        Staff applicant = application.getApplicant();
        return new ApplicationView(application.getCourseId(), applicant.getUserId(),
                applicant.getName(), applicant.getStaffId(), application.getCourseTitle(),
                application.getCourseCategory(), application.getTrainingProvider(),
                application.getCourseStartDate(), application.getCourseEndDate(),
                application.getTrainingDays(), application.getHalfDayPeriod(),
                application.getCourseFee(), application.getJustification(),
                application.getWorkDissemination(), application.getStatus(),
                application.getSubmittedAt(), application.getUpdatedAt(),
                application.getReviewedAt(), application.getDecisionReason(),
                application.getExperienceComments(), application.getReviewer() == null ? null : application.getReviewer().getName(),
                application.getVersion());
    }

    public record ApplicationGroup(Integer employeeId, String employeeName, String staffId,
            List<ApplicationView> applications) {
        public ApplicationGroup {
            applications = List.copyOf(applications);
        }
    }

    public record ApplicationView(Integer applicationId, Integer applicantId, String applicantName,
            String staffId, String title, CourseCategoryType category, String provider,
            LocalDate startDate, LocalDate endDate, Double trainingDays, String halfDayPeriod,
            BigDecimal fee, String justification, String workDissemination, ApplicationStatus status,
            LocalDateTime submittedAt, LocalDateTime updatedAt, LocalDateTime reviewedAt,
            String decisionReason, String experienceComments, String reviewerName, Long version) {
    }

    // Require a reason for either decision and check the version the manager actually reviewed.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void decide(Integer managerId, Integer applicationId, String decision, String reason, Long version) {
        Manager reviewer = getManager(managerId);
        if (!"approve".equals(decision) && !"reject".equals(decision)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose approve or reject.");
        }
        if (reason == null || reason.isBlank() || reason.trim().length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A decision reason is required (up to 2000 characters).");
        }
        Integer employeeId = courseApplicationRepo.findApplicantIdForManager(applicationId, managerId)
                .orElseThrow(ManagerService::notFound);
        Staff employee = new Staff(); employee.setUserId(employeeId);
        policy.lockEmployee(employee);
        CourseApplication course = courseApplicationRepo.lockForManager(applicationId, managerId)
                .orElseThrow(ManagerService::notFound);
        if (version == null || !version.equals(course.getVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This application changed. Review the latest details before deciding.");
        }
        if (course.getStatus() != ApplicationStatus.APPLIED && course.getStatus() != ApplicationStatus.UPDATED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This application has already been decided or withdrawn.");
        }
        if ("approve".equals(decision)) {
            try { policy.validateForApproval(course); }
            catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
        }
        course.setStatus("approve".equals(decision) ? ApplicationStatus.APPROVED : ApplicationStatus.REJECTED);
        course.setReviewer(reviewer);
        course.setDecisionReason(reason.trim());
        course.setReviewedAt(LocalDateTime.now());
        course.setUpdatedAt(course.getReviewedAt());
        courseApplicationRepo.save(course);
    }

    // Show committed use separately from pending reservations, plus other approved courses in this period.
    @Transactional(readOnly = true)
    public DecisionSupport getDecisionSupport(Integer managerId, Integer applicationId) {
        getManager(managerId);
        CourseApplication course = courseApplicationRepo.findForManager(applicationId, managerId)
                .orElseThrow(ManagerService::notFound);
        int currentYear = LocalDate.now().getYear();
        int requestYear = course.getCourseStartDate().getYear();
        var overlapping = courseApplicationRepo.findApprovedDuring(managerId, course.getApplicant().getUserId(),
                ApplicationStatus.APPROVED, course.getCourseStartDate(), course.getCourseEndDate()).stream()
                .filter(other -> policy.overlaps(course, other)).map(ManagerService::toView).toList();
        return new DecisionSupport(currentYear, policy.summaryForYear(course.getApplicant(), currentYear, null),
                requestYear, policy.summaryForYear(course.getApplicant(), requestYear, null), overlapping);
    }

    // Return current direct reports only; the manager's own courses stay in the Staff workspace.
    @Transactional(readOnly = true)
    public List<EmployeeView> getSubordinates(Integer managerId) {
        getManager(managerId);
        return staffRepo.findByManager_UserId(managerId).stream()
                .filter(staff -> !managerId.equals(staff.getUserId()))
                .map(staff -> new EmployeeView(staff.getUserId(), staff.getName(), staff.getStaffId())).toList();
    }

    // The selected employee must still report to this manager before any annual history is returned.
    @Transactional(readOnly = true)
    public List<ApplicationView> getEmployeeHistory(Integer managerId, Integer employeeId) {
        getSubordinates(managerId).stream().filter(staff -> staff.employeeId().equals(employeeId))
                .findFirst().orElseThrow(ManagerService::notFound);
        Staff employee = new Staff(); employee.setUserId(employeeId);
        return policy.findForStaffAndYear(employee, LocalDate.now().getYear()).stream()
                .map(ManagerService::toView).toList();
    }

    // Use the same response for a missing application and one belonging to another team.
    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Course application or employee not found.");
    }

    public record DecisionSupport(int currentYear, CourseApplicationService.Summary currentAllowance,
            int requestYear, CourseApplicationService.Summary requestAllowance, List<ApplicationView> overlapping) {}
    public record EmployeeView(Integer employeeId, String name, String staffId) {}
}
