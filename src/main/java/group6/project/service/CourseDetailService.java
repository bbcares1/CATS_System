package group6.project.service;

import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import group6.project.model.*;
import group6.project.model.form.CatalogueCourseForm;
import group6.project.repo.*;

@Service
public class CourseDetailService {
    private final CourseDetailRepo courses;
    private final CourseCategoryRepository categories;
    private final CourseBatchRepo batches;
    private final CourseApplicationService policy;

    // Admin maintains offers; applications keep a separate snapshot of their agreed details.
    public CourseDetailService(CourseDetailRepo courses, CourseCategoryRepository categories,
            CourseBatchRepo batches, CourseApplicationService policy) {
        this.courses = courses; this.categories = categories; this.batches = batches; this.policy = policy;
    }

    // Archived offers stay visible to Admin so their history and schedules are retained.
    public List<CourseDetail> all() {
        return courses.findAll().stream().sorted(Comparator.comparing(CourseDetail::getTitle,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))).toList();
    }

    // A missing record produces a normal 404 rather than an empty edit form.
    public CourseDetail get(Integer id) {
        return courses.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));
    }

    // Only the three assignment categories define application rules; their labels can be maintained.
    public List<CourseCategory> categories() {
        return categories.findAll().stream().filter(c -> c.getKind() != null).toList();
    }

    // Bind editable fields only, and reject edits made against a stale offer.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CourseDetail save(Integer id, CatalogueCourseForm form) {
        CourseDetail course = id == null ? new CourseDetail() : courses.lockById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));
        if (id != null && !java.util.Objects.equals(form.getVersion(), course.getVersion())) {
            throw new IllegalArgumentException("This course changed. Reload it before saving.");
        }
        CourseCategory category = categories.findById(form.getCategoryId())
                .filter(c -> c.getKind() != null).orElseThrow(() -> new IllegalArgumentException("Choose a supported category."));
        course.setTitle(text(form.getTitle(), "Course title", 255));
        course.setTrainingProvider(text(form.getTrainingProvider(), "Training provider", 255));
        course.setCourseCategory(category);
        if (form.getFee() == null || form.getFee().signum() < 0 || form.getFee().scale() > 2
                || form.getFee().compareTo(new java.math.BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException("Enter a non-negative fee with at most two decimal places.");
        }
        course.setCourseFee(category.getKind() == CourseCategoryType.INTERNAL_TRAINING ? java.math.BigDecimal.ZERO : form.getFee());
        if (form.getDescription() != null && form.getDescription().length() > 2000) {
            throw new IllegalArgumentException("Description cannot exceed 2000 characters.");
        }
        course.setCourseDescription(form.getDescription());
        course.setCustomDatesAllowed(form.isCustomDatesAllowed()); course.setActive(form.isActive());
        if (id != null) {
            for (CourseBatch batch : batches.findByCourseDetail_CourseIdOrderByCourseStartDateAsc(id)) {
                if (batch.isActive() && batch.getCourseEndDate().isAfter(java.time.LocalDate.now())) {
                    policy.validateSchedule(category.getKind(), batch.getCourseStartDate(), batch.getCourseEndDate(), batch.getHalfDayPeriod(), false);
                }
            }
        }
        return courses.saveAndFlush(course);
    }

    // Archive instead of deleting a course referenced by applications or scheduled sessions.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void archive(Integer id) {
        CourseDetail course = courses.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        course.setActive(false);
    }

    // Copy a saved offer into the form without exposing identity or relationships for binding.
    public CatalogueCourseForm form(CourseDetail course) {
        CatalogueCourseForm form = new CatalogueCourseForm();
        form.setVersion(course.getVersion()); form.setTitle(course.getTitle()); form.setTrainingProvider(course.getTrainingProvider());
        form.setCategoryId(course.getCourseCategory() == null ? null : course.getCourseCategory().getCategoryId());
        form.setFee(course.getCourseFee()); form.setDescription(course.getCourseDescription());
        form.setActive(course.isActive()); form.setCustomDatesAllowed(course.isCustomDatesAllowed());
        return form;
    }

    // Keep required catalogue text inside the corresponding database column limits.
    private String text(String value, String label, int max) {
        if (value == null || value.isBlank() || value.trim().length() > max) {
            throw new IllegalArgumentException(label + " is required and cannot exceed " + max + " characters.");
        }
        return value.trim();
    }
}
