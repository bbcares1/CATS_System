package group6.project.repo;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import group6.project.model.TrainingCalendarPolicy;

public interface TrainingCalendarPolicyRepo extends JpaRepository<TrainingCalendarPolicy,Integer> {
    // Schedule writes share the calendar until their validation and save have committed.
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select p from TrainingCalendarPolicy p where p.id=1")
    Optional<TrainingCalendarPolicy> readCalendar();

    // Holiday edits wait for those writes before checking affected applications/batches.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TrainingCalendarPolicy p where p.id=1")
    Optional<TrainingCalendarPolicy> editCalendar();
}
