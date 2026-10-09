package group6.project.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import group6.project.model.ApplicationStatus;
import group6.project.model.CourseApplication;

public interface CourseApplicationRepo extends JpaRepository<CourseApplication,Integer>{
    List<CourseApplication> findByApplicant_UserIdAndCourseStartDateBetweenOrderByCourseStartDateAsc(
            Integer userId, java.time.LocalDate from, java.time.LocalDate to);

    List<CourseApplication> findByApplicant_UserIdAndStatusIn(
            Integer userId, List<ApplicationStatus> statuses);

    @Query("""
            select a from CourseApplication a join fetch a.applicant s
            where s.manager.userId = :managerId and s.userId <> :managerId
              and a.status in :statuses
            order by s.name, s.userId, a.courseStartDate, a.courseId
            """)
    List<CourseApplication> findPendingForManager(
            @Param("managerId") Integer managerId,
            @Param("statuses") List<ApplicationStatus> statuses);

    @Query("""
            select a from CourseApplication a join fetch a.applicant s
            where a.courseId = :applicationId
              and s.manager.userId = :managerId and s.userId <> :managerId
            """)
    Optional<CourseApplication> findForManager(
            @Param("applicationId") Integer applicationId,
            @Param("managerId") Integer managerId);
    // Pending and approved/completed bookings reserve seats in a scheduled batch.
    long countByCatalogueBatch_BatchIdAndStatusIn(Long batchId, List<ApplicationStatus> statuses);

    boolean existsByCatalogueBatch_BatchId(Long batchId);

    // Historical participants prevent physical account deletion.
    boolean existsByApplicant_UserIdOrReviewer_UserId(Integer applicantId, Integer reviewerId);
    boolean existsByApplicant_UserIdAndStatusIn(Integer id, List<ApplicationStatus> statuses);

    // Read identity without caching an application before its employee lock is acquired.
    @Query("""
            select a.applicant.userId from CourseApplication a
            where a.courseId = :id and a.applicant.manager.userId = :managerId
              and a.applicant.userId <> :managerId
            """)
    Optional<Integer> findApplicantIdForManager(@Param("id") Integer id, @Param("managerId") Integer managerId);

    // Locking reads see the latest state even with MySQL's repeatable-read isolation.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a from CourseApplication a join fetch a.applicant s
            where a.courseId = :id and s.manager.userId = :managerId and s.userId <> :managerId
            """)
    Optional<CourseApplication> lockForManager(@Param("id") Integer id, @Param("managerId") Integer managerId);

    // Decision support is restricted to other direct reports with approved courses in this period.
    @Query("""
            select a from CourseApplication a join fetch a.applicant s
            where s.manager.userId = :managerId and s.userId <> :employeeId and s.userId <> :managerId
              and a.status = :status and a.courseStartDate <= :end and a.courseEndDate >= :start
            order by a.courseStartDate, s.name
            """)
    List<CourseApplication> findApprovedDuring(@Param("managerId") Integer managerId,
            @Param("employeeId") Integer employeeId, @Param("status") ApplicationStatus status,
            @Param("start") java.time.LocalDate start, @Param("end") java.time.LocalDate end);
}
