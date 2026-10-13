// Finds available catalogue courses and their date options.
package group6.project.service;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import group6.project.model.*;
import group6.project.repo.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class CourseCatalogueService {
    private final CourseDetailRepo courses;
    private final CourseBatchRepo batches;
    private final CourseApplicationRepo applications;
    private static final List<ApplicationStatus> SEATS =
            List.of(
                    ApplicationStatus.APPLIED,
                    ApplicationStatus.UPDATED,
                    ApplicationStatus.APPROVED,
                    ApplicationStatus.COMPLETED);

    public CourseCatalogueService(
            CourseDetailRepo courses, CourseBatchRepo batches, CourseApplicationRepo applications) {
        this.courses = courses;
        this.batches = batches;
        this.applications = applications;
    }

    // Match title, provider and description while keeping archived offers out of Staff pages.
    public List<CourseDetail> search(
            String query, CourseCategoryType category, Integer providerId) {
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<CourseDetail> result = new ArrayList<>();
        for (CourseDetail course : courses.findAll()) {
            if (!available(course)) continue;
            if (category != null && course.getCourseCategory().getKind() != category) continue;
            if (providerId != null && !providerId.equals(course.getProvider().getProviderId()))
                continue;
            String text =
                    course.getTitle()
                            + " "
                            + course.getProvider().getName()
                            + " "
                            + course.getCourseDescription();
            if (text.toLowerCase(Locale.ROOT).contains(term)) result.add(course);
        }
        result.sort(Comparator.comparing(CourseDetail::getTitle, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    // Shared availability checks prevent direct links from exposing archived offers.
    public CourseDetail offer(Integer id) {
        return courses.findById(id)
                .filter(this::available)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Course not found."));
    }

    // Only future, active batches are offered; remaining seats include pending reservations.
    public List<BatchChoice> schedules(Integer id) {
        List<BatchChoice> choices = new ArrayList<>();
        for (CourseBatch batch : batches.findByCourseDetail_CourseIdOrderByCourseStartDateAsc(id)) {
            if (!batch.isActive()
                    || batch.getCourseStartDate() == null
                    || !batch.getCourseStartDate().isAfter(LocalDate.now())) continue;
            long used =
                    applications.countByCatalogueBatch_BatchIdAndStatusIn(
                            batch.getBatchId(), SEATS);
            choices.add(new BatchChoice(batch, Math.max(0, batch.getCapacity() - used)));
        }
        return choices;
    }

    // Disabling a provider also stops new applications for its catalogue courses.
    public boolean available(CourseDetail course) {
        return course.isActive()
                && course.getProvider() != null
                && course.getProvider().isActive()
                && course.getCourseCategory() != null
                && course.getCourseCategory().getKind() != null;
    }

    public record BatchChoice(CourseBatch batch, long places) {}
}
