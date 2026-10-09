package group6.project.repo;

import group6.project.model.CourseCategory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Integer> {
    boolean existsByCategoryNameIgnoreCaseAndCategoryIdNot(String label, Integer id);
}
