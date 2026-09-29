package group6.project.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import group6.project.model.CourseApplication;

public interface CourseApplicationRepo extends JpaRepository<CourseApplication,Integer>{
    
}
