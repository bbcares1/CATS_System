package group6.project.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import group6.project.model.ApplicationStatus;
import group6.project.model.CourseApplication;
import group6.project.model.CourseCategoryType;
import group6.project.model.Manager;
import group6.project.model.Staff;
import group6.project.model.User;
import group6.project.repo.CourseApplicationRepo;
import group6.project.repo.ManagerRepo;

@Service
public class ManagerService {

    private final ManagerRepo managerRepo;
    private final CourseApplicationRepo courseApplicationRepo;

    public ManagerService(ManagerRepo managerRepo, CourseApplicationRepo courseApplicationRepo) {
        this.managerRepo = managerRepo;
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
        User applicant = application.getApplicant();
        return new ApplicationView(application.getCourseId(), applicant.getUserId(),
                applicant.getName(), applicant.getStaffId(), application.getCourseTitle(),
                application.getCourseCategory(), application.getTrainingProvider(),
                application.getCourseStartDate(), application.getCourseEndDate(),
                application.getTrainingDays(), application.getHalfDayPeriod(),
                application.getCourseFee(), application.getJustification(),
                application.getWorkDissemination(), application.getStatus(),
                application.getSubmittedAt(), application.getUpdatedAt(),
                application.getReviewedAt(), application.getDecisionReason(),
                application.getExperienceComments());
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
            double fee, String justification, String workDissemination, ApplicationStatus status,
            LocalDateTime submittedAt, LocalDateTime updatedAt, LocalDateTime reviewedAt,
            String decisionReason, String experienceComments) {
    }

    // The methods below come from the class diagram.
    // They will be filled in once CourseApplication and CourseFeeApplication are ready.

    public String approveCourseApplication() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String rejectCourseApplication() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void employeeCourseHistory() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void approveFeeClaim() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
