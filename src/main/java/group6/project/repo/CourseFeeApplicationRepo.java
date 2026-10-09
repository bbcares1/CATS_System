package group6.project.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import group6.project.model.CourseFeeApplication;

public interface CourseFeeApplicationRepo
  extends JpaRepository<CourseFeeApplication, Integer>{

  // Personal claims: Find claims submitted by this employee.
  List<CourseFeeApplication> findByApplicant_UserId(Integer userId);

}
