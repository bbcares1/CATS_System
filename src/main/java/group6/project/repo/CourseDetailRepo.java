package group6.project.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import group6.project.model.CourseDetail;

public interface CourseDetailRepo extends JpaRepository<CourseDetail,Integer>{
    
}
