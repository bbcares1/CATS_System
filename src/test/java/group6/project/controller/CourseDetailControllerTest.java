// We check that date calculation stays in the course form and never saves it.
package group6.project.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import group6.project.form.CourseForm;
import group6.project.model.CourseCategory;
import group6.project.model.CourseCategoryType;
import group6.project.service.*;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;

import java.time.LocalDate;
import java.util.List;

class CourseDetailControllerTest {
    private final CourseDetailService courses = mock(CourseDetailService.class);
    private final CourseCategoryService categories = mock(CourseCategoryService.class);
    private final CourseScheduleService schedules = mock(CourseScheduleService.class);
    private final CourseDetailController controller =
            new CourseDetailController(
                    courses,
                    categories,
                    mock(CourseProviderService.class),
                    mock(CourseBatchService.class),
                    schedules);

    @Test
    void openingCourseFormDoesNotCalculateOrSave() {
        var model = new ExtendedModelMap();
        assertEquals("admin-course-form", controller.create(model));
        assertNotNull(model.getAttribute("form"));
        verifyNoInteractions(schedules, courses);
    }

    @Test
    void calculationKeepsTheCourseDetailsAndFillsTheDates() {
        var form = form(CourseCategoryType.EXTERNAL_COURSE);
        when(schedules.calculate(CourseCategoryType.EXTERNAL_COURSE, form.getStartDate(), 1, null))
                .thenReturn(
                        new CourseScheduleService.Schedule(
                                form.getStartDate(),
                                form.getStartDate(),
                                1,
                                List.of(form.getStartDate())));
        var model = new ExtendedModelMap();
        controller.calculate(null, form, new BeanPropertyBindingResult(form, "form"), model);
        assertEquals(form.getStartDate(), form.getEndDate());
        assertEquals("Java practice", ((CourseForm) model.getAttribute("form")).getTitle());
        verifyNoInteractions(courses);
    }

    @Test
    void invalidInputKeepsTheFormWithoutCalculating() {
        var form = form(CourseCategoryType.EXTERNAL_COURSE);
        var binding = new BeanPropertyBindingResult(form, "form");
        binding.rejectValue("days", "invalid");
        assertEquals(
                "admin-course-form",
                controller.calculate(null, form, binding, new ExtendedModelMap()));
        verifyNoInteractions(schedules, courses);
    }

    @Test
    void calendarRuleFailureReturnsToTheSameForm() {
        var form = form(CourseCategoryType.EXTERNAL_COURSE);
        when(schedules.calculate(CourseCategoryType.EXTERNAL_COURSE, form.getStartDate(), 1, null))
                .thenThrow(
                        new IllegalArgumentException(
                                "The schedule must fit within one calendar year."));
        var binding = new BeanPropertyBindingResult(form, "form");
        controller.calculate(null, form, binding, new ExtendedModelMap());
        assertTrue(binding.hasGlobalErrors());
        assertTrue(binding.getGlobalError().getDefaultMessage().contains("calendar year"));
    }

    @Test
    void halfDayUsesTheSharedCalculator() {
        var form = form(CourseCategoryType.INTERNAL_TRAINING);
        form.setDays(0.5);
        form.setHalfDayPeriod("PM");
        when(schedules.calculate(
                        CourseCategoryType.INTERNAL_TRAINING, form.getStartDate(), 0.5, "PM"))
                .thenReturn(
                        new CourseScheduleService.Schedule(
                                form.getStartDate(),
                                form.getStartDate(),
                                0.5,
                                List.of(form.getStartDate())));
        controller.calculate(
                null, form, new BeanPropertyBindingResult(form, "form"), new ExtendedModelMap());
        verify(schedules)
                .calculate(CourseCategoryType.INTERNAL_TRAINING, form.getStartDate(), 0.5, "PM");
    }

    // The category comes from the catalogue, not a second choice in the calculator.
    private CourseForm form(CourseCategoryType kind) {
        var category = new CourseCategory();
        category.setKind(kind);
        when(categories.get(2)).thenReturn(category);
        var form = new CourseForm();
        form.setTitle("Java practice");
        form.setCategoryId(2);
        form.setStartDate(LocalDate.now().plusDays(10));
        form.setDays(1.0);
        return form;
    }
}
