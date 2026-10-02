package group6.project.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import group6.project.model.CourseCategory;
import group6.project.repo.CourseCategoryRepository;

@Service 
public class CourseCategoryService {
  private final CourseCategoryRepository courseCatergoryRepository;
  public CourseCategoryService(CourseCategoryRepository courseCatergoryRepository){
    this.courseCatergoryRepository = courseCatergoryRepository;
  }

  public List<CourseCategory> getAllCategories(){
    return courseCatergoryRepository.findAll();
  }

  public Optional<CourseCategory> getCategoryById(Integer categoryId){
    return courseCatergoryRepository.findById(categoryId);
  }

  public CourseCategory createCategory(CourseCategory courseCategory) {
    return courseCatergoryRepository.save(courseCategory);
  }
  
  public void deleteCategory(Integer categoryId){
    courseCatergoryRepository.deleteById(categoryId);
  }

  public Optional<CourseCategory> updateCategory(CourseCategory courseCategory, Integer categoryId){
    Optional<CourseCategory> existingCategory = courseCatergoryRepository.findById(categoryId);
    if (existingCategory.isPresent()){
      CourseCategory existing = existingCategory.get();
      existing.setCategoryName(courseCategory.getCategoryName());
      CourseCategory saved = courseCatergoryRepository.save(existing);
      return Optional.of(saved);
    }
    return Optional.empty();
  }
  
}
