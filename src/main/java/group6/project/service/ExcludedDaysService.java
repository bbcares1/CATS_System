// We maintain public holidays and check their effect on saved training dates.
package group6.project.service;

import group6.project.model.ApplicationStatus;
import group6.project.model.ExcludedDays;
import group6.project.repo.CourseApplicationRepo;
import group6.project.repo.CourseBatchRepo;
import group6.project.repo.ExcludedDaysRepo;
import group6.project.repo.TrainingCalendarPolicyRepo;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExcludedDaysService {

    private final ExcludedDaysRepo excludedDaysRepo;
    private final TrainingCalendarPolicyRepo calendar;
    private final CourseApplicationRepo applications;
    private final CourseBatchRepo batches;

    public ExcludedDaysService(
            ExcludedDaysRepo excludedDaysRepo,
            TrainingCalendarPolicyRepo calendar,
            CourseApplicationRepo applications,
            CourseBatchRepo batches) {
        this.excludedDaysRepo = excludedDaysRepo;
        this.calendar = calendar;
        this.applications = applications;
        this.batches = batches;
    }

    // Check configured public holidays.
    public boolean isExcludedDay(LocalDate date) {
        return excludedDaysRepo.existsByDate(date);
    }

    // Weekends and configured holidays do not count as training days.
    public boolean isWorkingDay(LocalDate date) {
        boolean isWeekend =
                date.getDayOfWeek() == DayOfWeek.SATURDAY
                        || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        return !isWeekend && !isExcludedDay(date);
    }

    // Show holiday dates in chronological order.
    public List<ExcludedDays> getAllExcludedDays() {
        return excludedDaysRepo.findAll(Sort.by("date"));
    }

    // Load one holiday for its edit form.
    public ExcludedDays getExcludedDayById(Integer id) {
        return excludedDaysRepo
                .findById(id)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Excluded day not found with id: " + id));
    }

    @Transactional
    // Add a unique date only when existing schedules remain valid.
    public ExcludedDays addExcludedDay(ExcludedDays excludedDay) {
        calendar.editCalendar().orElseThrow();
        requireUnusedDate(excludedDay.getDate());

        if (excludedDaysRepo.existsByDate(excludedDay.getDate())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This holiday date already exists.");
        }

        return excludedDaysRepo.save(excludedDay);
    }

    @Transactional
    // Save a label or safe date change from the current version.
    public ExcludedDays updateExcludedDay(Integer id, ExcludedDays excludedDay) {

        calendar.editCalendar().orElseThrow();
        ExcludedDays existingDay =
                excludedDaysRepo
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Excluded day not found with id: " + id));

        if (excludedDay.getVersion() == null
                || !excludedDay.getVersion().equals(existingDay.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This holiday changed. Reload it before saving.");
        }
        if (excludedDaysRepo.existsByDateAndIdNot(excludedDay.getDate(), id)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This holiday date already exists.");
        }

        if (!existingDay.getDate().equals(excludedDay.getDate())) {
            requireUnusedDate(existingDay.getDate());
            requireUnusedDate(excludedDay.getDate());
        }
        existingDay.setDate(excludedDay.getDate());
        existingDay.setDescription(excludedDay.getDescription());

        return excludedDaysRepo.save(existingDay);
    }

    @Transactional
    // Remove an unused holiday after checking its saved version.
    public void deleteExcludedDay(Integer id, Long version) {
        calendar.editCalendar().orElseThrow();
        ExcludedDays day = getExcludedDayById(id);
        if (version == null || !version.equals(day.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This holiday changed. Reload it before deleting.");
        }
        requireUnusedDate(day.getDate());

        if (!excludedDaysRepo.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Excluded day not found with id: " + id);
        }

        excludedDaysRepo.deleteById(id);
    }

    // Do not silently change the duration already reserved by a course or advertised schedule.
    private void requireUnusedDate(LocalDate date) {
        if (date.getDayOfWeek().getValue() > 5) return;
        if (applications.countAffectedByHoliday(
                        date,
                        List.of(
                                ApplicationStatus.APPLIED,
                                ApplicationStatus.UPDATED,
                                ApplicationStatus.APPROVED))
                > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "This date is used by an active course application.");
        }
        for (var batch : batches.findAll()) {
            if (batch.isActive()
                    && !batch.getCourseEndDate().isBefore(LocalDate.now())
                    && !date.isBefore(batch.getCourseStartDate())
                    && !date.isAfter(batch.getCourseEndDate())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "This date is used by an active course schedule.");
            }
        }
    }
}
