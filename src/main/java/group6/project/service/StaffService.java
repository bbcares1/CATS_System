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

    // Discussion: CourseApplicationService.summary() should also count completed courses. Keep the extra totals here until agreed.
    public CourseApplicationService.Summary summary(CourseApplication form, Staff staff, Integer id) {
        if (form.getCourseStartDate() != null && form.getCourseEndDate() != null
                && form.getCourseEndDate().isBefore(form.getCourseStartDate())) {
            throw new IllegalArgumentException("The end date cannot be before the start date.");
        }
        CourseApplicationService.Summary total = courseApplicationService.summary(form, staff, id);
        int year = LocalDate.now().getYear();
        if (form.getCourseStartDate() != null) {
            year = form.getCourseStartDate().getYear();
        }
        // Annual allowance - Include completed courses in the days and budget used.
        double completedDays = 0;
        double completedFees = 0;
        for (CourseApplication course : getCourseHistory(staff, year)) {
            if (course.getStatus() == ApplicationStatus.COMPLETED && !course.getCourseId().equals(id)) {
                if (course.getTrainingDays() != null) {
                    completedDays += course.getTrainingDays();
                }
                completedFees += course.getCourseFee();
            }
        }
        return new CourseApplicationService.Summary(total.requestedDays(),
                Math.max(0, total.remainingDays() - completedDays),
                Math.max(0, total.remainingBudget() - completedFees),
                total.usedDays() + completedDays, total.usedBudget() + completedFees);
    }

    // Application validation - Check staff rules
    private void checkStaffApplication(CourseApplication form, Staff staff, Integer id) {
        // Required fields: Check title, category, course dates and justification.
        boolean titleMissing = form.getCourseTitle() == null || form.getCourseTitle().isBlank();
        boolean categoryMissing = form.getCourseCategory() == null;
        boolean datesMissing = form.getCourseStartDate() == null || form.getCourseEndDate() == null;
        boolean justificationMissing = form.getJustification() == null || form.getJustification().isBlank();
        if (titleMissing || categoryMissing || datesMissing || justificationMissing) {
            throw new IllegalArgumentException("Course title, category, dates and justification are required.");
        }
        double fee = form.getCourseFee();
        boolean halfDay = form.getHalfDayPeriod() != null && !form.getHalfDayPeriod().isBlank();
        // Half-day training: Allow half-day periods only for internal training.
        if (halfDay && form.getCourseCategory() != CourseCategoryType.INTERNAL_TRAINING) {
            throw new IllegalArgumentException("Please check the half-day selection.");
        }
        CourseApplicationService.Summary total = summary(form, staff, id);
        // Training days: Check the remaining annual allowance.
        if (total.requestedDays() > total.remainingDays()) {
            throw new IllegalArgumentException("Not enough training days remaining.");
        }
        // Training budget: Check external course and certification fees against the remaining budget.
        if (form.getCourseCategory() != CourseCategoryType.INTERNAL_TRAINING) {
            boolean feeWithinBudget = fee >= 0 && fee <= total.remainingBudget();
            if (!feeWithinBudget) {
                throw new IllegalArgumentException("Please check the course fee and remaining budget.");
            }
        }
        // Discussion: CourseApplicationService.overlaps() should treat only AM and PM as separate sessions
        // Course overlap: Check against applied, updated and approved courses.
        for (CourseApplication other : getCourseHistory(staff, form.getCourseStartDate().getYear())) {
            if (other.getCourseId().equals(id)) {
                continue;
            }
            boolean activeApplication = isPending(other) || other.getStatus() == ApplicationStatus.APPROVED;
            if (!activeApplication) {
                continue;
            }
            if (form.getCourseEndDate().isBefore(other.getCourseStartDate())
                    || other.getCourseEndDate().isBefore(form.getCourseStartDate())) {
                continue;
            }
            boolean otherHalfDay = "AM".equals(other.getHalfDayPeriod()) || "PM".equals(other.getHalfDayPeriod());
            if (halfDay && otherHalfDay) {
                boolean oneDay = form.getCourseStartDate().equals(form.getCourseEndDate())
                        && other.getCourseStartDate().equals(other.getCourseEndDate());
                boolean differentSessions = !form.getHalfDayPeriod().equals(other.getHalfDayPeriod());
                if (oneDay && differentSessions) {
                    continue;
                }
            }
            throw new IllegalArgumentException("The course dates clash with another application.");
        }
    }

    @Transactional
    public CourseApplication saveApplication(Integer id, CourseApplication form, Staff staff) {
        checkStaffApplication(form, staff, id);
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

    // Discussion: CourseApplicationService.complete() allows completion on the end date
    public void completeApplication(Integer id, String comments, Staff staff) {
        CourseApplication course = getCourseApplication(id, staff);
        // Complete course: Check the end date; the shared service checks status and comments.
        if (course.getCourseEndDate() == null || !course.getCourseEndDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("A course can only be marked completed after it ends.");
        }
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
                    && application.getCourseFee() > 0 && !alreadyClaimed(application.getCourseId(), claims)) {
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
                || course.getCourseFee() <= 0) {
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
                total += claim.getCourseApplication().getCourseFee();
            }
        }
        return total;
    }

}
