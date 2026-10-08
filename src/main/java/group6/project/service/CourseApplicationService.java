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
            List.of(ApplicationStatus.APPLIED, ApplicationStatus.UPDATED, ApplicationStatus.APPROVED, ApplicationStatus.COMPLETED);
    private final CourseApplicationRepo courseApplicationRepo;
    private final TrainingEntitlementRepo entitlementRepo;
    private final ExcludedDaysRepo excludedDaysRepo;

    public CourseApplicationService(CourseApplicationRepo courseApplicationRepo,
            TrainingEntitlementRepo entitlementRepo, ExcludedDaysRepo excludedDaysRepo) {
        this.courseApplicationRepo = courseApplicationRepo;
        this.entitlementRepo = entitlementRepo;
        this.excludedDaysRepo = excludedDaysRepo;
    }

    public List<CourseApplication> findForStaffAndYear(Staff staff, int year) {
        return courseApplicationRepo.findByApplicant_UserIdAndCourseStartDateBetweenOrderByCourseStartDateAsc(
                staff.getUserId(), LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    public CourseApplication getOwned(Integer id, Staff staff) {
        CourseApplication application = courseApplicationRepo.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Course application was not found."));
        if (application.getApplicant() == null
                || !java.util.Objects.equals(application.getApplicant().getUserId(), staff.getUserId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "You can only access your own course applications.");
        }
        return application;
    }

    @Transactional
    public CourseApplication create(CourseApplication form, Staff staff) {
        validateAndPrepare(form, staff, null, true);
        form.setCourseId(null);
        form.setReviewedBy(null);
        form.setReviewedAt(null);
        form.setDecisionReason(null);
        form.setExperienceComments(null);
        form.setApplicant(staff);
        form.setStatus(ApplicationStatus.APPLIED);
        form.setSubmittedAt(LocalDateTime.now());
        form.setUpdatedAt(LocalDateTime.now());
        return courseApplicationRepo.save(form);
    }

    @Transactional
    public CourseApplication update(Integer id, CourseApplication form, Staff staff) {
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

    @Transactional
    public void delete(Integer id, Staff staff) {
        CourseApplication application = getOwned(id, staff);
        if (application.getStatus() != ApplicationStatus.APPLIED
                && application.getStatus() != ApplicationStatus.UPDATED) {
            throw new IllegalStateException("Only Applied or Updated applications can be deleted.");
        }
        application.setStatus(ApplicationStatus.DELETED);
        application.setUpdatedAt(LocalDateTime.now());
        courseApplicationRepo.save(application);
    }

    @Transactional
    public void cancel(Integer id, Staff staff) {
        CourseApplication application = getOwned(id, staff);
        if (application.getStatus() != ApplicationStatus.APPROVED) {
            throw new IllegalStateException("Only Approved applications can be cancelled.");
        }
        application.setStatus(ApplicationStatus.CANCELLED);
        application.setUpdatedAt(LocalDateTime.now());
        courseApplicationRepo.save(application);
    }

    @Transactional
    public void complete(Integer id, String comments, Staff staff) {
        CourseApplication application = getOwned(id, staff);
        if (application.getStatus() != ApplicationStatus.APPROVED) {
            throw new IllegalStateException("Only Approved applications can be completed.");
        }
        if (application.getCourseEndDate() == null
                || application.getCourseEndDate().isAfter(LocalDate.now())) {
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

    public List<CourseApplication> pendingForManager(group6.project.model.Manager manager) {
        return courseApplicationRepo.findByApplicant_Manager_UserIdAndStatusInOrderBySubmittedAtAsc(
                manager.getUserId(), List.of(ApplicationStatus.APPLIED, ApplicationStatus.UPDATED));
    }

    public List<CourseApplication> employeeCourseHistory(Staff staff, group6.project.model.Manager manager) {
        requireDirectReport(staff, manager);
        return courseApplicationRepo.findByApplicant_UserIdOrderByCourseStartDateDesc(staff.getUserId());
    }

    @Transactional
    public CourseApplication review(Integer id, group6.project.model.Manager manager,
            boolean approved, String reason) {
        CourseApplication application = courseApplicationRepo.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Course application was not found."));
        requireDirectReport(application.getApplicant(), manager);
        if (application.getStatus() != ApplicationStatus.APPLIED
                && application.getStatus() != ApplicationStatus.UPDATED) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Only pending applications can be reviewed.");
        }
        if (!approved && (reason == null || reason.isBlank())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "A rejection reason is required.");
        }
        if (approved) {
            try {
                validateAndPrepare(application, application.getApplicant(), id, true);
            } catch (IllegalArgumentException error) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT, error.getMessage());
            }
        }
        application.setStatus(approved ? ApplicationStatus.APPROVED : ApplicationStatus.REJECTED);
        application.setDecisionReason(reason == null ? null : reason.trim());
        application.setReviewedBy(manager);
        application.setReviewedAt(LocalDateTime.now());
        application.setUpdatedAt(application.getReviewedAt());
        return courseApplicationRepo.save(application);
    }

    private void requireDirectReport(Staff staff, group6.project.model.Manager manager) {
        if (staff == null || staff.getManager() == null || manager.getUserId() == null
                || !manager.getUserId().equals(staff.getManager().getUserId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "You can only access your direct reports' applications.");
        }
    }

    public Summary summary(CourseApplication form, Staff staff, Integer excludedId) {
        if (excludedId != null) getOwned(excludedId, staff);
        int year = form.getCourseStartDate() == null ? LocalDate.now().getYear()
                : form.getCourseStartDate().getYear();
        double days = 0;
        if (form.getCourseStartDate() != null && form.getCourseEndDate() != null
                && form.getCourseCategory() != null) {
            validateSchedule(form);
            days = calculateTrainingDays(form);
        }
        List<CourseApplication> used = usedApplications(staff, year, excludedId);
        double usedDays = used.stream().mapToDouble(a -> a.getTrainingDays() == null ? 0 : a.getTrainingDays()).sum();
        double usedBudget = used.stream().mapToDouble(CourseApplication::getCourseFee).sum();
        return new Summary(days, Math.max(0, allowanceDays(staff, year) - usedDays),
                Math.max(0, allowanceBudget(staff, year) - usedBudget), usedDays, usedBudget);
    }

    private double allowanceDays(Staff staff, int year) {
        return entitlementRepo.findByStaff_UserIdAndYear(staff.getUserId(), year)
                .map(e -> e.getStaff() != null && e.getStaff().getTrainingDays() != null
                        ? e.getStaff().getTrainingDays() : staff.getTrainingDays())
                .orElse(staff.getTrainingDays() == null ? 0 : staff.getTrainingDays());
    }

    private double allowanceBudget(Staff staff, int year) {
        return entitlementRepo.findByStaff_UserIdAndYear(staff.getUserId(), year)
                .map(e -> e.getStaff() != null && e.getStaff().getTrainingBudget() != null
                        ? e.getStaff().getTrainingBudget() : staff.getTrainingBudget())
                .orElse(staff.getTrainingBudget() == null ? 0 : staff.getTrainingBudget());
    }

    public record Summary(double requestedDays, double remainingDays, double remainingBudget,
            double usedDays, double usedBudget) {}

    private void validateAndPrepare(CourseApplication form, Staff staff, Integer excludedId,
            boolean futureRequired) {
        validateBasic(form);
        if (futureRequired && !form.getCourseStartDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("The course start date must be after today.");
        }
        validateSchedule(form);
        if (!Double.isFinite(form.getCourseFee()) || form.getCourseFee() < 0) {
            throw new IllegalArgumentException("Course fee must be a finite, non-negative amount.");
        }
        if (form.getCourseCategory() == CourseCategoryType.INTERNAL_TRAINING) form.setCourseFee(0);
        form.setTrainingDays(calculateTrainingDays(form));
        Summary summary = summary(form, staff, excludedId);
        if (summary.requestedDays() > summary.remainingDays() + 0.0001) {
            throw new IllegalArgumentException("Training days exceed the remaining annual allowance.");
        }
        if (form.getCourseCategory() != CourseCategoryType.INTERNAL_TRAINING
                && form.getCourseFee() > summary.remainingBudget() + 0.0001) {
            throw new IllegalArgumentException("Course fee exceeds the remaining annual training budget.");
        }
        for (CourseApplication other : usedApplications(staff, form.getCourseStartDate().getYear(), excludedId)) {
            if (overlaps(form, other)) {
                throw new IllegalArgumentException("The course overlaps another active application.");
            }
        }
    }

    private void validateSchedule(CourseApplication form) {
        if (form.getCourseEndDate().isBefore(form.getCourseStartDate())) {
            throw new IllegalArgumentException("The end date cannot be before the start date.");
        }
        if (form.getCourseStartDate().getYear() != form.getCourseEndDate().getYear()) {
            throw new IllegalArgumentException("A course must be within one calendar year.");
        }
        if (isNonWorkingDay(form.getCourseStartDate()) || isNonWorkingDay(form.getCourseEndDate())) {
            throw new IllegalArgumentException("Start and end dates must be working days.");
        }
        String half = form.getHalfDayPeriod();
        if (half != null && half.isBlank()) form.setHalfDayPeriod(null);
        if (form.getHalfDayPeriod() != null) {
            if (form.getCourseCategory() != CourseCategoryType.INTERNAL_TRAINING
                    || !Set.of("AM", "PM").contains(form.getHalfDayPeriod())) {
                throw new IllegalArgumentException("Only Internal Training supports AM or PM half-day sessions.");
            }
            if (!form.getCourseStartDate().equals(form.getCourseEndDate())) {
                throw new IllegalArgumentException("A half-day session must start and end on the same date.");
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

    private boolean isNonWorkingDay(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY
                || excludedDaysRepo.existsByDate(date);
    }

    private double calculateTrainingDays(CourseApplication application) {
        double days = 0;
        for (LocalDate date = application.getCourseStartDate();
                !date.isAfter(application.getCourseEndDate()); date = date.plusDays(1)) {
            if (!isNonWorkingDay(date)) {
                days += 1;
            }
        }
        if ("AM".equals(application.getHalfDayPeriod()) || "PM".equals(application.getHalfDayPeriod())) {
            days -= 0.5;
        }
        return round(days);
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
        if (first.getCourseStartDate().equals(first.getCourseEndDate())
                && second.getCourseStartDate().equals(second.getCourseEndDate())
                && (("AM".equals(first.getHalfDayPeriod()) && "PM".equals(second.getHalfDayPeriod()))
                    || ("PM".equals(first.getHalfDayPeriod()) && "AM".equals(second.getHalfDayPeriod())))) {
            return false;
        }
        return true;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
