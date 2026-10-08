package group6.project.service;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.time.YearMonth;
import java.time.DayOfWeek;
import java.util.Set;
import group6.project.model.CourseApplication;
import group6.project.repo.CourseApplicationRepo;
import org.springframework.stereotype.Service;
@Service 
public class CourseScheduleService {
  private final ExcludedDaysService excludedDaysService;
  private final CourseApplicationRepo courseApplicationRepo;

   public CourseScheduleService(ExcludedDaysService excludedDaysService,       
    CourseApplicationRepo courseApplicationRepo) {
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
   // Check whether a date can be used for training
  private boolean isTrainingDay(
          LocalDate date,
          Set<LocalDate> weekendTrainingDates) {
    // Always skip Admin excluded days
      if (excludedDaysService.isExcludedDay(date)) {
          return false;
    }
    DayOfWeek dayOfWeek = date.getDayOfWeek();
    // Normal working days
       if (dayOfWeek != DayOfWeek.SATURDAY
              && dayOfWeek != DayOfWeek.SUNDAY) {
           return true;
    }
    // Only selected weekend dates are allowed
       return weekendTrainingDates.contains(date);
   }
    // Calculate schedule without weekend training
   public Schedule calculateSchedule(
            LocalDate requestedStartDate,
            double trainingDays) {
        return calculateSchedule(
                requestedStartDate,
                trainingDays,
                Set.of());
   }
    // Calculate schedule with selected weekend training dates
   public Schedule calculateSchedule(
           LocalDate requestedStartDate,
           double trainingDays,
            Set<LocalDate> weekendTrainingDates) {
        // Validate requested start date
        if (requestedStartDate == null) {
            throw new IllegalArgumentException(
                    "Start date is required.");
        }
        // Validate training days
        if (!Double.isFinite(trainingDays)
                || trainingDays <= 0
                || trainingDays % 0.5 != 0) {
            throw new IllegalArgumentException(
                    "Training days must be positive whole or half days.");
        }
        // Default to no weekend training
        if (weekendTrainingDates == null) {
            weekendTrainingDates = Set.of();
        }
        LocalDate currentDate = requestedStartDate;
        // Find the first valid training day
        while (!isTrainingDay(currentDate, weekendTrainingDates)) {
            currentDate = currentDate.plusDays(1);
        }
        LocalDate actualStartDate = currentDate;
        // Count training time in half-day units
        int remainingHalfDays =
                (int) Math.round(trainingDays * 2);
        LocalDate actualEndDate = actualStartDate;
        // Calculate the actual end date
        while (remainingHalfDays > 0) {
            if (isTrainingDay(currentDate, weekendTrainingDates)) {
                // One full day = 2 half-day units
                int usedHalfDays =Math.min(2, remainingHalfDays);
                remainingHalfDays -= usedHalfDays;
                actualEndDate = currentDate;
            }
            // Move to next date if training is not finished
            if (remainingHalfDays > 0) {
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
  // Get training dates without weekend training
  public List<LocalDate> getTrainingDates(Schedule schedule) {
      return getTrainingDates(schedule, Set.of());
  }
  // Get training dates including selected weekends
  public List<LocalDate> getTrainingDates(
          Schedule schedule,
          Set<LocalDate> weekendTrainingDates) {
      List<LocalDate> trainingDates = new ArrayList<>();
      if (weekendTrainingDates == null) {
          weekendTrainingDates = Set.of();
      }
      LocalDate currentDate = schedule.actualStartDate();
      while (!currentDate.isAfter(schedule.actualEndDate())) {
          if (isTrainingDay(currentDate, weekendTrainingDates)) {
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
