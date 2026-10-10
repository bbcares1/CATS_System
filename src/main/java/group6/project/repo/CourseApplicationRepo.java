package group6.project.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    boolean existsByApplicant_UserIdOrReviewer_UserId(Integer applicant, Integer reviewer);
    boolean existsByApprovalManager_UserId(Integer manager);
    boolean existsByApprovalManager_UserIdAndStatusIn(Integer manager, List<ApplicationStatus> statuses);
    boolean existsByApplicant_UserIdAndStatusIn(Integer applicant, List<ApplicationStatus> statuses);
}
