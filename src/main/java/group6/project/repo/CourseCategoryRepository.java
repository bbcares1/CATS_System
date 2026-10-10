// Reads and saves catalogue categories.
package group6.project.repo;

import group6.project.model.CourseCategory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Integer> {
    boolean existsByCategoryNameIgnoreCase(String name);

    boolean existsByCategoryNameIgnoreCaseAndCategoryIdNot(String name, Integer id);
}
