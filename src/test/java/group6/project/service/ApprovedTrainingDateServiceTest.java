
package group6.project.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import group6.project.model.ApprovedTrainingDate;
import group6.project.model.CourseApplication;
import group6.project.repo.ApprovedTrainingDateRepo;
import group6.project.repo.CourseApplicationRepo;

@ExtendWith(MockitoExtension.class)
class ApprovedTrainingDateServiceTest {

    @Mock
    private ApprovedTrainingDateRepo trainingDateRepo;

    @Mock
    private CourseApplicationRepo courseApplicationRepo;

    @Mock
    private ExcludedDaysService excludedDaysService;

    @InjectMocks
    private ApprovedTrainingDateService service;

    @Test
    void shouldSaveNormalTrainingDates() {

        // Arrange: Prepare a course application
        CourseApplication course = new CourseApplication();

        course.setCourseStartDate(LocalDate.of(2026, 10, 12));
        course.setCourseEndDate(LocalDate.of(2026, 10, 14));
        course.setTrainingDays(3.0);

        when(courseApplicationRepo.findById(1))
                .thenReturn(Optional.of(course));

        List<LocalDate> trainingDates = List.of(
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 13),
                LocalDate.of(2026, 10, 14));

        // Act and Assert
        assertDoesNotThrow(() ->
                service.saveTrainingDates(
                        1,
                        trainingDates,
                        null));

        // Verify three training dates were saved
        verify(trainingDateRepo,
                org.mockito.Mockito.times(3))
                .save(any(ApprovedTrainingDate.class));

