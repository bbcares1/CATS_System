package group6.project.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import group6.project.model.CourseCategory;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Integer> {
    boolean existsByCategoryNameIgnoreCase(String name);
    boolean existsByCategoryNameIgnoreCaseAndCategoryIdNot(String name, Integer id);

}
