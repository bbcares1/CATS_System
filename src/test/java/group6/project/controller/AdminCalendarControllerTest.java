
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

import group6.project.model.CourseBatch;
import group6.project.model.CourseDetail;
import group6.project.service.AdminService;
import group6.project.service.CourseCategoryService;
import group6.project.service.CourseBatchService;
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

    @Mock
    private CourseBatchService courseBatchService;

    private AdminController adminController;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        adminController = new AdminController(
                adminService,
                courseCategoryService,
                excludedDaysService,
                courseScheduleService,
                courseBatchService);
    }

    // Test 1: Display selected month
    @Test
    void shouldDisplaySelectedMonth() {

        when(courseBatchService.getAllBatches())
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

        CourseBatch batch = new CourseBatch();
        batch.setBatchId(1L);
        batch.setCourseStartDate(LocalDate.of(2026, 10, 29));
        batch.setTrainingDays(5.0);
        CourseDetail detail = new CourseDetail();
        detail.setTitle("Java Training");
        batch.setCourseDetail(detail);

        Long batchId = 1L;

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

        when(courseBatchService.getAllBatches())
                .thenReturn(Collections.emptyList());

        when(excludedDaysService.getAllExcludedDays())
                .thenReturn(Collections.emptyList());

        when(courseBatchService.getBatchById(batchId))
                .thenReturn(java.util.Optional.of(batch));
        when(courseBatchService.updateScheduleDates(
                batchId,
                LocalDate.of(2026, 10, 29),
                LocalDate.of(2026, 11, 4)))
                .thenReturn(batch);

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
                batchId,
                requestedStartDate,
                "",
                model,
                new MockHttpSession());

        assertEquals("CourseCalendar", viewName);

        assertEquals(
                batch,
                model.getAttribute("selectedBatch"));

        assertEquals(
                schedule,
                model.getAttribute("schedule"));

        assertEquals(
                trainingDates,
                model.getAttribute("trainingDates"));

        assertEquals(
                batchId,
                model.getAttribute("selectedBatchId"));

        assertEquals(
                requestedStartDate,
                model.getAttribute("requestedStartDate"));

        verify(courseScheduleService).generateCalendars(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30));
        verify(courseBatchService).updateScheduleDates(
                batchId,
                LocalDate.of(2026, 10, 29),
                LocalDate.of(2026, 11, 4));
    }

    // Test 3: Keep selected weekend training dates
    @Test
    void shouldKeepWeekendDatesWhenSwitchingMonths() {

        CourseBatch batch = new CourseBatch();
        batch.setBatchId(1L);
        batch.setCourseStartDate(LocalDate.of(2026, 10, 12));
        batch.setTrainingDays(15.0);
        CourseDetail detail = new CourseDetail();
        detail.setTitle("Java Training");
        batch.setCourseDetail(detail);

        Long batchId = 1L;

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

        when(courseBatchService.getAllBatches())
                .thenReturn(Collections.emptyList());

        when(excludedDaysService.getAllExcludedDays())
                .thenReturn(Collections.emptyList());

        when(courseBatchService.getBatchById(batchId))
                .thenReturn(java.util.Optional.of(batch));
        when(courseBatchService.updateScheduleDates(
                batchId,
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 30)))
                .thenReturn(batch);

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
                batchId,
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
        verify(courseBatchService).updateScheduleDates(
                batchId,
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 30));
    }

    // Test 4: No weekend dates selected
    @Test
    void shouldUseDefaultWhenNoWeekendDatesSelected() {

        CourseBatch batch = new CourseBatch();
        batch.setBatchId(2L);
        batch.setCourseStartDate(LocalDate.of(2026, 10, 12));
        batch.setTrainingDays(3.0);
        CourseDetail detail = new CourseDetail();
        detail.setTitle("Python Programming");
        batch.setCourseDetail(detail);

        Long batchId = 2L;

        LocalDate requestedStartDate =
                LocalDate.of(2026, 10, 12);

        CourseScheduleService.Schedule schedule =
                new CourseScheduleService.Schedule(
                        requestedStartDate,
                        LocalDate.of(2026, 10, 12),
                        LocalDate.of(2026, 10, 14),
                        3.0);

        when(courseBatchService.getAllBatches())
                .thenReturn(Collections.emptyList());

        when(excludedDaysService.getAllExcludedDays())
                .thenReturn(Collections.emptyList());

        when(courseBatchService.getBatchById(batchId))
                .thenReturn(java.util.Optional.of(batch));

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
                batchId,
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
        Long batchId = 9L;
        LocalDate requestedStartDate = LocalDate.of(2026, 11, 2);
        CourseBatch batch = new CourseBatch();
        batch.setBatchId(batchId);
        batch.setCourseStartDate(requestedStartDate);
        batch.setTrainingDays(3.0);
        CourseDetail detail = new CourseDetail();
        detail.setTitle("Session-restored course");
        batch.setCourseDetail(detail);
        CourseScheduleService.Schedule schedule = new CourseScheduleService.Schedule(
                requestedStartDate,
                requestedStartDate,
                LocalDate.of(2026, 11, 4),
                3.0);
        MockHttpSession session = new MockHttpSession();

        when(courseBatchService.getAllBatches()).thenReturn(List.of(batch));
        when(excludedDaysService.getAllExcludedDays()).thenReturn(Collections.emptyList());
        when(courseBatchService.getBatchById(batchId)).thenReturn(java.util.Optional.of(batch));
        when(courseBatchService.updateScheduleDates(
                batchId,
                requestedStartDate,
                LocalDate.of(2026, 11, 4)))
                .thenReturn(batch);
        when(courseScheduleService.calculateSchedule(requestedStartDate, 3.0, Set.of()))
                .thenReturn(schedule);
        when(courseScheduleService.getTrainingDates(schedule, Set.of()))
                .thenReturn(List.of(
                        requestedStartDate,
                        LocalDate.of(2026, 11, 3),
                        LocalDate.of(2026, 11, 4)));

        adminController.showCourseCalendar(
                "2026-11",
                batchId,
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
        assertEquals(batchId, restoredModel.getAttribute("selectedBatchId"));
        assertEquals(requestedStartDate, restoredModel.getAttribute("requestedStartDate"));
        assertEquals("2026-11", restoredModel.getAttribute("currentMonth"));
        assertEquals(schedule, restoredModel.getAttribute("schedule"));
    }
}
