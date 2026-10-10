// We maintain category labels without changing the rules of courses already using them.
package group6.project.service;

import static org.springframework.http.HttpStatus.*;

import group6.project.form.CourseCategoryForm;
import group6.project.model.CourseCategory;
import group6.project.repo.CourseCategoryRepository;
import group6.project.repo.CourseDetailRepo;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class CourseCategoryService {
    private final CourseCategoryRepository categories;
    private final CourseDetailRepo courses;

    public CourseCategoryService(CourseCategoryRepository categories, CourseDetailRepo courses) {
        this.categories = categories;
        this.courses = courses;
    }

    // Admin and course forms share the same category choices.
    public List<CourseCategory> getAllCategories() {
        return categories.findAll(Sort.by("categoryName"));
    }

    // Return one saved category or a clear missing-record error.
    public CourseCategory get(Integer id) {
        return categories
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Category not found."));
    }

    // Copy editable values rather than binding a persistent entity to a request.
    public CourseCategoryForm form(Integer id) {
        CourseCategory category = get(id);
        CourseCategoryForm form = new CourseCategoryForm();
        form.setVersion(category.getVersion());
        form.setCategoryName(category.getCategoryName());
        form.setKind(category.getKind());
        return form;
    }

    // Renaming is safe; a used category must keep its type so existing schedules stay valid.
    @Transactional
    public void save(Integer id, CourseCategoryForm form) {
        CourseCategory category = id == null ? new CourseCategory() : get(id);
        if (id != null && !Objects.equals(form.getVersion(), category.getVersion())) {
            throw new ResponseStatusException(CONFLICT, "This category changed. Reload it.");
        }
        String name = form.getCategoryName().trim();
        boolean duplicate =
                id == null
                        ? categories.existsByCategoryNameIgnoreCase(name)
                        : categories.existsByCategoryNameIgnoreCaseAndCategoryIdNot(name, id);
        if (duplicate)
            throw new ResponseStatusException(BAD_REQUEST, "That category name is already used.");
        if (id != null
                && category.getKind() != form.getKind()
                && courses.existsByCourseCategory_CategoryId(id)) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "A category used by courses must keep its type.");
        }
        category.setCategoryName(name);
        category.setKind(form.getKind());
        categories.save(category);
    }

    // A referenced category must first be removed from its courses.
    @Transactional
    public void delete(Integer id, Long version) {
        CourseCategory category = get(id);
        if (!Objects.equals(version, category.getVersion()))
            throw new ResponseStatusException(CONFLICT, "Reload this category.");
        if (courses.existsByCourseCategory_CategoryId(id))
            throw new ResponseStatusException(BAD_REQUEST, "This category is used by courses.");
        categories.delete(category);
    }
}
