package group6.project.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
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
    private final TrainingEntitlementService entitlements;
    private final CourseApplicationRepo courseApplicationRepo;
    private final CourseFeeApplicationService courseFeeApplicationService;
    private final CourseFeeApplicationRepo courseFeeApplicationRepo;

    public StaffService(StaffRepo staffRepo, CourseApplicationService courseApplicationService,
            CourseApplicationRepo courseApplicationRepo, CourseFeeApplicationService courseFeeApplicationService,
            CourseFeeApplicationRepo courseFeeApplicationRepo, TrainingEntitlementService entitlements) {
        this.entitlements = entitlements;
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

    // The dashboard uses the same annual calculation as application and Manager pages.
    public TrainingEntitlementService.AnnualSummary summary(Staff staff, int year) {
        return entitlements.summary(staff, year, null);
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
        CourseApplication course = courseApplicationService.getOwned(courseId, staff);
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

    public CourseFeeApplication getClaim(Integer id) {
        return courseFeeApplicationRepo.findById(id).orElse(null);
    }

    // Reimbursement total - Add fees already reimbursed for the selected year
    public double getReimbursedFees(Staff staff, int year) {
        double total = 0;
        for (CourseFeeApplication claim : getClaims(staff)) {
            if (claim.getReimbursedAt() != null && claim.getCourseApplication() != null
                    && claim.getCourseApplication().getCourseStartDate().getYear() == year) {
                total += claim.getCourseApplication().getCourseFee().doubleValue();
            }
        }
        return total;
    }

}
