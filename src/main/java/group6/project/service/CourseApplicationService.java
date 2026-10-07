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
                .orElseThrow(() -> new IllegalArgumentException("Course application was not found."));
        if (application.getApplicant() == null
                || !application.getApplicant().getUserId().equals(staff.getUserId())) {
            throw new IllegalArgumentException("You can only access your own course applications.");
        }
        return application;
    }

    @Transactional
    public CourseApplication create(CourseApplication form, Staff staff) {
        validateAndPrepare(form, staff, null, true);
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

    public Summary summary(CourseApplication form, Staff staff, Integer excludedId) {
        if (form.getCourseCategory() == null || form.getCourseStartDate() == null
                || form.getCourseEndDate() == null) {
            double allowance = allowanceDays(staff, LocalDate.now().getYear());
            double budget = allowanceBudget(staff, LocalDate.now().getYear());
            List<CourseApplication> used = usedApplications(staff, LocalDate.now().getYear(), excludedId);
            double usedDays = used.stream().mapToDouble(a -> a.getTrainingDays() == null ? 0 : a.getTrainingDays()).sum();
            double usedBudget = used.stream().mapToDouble(a -> a.getCourseFee()).sum();
            return new Summary(0, Math.max(0, allowance - usedDays),
                    Math.max(0, budget - usedBudget), usedDays, usedBudget);
        }
        validateBasic(form);
        double days = form.getCourseStartDate() == null || form.getCourseEndDate() == null
                ? 0 : calculateTrainingDays(form);
        double usedDays = usedApplications(staff, form.getCourseStartDate() == null
                ? LocalDate.now().getYear() : form.getCourseStartDate().getYear(), excludedId)
                .stream().mapToDouble(a -> a.getTrainingDays() == null ? 0 : a.getTrainingDays()).sum();
        double usedBudget = usedApplications(staff, form.getCourseStartDate() == null
                ? LocalDate.now().getYear() : form.getCourseStartDate().getYear(), excludedId)
                .stream().mapToDouble(a -> a.getCourseFee()).sum();
        int year = form.getCourseStartDate().getYear();
        double entitlementDays = allowanceDays(staff, year);
        double entitlementBudget = allowanceBudget(staff, year);
        return new Summary(days, Math.max(0, entitlementDays == 0 ? 0 : entitlementDays - usedDays),
                Math.max(0, entitlementBudget - usedBudget),
                usedDays, usedBudget);
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
        if (form.getCourseEndDate().isBefore(form.getCourseStartDate())) {
            throw new IllegalArgumentException("The end date cannot be before the start date.");
        }
        if (form.getCourseStartDate().getYear() != form.getCourseEndDate().getYear()) {
            throw new IllegalArgumentException("A course must be within one calendar year.");
        }
        if (isNonWorkingDay(form.getCourseStartDate()) || isNonWorkingDay(form.getCourseEndDate())) {
            throw new IllegalArgumentException("Start and end dates must be working days.");
        }
        if (form.getCourseFee() < 0) {
            throw new IllegalArgumentException("Course fee cannot be negative.");
        }
        if (form.getCourseCategory() == CourseCategoryType.INTERNAL_TRAINING) {
            form.setCourseFee(0);
        } else if ("HALF_DAY".equals(form.getHalfDayPeriod())
                || "AM".equals(form.getHalfDayPeriod()) || "PM".equals(form.getHalfDayPeriod())) {
            throw new IllegalArgumentException("Only Internal Training supports half-day sessions.");
        }
        if (form.getCourseCategory() == CourseCategoryType.INTERNAL_TRAINING
                && form.getHalfDayPeriod() != null && !form.getHalfDayPeriod().isBlank()
                && !Set.of("AM", "PM").contains(form.getHalfDayPeriod())) {
            throw new IllegalArgumentException("Half-day period must be AM or PM.");
        }
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
                && first.getHalfDayPeriod() != null && second.getHalfDayPeriod() != null
                && !first.getHalfDayPeriod().equals(second.getHalfDayPeriod())) {
            return false;
        }
        return true;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
