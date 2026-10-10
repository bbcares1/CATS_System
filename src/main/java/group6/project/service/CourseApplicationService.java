package group6.project.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import group6.project.model.ApplicationStatus;
import group6.project.model.CourseApplication;
import group6.project.model.CourseCategoryType;
import group6.project.model.Staff;
import group6.project.repo.CourseApplicationRepo;
import group6.project.repo.ExcludedDaysRepo;
import group6.project.repo.TrainingEntitlementRepo;

@Service
public class CourseApplicationService {
    private static final List<ApplicationStatus> ACTIVE_STATUSES =
            List.of(ApplicationStatus.APPLIED, ApplicationStatus.UPDATED, ApplicationStatus.APPROVED);
    private final CourseApplicationRepo courseApplicationRepo;
    private final TrainingEntitlementService entitlements;
    private final group6.project.repo.StaffRepo employees;
    private final ExcludedDaysRepo excludedDaysRepo;

    public CourseApplicationService(CourseApplicationRepo courseApplicationRepo,
            TrainingEntitlementService entitlements, ExcludedDaysRepo excludedDaysRepo, group6.project.repo.StaffRepo employees) {
        this.courseApplicationRepo = courseApplicationRepo;
        this.entitlements = entitlements;
        this.employees = employees;
        this.excludedDaysRepo = excludedDaysRepo;
    }

