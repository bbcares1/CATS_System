package group6.project.service;

import static org.springframework.http.HttpStatus.*;

import group6.project.form.CatalogueApplicationForm;
import group6.project.form.CourseApplicationForm;
import group6.project.form.DecisionForm;
import group6.project.model.*;
import group6.project.repo.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class CourseApplicationService {
    private static final List<ApplicationStatus> ACTIVE =
            List.of(
                    ApplicationStatus.APPLIED,
                    ApplicationStatus.UPDATED,
                    ApplicationStatus.APPROVED);
    private static final List<ApplicationStatus> SEATS =
            List.of(
                    ApplicationStatus.APPLIED,
                    ApplicationStatus.UPDATED,
                    ApplicationStatus.APPROVED,
                    ApplicationStatus.COMPLETED);
    private final CourseApplicationRepo applications;
    private final TrainingEntitlementService entitlements;
    private final UserRepo users;
    private final ExcludedDaysRepo holidays;
    private final TrainingCalendarPolicyRepo calendar;
    private final CourseDetailRepo courses;
    private final CourseBatchRepo batches;
    private final ApprovalRoutingService routing;

    // All application changes, including Manager decisions, are saved here.
    public CourseApplicationService(
            CourseApplicationRepo applications,
            TrainingEntitlementService entitlements,
            UserRepo users,
            ExcludedDaysRepo holidays,
            TrainingCalendarPolicyRepo calendar,
            CourseDetailRepo courses,
            CourseBatchRepo batches,
            ApprovalRoutingService routing) {
        this.applications = applications;
        this.entitlements = entitlements;
        this.users = users;
        this.holidays = holidays;
        this.calendar = calendar;
        this.courses = courses;
        this.batches = batches;
        this.routing = routing;
    }

    // Personal history includes every status, including deleted and cancelled requests.
    public List<CourseApplication> findForStaffAndYear(User employee, int year) {
        entitlements.validateYear(year);
        return applications
                .findByApplicant_UserIdAndCourseStartDateBetweenOrderByCourseStartDateAsc(
                        employee.getUserId(), LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    // A guessed ID must not reveal another employee's course.
    public CourseApplication getOwned(Integer id, User employee) {
        CourseApplication application =
                applications
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                NOT_FOUND, "Application not found."));
        requireOwner(application, employee);
        return application;
    }

    // External courses need their own details; identity and status never come from the form.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CourseApplication createOther(CourseApplicationForm form, User actor) {
        User employee = lockEmployee(actor.getUserId(), actor.getUserId(), form.getReviewerId());
        calendar.readCalendar().orElseThrow();
        CourseApplication application = new CourseApplication();
        copyOther(form, application);
        application.setApplicant(employee);
        application.setApprovalManager(routing.resolveReviewer(employee, form.getReviewerId()));
        validate(application, null);
        application.setSubmittedAt(LocalDateTime.now());
        application.setUpdatedAt(application.getSubmittedAt());
        return applications.save(application);
    }

    // The offer supplies the price and category; a batch supplies its fixed dates.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CourseApplication createFromCatalogue(
            Integer courseId, CatalogueApplicationForm form, User actor) {
        User employee = lockEmployee(actor.getUserId(), actor.getUserId(), form.getReviewerId());
        calendar.readCalendar().orElseThrow();
        CourseDetail offer =
                courses.lockById(courseId)
                        .orElseThrow(
                                () -> new ResponseStatusException(NOT_FOUND, "Course not found."));
        checkVersion(form.getCourseVersion(), offer.getVersion());
        if (!offer.isActive() || !offer.getProvider().isActive()) {
            throw new ResponseStatusException(BAD_REQUEST, "This course is no longer available.");
        }
        CourseApplication application = new CourseApplication();
        application.setApplicant(employee);
        application.setApprovalManager(routing.resolveReviewer(employee, form.getReviewerId()));
        application.setCatalogueCourse(offer);
        application.setCourseTitle(offer.getTitle());
        application.setCourseCategory(offer.getCourseCategory().getKind());
        application.setTrainingProvider(offer.getProvider().getName());
        application.setCourseFee(offer.getCourseFee());
        copyDatesAndReason(form, application);
        if (form.getBatchId() != null) {
            CourseBatch batch =
                    batches.lockById(form.getBatchId())
                            .orElseThrow(
                                    () ->
                                            new ResponseStatusException(
                                                    BAD_REQUEST, "Choose an available schedule."));
            checkVersion(form.getBatchVersion(), batch.getVersion());
            if (!batch.isActive() || !batch.getCourseDetail().getCourseId().equals(courseId)) {
                throw new ResponseStatusException(
                        BAD_REQUEST, "Choose a schedule for this course.");
            }
            if (applications.countByCatalogueBatch_BatchIdAndStatusIn(batch.getBatchId(), SEATS)
                    >= batch.getCapacity()) {
                throw new ResponseStatusException(
                        BAD_REQUEST, "This schedule is full. Choose another schedule.");
            }
            application.setCatalogueBatch(batch);
            application.setCourseStartDate(batch.getCourseStartDate());
            application.setCourseEndDate(batch.getCourseEndDate());
            application.setHalfDayPeriod(batch.getHalfDayPeriod());
        } else if (!offer.isCustomDatesAllowed()) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose one of the listed schedules.");
        }
        validate(application, null);
        application.setSubmittedAt(LocalDateTime.now());
        application.setUpdatedAt(application.getSubmittedAt());
        return applications.save(application);
    }

    // Only pending requests can change, and every edit must still fit the annual allowance.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CourseApplication updateOther(Integer id, CourseApplicationForm form, User actor) {
        CourseApplication application = lockApplication(id, actor);
        requireOwner(application, actor);
        checkVersion(form.getVersion(), application.getVersion());
        requirePending(application);
        if (application.getCatalogueCourse() != null)
            throw new ResponseStatusException(BAD_REQUEST, "Use the catalogue application form.");
        copyOther(form, application);
        validate(application, id);
        application.setStatus(ApplicationStatus.UPDATED);
        application.setUpdatedAt(LocalDateTime.now());
        return application;
    }

    // Keep the original offer snapshot and fixed schedule, even after Admin edits the catalogue.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CourseApplication updateCatalogue(
            Integer id, CatalogueApplicationForm form, User actor) {
        CourseApplication application = lockApplication(id, actor);
        requireOwner(application, actor);
        checkVersion(form.getVersion(), application.getVersion());
        requirePending(application);
        if (application.getCatalogueCourse() == null)
            throw new ResponseStatusException(BAD_REQUEST, "Use the other-course form.");
        if (application.getCatalogueBatch() == null) copyDatesAndReason(form, application);
        else {
            application.setJustification(form.getJustification());
            application.setWorkDissemination(form.getWorkDissemination());
        }
        validate(application, id);
        application.setStatus(ApplicationStatus.UPDATED);
        application.setUpdatedAt(LocalDateTime.now());
        return application;
    }

    // Both decisions require a reason; rejecting an expired request must remain possible.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CourseApplication decide(Integer id, DecisionForm form, User actor) {
        CourseApplication application = lockApplication(id, actor);
        User manager = users.findById(actor.getUserId()).orElseThrow();
        if (!(manager instanceof Manager)
                || !manager.isActive()
                || application.getApplicant().getUserId().equals(actor.getUserId())
                || application.getApprovalManager() == null
                || !application.getApprovalManager().getUserId().equals(actor.getUserId())) {
            throw new ResponseStatusException(
                    FORBIDDEN, "Only the assigned Manager can decide this application.");
        }
        checkVersion(form.getVersion(), application.getVersion());
        requirePending(application);
        String reason = required(form.getReason(), "Decision reason", 2000);
        if (form.getApproved() == null)
            throw new ResponseStatusException(BAD_REQUEST, "Choose approve or reject.");
        if (form.getApproved()) validate(application, id);
        application.setStatus(
                form.getApproved() ? ApplicationStatus.APPROVED : ApplicationStatus.REJECTED);
        application.setDecisionReason(reason);
        application.setReviewer(manager);
        application.setReviewedAt(LocalDateTime.now());
        application.setUpdatedAt(application.getReviewedAt());
        return application;
    }

    // Deletion keeps the history while releasing the pending reservation.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(Integer id, Long version, User actor) {
        CourseApplication application = lockApplication(id, actor);
        requireOwner(application, actor);
        checkVersion(version, application.getVersion());
        requirePending(application);
        application.setStatus(ApplicationStatus.DELETED);
        application.setUpdatedAt(LocalDateTime.now());
    }

    // An approved course can be cancelled without removing the approval history.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancel(Integer id, Long version, User actor) {
        CourseApplication application = lockApplication(id, actor);
        requireOwner(application, actor);
        checkVersion(version, application.getVersion());
        if (application.getStatus() != ApplicationStatus.APPROVED) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "Only approved applications can be cancelled.");
        }
        application.setStatus(ApplicationStatus.CANCELLED);
        application.setUpdatedAt(LocalDateTime.now());
    }

    // Completion needs learning comments and does not release the year's used allowance.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void complete(Integer id, Long version, String comments, User actor) {
        CourseApplication application = lockApplication(id, actor);
        requireOwner(application, actor);
        checkVersion(version, application.getVersion());
        if (application.getStatus() != ApplicationStatus.APPROVED
                || !application.getCourseEndDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "Complete an approved course after its end date.");
        }
        application.setExperienceComments(required(comments, "Experience comments", 2000));
        application.setStatus(ApplicationStatus.COMPLETED);
        application.setUpdatedAt(LocalDateTime.now());
    }

    // A version from an old page must never silently overwrite a more recent change.
    private void checkVersion(Long supplied, Long saved) {
        if (supplied == null || !supplied.equals(saved)) {
            throw new ResponseStatusException(
                    CONFLICT, "This record changed. Reload the page and try again.");
        }
    }

    // Account locks come first, followed by the calendar and then the application.
    private CourseApplication lockApplication(Integer id, User actor) {
        Integer applicantId =
                applications
                        .applicantId(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                NOT_FOUND, "Application not found."));
        lockEmployee(applicantId, actor.getUserId(), null);
        calendar.readCalendar().orElseThrow();
        return applications
                .lockById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(NOT_FOUND, "Application not found."));
    }

    // Lock the few accounts involved in ID order, so concurrent requests share the same allowance.
    private User lockEmployee(Integer employeeId, Integer actorId, Integer selectedReviewer) {
        Integer reportingId = users.reportingManagerId(employeeId).orElse(null);
        Set<Integer> ids = new TreeSet<>();
        ids.add(employeeId);
        ids.add(actorId);
        if (reportingId != null) ids.add(reportingId);
        if (selectedReviewer != null) ids.add(selectedReviewer);
        List<User> locked = users.lockParticipants(new ArrayList<>(ids));
        User employee =
                locked.stream()
                        .filter(user -> employeeId.equals(user.getUserId()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                NOT_FOUND, "Employee not found."));
        User actor =
                locked.stream()
                        .filter(user -> actorId.equals(user.getUserId()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                FORBIDDEN, "Account not available."));
        if (!actor.isActive())
            throw new ResponseStatusException(FORBIDDEN, "Account not available.");
        Integer currentReportingId =
                employee.getManager() == null ? null : employee.getManager().getUserId();
        if (!Objects.equals(reportingId, currentReportingId)) {
            throw new ResponseStatusException(
                    CONFLICT, "Your reporting manager changed. Reload the page.");
        }
        return employee;
    }

    // Allowance, overlap and dates are checked again on approval, not just when the form opens.
    private void validate(CourseApplication application, Integer excludedId) {
        if (!(application.getApplicant() instanceof Staff)
                || !application.getApplicant().isActive()) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "The applicant must be an active employee.");
        }
        application.setCourseTitle(required(application.getCourseTitle(), "Course title", 255));
        application.setTrainingProvider(
                required(application.getTrainingProvider(), "Provider", 255));
        application.setJustification(
                required(application.getJustification(), "Justification", 2000));
        if (application.getWorkDissemination() != null
                && application.getWorkDissemination().length() > 2000) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "Work dissemination must be at most 2000 characters.");
        }
        BigDecimal fee = application.getCourseFee();
        if (fee == null
                || fee.signum() < 0
                || fee.scale() > 2
                || fee.precision() - fee.scale() > 10) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "Use a non-negative fee with up to two decimal places.");
        }
        if (application.getCourseCategory() == CourseCategoryType.INTERNAL_TRAINING)
            application.setCourseFee(BigDecimal.ZERO);
        Set<LocalDate> excluded = new HashSet<>();
        for (ExcludedDays holiday : holidays.findAll()) excluded.add(holiday.getDate());
        try {
            application.setTrainingDays(
                    TrainingDayCalculator.count(
                            application.getCourseCategory(),
                            application.getCourseStartDate(),
                            application.getCourseEndDate(),
                            application.getHalfDayPeriod(),
                            excluded,
                            true));
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(BAD_REQUEST, error.getMessage());
        }
        var annual =
                entitlements.summary(
                        application.getApplicant(),
                        application.getCourseStartDate().getYear(),
                        excludedId);
        if (application.getTrainingDays() > annual.remainingDays()) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "Training days exceed the remaining annual allowance.");
        }
        if (application.getCourseFee().compareTo(annual.remainingBudget()) > 0) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "Course fee exceeds the remaining annual training budget.");
        }
        for (CourseApplication other :
                applications.findByApplicant_UserIdAndStatusIn(
                        application.getApplicant().getUserId(), ACTIVE)) {
            if (!Objects.equals(other.getCourseId(), excludedId) && overlaps(application, other)) {
                throw new ResponseStatusException(
                        BAD_REQUEST, "The course overlaps another active application.");
            }
        }
    }

    // Morning and afternoon requests on the same date do not overlap each other.
    public static boolean overlaps(CourseApplication first, CourseApplication second) {
        if (first.getCourseEndDate().isBefore(second.getCourseStartDate())
                || second.getCourseEndDate().isBefore(first.getCourseStartDate())) return false;
        boolean opposite =
                "AM".equals(first.getHalfDayPeriod()) && "PM".equals(second.getHalfDayPeriod())
                        || "PM".equals(first.getHalfDayPeriod())
                                && "AM".equals(second.getHalfDayPeriod());
        return !(first.getCourseStartDate().equals(first.getCourseEndDate())
                && second.getCourseStartDate().equals(second.getCourseEndDate())
                && opposite);
    }

    // Keep ownership failures indistinguishable from unknown application IDs.
    private void requireOwner(CourseApplication application, User employee) {
        if (!application.getApplicant().getUserId().equals(employee.getUserId())) {
            throw new ResponseStatusException(NOT_FOUND, "Application not found.");
        }
    }

    // Editing or deciding a finished request would overwrite its history.
    public void requirePending(CourseApplication application) {
        if (application.getStatus() != ApplicationStatus.APPLIED
                && application.getStatus() != ApplicationStatus.UPDATED) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "Only Applied or Updated applications can be changed.");
        }
    }

    // Enforce the same text limits even when a service is called outside an HTML form.
    private String required(String value, String label, int max) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    label + " is required and must be at most " + max + " characters.");
        }
        return value.trim();
    }

    // Copy only editable details; never copy status, applicant or approval fields.
    private void copyOther(CourseApplicationForm form, CourseApplication application) {
        application.setCourseTitle(form.getCourseTitle());
        application.setCourseCategory(form.getCourseCategory());
        application.setTrainingProvider(form.getTrainingProvider());
        application.setCourseStartDate(form.getCourseStartDate());
        application.setCourseEndDate(form.getCourseEndDate());
        application.setCourseFee(form.getCourseFee());
        application.setHalfDayPeriod(form.getHalfDayPeriod());
        application.setJustification(form.getJustification());
        application.setWorkDissemination(form.getWorkDissemination());
    }

    // This is used only for a custom-date offer; fixed dates are loaded from its schedule.
    private void copyDatesAndReason(CatalogueApplicationForm form, CourseApplication application) {
        application.setCourseStartDate(form.getCourseStartDate());
        application.setCourseEndDate(form.getCourseEndDate());
        application.setHalfDayPeriod(form.getHalfDayPeriod());
        application.setJustification(form.getJustification());
        application.setWorkDissemination(form.getWorkDissemination());
    }
}
