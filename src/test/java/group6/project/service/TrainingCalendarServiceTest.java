package group6.project.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import group6.project.model.*;
import group6.project.repo.*;

class TrainingCalendarServiceTest {
    // Month padding keeps Monday/Sunday columns aligned, even when no one has a course.
    @Test void emptyMonthStillHasCompleteWeeks() {
        var applications=mock(CourseApplicationRepo.class);var holidays=mock(ExcludedDaysRepo.class);
        var weeks=new TrainingCalendarService(applications,holidays).month(YearMonth.of(2026,2),null);
        assertTrue(weeks.stream().allMatch(w->w.size()==7));assertEquals(DayOfWeek.MONDAY,weeks.getFirst().getFirst().date().getDayOfWeek());
        assertEquals(DayOfWeek.SUNDAY,weeks.getLast().getLast().date().getDayOfWeek());
        assertEquals(28,weeks.stream().flatMap(List::stream).filter(TrainingCalendarService.Day::currentMonth).count());
    }

    // An invalid year is rejected before any unbounded date/query work begins.
    @Test void unsupportedYearIsRejected() {
        var applications=mock(CourseApplicationRepo.class);var holidays=mock(ExcludedDaysRepo.class);
        assertThrows(IllegalArgumentException.class,()->new TrainingCalendarService(applications,holidays).month(YearMonth.of(9999,1),null));
        verifyNoInteractions(applications,holidays);
    }
}
