package group6.project.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import group6.project.repo.TrainingCalendarPolicyRepo;
import group6.project.repo.CourseApplicationRepo;
import group6.project.repo.CourseBatchRepo;
import group6.project.model.ApplicationStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import group6.project.model.ExcludedDays;
import group6.project.repo.ExcludedDaysRepo;

@Service
public class ExcludedDaysService {

    private final ExcludedDaysRepo excludedDaysRepo;
    private final TrainingCalendarPolicyRepo calendar;
    private final CourseApplicationRepo applications;
    private final CourseBatchRepo batches;

    // Holiday edits share the calendar lock with schedule and application validation.
    public ExcludedDaysService(ExcludedDaysRepo excludedDaysRepo, TrainingCalendarPolicyRepo calendar,
            CourseApplicationRepo applications, CourseBatchRepo batches) {
        this.excludedDaysRepo = excludedDaysRepo;
        this.calendar = calendar;
        this.applications = applications;
        this.batches = batches;
    }

    public boolean isExcludedDay(LocalDate date) {
        return excludedDaysRepo.existsByDate(date);
    }
    public boolean isWorkingDay(LocalDate date) {
        boolean isWeekend =
                date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        return !isWeekend && !isExcludedDay(date);
    }

    public List<ExcludedDays> getAllExcludedDays() {
        return excludedDaysRepo.findAll();
    }

    public ExcludedDays getExcludedDayById(Integer id) {
        return excludedDaysRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Excluded day not found with id: " + id));
    }

    @Transactional
    public ExcludedDays addExcludedDay(ExcludedDays excludedDay) {
        calendar.editCalendar().orElseThrow();
        requireUnusedDate(excludedDay.getDate());

        if (excludedDaysRepo.existsByDate(excludedDay.getDate())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This holiday date already exists.");
        }

        return excludedDaysRepo.save(excludedDay);
    }

    @Transactional
    public ExcludedDays updateExcludedDay(
            Integer id,
            ExcludedDays excludedDay) {

        calendar.editCalendar().orElseThrow();
        ExcludedDays existingDay = excludedDaysRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Excluded day not found with id: " + id));

        if (excludedDaysRepo.existsByDateAndIdNot(
                excludedDay.getDate(), id)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This holiday date already exists.");
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
    public void deleteExcludedDay(Integer id) {
        calendar.editCalendar().orElseThrow();
        requireUnusedDate(getExcludedDayById(id).getDate());

        if (!excludedDaysRepo.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Excluded day not found with id: " + id);
        }

        excludedDaysRepo.deleteById(id);
    }
    // Do not silently change the duration already reserved by a course or advertised schedule.
    private void requireUnusedDate(LocalDate date) {
        if (date.getDayOfWeek().getValue() > 5) return;
        if (applications.countAffectedByHoliday(date, List.of(ApplicationStatus.APPLIED,
                ApplicationStatus.UPDATED, ApplicationStatus.APPROVED)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This date is used by an active course application.");
        }
        for (var batch : batches.findAll()) {
            if (batch.isActive() && !batch.getCourseEndDate().isBefore(LocalDate.now())
                    && !date.isBefore(batch.getCourseStartDate()) && !date.isAfter(batch.getCourseEndDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This date is used by an active course schedule.");
            }
        }
    }
}
