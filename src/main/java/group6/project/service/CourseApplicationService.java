package group6.project.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import group6.project.model.ApplicationStatus;
import group6.project.model.CourseApplication;
import group6.project.model.CourseCategoryType;
import group6.project.model.User;
import group6.project.model.Staff;
import group6.project.repo.CourseApplicationRepo;
import group6.project.repo.ExcludedDaysRepo;
import group6.project.repo.StaffRepo;
import group6.project.repo.TrainingEntitlementRepo;

@Service
public class CourseApplicationService {
    public static final List<ApplicationStatus> RESERVED_STATUSES = List.of(
            ApplicationStatus.APPLIED, ApplicationStatus.UPDATED, ApplicationStatus.APPROVED,
            ApplicationStatus.COMPLETED);
    private final CourseApplicationRepo applications;
    private final TrainingEntitlementRepo entitlements;
    private final ExcludedDaysRepo holidays;
    private final StaffRepo employees;
    private final ApprovalRoutingService routing;
    private final group6.project.repo.TrainingCalendarPolicyRepo calendar;

    // Both workspaces share this policy and the same persisted employee identity.
    public CourseApplicationService(CourseApplicationRepo applications,
            TrainingEntitlementRepo entitlements, ExcludedDaysRepo holidays, StaffRepo employees, ApprovalRoutingService routing,
            group6.project.repo.TrainingCalendarPolicyRepo calendar) {
        this.applications = applications;
        this.entitlements = entitlements;
        this.holidays = holidays;
        this.employees = employees;
        this.routing = routing;
        this.calendar = calendar;
    }

    // Keep soft-deleted and cancelled records in the employee's annual history.
    public List<CourseApplication> findForStaffAndYear(User staff, int year) {
        return applications.findByApplicant_UserIdAndCourseStartDateBetweenOrderByCourseStartDateAsc(
                staff.getUserId(), LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    // A caller cannot select another employee's application by changing a URL.
    public CourseApplication getOwned(Integer id, User staff) {
        CourseApplication course = applications.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        if (course.getApplicant() == null || !staff.getUserId().equals(course.getApplicant().getUserId())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        }
        return course;
    }

    // Copy only applicant-editable fields; status, identity and review data come from the server.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CourseApplication create(CourseApplication form, User staff) {
        User reviewer = prepareReviewer(staff, form.getApprovalManagerId());
        lockEmployee(staff);
        validateAndPrepare(form, staff, null, true);
        CourseApplication course = new CourseApplication();
        copyDetails(form, course);
        course.setApplicant(staff);
        course.setApprovalManager(reviewer);
        course.setStatus(ApplicationStatus.APPLIED);
        course.setSubmittedAt(LocalDateTime.now());
        course.setUpdatedAt(course.getSubmittedAt());
        return applications.save(course);
    }

    // Acquire account locks before catalogue locks as well as for the other-course form.
    public User prepareReviewer(User staff, Integer selectedId) { return routing.forSubmission(staff, selectedId); }

    // Editing releases this application's old reservation before checking the replacement.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CourseApplication update(Integer id, CourseApplication form, User staff) {
        lockEmployee(staff);
        CourseApplication course = getOwned(id, staff);
        requirePending(course);
        if (form.getVersion() == null || !java.util.Objects.equals(form.getVersion(), course.getVersion())) {
            throw new IllegalStateException("This application changed. Reload it before editing.");
        }
        if (course.getCatalogueCourse() != null) {
            // Catalogue edits never replace the price/provider/category originally requested.
            form.setCourseTitle(course.getCourseTitle());
            form.setCourseCategory(course.getCourseCategory());
            form.setTrainingProvider(course.getTrainingProvider());
            form.setCourseFee(course.getCourseFee());
            form.setCatalogueCourse(course.getCatalogueCourse());
            form.setCatalogueBatch(course.getCatalogueBatch());
            if (course.getCatalogueBatch() != null) {
                form.setCourseStartDate(course.getCourseStartDate());
                form.setCourseEndDate(course.getCourseEndDate());
                form.setHalfDayPeriod(course.getHalfDayPeriod());
            }
        }
        validateAndPrepare(form, staff, id, true);
        copyDetails(form, course);
        course.setStatus(ApplicationStatus.UPDATED);
        course.setUpdatedAt(LocalDateTime.now());
        return applications.save(course);
    }

