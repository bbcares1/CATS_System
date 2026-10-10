package group6.project.controller;

import group6.project.service.CourseApplicationService;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import group6.project.model.CourseCategory;
import group6.project.repo.CourseCategoryRepository;
import group6.project.service.CourseCategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;



@Controller 
public class CourseCategoryController {
  private final CourseApplicationService courseApplicationService;
  private final CourseCategoryRepository courseCategoryRepository;
  private final CourseCategoryService courseCategoryService;

  //Constructor points out the object
  public CourseCategoryController(CourseCategoryService courseCategoryService, CourseCategoryRepository courseCategoryRepository, CourseApplicationService courseApplicationService){
    this.courseCategoryService = courseCategoryService;
    this.courseCategoryRepository = courseCategoryRepository;
    this.courseApplicationService = courseApplicationService;
  }

  //Show the category list
  @GetMapping("/admin/categories")
  public String getAllCategories(Model model) {

    model.addAttribute("categories", courseCategoryService.getAllCategories());

    return "course-category-list";
  }
  
  //Open an empty create-category-form
  @GetMapping("/admin/categories/new")
  public String showCreateCategoryForm(Model model) {
    CourseCategory courseCategory = new CourseCategory();
    model.addAttribute("courseCategory", courseCategory);

    return "course-category-form";
  }

  //Save the created category
  @PostMapping("/admin/categories")
    public String saveCategory(
    @Valid @ModelAttribute("courseCategory") CourseCategory courseCategory,
    BindingResult result,
    Model model,
    RedirectAttributes redirectAttributes
    ) {
    if (result.hasErrors()) {
      return "course-category-form";
    }
    courseCategoryService.createCategory(courseCategory);
    redirectAttributes.addFlashAttribute("success", "Course category created successfully");

    return "redirect:/admin/categories";
  }

  //Delete category
  @GetMapping("/admin/categories/delete")
  public String showDeleteCategoryForm(Model model) {
    model.addAttribute("categories", courseCategoryService.getAllCategories());

    return "course-category-delete";
  }

  //Submit delete
  @PostMapping("/admin/categories/delete")
    public String deleteCategory(
    @RequestParam Integer categoryId,
    RedirectAttributes redirectAttributes){
    courseCategoryService.deleteCategory(categoryId);
    redirectAttributes.addFlashAttribute("success", "Course category deleted successfully");

    return "redirect:/admin/categories";
  }

  @PostMapping("/admin/categories/delete/{id}")
  public String deleteCategoryById(
      @PathVariable("id") Integer categoryId,
      RedirectAttributes redirectAttributes) {
    courseCategoryService.deleteCategory(categoryId);
    redirectAttributes.addFlashAttribute("success", "Course category deleted successfully");
    return "redirect:/admin/categories";
  }

  //Get edit category
  @GetMapping("/admin/categories/edit/{id}")
  public String showEditCategoryForm(@PathVariable("id") Integer categoryId, Model model) {
    Optional<CourseCategory> existingCategory = courseCategoryService.getCategoryById(categoryId);
    if (existingCategory.isPresent()){
      CourseCategory category = existingCategory.get();
      model.addAttribute("courseCategory", category);
      return "course-category-edit";
    }
    return "redirect:/admin/categories";
  }

  @PostMapping("/admin/categories/edit/{id}")
  public String updateCategory(
      @PathVariable ("id") Integer categoryId,
      @Valid @ModelAttribute("courseCategory") CourseCategory courseCategory,
      BindingResult result,
      RedirectAttributes redirectAttributes) {
      if (result.hasErrors()) {
        courseCategory.setCategoryId(categoryId);
        return "course-category-edit";
      }
      courseCategoryService.updateCategory(courseCategory, categoryId);
      redirectAttributes.addFlashAttribute("success", "Course category updated successfully");
      return "redirect:/admin/categories";
  }

}
