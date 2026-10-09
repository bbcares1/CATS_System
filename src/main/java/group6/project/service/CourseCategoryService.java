package group6.project.service;

import group6.project.model.CourseCategory;
import group6.project.repo.CourseCategoryRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CourseCategoryService {
    private final CourseCategoryRepository categories;

    // The assignment's three category kinds stay stable while Admin maintains their labels.
    public CourseCategoryService(CourseCategoryRepository categories) {
        this.categories = categories;
    }

    // Keep unrecognised legacy categories visible rather than deleting referenced records.
    public List<CourseCategory> getAllCategories() {
        return categories.findAll();
    }

    // Renaming changes the display label only, never the associated fee/date rules.
    @Transactional
    public void rename(Integer id, String label) {
        CourseCategory category =
                categories
                        .findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (label == null || label.isBlank() || label.trim().length() > 255) {
            throw new IllegalArgumentException(
                    "Category name is required and cannot exceed 255 characters.");
        }
        if (categories.existsByCategoryNameIgnoreCaseAndCategoryIdNot(label.trim(), id)) {
            throw new IllegalArgumentException("Category name already exists.");
        }
        category.setCategoryName(label.trim());
        categories.saveAndFlush(category);
    }
}
