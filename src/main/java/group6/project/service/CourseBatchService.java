package group6.project.service;

import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import group6.project.model.*;
import group6.project.model.form.CatalogueBatchForm;
import group6.project.repo.*;

@Service
public class CourseBatchService {
    private final CourseBatchRepo batches;
    private final CourseDetailRepo courses;
    private final CourseApplicationRepo applications;
    private final CourseApplicationService policy;

    // Scheduled sessions share the employee application's date and category rules.
    public CourseBatchService(CourseBatchRepo batches, CourseDetailRepo courses,
            CourseApplicationRepo applications, CourseApplicationService policy) {
        this.batches = batches; this.courses = courses; this.applications = applications; this.policy = policy;
    }

    // Include archived sessions for Admin; employees see only bookable future sessions.
    public List<CourseBatch> getAllBatches() { return batches.findAll(); }

    // Return a clear 404 for unknown schedule links.
    public CourseBatch get(Long id) {
        return batches.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found."));
    }

    // Lock in the same course/batch order as submission, without loading a stale batch first.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CourseBatch save(Long id, CatalogueBatchForm form) {
        Integer courseId = id == null ? form.getCourseId() : batches.courseIdForBatch(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found."));
        CourseDetail course = courses.lockById(courseId).orElseThrow(() -> new IllegalArgumentException("Choose a course."));
        CourseBatch batch = id == null ? new CourseBatch() : batches.lockById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (id != null && !Objects.equals(form.getVersion(), batch.getVersion())) {
            throw new IllegalArgumentException("This session changed. Reload it before saving.");
        }
        if (!courseId.equals(form.getCourseId())) throw new IllegalArgumentException("Create a new session to use a different course.");
        if (course.getCourseCategory() == null || course.getCourseCategory().getKind() == null) {
            throw new IllegalArgumentException("Configure the course category first.");
        }
        boolean hasHistory = id != null && applications.existsByCatalogueBatch_BatchId(id);
        if (hasHistory && (!Objects.equals(batch.getCourseStartDate(), form.getStartDate())
                || !Objects.equals(batch.getCourseEndDate(), form.getEndDate())
                || !Objects.equals(normalize(batch.getHalfDayPeriod()), normalize(form.getHalfDayPeriod())))) {
            throw new IllegalArgumentException("Applications already use these dates. Archive this session and create a new one.");
        }
        if (!hasHistory) policy.validateSchedule(course.getCourseCategory().getKind(), form.getStartDate(),
                form.getEndDate(), form.getHalfDayPeriod(), true);
        long reserved = id == null ? 0 : applications.countByCatalogueBatch_BatchIdAndStatusIn(id, CourseApplicationService.RESERVED_STATUSES);
        if (form.getCapacity() == null || form.getCapacity() < 1 || form.getCapacity() > 100000 || form.getCapacity() < reserved) {
            throw new IllegalArgumentException("Capacity must be at least 1 and cannot be below existing reservations.");
        }
        batch.setCourseDetail(course); batch.setCourseStartDate(form.getStartDate()); batch.setCourseEndDate(form.getEndDate());
        batch.setHalfDayPeriod(normalize(form.getHalfDayPeriod())); batch.setCapacity(form.getCapacity()); batch.setActive(form.isActive());
        return batches.saveAndFlush(batch);
    }

    // Stop new bookings while preserving every application and its original schedule.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void archive(Long id) {
        Integer courseId = batches.courseIdForBatch(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        courses.lockById(courseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        batches.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).setActive(false);
    }

    // The edit form carries the version and editable schedule fields only.
    public CatalogueBatchForm form(CourseBatch batch) {
        CatalogueBatchForm form = new CatalogueBatchForm();
        form.setVersion(batch.getVersion()); form.setCourseId(batch.getCourseDetail().getCourseId());
        form.setStartDate(batch.getCourseStartDate()); form.setEndDate(batch.getCourseEndDate());
        form.setHalfDayPeriod(batch.getHalfDayPeriod()); form.setCapacity(batch.getCapacity()); form.setActive(batch.isActive());
        return form;
    }

    // Empty means a full working day; AM/PM are checked by the shared policy.
    private String normalize(String period) { return period == null || period.isBlank() ? null : period; }
}