    public List<CourseApplication> findForStaffAndYear(Staff staff, int year) {
        return courseApplicationRepo.findByApplicant_UserIdAndCourseStartDateBetweenOrderByCourseStartDateAsc(
                staff.getUserId(), LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    public CourseApplication getOwned(Integer id, Staff staff) {
        CourseApplication application = courseApplicationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course application was not found."));
        if (application.getApplicant() == null
                || !application.getApplicant().getUserId().equals(staff.getUserId())) {
            throw new IllegalArgumentException("You can only access your own course applications.");
        }
        return application;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public CourseApplication create(CourseApplication form, Staff staff) {
        lockEmployee(staff);
        validateAndPrepare(form, staff, null, true);
        form.setApplicant(staff);
        form.setStatus(ApplicationStatus.APPLIED);
        form.setSubmittedAt(LocalDateTime.now());
        form.setUpdatedAt(LocalDateTime.now());
        return courseApplicationRepo.save(form);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public CourseApplication update(Integer id, CourseApplication form, Staff staff) {
        lockEmployee(staff);
        CourseApplication existing = getOwned(id, staff);
        if (existing.getStatus() != ApplicationStatus.APPLIED
                && existing.getStatus() != ApplicationStatus.UPDATED) {
            throw new IllegalStateException("Only Applied or Updated applications can be edited.");
        }
        validateAndPrepare(form, staff, id, true);
        existing.setCourseTitle(form.getCourseTitle());
        existing.setCourseCategory(form.getCourseCategory());
        existing.setTrainingProvider(form.getTrainingProvider());
        existing.setCourseStartDate(form.getCourseStartDate());
        existing.setCourseEndDate(form.getCourseEndDate());
        existing.setCourseFee(form.getCourseFee());
        existing.setJustification(form.getJustification());
        existing.setWorkDissemination(form.getWorkDissemination());
        existing.setTrainingDays(form.getTrainingDays());
        existing.setHalfDayPeriod(form.getHalfDayPeriod());
        existing.setStatus(ApplicationStatus.UPDATED);
        existing.setUpdatedAt(LocalDateTime.now());
        return courseApplicationRepo.save(existing);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void delete(Integer id, Staff staff) {
        lockEmployee(staff);
        CourseApplication application = getOwned(id, staff);
        if (application.getStatus() != ApplicationStatus.APPLIED
                && application.getStatus() != ApplicationStatus.UPDATED) {
            throw new IllegalStateException("Only Applied or Updated applications can be deleted.");
        }
        application.setStatus(ApplicationStatus.DELETED);
        application.setUpdatedAt(LocalDateTime.now());
        courseApplicationRepo.save(application);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void cancel(Integer id, Staff staff) {
        lockEmployee(staff);
        CourseApplication application = getOwned(id, staff);
        if (application.getStatus() != ApplicationStatus.APPROVED) {
            throw new IllegalStateException("Only Approved applications can be cancelled.");
        }
        application.setStatus(ApplicationStatus.CANCELLED);
        application.setUpdatedAt(LocalDateTime.now());
        courseApplicationRepo.save(application);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void complete(Integer id, String comments, Staff staff) {
        lockEmployee(staff);
        CourseApplication application = getOwned(id, staff);
        if (application.getStatus() != ApplicationStatus.APPROVED) {
            throw new IllegalStateException("Only Approved applications can be completed.");
        }
        if (application.getCourseEndDate() == null
                || !application.getCourseEndDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("A course can only be marked completed after it ends.");
        }
        if (comments == null || comments.isBlank()) {
            throw new IllegalArgumentException("Experience comments are required.");
        }
        application.setExperienceComments(comments.trim());
        application.setStatus(ApplicationStatus.COMPLETED);
        application.setUpdatedAt(LocalDateTime.now());
        courseApplicationRepo.save(application);
    }

    // A partial form can show annual allowance before dates have been chosen.
    public Summary summary(CourseApplication form, Staff staff, Integer excludedId) {
        int year = form.getCourseStartDate() == null ? LocalDate.now().getYear() : form.getCourseStartDate().getYear();
        var annual = entitlements.summary(staff, year, excludedId);
        double requested = 0;
        if (form.getCourseCategory() != null && form.getCourseStartDate() != null && form.getCourseEndDate() != null) {
            requested = calculateTrainingDays(form, false);
        }
        return new Summary(requested, annual.remainingDays(), annual.remainingBudget(),
                annual.usedDays(), annual.usedBudget(), annual.reservedDays(), annual.reservedBudget());
    }

    public record Summary(double requestedDays, double remainingDays, BigDecimal remainingBudget,
            double usedDays, BigDecimal usedBudget, double reservedDays, BigDecimal reservedBudget) {}

    // Submission, editing and the annual Admin form coordinate on the same employee row.
    private void lockEmployee(Staff staff) {
        employees.lockById(staff.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found."));
    }

    // Apply one set of rules regardless of which employee page submitted the application.
    private void validateAndPrepare(CourseApplication form, Staff staff, Integer excludedId, boolean futureRequired) {
        validateBasic(form);
        BigDecimal fee = form.getCourseFee();
        if (fee == null || fee.signum() < 0 || fee.scale() > 2
                || fee.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException("Course fee must be a non-negative amount with up to two decimal places.");
        }
        if (form.getCourseCategory() == CourseCategoryType.INTERNAL_TRAINING) form.setCourseFee(BigDecimal.ZERO);
        form.setTrainingDays(calculateTrainingDays(form, futureRequired));
        var total = entitlements.summary(staff, form.getCourseStartDate().getYear(), excludedId);
        if (form.getTrainingDays() > total.remainingDays()) {
            throw new IllegalArgumentException("Training days exceed the remaining annual allowance.");
        }
        if (form.getCourseFee().compareTo(total.remainingBudget()) > 0) {
            throw new IllegalArgumentException("Course fee exceeds the remaining annual training budget.");
        }
        for (CourseApplication other : usedApplications(staff, form.getCourseStartDate().getYear(), excludedId)) {
            if (overlaps(form, other)) {
                throw new IllegalArgumentException("The course overlaps another active application.");
            }
        }
    }

    private void validateBasic(CourseApplication form) {
        if (form.getCourseTitle() == null || form.getCourseTitle().isBlank()
                || form.getCourseCategory() == null || form.getCourseStartDate() == null
                || form.getCourseEndDate() == null || form.getJustification() == null
                || form.getJustification().isBlank()) {
            throw new IllegalArgumentException("Course title, category, dates and justification are required.");
        }
    }

    // Load the calendar once, then let the pure date utility count the working days.
    private double calculateTrainingDays(CourseApplication course, boolean future) {
        Set<LocalDate> holidays = new java.util.HashSet<>();
        for (var holiday : excludedDaysRepo.findAll()) holidays.add(holiday.getDate());
        return TrainingDayCalculator.count(course.getCourseCategory(), course.getCourseStartDate(),
                course.getCourseEndDate(), course.getHalfDayPeriod(), holidays, future);
    }

    private List<CourseApplication> usedApplications(Staff staff, int year, Integer excludedId) {
        return courseApplicationRepo.findByApplicant_UserIdAndStatusIn(staff.getUserId(), ACTIVE_STATUSES)
                .stream()
                .filter(a -> a.getCourseStartDate() != null && a.getCourseStartDate().getYear() == year)
                .filter(a -> excludedId == null || !a.getCourseId().equals(excludedId))
                .toList();
    }

    private boolean overlaps(CourseApplication first, CourseApplication second) {
        if (first.getCourseEndDate().isBefore(second.getCourseStartDate())
                || second.getCourseEndDate().isBefore(first.getCourseStartDate())) {
            return false;
        }
        boolean oppositePeriods = "AM".equals(first.getHalfDayPeriod()) && "PM".equals(second.getHalfDayPeriod())
                || "PM".equals(first.getHalfDayPeriod()) && "AM".equals(second.getHalfDayPeriod());
        if (first.getCourseStartDate().equals(first.getCourseEndDate())
                && second.getCourseStartDate().equals(second.getCourseEndDate()) && oppositePeriods) {
            return false;
        }
        return true;
    }

}
