package group6.project.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import group6.project.model.ApplicationStatus;
import group6.project.model.CourseApplication;
import group6.project.model.CourseFeeApplication;
import group6.project.model.CourseCategoryType;
import group6.project.model.Staff;
import group6.project.repo.CourseApplicationRepo;
import group6.project.repo.CourseFeeApplicationRepo;
import group6.project.repo.StaffRepo;

@Service
public class StaffService {
    private final StaffRepo staffRepo;
    private final CourseApplicationService courseApplicationService;
    private final CourseApplicationRepo courseApplicationRepo;
    private final CourseFeeApplicationService courseFeeApplicationService;
    private final CourseFeeApplicationRepo courseFeeApplicationRepo;

    public StaffService(StaffRepo staffRepo, CourseApplicationService courseApplicationService,
            CourseApplicationRepo courseApplicationRepo, CourseFeeApplicationService courseFeeApplicationService,
            CourseFeeApplicationRepo courseFeeApplicationRepo) {
        this.staffRepo = staffRepo;
        this.courseApplicationService = courseApplicationService;
        this.courseApplicationRepo = courseApplicationRepo;
        this.courseFeeApplicationService = courseFeeApplicationService;
        this.courseFeeApplicationRepo = courseFeeApplicationRepo;
    }

    // Staff details - Find staff records.
    public List<Staff> getAllStaff() {
        return staffRepo.findAll();
    }

    public List<Staff> getStaffByManager(Integer managerId) {
        return staffRepo.findByManager_UserId(managerId);
    }

    public Staff getStaff(Integer id) {
        return staffRepo.findById(id).orElse(null);
    }

    // Course applications - Prepare and retrieve application details.
    public List<CourseApplication> getCourseHistory(Staff staff, int year) {
        // Personal course history - Find this employee's applications for the selected year
        return courseApplicationService.findForStaffAndYear(staff, year);
    }

    public CourseApplication getCourseApplication(Integer id, Staff staff) {
        return courseApplicationService.getOwned(id, staff);
    }

    // Edit application - Allow changes only before approval or rejection
    public boolean isPending(CourseApplication application) {
        return application.getStatus() == ApplicationStatus.APPLIED
                || application.getStatus() == ApplicationStatus.UPDATED;
    }

    // Every workspace uses the same allowance totals and validation rules.
    public CourseApplicationService.Summary summary(CourseApplication form, Staff staff, Integer id) {
        return courseApplicationService.summary(form, staff, id);
    }

    @Transactional
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
    public List<CourseFeeApplication> getClaims(Staff staff) {
        return courseFeeApplicationRepo.findByApplicant_UserId(staff.getUserId());
    }

    private boolean alreadyClaimed(Integer courseId, List<CourseFeeApplication> claims) {
        for (CourseFeeApplication claim : claims) {
            if (claim.getCourseApplication() != null
                    && courseId.equals(claim.getCourseApplication().getCourseId())) {
                return true;
            }
        }
        return false;
    }

    public List<CourseApplication> getClaimableCourses(Staff staff) {
        // Claim eligibility: Load existing claims before checking the courses.
        List<CourseFeeApplication> claims = getClaims(staff);
        List<CourseApplication> result = new ArrayList<>();
        for (CourseApplication application : courseApplicationRepo.findByApplicant_UserIdAndStatusIn(
                staff.getUserId(), List.of(ApplicationStatus.COMPLETED))) {
            if (application.getCourseCategory() != CourseCategoryType.INTERNAL_TRAINING
                    && application.getCourseFee().signum() > 0 && !alreadyClaimed(application.getCourseId(), claims)) {
                result.add(application);
            }
        }
        return result;
    }

    @Transactional
    // Discussion: Move eligibility checks to CourseFeeApplicationService.submitApplication()
    public void submitClaim(Integer courseId, boolean paidPersonally, MultipartFile receipt,
            MultipartFile certificate, Staff staff) {
        CourseApplication course = getCourseApplication(courseId, staff);
        // Fee claim eligibility - Check personal payment and course completion.
        if (!paidPersonally) {
            throw new IllegalArgumentException("Only personally paid course fees can be claimed.");
        }
        if (course.getStatus() != ApplicationStatus.COMPLETED
                || course.getCourseCategory() == CourseCategoryType.INTERNAL_TRAINING
                || course.getCourseFee().signum() <= 0) {
            throw new IllegalArgumentException("Only completed fee-paying courses can be claimed.");
        }
        if (alreadyClaimed(courseId, getClaims(staff))) {
            throw new IllegalArgumentException("A claim already exists for this course.");
        }
        CourseFeeApplication claim = new CourseFeeApplication();
        claim.setApplicant(staff);
        claim.setCourseApplication(course);
        claim.setReceipt(readFile(receipt));
        claim.setCertificate(readFile(certificate));
        claim.setReceiptFileName(receipt.getOriginalFilename());
        claim.setCertificateFileName(certificate.getOriginalFilename());
        claim.setReceiptContentType(receipt.getContentType());
        claim.setCertificateContentType(certificate.getContentType());
        courseFeeApplicationService.submitApplication(claim);
    }

    // Claim documents - Require the receipt and completion certificate.
    private byte[] readFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload both the receipt and certificate.");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the document. Please upload it again.");
        }
    }

    // Claim details and downloads belong to the submitting employee, including a Manager's own claims.
    public CourseFeeApplication getClaim(Integer id, Staff staff) {
        CourseFeeApplication claim = courseFeeApplicationRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found."));
        if (claim.getApplicant() == null || !staff.getUserId().equals(claim.getApplicant().getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found.");
        }
        return claim;
    }

    // Reimbursement total - Add fees already reimbursed for the selected year
    public BigDecimal getReimbursedFees(Staff staff, int year) {
        BigDecimal total = BigDecimal.ZERO;
        for (CourseFeeApplication claim : getClaims(staff)) {
            if (claim.getReimbursedAt() != null && claim.getCourseApplication() != null
                    && claim.getCourseApplication().getCourseStartDate().getYear() == year) {
                total = total.add(claim.getCourseApplication().getCourseFee());
            }
        }
        return total;
    }

}
