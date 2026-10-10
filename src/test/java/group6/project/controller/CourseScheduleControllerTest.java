package group6.project.controller;

import group6.project.form.ScheduleForm;
import group6.project.model.CourseCategoryType;
import group6.project.service.CourseScheduleService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CourseScheduleControllerTest {
    private final CourseScheduleService schedules = mock(CourseScheduleService.class);
    private final CourseScheduleController controller = new CourseScheduleController(schedules);

    @Test
    void openingCalculatorDoesNotCalculateOrSave() {
        var model = new ExtendedModelMap();
        assertEquals("course-schedule", controller.show(model));
        assertNotNull(model.getAttribute("form"));
        verifyNoInteractions(schedules);
    }

    @Test
    void previewShowsCalculatedWorkingDates() {
        var form = form();
        var expected = new CourseScheduleService.Schedule(form.getStartDate(), form.getStartDate(), 1, List.of(form.getStartDate()));
        when(schedules.calculate(form.getCategory(), form.getStartDate(), 1, null)).thenReturn(expected);
        var model = new ExtendedModelMap();
        controller.preview(form, new BeanPropertyBindingResult(form, "form"), model);
        assertEquals(expected, model.getAttribute("schedule"));
    }

    @Test
    void invalidFieldsKeepFormWithoutCallingService() {
        var form = form();
        var binding = new BeanPropertyBindingResult(form, "form");
        binding.rejectValue("days", "invalid");
        assertEquals("course-schedule", controller.preview(form, binding, new ExtendedModelMap()));
        verifyNoInteractions(schedules);
    }

    @Test
    void invalidRuleBecomesAFormError() {
        var form = form();
        when(schedules.calculate(form.getCategory(), form.getStartDate(), 1, null))
                .thenThrow(new IllegalArgumentException("The schedule must fit within one calendar year."));
        var binding = new BeanPropertyBindingResult(form, "form");
        controller.preview(form, binding, new ExtendedModelMap());
        assertTrue(binding.hasGlobalErrors());
        assertTrue(binding.getGlobalError().getDefaultMessage().contains("calendar year"));
    }

    @Test
    void halfDayChoiceIsPassedToTheSharedRules() {
        var form = form();
        form.setCategory(CourseCategoryType.INTERNAL_TRAINING);
        form.setDays(0.5);
        form.setHalfDayPeriod("PM");
        controller.preview(form, new BeanPropertyBindingResult(form, "form"), new ExtendedModelMap());
        verify(schedules).calculate(form.getCategory(), form.getStartDate(), 0.5, "PM");
    }

    // Only user-editable preview fields are supplied; there is no batch ID to mutate.
    private ScheduleForm form() {
        var form = new ScheduleForm();
        form.setCategory(CourseCategoryType.EXTERNAL_COURSE);
        form.setStartDate(LocalDate.now().plusDays(10));
        form.setDays(1.0);
        return form;
    }
}
