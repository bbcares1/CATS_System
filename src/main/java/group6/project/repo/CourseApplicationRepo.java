package group6.project.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import group6.project.model.ApplicationStatus;
import group6.project.model.CourseApplication;

public interface CourseApplicationRepo extends JpaRepository<CourseApplication,Integer>{
    @Query("select a.applicant.userId from CourseApplication a where a.courseId=:id")
    Optional<Integer> applicantId(@Param("id") Integer id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CourseApplication a where a.courseId=:id")
    Optional<CourseApplication> lockById(@Param("id") Integer id);

    @Query("select count(a) from CourseApplication a where a.status in :statuses and a.courseStartDate <= :date and a.courseEndDate >= :date")
    long countAffectedByHoliday(@Param("date") java.time.LocalDate date, @Param("statuses") List<ApplicationStatus> statuses);
    boolean existsByCatalogueCourse_CourseId(Integer id);
    boolean existsByCatalogueBatch_BatchId(Long id);
    long countByCatalogueBatch_BatchIdAndStatusIn(Long id, List<ApplicationStatus> statuses);
    List<CourseApplication> findByApplicant_UserIdAndCourseStartDateBetweenOrderByCourseStartDateAsc(
            Integer userId, java.time.LocalDate from, java.time.LocalDate to);

    List<CourseApplication> findByApplicant_UserIdAndStatusIn(
            Integer userId, List<ApplicationStatus> statuses);

    @Query("""
            select a from CourseApplication a join fetch a.applicant s
            where a.approvalManager.userId = :managerId and s.userId <> :managerId
              and a.status in :statuses
            order by s.name, s.userId, a.courseStartDate, a.courseId
            """)
    List<CourseApplication> findPendingForManager(
            @Param("managerId") Integer managerId,
            @Param("statuses") List<ApplicationStatus> statuses);

    @Query("""
            select a from CourseApplication a join fetch a.applicant s
            left join s.manager m left join a.approvalManager r
            where a.courseId = :applicationId
              and (m.userId = :managerId or r.userId = :managerId)
              and s.userId <> :managerId
            """)
    Optional<CourseApplication> findForManager(
            @Param("applicationId") Integer applicationId,
            @Param("managerId") Integer managerId);

    boolean existsByApplicant_UserIdOrReviewer_UserId(Integer applicant, Integer reviewer);
    boolean existsByApprovalManager_UserId(Integer manager);
    boolean existsByApprovalManager_UserIdAndStatusIn(Integer manager, List<ApplicationStatus> statuses);
    boolean existsByApplicant_UserIdAndStatusIn(Integer applicant, List<ApplicationStatus> statuses);
}
