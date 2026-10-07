package group6.project.service;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.time.YearMonth;
import group6.project.model.CourseApplication;
import group6.project.repo.CourseApplicationRepo;
import org.springframework.stereotype.Service;
@Service 
public class CourseScheduleService {
  private final ExcludedDaysService excludedDaysService;
  private final CourseApplicationRepo courseApplicationRepo;

  public CourseScheduleService(ExcludedDaysService excludedDaysService,           CourseApplicationRepo courseApplicationRepo) {
      this.excludedDaysService = excludedDaysService;
      this.courseApplicationRepo = courseApplicationRepo;
  }
  public List<CourseApplication> getAllCourses() {
    return courseApplicationRepo.findAll();
  }
  public CourseApplication getCourse(Integer id) {
    return courseApplicationRepo.findById(id)
            .orElseThrow(() ->
                    new IllegalArgumentException(
                            "Course application was not found."));
  }
  public Schedule calculateSchedule(
        LocalDate requestedStartDate,
        double trainingDays) {
     if (requestedStartDate == null) {
         throw new IllegalArgumentException("Start date is required.");
     }
     if (trainingDays <= 0) {
         throw new IllegalArgumentException("Training days must be greater than zero");
     }
     if (trainingDays % 0.5 != 0) {
         throw new IllegalArgumentException("Training days must be in whole or half days");
     }
     LocalDate currentDate = requestedStartDate;

     // Find the first valid working day
     while (!excludedDaysService.isWorkingDay(currentDate)) {
         currentDate = currentDate.plusDays(1);
     }
     LocalDate actualStartDate = currentDate;

     // Number of whole training days
     int wholeDays = (int) Math.floor(trainingDays);

     // Whether there is an additional half day
     boolean hasHalfDay = trainingDays - wholeDays >= 0.5;
     double remainingDays = wholeDays;
     if  (hasHalfDay) {
        remainingDays += 0.5;
     }
     LocalDate actualEndDate = actualStartDate;
     while (remainingDays > 0) {
         if (excludedDaysService.isWorkingDay(currentDate)) {
             if (remainingDays >= 1) {
                remainingDays -= 1;
            } else {
                // Remaining 0.5 day
                remainingDays = 0;
            }
            actualEndDate = currentDate;
        }
         if (remainingDays > 0) {
            currentDate = currentDate.plusDays(1);
        }
     }
     return new Schedule(
            requestedStartDate,
            actualStartDate,
            actualEndDate,
            trainingDays);
  }
  public List<List<LocalDate>> generateCalendar(LocalDate date) {
     YearMonth yearMonth = YearMonth.from(date);
     LocalDate firstDay = yearMonth.atDay(1);
     LocalDate lastDay = yearMonth.atEndOfMonth();
     List<List<LocalDate>> weeks = new ArrayList<>();
     List<LocalDate> week = new ArrayList<>();
     int firstDayPosition = firstDay.getDayOfWeek().getValue();
     for (int i = 1; i < firstDayPosition; i++) {
         week.add(null);
     }
     LocalDate currentDate = firstDay;
     while (!currentDate.isAfter(lastDay)) {
         week.add(currentDate);
         if (week.size() == 7) {
             weeks.add(week);
             week = new ArrayList<>();
         }
         currentDate = currentDate.plusDays(1);
     }
     if (!week.isEmpty()) {
         while (week.size() < 7) {
             week.add(null);
         }
         weeks.add(week);
     }
     return weeks;
  }
  public List<CalendarMonth> generateCalendars(
          LocalDate startDate,
          LocalDate endDate) {
      List<CalendarMonth> calendars = new ArrayList<>();
      YearMonth currentMonth = YearMonth.from(startDate);
      YearMonth endMonth = YearMonth.from(endDate);
      while (!currentMonth.isAfter(endMonth)) {
          LocalDate monthDate = currentMonth.atDay(1);
          calendars.add(
                  new CalendarMonth(
                          currentMonth.getMonth().toString(),
                          currentMonth.getYear(),
                          generateCalendar(monthDate)));
          currentMonth = currentMonth.plusMonths(1);
       }
    return calendars;
  }
  public List<LocalDate> getTrainingDates(Schedule schedule) {
     List<LocalDate> trainingDates = new ArrayList<>();
     LocalDate currentDate = schedule.actualStartDate();
     while (!currentDate.isAfter(schedule.actualEndDate())) {
         if (excludedDaysService.isWorkingDay(currentDate)) {
            trainingDates.add(currentDate);
         }
         currentDate = currentDate.plusDays(1);
     }
     return trainingDates;
  }
  public record Schedule(
          LocalDate requestedStartDate,LocalDate actualStartDate,LocalDate actualEndDate,
          double trainingDays) {
  }
  public record CalendarMonth(
          String month,
          int year,
          List<List<LocalDate>> weeks) {
  }
}
