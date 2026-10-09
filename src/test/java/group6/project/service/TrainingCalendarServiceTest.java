package group6.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import group6.project.model.ApprovedTrainingDate;
import group6.project.model.CourseApplication;
import group6.project.repo.ApprovedTrainingDateRepo;
import group6.project.model.ApplicationStatus;

@ExtendWith(MockitoExtension.class)
class TrainingCalendarServiceTest {

    @Mock
    private ApprovedTrainingDateRepo trainingDateRepo;

    @Mock
    private CourseScheduleService courseScheduleService;

    @Mock
    private ExcludedDaysService excludedDaysService;

    @InjectMocks
    private TrainingCalendarService service;

    @Test
    void shouldLoadApprovedTrainingDatesForSelectedMonth() {

        // Arrange
        CourseApplication course = new CourseApplication();
        course.setCourseTitle("Java Training");

        LocalDate trainingDate =
                LocalDate.of(2026, 10, 17);

        ApprovedTrainingDate training =
                new ApprovedTrainingDate(
                        course,
                        trainingDate,
                        1.0);

        when(trainingDateRepo.findApprovedTrainingDates(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(training));

        // Act
        Map<LocalDate, List<ApprovedTrainingDate>> result =
                service.getTrainingByDate(
                        YearMonth.of(2026, 10));

        // Assert
        assertTrue(result.containsKey(trainingDate));

        assertEquals(
                1,
                result.get(trainingDate).size());

        assertEquals(
                "Java Training",
                result.get(trainingDate)
                        .get(0)
                        .getCourseApplication()
                        .getCourseTitle());

        // Verify repository query
        verify(trainingDateRepo).findApprovedTrainingDates(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31));
    }
    
    @Test
    void shouldDetectExcludedDayConflict() {

    // Arrange: Approved training on October 17
    LocalDate trainingDate =
            LocalDate.of(2026, 10, 17);

    CourseApplication course = new CourseApplication();
    course.setCourseTitle("Java Training");
    course.setStatus(ApplicationStatus.APPROVED);

    ApprovedTrainingDate training =
            new ApprovedTrainingDate(
                    course,
                    trainingDate,
                    1.0);

    // Mock approved training date
    when(trainingDateRepo.findApprovedTrainingDates(
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 31)))
            .thenReturn(List.of(training));

    // Admin later marks October 17 as an excluded day
    when(excludedDaysService.isExcludedDay(trainingDate))
            .thenReturn(true);

    // Act: Load approved training dates
    Map<LocalDate, List<ApprovedTrainingDate>> result =
            service.getTrainingByDate(
                    YearMonth.of(2026, 10));

    // Check holiday conflict
    boolean hasConflict =
            service.hasHolidayConflict(trainingDate);

    // Assert: Training is still displayed
    assertTrue(result.containsKey(trainingDate));

    assertEquals(
            "Java Training",
            result.get(trainingDate)
                    .get(0)
                    .getCourseApplication()
                    .getCourseTitle());

    // Assert: Conflict is detected
    assertTrue(hasConflict);

    // Verify excluded day was checked
    verify(excludedDaysService)
            .isExcludedDay(trainingDate);
  }
  
  @Test
  void shouldDisplayTrainingAcrossMonths() {

    // Arrange: One approved course crosses October and November
    CourseApplication course = new CourseApplication();
    course.setCourseTitle("Java Training");
    course.setStatus(ApplicationStatus.APPROVED);

    ApprovedTrainingDate octoberTraining =
            new ApprovedTrainingDate(
                    course,
                    LocalDate.of(2026, 10, 30),
                    1.0);

    ApprovedTrainingDate novemberTraining =
            new ApprovedTrainingDate(
                    course,
                    LocalDate.of(2026, 11, 2),
                    1.0);

    // Mock October query
    when(trainingDateRepo.findApprovedTrainingDates(
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 31)))
            .thenReturn(List.of(octoberTraining));

    // Mock November query
    when(trainingDateRepo.findApprovedTrainingDates(
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30)))
            .thenReturn(List.of(novemberTraining));

    // Act: Load October and November calendars
    Map<LocalDate, List<ApprovedTrainingDate>> october =
            service.getTrainingByDate(YearMonth.of(2026, 10));

    Map<LocalDate, List<ApprovedTrainingDate>> november =
            service.getTrainingByDate(YearMonth.of(2026, 11));

    // Assert: October shows October training
    assertTrue(october.containsKey(
            LocalDate.of(2026, 10, 30)));

    // Assert: November shows November training
    assertTrue(november.containsKey(
            LocalDate.of(2026, 11, 2)));

    // Both dates belong to the same course
    assertEquals(
            "Java Training",
            november.get(LocalDate.of(2026, 11, 2))
                    .get(0)
                    .getCourseApplication()
                    .getCourseTitle());

    // Verify both monthly queries
    verify(trainingDateRepo).findApprovedTrainingDates(
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 31));

    verify(trainingDateRepo).findApprovedTrainingDates(
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30));
  }


}
