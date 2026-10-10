package group6.project.repo;

import group6.project.model.CourseDetail;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseDetailRepo extends JpaRepository<CourseDetail, Integer> {
    boolean existsByCourseCategory_CategoryId(Integer id);

    boolean existsByProvider_ProviderId(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CourseDetail c where c.courseId = :id")
    Optional<CourseDetail> lockById(@Param("id") Integer id);
}
