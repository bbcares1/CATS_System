package group6.project.repo;

import group6.project.model.CourseBatch;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseBatchRepo extends JpaRepository<CourseBatch, Long> {
    List<CourseBatch> findByCourseDetail_CourseIdOrderByCourseStartDateAsc(Integer courseId);

    // Read just the parent identity before obtaining locks in course/batch order.
    @Query("select b.courseDetail.courseId from CourseBatch b where b.batchId=:id")
    Optional<Integer> courseIdForBatch(@Param("id") Long id);

    // Serialize seat reservations and schedule edits for the same batch.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from CourseBatch b where b.batchId=:id")
    Optional<CourseBatch> lockById(@Param("id") Long id);
}
