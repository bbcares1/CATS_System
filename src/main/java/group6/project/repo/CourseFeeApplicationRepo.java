package group6.project.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;

import group6.project.model.CourseFeeApplication;
import group6.project.model.ApplicationStatus;

public interface CourseFeeApplicationRepo
  extends JpaRepository<CourseFeeApplication, Integer>{

  // Personal claims: Find claims submitted by this employee.
  List<CourseFeeApplication> findByApplicant_UserId(Integer userId);
  boolean existsByApplicant_UserId(Integer userId);
  boolean existsByApplicant_UserIdAndApplicationStatus(Integer userId, ApplicationStatus status);

  boolean existsByCourseApplication_CourseId(Integer id);
  boolean existsByApprovalManager_UserIdAndApplicationStatus(Integer id, ApplicationStatus status);
  boolean existsByApplicant_UserIdOrReviewer_UserIdOrApprovalManager_UserIdOrReimbursedBy_UserId(Integer a,Integer b,Integer c,Integer d);
  List<CourseFeeApplication> findByApprovalManager_UserIdAndApplicationStatusOrderBySubmittedAtAsc(Integer id, ApplicationStatus status);
  List<CourseFeeApplication> findByApplicationStatusOrderByReviewedAtAsc(ApplicationStatus status);
  @Query("select c.applicant.userId from CourseFeeApplication c where c.applicationId=:id")
  Optional<Integer> applicantId(@Param("id") Integer id);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from CourseFeeApplication c where c.applicationId=:id")
  Optional<CourseFeeApplication> lockById(@Param("id") Integer id);
}
