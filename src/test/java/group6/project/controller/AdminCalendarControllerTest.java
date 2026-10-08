
package group6.project.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.mock.web.MockHttpSession;

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

    // Test 1: Display selected month
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
                null,
                model,
                new MockHttpSession());

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

    // Test 2: Keep course when switching months
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

        when(courseScheduleService.getAllCourses())
                .thenReturn(Collections.emptyList());

        when(excludedDaysService.getAllExcludedDays())
                .thenReturn(Collections.emptyList());

        when(courseScheduleService.getCourse(courseId))
                .thenReturn(course);

        when(courseScheduleService.calculateSchedule(
                requestedStartDate,
                5.0,
                Set.of()))
                .thenReturn(schedule);

        when(courseScheduleService.getTrainingDates(
                schedule,
                Set.of()))
                .thenReturn(trainingDates);

        Model model = new ExtendedModelMap();

        String viewName = adminController.showCourseCalendar(
                "2026-11",
                courseId,
                requestedStartDate,
                "",
                model,
                new MockHttpSession());

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

    // Test 3: Keep selected weekend training dates
    @Test
    void shouldKeepWeekendDatesWhenSwitchingMonths() {

        CourseApplication course = new CourseApplication();
        course.setCourseTitle("Java Training");
        course.setTrainingDays(15.0);

        Integer courseId = 1;

        LocalDate requestedStartDate =
                LocalDate.of(2026, 10, 12);

        String weekendTrainingDates =
                "2026-10-17,2026-10-24";

        Set<LocalDate> selectedWeekends = Set.of(
                LocalDate.of(2026, 10, 17),
                LocalDate.of(2026, 10, 24));

        CourseScheduleService.Schedule schedule =
                new CourseScheduleService.Schedule(
                        requestedStartDate,
                        LocalDate.of(2026, 10, 12),
                        LocalDate.of(2026, 10, 30),
                        15.0);

        List<LocalDate> trainingDates = List.of(
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 13),
                LocalDate.of(2026, 10, 17),
                LocalDate.of(2026, 10, 24),
                LocalDate.of(2026, 10, 30));

        when(courseScheduleService.getAllCourses())
                .thenReturn(Collections.emptyList());

        when(excludedDaysService.getAllExcludedDays())
                .thenReturn(Collections.emptyList());

        when(courseScheduleService.getCourse(courseId))
                .thenReturn(course);

        when(courseScheduleService.calculateSchedule(
                requestedStartDate,
                15.0,
                selectedWeekends))
                .thenReturn(schedule);

        when(courseScheduleService.getTrainingDates(
                schedule,
                selectedWeekends))
                .thenReturn(trainingDates);

        Model model = new ExtendedModelMap();

        String viewName = adminController.showCourseCalendar(
                "2026-10",
                courseId,
                requestedStartDate,
                weekendTrainingDates,
                model,
                new MockHttpSession());

        assertEquals("CourseCalendar", viewName);

        assertEquals(
                weekendTrainingDates,
                model.getAttribute("weekendTrainingDates"));

        assertEquals(
                trainingDates,
                model.getAttribute("trainingDates"));

        verify(courseScheduleService).calculateSchedule(
                requestedStartDate,
                15.0,
                selectedWeekends);

        verify(courseScheduleService).getTrainingDates(
                schedule,
                selectedWeekends);
    }

    // Test 4: No weekend dates selected
    @Test
    void shouldUseDefaultWhenNoWeekendDatesSelected() {

        CourseApplication course = new CourseApplication();
        course.setCourseTitle("Python Programming");
        course.setTrainingDays(3.0);

        Integer courseId = 2;

        LocalDate requestedStartDate =
                LocalDate.of(2026, 10, 12);

        CourseScheduleService.Schedule schedule =
                new CourseScheduleService.Schedule(
                        requestedStartDate,
                        LocalDate.of(2026, 10, 12),
                        LocalDate.of(2026, 10, 14),
                        3.0);

        when(courseScheduleService.getAllCourses())
                .thenReturn(Collections.emptyList());

        when(excludedDaysService.getAllExcludedDays())
                .thenReturn(Collections.emptyList());

        when(courseScheduleService.getCourse(courseId))
                .thenReturn(course);

        when(courseScheduleService.calculateSchedule(
                requestedStartDate,
                3.0,
                Set.of()))
                .thenReturn(schedule);

        when(courseScheduleService.getTrainingDates(
                schedule,
                Set.of()))
                .thenReturn(List.of(
                        LocalDate.of(2026, 10, 12),
                        LocalDate.of(2026, 10, 13),
                        LocalDate.of(2026, 10, 14)));

        Model model = new ExtendedModelMap();

        String viewName = adminController.showCourseCalendar(
                "2026-10",
                courseId,
                requestedStartDate,
                "",
                model,
                new MockHttpSession());

        assertEquals("CourseCalendar", viewName);

        assertEquals(
                "",
                model.getAttribute("weekendTrainingDates"));

        verify(courseScheduleService).calculateSchedule(
                requestedStartDate,
                3.0,
                Set.of());

        verify(courseScheduleService).getTrainingDates(
                schedule,
                Set.of());
    }

    @Test
    void shouldRestoreLastCalculatedScheduleFromSession() {
        Integer courseId = 9;
        LocalDate requestedStartDate = LocalDate.of(2026, 11, 2);
        CourseApplication course = new CourseApplication();
        course.setCourseTitle("Session-restored course");
        course.setTrainingDays(3.0);
        CourseScheduleService.Schedule schedule = new CourseScheduleService.Schedule(
                requestedStartDate,
                requestedStartDate,
                LocalDate.of(2026, 11, 4),
                3.0);
        MockHttpSession session = new MockHttpSession();

        when(courseScheduleService.getAllCourses()).thenReturn(List.of(course));
        when(excludedDaysService.getAllExcludedDays()).thenReturn(Collections.emptyList());
        when(courseScheduleService.getCourse(courseId)).thenReturn(course);
        when(courseScheduleService.calculateSchedule(requestedStartDate, 3.0, Set.of()))
                .thenReturn(schedule);
        when(courseScheduleService.getTrainingDates(schedule, Set.of()))
                .thenReturn(List.of(
                        requestedStartDate,
                        LocalDate.of(2026, 11, 3),
                        LocalDate.of(2026, 11, 4)));

        adminController.showCourseCalendar(
                "2026-11",
                courseId,
                requestedStartDate,
                "",
                new ExtendedModelMap(),
                session);

        Model restoredModel = new ExtendedModelMap();
        String viewName = adminController.showCourseCalendar(
                null,
                null,
                null,
                null,
                restoredModel,
                session);

        assertEquals("CourseCalendar", viewName);
        assertEquals(courseId, restoredModel.getAttribute("selectedCourseId"));
        assertEquals(requestedStartDate, restoredModel.getAttribute("requestedStartDate"));
        assertEquals("2026-11", restoredModel.getAttribute("currentMonth"));
        assertEquals(schedule, restoredModel.getAttribute("schedule"));
    }
}
