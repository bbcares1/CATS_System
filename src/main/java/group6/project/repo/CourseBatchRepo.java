package group6.project.repo;

import group6.project.model.CourseBatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface CourseBatchRepo extends JpaRepository<CourseBatch, Long> {
    List<CourseBatch> findByCourseDetail_CourseIdOrderByCourseStartDateAsc(Integer id);
    boolean existsByCourseDetail_CourseId(Integer id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from CourseBatch b where b.batchId = :id")
    Optional<CourseBatch> lockById(@Param("id") Long id);
}
