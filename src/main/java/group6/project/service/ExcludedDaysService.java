package group6.project.service;

import group6.project.model.*;
import group6.project.model.form.HolidayForm;
import group6.project.repo.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@Service
public class ExcludedDaysService {
    private final ExcludedDaysRepo holidays;
    private final TrainingCalendarPolicyRepo calendar;
    private final CourseApplicationRepo applications;
    private final CourseBatchRepo batches;

    // Holiday maintenance shares the schedule guard used by application/batch validation.
    public ExcludedDaysService(
            ExcludedDaysRepo holidays,
            TrainingCalendarPolicyRepo calendar,
            CourseApplicationRepo applications,
            CourseBatchRepo batches) {
        this.holidays = holidays;
        this.calendar = calendar;
        this.applications = applications;
        this.batches = batches;
    }

    // Public holidays and weekends are excluded by the same calendar rules.
    public boolean isExcludedDay(LocalDate date) {
        return holidays.existsByDate(date);
    }

    // Admin and the month view display holidays in date order.
    public List<ExcludedDays> getAllExcludedDays() {
        return holidays.findAll().stream()
                .sorted(Comparator.comparing(ExcludedDays::getDate))
                .toList();
    }

    // Invalid edit links are a normal 404.
    public ExcludedDays getExcludedDayById(Integer id) {
        return holidays.findById(id)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Holiday not found."));
    }

    // A DTO exposes only the date, label and current version.
    public HolidayForm form(Integer id) {
        HolidayForm form = new HolidayForm();
        if (id == null) return form;
        var day = getExcludedDayById(id);
        form.setDate(day.getDate());
        form.setDescription(day.getDescription());
        form.setVersion(day.getVersion());
        return form;
    }

    // Never silently change the duration of an active request or published future session.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void save(Integer id, HolidayForm form) {
        calendar.editCalendar().orElseThrow();
        if (form.getDate() == null
                || form.getDate().getYear() < 2000
                || form.getDate().getYear() > 2100
                || form.getDescription() == null
                || form.getDescription().isBlank()
                || form.getDescription().trim().length() > 255) {
            throw new IllegalArgumentException(
                    "Enter a holiday date between 2000 and 2100 and a description of up to 255"
                        + " characters.");
        }
        if (holidays.existsByDateAndIdNot(form.getDate(), id == null ? -1 : id))
            throw new IllegalArgumentException("This holiday date already exists.");
        ExcludedDays day = id == null ? new ExcludedDays() : getExcludedDayById(id);
        if (id != null && !Objects.equals(day.getVersion(), form.getVersion()))
            throw new IllegalArgumentException("This holiday changed. Reload it before saving.");
        if (!Objects.equals(day.getDate(), form.getDate())) {
            if (day.getDate() != null) requireUnusedDate(day.getDate());
            requireUnusedDate(form.getDate());
        }
        day.setDate(form.getDate());
        day.setDescription(form.getDescription().trim());
        holidays.saveAndFlush(day);
    }

    // Deleting a holiday needs its displayed version and the same active-schedule check.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(Integer id, Long version) {
        calendar.editCalendar().orElseThrow();
        ExcludedDays day = getExcludedDayById(id);
        if (!Objects.equals(day.getVersion(), version))
            throw new IllegalArgumentException("This holiday changed. Reload it before deleting.");
        requireUnusedDate(day.getDate());
        holidays.delete(day);
        holidays.flush();
    }

    // Weekend labels do not change working days; completed history keeps its saved duration.
    private void requireUnusedDate(LocalDate date) {
        if (date.getDayOfWeek().getValue() > 5) return;
        for (var status :
                List.of(
                        ApplicationStatus.APPLIED,
                        ApplicationStatus.UPDATED,
                        ApplicationStatus.APPROVED)) {
            if (!applications
                    .findByStatusAndCourseStartDateLessThanEqualAndCourseEndDateGreaterThanEqual(
                            status, date, date)
                    .isEmpty()) {
                throw new IllegalArgumentException(
                        "This date affects active applications. Resolve or reschedule them before"
                            + " changing the holiday.");
            }
        }
        if (batches.findAll().stream()
                .anyMatch(
                        b ->
                                b.isActive()
                                        && b.getCourseStartDate() != null
                                        && b.getCourseEndDate() != null
                                        && !b.getCourseEndDate().isBefore(LocalDate.now())
                                        && !date.isBefore(b.getCourseStartDate())
                                        && !date.isAfter(b.getCourseEndDate()))) {
            throw new IllegalArgumentException(
                    "This date affects a published session. Reschedule or archive that session"
                        + " first.");
        }
    }
}
