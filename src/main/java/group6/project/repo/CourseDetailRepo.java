package group6.project.repo;

import group6.project.model.CourseDetail;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseDetailRepo extends JpaRepository<CourseDetail, Integer> {
    // Keep an offer stable while an employee snapshots it or Admin changes its schedules.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CourseDetail c where c.courseId=:id")
    Optional<CourseDetail> lockById(@Param("id") Integer id);
}
