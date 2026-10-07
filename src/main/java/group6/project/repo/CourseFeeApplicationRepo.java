package group6.project.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import group6.project.model.CourseFeeApplication;

public interface CourseFeeApplicationRepo
  extends JpaRepository<CourseFeeApplication, Integer>{

}
