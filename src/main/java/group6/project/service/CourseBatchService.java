// Validates offered dates and capacity while preserving used schedules.
package group6.project.service;

import static org.springframework.http.HttpStatus.*;

import group6.project.form.CourseBatchForm;
import group6.project.model.*;
import group6.project.repo.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CourseBatchService {
    private final CourseBatchRepo batches;
    private final CourseDetailRepo courses;
    private final CourseApplicationRepo applications;
    private final ExcludedDaysRepo holidays;
    private final TrainingCalendarPolicyRepo calendar;
    private static final List<ApplicationStatus> SEAT_STATUSES =
            List.of(
                    ApplicationStatus.APPLIED,
                    ApplicationStatus.UPDATED,
                    ApplicationStatus.APPROVED,
                    ApplicationStatus.COMPLETED);

    public CourseBatchService(
            CourseBatchRepo batches,
            CourseDetailRepo courses,
            CourseApplicationRepo applications,
            ExcludedDaysRepo holidays,
            TrainingCalendarPolicyRepo calendar) {
        this.batches = batches;
        this.courses = courses;
        this.applications = applications;
        this.holidays = holidays;
        this.calendar = calendar;
    }

    public List<CourseBatch> forCourse(Integer courseId) {
        return batches.findByCourseDetail_CourseIdOrderByCourseStartDateAsc(courseId);
    }

    public CourseBatch get(Long id) {
        return batches.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Schedule not found."));
    }

    // Training days are calculated by the server, not accepted from the form.
    public CourseBatchForm form(Long id) {
        CourseBatch batch = get(id);
        CourseBatchForm form = new CourseBatchForm();
        form.setVersion(batch.getVersion());
        form.setCourseId(batch.getCourseDetail().getCourseId());
        form.setStartDate(batch.getCourseStartDate());
        form.setEndDate(batch.getCourseEndDate());
        form.setHalfDayPeriod(batch.getHalfDayPeriod());
        form.setCapacity(batch.getCapacity());
        form.setActive(batch.isActive());
        return form;
    }

    // Used schedules keep their dates; capacity cannot drop below reserved places.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CourseBatch save(Long id, CourseBatchForm form) {
        calendar.readCalendar().orElseThrow();
        CourseDetail course =
                courses.lockById(form.getCourseId())
                        .orElseThrow(
                                () -> new ResponseStatusException(BAD_REQUEST, "Choose a course."));
        CourseBatch batch =
                id == null
                        ? new CourseBatch()
                        : batches.lockById(id)
                                .orElseThrow(
                                        () ->
                                                new ResponseStatusException(
                                                        NOT_FOUND, "Schedule not found."));
        if (id != null && !Objects.equals(form.getVersion(), batch.getVersion())) {
            throw new ResponseStatusException(
                    CONFLICT, "This schedule changed. Reload it before editing.");
        }
        String halfDay =
                form.getHalfDayPeriod() == null || form.getHalfDayPeriod().isBlank()
                        ? null
                        : form.getHalfDayPeriod();
        boolean used = id != null && applications.existsByCatalogueBatch_BatchId(id);
        if (used
                && (!Objects.equals(form.getCourseId(), batch.getCourseDetail().getCourseId())
                        || !Objects.equals(form.getStartDate(), batch.getCourseStartDate())
                        || !Objects.equals(form.getEndDate(), batch.getCourseEndDate())
                        || !Objects.equals(halfDay, batch.getHalfDayPeriod()))) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "This schedule has applications. Create a new schedule to change its dates.");
        }
        if (id != null
                && applications.countByCatalogueBatch_BatchIdAndStatusIn(id, SEAT_STATUSES)
                        > form.getCapacity()) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "Capacity cannot be lower than the reserved places.");
        }
        Set<LocalDate> excluded =
                holidays.findAll().stream().map(ExcludedDays::getDate).collect(Collectors.toSet());
        double days;
        try {
            days =
                    TrainingDayCalculator.count(
                            course.getCourseCategory().getKind(),
                            form.getStartDate(),
                            form.getEndDate(),
                            halfDay,
                            excluded,
                            !used);
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(BAD_REQUEST, error.getMessage());
        }
        batch.setCourseDetail(course);
        batch.setCourseStartDate(form.getStartDate());
        batch.setCourseEndDate(form.getEndDate());
        batch.setHalfDayPeriod(halfDay);
        batch.setTrainingDays(days);
        batch.setCapacity(form.getCapacity());
        batch.setActive(form.isActive());
        return batches.save(batch);
    }

    // Removing a used schedule archives it without deleting applications.
    @Transactional
    public void remove(Long id, Long version) {
        CourseBatch batch =
                batches.lockById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                NOT_FOUND, "Schedule not found."));
        if (!Objects.equals(version, batch.getVersion()))
            throw new ResponseStatusException(CONFLICT, "Reload this schedule.");
        if (applications.existsByCatalogueBatch_BatchId(id)) batch.setActive(false);
        else batches.delete(batch);
    }
}
