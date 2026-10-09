package group6.project.service;

import group6.project.model.*;
import group6.project.model.form.CatalogueApplicationForm;
import group6.project.repo.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class CourseCatalogueService {
    private final CourseDetailRepo courses;
    private final CourseBatchRepo batches;
    private final CourseApplicationRepo applications;
    private final CourseApplicationService policy;

    // Catalogue choices use the same application policy as courses outside the catalogue.
    public CourseCatalogueService(
            CourseDetailRepo courses,
            CourseBatchRepo batches,
            CourseApplicationRepo applications,
            CourseApplicationService policy) {
        this.courses = courses;
        this.batches = batches;
        this.applications = applications;
        this.policy = policy;
    }

    // Search only published offers with a configured mandatory category.
    @Transactional(readOnly = true)
    public List<CourseDetail> search(String query, CourseCategoryType category, String provider) {
        String term = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        return courses.findAll().stream()
                .filter(this::available)
                .filter(c -> category == null || c.getCourseCategory().getKind() == category)
                .filter(
                        c ->
                                provider == null
                                        || provider.isBlank()
                                        || provider.equals(c.getTrainingProvider()))
                .filter(
                        c ->
                                (c.getTitle() + " " + c.getTrainingProvider())
                                        .toLowerCase(java.util.Locale.ROOT)
                                        .contains(term))
                .sorted(Comparator.comparing(CourseDetail::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    // Archived or incomplete offers cannot be used to create another application.
    public CourseDetail offer(Integer id) {
        return courses.findById(id)
                .filter(this::available)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Course not found."));
    }

    // Show future schedules and remaining seats without exposing other applicants.
    @Transactional(readOnly = true)
    public List<BatchChoice> schedules(Integer courseId) {
        return batches.findByCourseDetail_CourseIdOrderByCourseStartDateAsc(courseId).stream()
                .filter(
                        b ->
                                b.isActive()
                                        && b.getCourseEndDate() != null
                                        && b.getCourseStartDate() != null
                                        && b.getCourseStartDate().isAfter(LocalDate.now()))
                .filter(b -> b.getCapacity() != null && b.getCapacity() > 0)
                .map(
                        b ->
                                new BatchChoice(
                                        b.getBatchId(),
                                        b.getVersion(),
                                        b.getCourseStartDate(),
                                        b.getCourseEndDate(),
                                        b.getHalfDayPeriod(),
                                        Math.max(
                                                0,
                                                b.getCapacity()
                                                        - applications
                                                                .countByCatalogueBatch_BatchIdAndStatusIn(
                                                                        b.getBatchId(),
                                                                        CourseApplicationService
                                                                                .RESERVED_STATUSES))))
                .toList();
    }

    // A preview calculates dates and allowance without reserving a place or saving a request.
    @Transactional(readOnly = true)
    public CourseApplicationService.Summary preview(
            Integer id, CatalogueApplicationForm form, Staff staff) {
        return policy.summary(prepare(offer(id), form, false), staff, null);
    }

    // Lock employee, course and batch in that order; committed reads see reservations after a lock
    // wait.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CourseApplication submit(Integer id, CatalogueApplicationForm form, Staff staff) {
        policy.prepareReviewer(staff, form.getApprovalManagerId());
        policy.lockEmployee(staff);
        CourseDetail course =
                courses.lockById(id)
                        .filter(this::available)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Course not found."));
        return policy.create(prepare(course, form, true), staff);
    }

    // Pending catalogue edits keep the original price/provider/category; scheduled dates are also
    // fixed.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CourseApplication edit(Integer id, CatalogueApplicationForm form, Staff staff) {
        policy.lockEmployee(staff);
        CourseApplication existing = policy.getOwned(id, staff);
        policy.requirePending(existing);
        if (existing.getCatalogueCourse() == null)
            throw new IllegalArgumentException("Use the other-course form for this application.");
        if (form.getApplicationVersion() == null
                || !form.getApplicationVersion().equals(existing.getVersion())) {
            throw new IllegalArgumentException(
                    "This application changed. Reload it before editing.");
        }
        CourseApplication update = new CourseApplication();
        update.setVersion(form.getApplicationVersion());
        update.setJustification(form.getJustification());
        update.setWorkDissemination(form.getWorkDissemination());
        update.setCourseStartDate(form.getStartDate());
        update.setCourseEndDate(form.getEndDate());
        update.setHalfDayPeriod(form.getHalfDayPeriod());
        return policy.update(id, update, staff);
    }

    // Server-owned catalogue metadata and version checks protect against modified or stale forms.
    private CourseApplication prepare(
            CourseDetail course, CatalogueApplicationForm form, boolean reserve) {
        if (form.getCourseVersion() == null
                || !form.getCourseVersion().equals(course.getVersion())) {
            throw new IllegalArgumentException(
                    "The course offer changed. Review the latest details before applying.");
        }
        CourseApplication request = new CourseApplication();
        request.setCatalogueCourse(course);
        request.setApprovalManagerId(form.getApprovalManagerId());
        request.setCourseTitle(course.getTitle());
        request.setCourseCategory(course.getCourseCategory().getKind());
        request.setTrainingProvider(course.getTrainingProvider());
        request.setCourseFee(course.getCourseFee());
        request.setJustification(form.getJustification());
        request.setWorkDissemination(form.getWorkDissemination());
        if (form.getScheduledBatch() != null && !form.getScheduledBatch().isBlank()) {
            long[] choice = batchKey(form.getScheduledBatch());
            CourseBatch batch =
                    (reserve ? batches.lockById(choice[0]) : batches.findById(choice[0]))
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Choose an available scheduled session."));
            if (!batch.isActive()
                    || batch.getCourseDetail() == null
                    || !course.getCourseId().equals(batch.getCourseDetail().getCourseId())
                    || batch.getVersion() == null
                    || batch.getVersion() != choice[1]) {
                throw new IllegalArgumentException(
                        "The scheduled session changed. Choose a current session.");
            }
            if (batch.getCapacity() == null
                    || batch.getCapacity() <= 0
                    || applications.countByCatalogueBatch_BatchIdAndStatusIn(
                                    batch.getBatchId(), CourseApplicationService.RESERVED_STATUSES)
                            >= batch.getCapacity()) {
                throw new IllegalArgumentException(
                        "This session has no places available. Choose another date.");
            }
            request.setCatalogueBatch(batch);
            request.setCourseStartDate(batch.getCourseStartDate());
            request.setCourseEndDate(batch.getCourseEndDate());
            request.setHalfDayPeriod(batch.getHalfDayPeriod());
        } else {
            if (!course.isCustomDatesAllowed())
                throw new IllegalArgumentException("Choose a scheduled session.");
            request.setCourseStartDate(form.getStartDate());
            request.setCourseEndDate(form.getEndDate());
            request.setHalfDayPeriod(form.getHalfDayPeriod());
        }
        if (request.getCourseStartDate() == null || request.getCourseEndDate() == null) {
            throw new IllegalArgumentException(
                    "Choose a scheduled session or enter both custom dates.");
        }
        return request;
    }

    // Hidden batch versions avoid submitting a different schedule from the one shown on the form.
    private long[] batchKey(String value) {
        try {
            String[] parts = value.split(":", -1);
            if (parts.length != 2) throw new NumberFormatException();
            return new long[] {Long.parseLong(parts[0]), Long.parseLong(parts[1])};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Choose a current scheduled session.");
        }
    }

    // Incomplete legacy offers stay in Admin's list until their missing data is supplied.
    private boolean available(CourseDetail course) {
        return course.isActive()
                && course.getCourseFee() != null
                && course.getCourseCategory() != null
                && course.getCourseCategory().getKind() != null
                && course.getTitle() != null
                && !course.getTitle().isBlank()
                && course.getTrainingProvider() != null
                && !course.getTrainingProvider().isBlank();
    }

    public record BatchChoice(
            Long id, Long version, LocalDate start, LocalDate end, String halfDay, long places) {
        // Keep implementation IDs/versions out of the readable schedule label.
        public String label() {
            DateTimeFormatter format =
                    DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH);
            return start.format(format)
                    + (start.equals(end) ? "" : " – " + end.format(format))
                    + (halfDay == null || halfDay.isBlank() ? "" : " · " + halfDay)
                    + " · "
                    + places
                    + " places left";
        }
    }
}