        // Verify excluded days were checked
        verify(excludedDaysService,
                org.mockito.Mockito.times(3))
                .isExcludedDay(any(LocalDate.class));
    }    
    @Test
    void shouldSaveSelectedSaturdayTraining() {

    // Arrange: Course includes one Saturday
    CourseApplication course = new CourseApplication();

    course.setCourseStartDate(LocalDate.of(2026, 10, 16));
    course.setCourseEndDate(LocalDate.of(2026, 10, 19));
    course.setTrainingDays(2.0);

    when(courseApplicationRepo.findById(1))
            .thenReturn(Optional.of(course));

    // Friday and Saturday are selected training dates
    List<LocalDate> trainingDates = List.of(
            LocalDate.of(2026, 10, 16),
            LocalDate.of(2026, 10, 17));

    // Act
    service.saveTrainingDates(
            1,
            trainingDates,
            null);

    // Assert: Both dates are saved
    verify(trainingDateRepo,
            org.mockito.Mockito.times(2))
            .save(any(ApprovedTrainingDate.class));

    // Verify the selected Saturday is checked
    verify(excludedDaysService)
            .isExcludedDay(LocalDate.of(2026, 10, 17));
  }
  
  @Test
  void shouldSaveHalfDayTraining() {

    // Arrange: Course has 1.5 training days
    CourseApplication course = new CourseApplication();

    course.setCourseStartDate(
            LocalDate.of(2026, 10, 12));

    course.setCourseEndDate(
            LocalDate.of(2026, 10, 13));

    course.setTrainingDays(1.5);

    when(courseApplicationRepo.findById(1))
            .thenReturn(Optional.of(course));

    // Monday = 1 full day
    // Tuesday = 0.5 day
    List<LocalDate> trainingDates = List.of(
            LocalDate.of(2026, 10, 12),
            LocalDate.of(2026, 10, 13));

    LocalDate halfDayDate =
            LocalDate.of(2026, 10, 13);

    // Act
    service.saveTrainingDates(
            1,
            trainingDates,
            halfDayDate);

    // Assert: Two training date records are saved
    org.mockito.ArgumentCaptor<ApprovedTrainingDate> captor =
            org.mockito.ArgumentCaptor.forClass(
                    ApprovedTrainingDate.class);

    verify(trainingDateRepo,
            org.mockito.Mockito.times(2))
            .save(captor.capture());

    List<ApprovedTrainingDate> savedDates =
            captor.getAllValues();

    // Verify total duration = 1.5 days
    double totalDuration = savedDates.stream()
            .mapToDouble(
                    ApprovedTrainingDate::getTrainingDuration)
            .sum();

    assertEquals(1.5, totalDuration, 0.0001);

    // Verify Tuesday is the half-day
    ApprovedTrainingDate halfDay = savedDates.stream()
            .filter(d -> d.getTrainingDate()
                    .equals(halfDayDate))
            .findFirst()
            .orElseThrow();

    assertEquals(0.5,
            halfDay.getTrainingDuration(),
            0.0001);
  }
  
  @Test
  void shouldRejectDuplicateTrainingDates() {

    // Arrange: Prepare a course application
    CourseApplication course = new CourseApplication();

    course.setCourseStartDate(
            LocalDate.of(2026, 10, 12));

    course.setCourseEndDate(
            LocalDate.of(2026, 10, 16));

    course.setTrainingDays(2.0);

    when(courseApplicationRepo.findById(1))
            .thenReturn(Optional.of(course));

    // Same date appears twice
    List<LocalDate> trainingDates = List.of(
            LocalDate.of(2026, 10, 12),
            LocalDate.of(2026, 10, 12));

    // Act & Assert
    IllegalArgumentException exception =
            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.saveTrainingDates(
                            1,
                            trainingDates,
                            null));

    assertEquals(
            "Duplicate training dates are not allowed.",
            exception.getMessage());

    // No training dates should be saved
    verify(trainingDateRepo, never())
            .save(any(ApprovedTrainingDate.class));
  }

  @Test
  void shouldRejectTrainingOnExcludedDay() {

    // Arrange: Prepare a course application
    CourseApplication course = new CourseApplication();

    course.setCourseStartDate(
            LocalDate.of(2026, 10, 12));

    course.setCourseEndDate(
            LocalDate.of(2026, 10, 16));

    course.setTrainingDays(3.0);

    when(courseApplicationRepo.findById(1))
            .thenReturn(Optional.of(course));

    // October 14 is an Admin Excluded Day
    LocalDate excludedDate =
            LocalDate.of(2026, 10, 14);

    when(excludedDaysService.isExcludedDay(excludedDate))
            .thenReturn(true);

    List<LocalDate> trainingDates = List.of(
            LocalDate.of(2026, 10, 12),
            LocalDate.of(2026, 10, 13),
            excludedDate);

    // Act & Assert
    IllegalArgumentException exception =
            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.saveTrainingDates(
                            1,
                            trainingDates,
                            null));

    assertEquals(
            "Cannot schedule new training on an excluded day: 2026-10-14",
            exception.getMessage());

    // Verify no training dates are saved
    verify(trainingDateRepo, never())
            .save(any(ApprovedTrainingDate.class));
  }
  
  @Test
  void shouldRejectTrainingDaysMismatch() {

    // Arrange: Course requires 5 training days
    CourseApplication course = new CourseApplication();

    course.setCourseStartDate(
            LocalDate.of(2026, 10, 12));

    course.setCourseEndDate(
            LocalDate.of(2026, 10, 16));

    course.setTrainingDays(5.0);

    when(courseApplicationRepo.findById(1))
            .thenReturn(Optional.of(course));

    // Only 3 training dates are provided
    List<LocalDate> trainingDates = List.of(
            LocalDate.of(2026, 10, 12),
            LocalDate.of(2026, 10, 13),
            LocalDate.of(2026, 10, 14));

    // Act & Assert
    IllegalArgumentException exception =
            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.saveTrainingDates(
                            1,
                            trainingDates,
                            null));

    assertEquals(
            "Saved training dates must match application training days.",
            exception.getMessage());

    // No training records should be saved
    verify(trainingDateRepo, never())
            .save(any(ApprovedTrainingDate.class));
  }



}
