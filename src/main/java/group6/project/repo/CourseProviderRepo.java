package group6.project.repo;

import group6.project.model.CourseProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseProviderRepo extends JpaRepository<CourseProvider, Integer> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndProviderIdNot(String name, Integer id);
}