    // Delete means withdraw a pending request, while keeping its history.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(Integer id, User staff) {
        lockEmployee(staff);
        CourseApplication course = getOwned(id, staff);
        requirePending(course);
        changeStatus(course, ApplicationStatus.DELETED);
    }

    // Only an approved booking can be cancelled and release its allowance.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancel(Integer id, User staff) {
        lockEmployee(staff);
        CourseApplication course = getOwned(id, staff);
        requireStatus(course, ApplicationStatus.APPROVED, "Only Approved applications can be cancelled.");
        changeStatus(course, ApplicationStatus.CANCELLED);
    }

    // Completion is available from the day after the course ends and retains its allowance use.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void complete(Integer id, String comments, User staff) {
        lockEmployee(staff);
        CourseApplication course = getOwned(id, staff);
        requireStatus(course, ApplicationStatus.APPROVED, "Only Approved applications can be completed.");
        if (course.getCourseEndDate() == null || !course.getCourseEndDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("A course can only be marked completed after it ends.");
        }
        course.setExperienceComments(requiredText(comments, "Experience comments", 2000));
        changeStatus(course, ApplicationStatus.COMPLETED);
    }

    // A partial form still shows the employee's current allowance without needing every field.
    public Summary summary(CourseApplication form, User staff, Integer excludedId) {
        int year = form.getCourseStartDate() == null ? LocalDate.now().getYear()
                : form.getCourseStartDate().getYear();
        double requested = 0;
        if (form.getCourseStartDate() != null && form.getCourseEndDate() != null) {
            validateDates(form, false);
            requested = calculateTrainingDays(form);
        }
        Summary annual = summaryForYear(staff, year, excludedId);
        return new Summary(requested, annual.remainingDays(), annual.remainingBudget(),
                annual.usedDays(), annual.usedBudget(), annual.dayLimit(), annual.budget(),
                annual.committedDays(), annual.committedBudget());
    }

    // Completed courses consume allowance too; cancelled, rejected and deleted courses do not.
    public Summary summaryForYear(User staff, int year, Integer excludedId) {
        var allowance = entitlements.findByStaff_UserIdAndYear(staff.getUserId(), year);
        double limit = allowance.map(e -> e.getDayLimit()).orElse(0d);
        BigDecimal budget = allowance.map(e -> e.getBudget()).orElse(BigDecimal.ZERO);
        var used = reservedApplications(staff, year, excludedId);
        double days = used.stream().mapToDouble(a -> a.getTrainingDays() == null ? 0 : a.getTrainingDays()).sum();
        BigDecimal fees = used.stream().map(CourseApplication::getCourseFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var committed = used.stream().filter(a -> a.getStatus() == ApplicationStatus.APPROVED
                || a.getStatus() == ApplicationStatus.COMPLETED).toList();
        double committedDays = committed.stream().mapToDouble(a -> a.getTrainingDays() == null ? 0 : a.getTrainingDays()).sum();
        BigDecimal committedFees = committed.stream().map(CourseApplication::getCourseFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Summary(0, Math.max(0, limit - days), budget.subtract(fees).max(BigDecimal.ZERO),
                days, fees, limit, budget, committedDays, committedFees);
    }

    public record Summary(double requestedDays, double remainingDays, BigDecimal remainingBudget,
            double usedDays, BigDecimal usedBudget, double dayLimit, BigDecimal budget,
            double committedDays, BigDecimal committedBudget) {}

    // Manager approval reuses the same reservations and overlap rules without requiring a new start date.
    public void validateForApproval(CourseApplication course) {
        validateAndPrepare(course, course.getApplicant(), course.getCourseId(), false);
    }

    // One employee lock prevents concurrent requests from both seeing the same remaining allowance.
    public void lockEmployee(User staff) {
        Staff current = employees.lockById(staff.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Employee was not found."));
        if (!current.isActive() || staff.getVersion() != null
                && !java.util.Objects.equals(staff.getVersion(), current.getVersion())) {
            throw new IllegalArgumentException("Your account changed or was disabled. Sign in again before continuing.");
        }
    }

    // Apply server-side rules once, regardless of which page submits the request.
    private void validateAndPrepare(CourseApplication form, User staff, Integer excludedId, boolean future) {
        calendar.readCalendar().orElseThrow(() -> new IllegalStateException("Working calendar is not configured."));
        if (form.getCourseTitle() == null || form.getCourseTitle().isBlank()
                || form.getCourseCategory() == null || form.getCourseStartDate() == null
                || form.getCourseEndDate() == null || form.getJustification() == null
                || form.getJustification().isBlank()) {
            throw new IllegalArgumentException("Course title, category, dates and justification are required.");
        }
        form.setCourseTitle(requiredText(form.getCourseTitle(), "Course title", 255));
        form.setTrainingProvider(requiredText(form.getTrainingProvider(), "Training provider", 255));
        form.setJustification(requiredText(form.getJustification(), "Justification", 2000));
        if (form.getWorkDissemination() != null && form.getWorkDissemination().length() > 2000) {
            throw new IllegalArgumentException("Work arrangements cannot exceed 2000 characters.");
        }
        validateDates(form, future);
        BigDecimal fee = form.getCourseFee();
        if (fee == null || fee.signum() < 0 || fee.scale() > 2 || fee.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException("Course fee must be a non-negative amount with at most two decimal places.");
        }
        if (form.getCourseCategory() == CourseCategoryType.INTERNAL_TRAINING) {
            form.setCourseFee(BigDecimal.ZERO);
        }
        form.setTrainingDays(calculateTrainingDays(form));
        Summary total = summaryForYear(staff, form.getCourseStartDate().getYear(), excludedId);
        if (form.getTrainingDays() > total.remainingDays()) {
            throw new IllegalArgumentException("Training days exceed the remaining annual allowance.");
        }
        if (form.getCourseFee().compareTo(total.remainingBudget()) > 0) {
            throw new IllegalArgumentException("Course fee exceeds the remaining annual training budget.");
        }
        for (CourseApplication other : reservedApplications(staff, form.getCourseStartDate().getYear(), excludedId)) {
            if (other.getStatus() != ApplicationStatus.COMPLETED && overlaps(form, other)) {
                throw new IllegalArgumentException("The course overlaps another active application.");
            }
        }
    }

    // Admin schedules and employee applications use identical working-day and half-day rules.
    public void validateSchedule(CourseCategoryType category, LocalDate start, LocalDate end, String halfDay, boolean future) {
        calendar.readCalendar().orElseThrow(() -> new IllegalStateException("Working calendar is not configured."));
        if (start == null || end == null || category == null) throw new IllegalArgumentException("Category and both dates are required.");
        CourseApplication schedule = new CourseApplication();
        schedule.setCourseCategory(category); schedule.setCourseStartDate(start); schedule.setCourseEndDate(end); schedule.setHalfDayPeriod(halfDay);
        validateDates(schedule, future);
    }

    // Split cross-year courses into separate requests; AM/PM is a single-day Internal session.
    private void validateDates(CourseApplication course, boolean future) {
        LocalDate start = course.getCourseStartDate();
        LocalDate end = course.getCourseEndDate();
        if (future && !start.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("The course start date must be after today.");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("The end date cannot be before the start date.");
        }
        if (start.getYear() != end.getYear()) {
            throw new IllegalArgumentException("A course must be within one calendar year.");
        }
        if (isNonWorkingDay(start) || isNonWorkingDay(end)) {
            throw new IllegalArgumentException("Start and end dates must be working days.");
        }
        String period = course.getHalfDayPeriod();
        if (period != null && !period.isBlank()) {
            if (course.getCourseCategory() != CourseCategoryType.INTERNAL_TRAINING) {
                throw new IllegalArgumentException("Only Internal Training supports half-day sessions.");
            }
            if (!Set.of("AM", "PM").contains(period) || !start.equals(end)) {
                throw new IllegalArgumentException("AM/PM half-day sessions must be on one date.");
            }
        }
    }

    // Exclude weekends and the Admin-maintained public holidays from duration.
    private double calculateTrainingDays(CourseApplication course) {
        if ("AM".equals(course.getHalfDayPeriod()) || "PM".equals(course.getHalfDayPeriod())) {
            return 0.5;
        }
        double days = 0;
        for (LocalDate day = course.getCourseStartDate(); !day.isAfter(course.getCourseEndDate()); day = day.plusDays(1)) {
            if (!isNonWorkingDay(day)) {
                days++;
            }
        }
        return days;
    }

    // Check the same calendar for date validation and duration calculation.
    private boolean isNonWorkingDay(LocalDate day) {
        return day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY
                || holidays.existsByDate(day);
    }

    // Exclude the edited request so its existing reservation is not counted twice.
    private List<CourseApplication> reservedApplications(User staff, int year, Integer excludedId) {
        return applications.findByApplicant_UserIdAndStatusIn(staff.getUserId(), RESERVED_STATUSES).stream()
                .filter(a -> a.getCourseStartDate() != null && a.getCourseStartDate().getYear() == year)
                .filter(a -> excludedId == null || !excludedId.equals(a.getCourseId())).toList();
    }

    // Only opposite AM/PM sessions on the same date can avoid an otherwise overlapping period.
    public boolean overlaps(CourseApplication first, CourseApplication second) {
        if (first.getCourseEndDate().isBefore(second.getCourseStartDate())
                || second.getCourseEndDate().isBefore(first.getCourseStartDate())) {
            return false;
        }
        boolean sameDay = first.getCourseStartDate().equals(first.getCourseEndDate())
                && second.getCourseStartDate().equals(second.getCourseEndDate());
        boolean opposite = "AM".equals(first.getHalfDayPeriod()) && "PM".equals(second.getHalfDayPeriod())
                || "PM".equals(first.getHalfDayPeriod()) && "AM".equals(second.getHalfDayPeriod());
        return !(sameDay && opposite);
    }

    // Preserve the record identity and review fields while replacing only course details.
    private void copyDetails(CourseApplication from, CourseApplication to) {
        to.setCatalogueCourse(from.getCatalogueCourse());
        to.setCatalogueBatch(from.getCatalogueBatch());
        to.setCourseTitle(from.getCourseTitle());
        to.setCourseCategory(from.getCourseCategory());
        to.setTrainingProvider(from.getTrainingProvider());
        to.setCourseStartDate(from.getCourseStartDate());
        to.setCourseEndDate(from.getCourseEndDate());
        to.setCourseFee(from.getCourseFee());
        to.setJustification(from.getJustification());
        to.setWorkDissemination(from.getWorkDissemination());
        to.setTrainingDays(from.getTrainingDays());
        to.setHalfDayPeriod(from.getHalfDayPeriod());
    }

    // Only pending applications can be edited or deleted.
    public void requirePending(CourseApplication course) {
        if (course.getStatus() != ApplicationStatus.APPLIED && course.getStatus() != ApplicationStatus.UPDATED) {
            throw new IllegalStateException("Only Applied or Updated applications can be changed.");
        }
    }

    // Check the old state before any lifecycle write.
    private void requireStatus(CourseApplication course, ApplicationStatus expected, String message) {
        if (course.getStatus() != expected) {
            throw new IllegalStateException(message);
        }
    }

    // Every state transition records when it happened without deleting the history.
    private void changeStatus(CourseApplication course, ApplicationStatus status) {
        course.setStatus(status);
        course.setUpdatedAt(LocalDateTime.now());
        applications.save(course);
    }

    // Match text limits to database columns so long inputs produce useful validation errors.
    private String requiredText(String value, String label, int limit) {
        if (value == null || value.isBlank() || value.trim().length() > limit) {
            throw new IllegalArgumentException(label + " is required and cannot exceed " + limit + " characters.");
        }
        return value.trim();
    }
}
