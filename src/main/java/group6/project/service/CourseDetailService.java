package group6.project.service;

import group6.project.form.CourseForm;
import group6.project.model.*;
import group6.project.repo.*;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
@Transactional(readOnly = true)
public class CourseDetailService {
    private final CourseDetailRepo courses;
    private final CourseCategoryRepository categories;
    private final CourseProviderRepo providers;
    private final CourseBatchRepo batches;
    private final CourseApplicationRepo applications;

    // Course maintenance does not change snapshots in employees' applications.
    public CourseDetailService(CourseDetailRepo courses, CourseCategoryRepository categories,
            CourseProviderRepo providers, CourseBatchRepo batches, CourseApplicationRepo applications) {
        this.courses = courses;
        this.categories = categories;
        this.providers = providers;
        this.batches = batches;
        this.applications = applications;
    }

    // Keep the Admin list in a predictable order, including archived courses.
    public List<CourseDetail> all() {
        return courses.findAll().stream().sorted(Comparator.comparing(CourseDetail::getTitle,
                String.CASE_INSENSITIVE_ORDER)).toList();
    }

    // Missing links should show 404 rather than an empty edit form.
    public CourseDetail get(Integer id) {
        return courses.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Course not found."));
    }

    // Only the editable course fields are sent to the browser.
    public CourseForm form(Integer id) {
        CourseDetail course = get(id);
        CourseForm form = new CourseForm();
        form.setVersion(course.getVersion());
        form.setTitle(course.getTitle());
        form.setCourseDescription(course.getCourseDescription());
        form.setCourseFee(course.getCourseFee());
        form.setCategoryId(course.getCourseCategory().getCategoryId());
        form.setProviderId(course.getProvider().getProviderId());
        form.setCustomDatesAllowed(course.isCustomDatesAllowed());
        form.setActive(course.isActive());
        return form;
    }

    // Check selected records on the server; a posted ID is not proof it is valid.
    @Transactional
    public CourseDetail save(Integer id, CourseForm form) {
        CourseDetail course = id == null ? new CourseDetail() : courses.lockById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Course not found."));
        if (id != null && !Objects.equals(form.getVersion(), course.getVersion())) {
            throw new ResponseStatusException(CONFLICT, "This course changed. Reload it before editing.");
        }
        CourseCategory category = categories.findById(form.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Choose a course category."));
        CourseProvider provider = providers.findById(form.getProviderId())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Choose a training provider."));
        if (form.isActive() && !provider.isActive()) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose an active training provider before publishing.");
        }
        if (id != null && course.getCourseCategory().getKind() != category.getKind()
                && batches.existsByCourseDetail_CourseId(id)) {
            throw new ResponseStatusException(BAD_REQUEST, "A course with schedules must keep its category type. Create another course instead.");
        }
        course.setTitle(form.getTitle().trim());
        course.setCourseDescription(form.getCourseDescription());
        course.setCourseCategory(category);
        course.setProvider(provider);
        course.setCourseFee(category.getKind() == CourseCategoryType.INTERNAL_TRAINING ? BigDecimal.ZERO : form.getCourseFee());
        course.setCustomDatesAllowed(form.isCustomDatesAllowed());
        course.setActive(form.isActive());
        return courses.save(course);
    }

    // Referenced courses are archived so schedules and application history remain readable.
    @Transactional
    public void remove(Integer id, Long version) {
        CourseDetail course = courses.lockById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Course not found."));
        if (!Objects.equals(version, course.getVersion())) {
            throw new ResponseStatusException(CONFLICT, "This course changed. Reload the list.");
        }
        if (batches.existsByCourseDetail_CourseId(id) || applications.existsByCatalogueCourse_CourseId(id)) {
            course.setActive(false);
        } else {
            courses.delete(course);
        }
    }
}
