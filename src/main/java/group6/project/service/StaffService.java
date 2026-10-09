package group6.project.service;

import group6.project.model.ApplicationStatus;
import group6.project.model.CourseApplication;
import group6.project.model.CourseFeeApplication;
import group6.project.model.Staff;
import group6.project.repo.CourseFeeApplicationRepo;
import group6.project.repo.StaffRepo;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class StaffService {
    private final StaffRepo staffRepo;
    private final CourseApplicationService courseApplicationService;
    private final CourseFeeApplicationService courseFeeApplicationService;
    private final CourseFeeApplicationRepo courseFeeApplicationRepo;

    // Keep personal course and claim operations behind the Staff module's service contract.
    public StaffService(
            StaffRepo staffRepo,
            CourseApplicationService courseApplicationService,
            CourseFeeApplicationService courseFeeApplicationService,
            CourseFeeApplicationRepo courseFeeApplicationRepo) {
        this.staffRepo = staffRepo;
        this.courseApplicationService = courseApplicationService;
        this.courseFeeApplicationService = courseFeeApplicationService;
        this.courseFeeApplicationRepo = courseFeeApplicationRepo;
    }

    // Staff details - Find staff records.
    public List<Staff> getAllStaff() {
        return staffRepo.findAll();
    }

    // Read the current reporting relationship rather than a role-level hierarchy.
    public List<Staff> getStaffByManager(Integer managerId) {
        return staffRepo.findByManager_UserId(managerId);
    }

    // Look up an employee identity for the existing Staff module.
    public Staff getStaff(Integer id) {
        return staffRepo.findById(id).orElse(null);
    }

    // Course applications - Prepare and retrieve application details.
    public List<CourseApplication> getCourseHistory(Staff staff, int year) {
        // Personal course history - Find this employee's applications for the selected year
        return courseApplicationService.findForStaffAndYear(staff, year);
    }

    // Use the owner check before returning any personal application.
    public CourseApplication getCourseApplication(Integer id, Staff staff) {
        return courseApplicationService.getOwned(id, staff);
    }

    // Edit application - Allow changes only before approval or rejection
    public boolean isPending(CourseApplication application) {
        return application.getStatus() == ApplicationStatus.APPLIED
                || application.getStatus() == ApplicationStatus.UPDATED;
    }

    // Every workspace uses the same allowance totals and validation rules.
    public CourseApplicationService.Summary summary(
            CourseApplication form, Staff staff, Integer id) {
        return courseApplicationService.summary(form, staff, id);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    // New requests become Applied; pending edits reuse the shared policy.
    public CourseApplication saveApplication(Integer id, CourseApplication form, Staff staff) {
        if (id == null) {
            form.setCourseId(null);
            return courseApplicationService.create(form, staff);
        }
        return courseApplicationService.update(id, form, staff);
    }

    public void deleteApplication(Integer id, Staff staff) {
        // Delete application when pending
        courseApplicationService.delete(id, staff);
    }

    public void cancelApplication(Integer id, Staff staff) {
        // Cancel application after approved
        courseApplicationService.cancel(id, staff);
    }

    // Completion rules are shared with the legacy routes.
    public void completeApplication(Integer id, String comments, Staff staff) {
        courseApplicationService.complete(id, comments, staff);
    }

    // Fee claims: Retrieve claims and eligible courses.
    public org.springframework.data.domain.Page<group6.project.model.ClaimSummary> getClaims(
            Staff staff, int page, int size) {
        return courseFeeApplicationService.personal(staff.getUserId(), page, size);
    }

    // Eligibility and upload checks live in the claim service for both employee roles.
    public List<CourseApplication> getClaimableCourses(Staff staff) {
        return courseFeeApplicationService.eligible(staff);
    }

    // Delegate eligibility, reviewer and evidence checks to the claim workflow.
    public void submitClaim(
            Integer courseId,
            boolean paidPersonally,
            MultipartFile receipt,
            MultipartFile certificate,
            Staff staff,
            Integer approvalManagerId) {
        courseFeeApplicationService.submit(
                courseId, paidPersonally, receipt, certificate, staff, approvalManagerId);
    }

    // Claim details and downloads belong to the submitting employee, including a Manager's own
    // claims.
    public CourseFeeApplication getClaim(Integer id, Staff staff) {
        CourseFeeApplication claim =
                courseFeeApplicationRepo
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Claim not found."));
        if (claim.getApplicant() == null
                || !staff.getUserId().equals(claim.getApplicant().getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found.");
        }
        return claim;
    }

    // Reimbursement total - Add fees already reimbursed for the selected year
    public BigDecimal getReimbursedFees(Staff staff, int year) {
        return courseFeeApplicationService.reimbursed(staff, year);
    }
}
