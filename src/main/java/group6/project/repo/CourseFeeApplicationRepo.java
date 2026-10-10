package group6.project.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import group6.project.model.CourseFeeApplication;

public interface CourseFeeApplicationRepo
  extends JpaRepository<CourseFeeApplication, Integer>{

  // Personal claims: Find claims submitted by this employee.
  List<CourseFeeApplication> findByApplicant_UserId(Integer userId);


  boolean existsByApplicant_UserIdOrReviewer_UserIdOrApprovalManager_UserIdOrReimbursedBy_UserId(
      Integer applicant, Integer reviewer, Integer manager, Integer reimbursedBy);
  boolean existsByApprovalManager_UserIdAndApplicationStatus(Integer manager, group6.project.model.ApplicationStatus status);
  boolean existsByApplicant_UserIdAndApplicationStatus(Integer applicant, group6.project.model.ApplicationStatus status);
}
