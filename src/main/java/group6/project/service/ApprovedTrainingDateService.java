
package group6.project.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import group6.project.model.ApprovedTrainingDate;
import group6.project.model.CourseApplication;
import group6.project.repo.ApprovedTrainingDateRepo;
import group6.project.repo.CourseApplicationRepo;

@Service
public class ApprovedTrainingDateService {

    private final ApprovedTrainingDateRepo trainingDateRepo;
    private final CourseApplicationRepo courseApplicationRepo;
    private final ExcludedDaysService excludedDaysService;

    public ApprovedTrainingDateService(
            ApprovedTrainingDateRepo trainingDateRepo,
            CourseApplicationRepo courseApplicationRepo,
            ExcludedDaysService excludedDaysService) {

        this.trainingDateRepo = trainingDateRepo;
        this.courseApplicationRepo = courseApplicationRepo;
        this.excludedDaysService = excludedDaysService;
    }

    // Save confirmed training dates for an application
    @Transactional
    public void saveTrainingDates(
            Integer courseId,
            List<LocalDate> trainingDates,
            LocalDate halfDayDate) {

        CourseApplication course = courseApplicationRepo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Course application was not found."));

        if (trainingDates == null || trainingDates.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one training date is required.");
        }

        if (course.getCourseStartDate() == null
                || course.getCourseEndDate() == null) {
            throw new IllegalArgumentException(
                    "Course start and end dates are required.");
        }

        Set<LocalDate> uniqueDates = new HashSet<>(trainingDates);

        if (uniqueDates.size() != trainingDates.size()) {
            throw new IllegalArgumentException(
                    "Duplicate training dates are not allowed.");
        }

        if (halfDayDate != null && !uniqueDates.contains(halfDayDate)) {
            throw new IllegalArgumentException(
                    "Half-day date must be one of the training dates.");
        }

        double totalDays = 0;

        // Validate all dates before saving
        for (LocalDate date : uniqueDates) {

            if (date == null) {
                throw new IllegalArgumentException(
                        "Training date cannot be null.");
            }

            if (date.isBefore(course.getCourseStartDate())
                    || date.isAfter(course.getCourseEndDate())) {
                throw new IllegalArgumentException(
                        "Training date is outside the course period: " + date);
            }

            if (excludedDaysService.isExcludedDay(date)) {
                throw new IllegalArgumentException(
                        "Cannot schedule new training on an excluded day: "
                        + date);
            }

            double duration = date.equals(halfDayDate) ? 0.5 : 1.0;
            totalDays += duration;
        }

        // Check against approved application training days
        if (course.getTrainingDays() == null
                || Math.abs(totalDays - course.getTrainingDays()) > 0.0001) {
            throw new IllegalArgumentException(
                    "Saved training dates must match application training days.");
        }

        // Remove old dates and replace with confirmed dates
        List<ApprovedTrainingDate> existingDates =
                trainingDateRepo
                        .findByCourseApplication_CourseIdOrderByTrainingDateAsc(
                                courseId);

        trainingDateRepo.deleteAll(existingDates);
        trainingDateRepo.flush();

        for (LocalDate date : uniqueDates) {

            ApprovedTrainingDate training =
                    new ApprovedTrainingDate(
                            course,
                            date,
                            date.equals(halfDayDate) ? 0.5 : 1.0);

            trainingDateRepo.save(training);
        }
    }
}
