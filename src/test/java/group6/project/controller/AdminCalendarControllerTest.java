
package group6.project.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import group6.project.model.CourseApplication;
import group6.project.service.AdminService;
import group6.project.service.CourseCategoryService;
import group6.project.service.ExcludedDaysService;
import group6.project.service.CourseScheduleService;

public class AdminCalendarControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private CourseCategoryService courseCategoryService;

    @Mock
    private ExcludedDaysService excludedDaysService;

    @Mock
    private CourseScheduleService courseScheduleService;

    private AdminController adminController;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        adminController = new AdminController(
                adminService,
                courseCategoryService,
                excludedDaysService,
                courseScheduleService);
    }

    @Test
    void shouldDisplaySelectedMonth() {

        when(courseScheduleService.getAllCourses())
                .thenReturn(Collections.emptyList());

        when(excludedDaysService.getAllExcludedDays())
                .thenReturn(Collections.emptyList());

        Model model = new ExtendedModelMap();

        String viewName = adminController.showCourseCalendar(
                "2026-11",
                null,
                null,
                model);

        assertEquals("CourseCalendar", viewName);

        verify(courseScheduleService).generateCalendars(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30));

        assertEquals(
                "2026-10",
                model.getAttribute("previousMonth"));

        assertEquals(
                "2026-12",
                model.getAttribute("nextMonth"));
    }
    @Test
void shouldKeepCourseWhenSwitchingMonths() {

    CourseApplication course = new CourseApplication();
    course.setCourseTitle("Java Training");
    course.setTrainingDays(5.0);

    Integer courseId = 1;
    LocalDate requestedStartDate =
            LocalDate.of(2026, 10, 29);

    CourseScheduleService.Schedule schedule =
            new CourseScheduleService.Schedule(
                    requestedStartDate,
                    LocalDate.of(2026, 10, 29),
                    LocalDate.of(2026, 11, 4),
                    5.0);

    List<LocalDate> trainingDates = List.of(
            LocalDate.of(2026, 10, 29),
            LocalDate.of(2026, 10, 30),
            LocalDate.of(2026, 11, 2),
            LocalDate.of(2026, 11, 3),
            LocalDate.of(2026, 11, 4));

    when(courseScheduleService.getCourse(courseId))
            .thenReturn(course);

    when(courseScheduleService.calculateSchedule(
            requestedStartDate, 5.0))
            .thenReturn(schedule);

    when(courseScheduleService.getTrainingDates(schedule))
            .thenReturn(trainingDates);

    when(courseScheduleService.getAllCourses())
            .thenReturn(Collections.emptyList());

    when(excludedDaysService.getAllExcludedDays())
            .thenReturn(Collections.emptyList());

    Model model = new ExtendedModelMap();

    String viewName = adminController.showCourseCalendar(
            "2026-11",
            courseId,
            requestedStartDate,
            model);

    assertEquals("CourseCalendar", viewName);

    assertEquals(
            course,
            model.getAttribute("selectedCourse"));

    assertEquals(
            schedule,
            model.getAttribute("schedule"));

    assertEquals(
            trainingDates,
            model.getAttribute("trainingDates"));

    assertEquals(
            courseId,
            model.getAttribute("selectedCourseId"));

    assertEquals(
            requestedStartDate,
            model.getAttribute("requestedStartDate"));

    verify(courseScheduleService).generateCalendars(
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30));
    }
}
