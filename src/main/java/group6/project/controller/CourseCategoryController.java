package group6.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import group6.project.service.CourseCategoryService;

@Controller
@RequestMapping("/admin/categories")
public class CourseCategoryController {
    private final CourseCategoryService categories;

    // Category maintenance does not need application data or direct repository access.
    public CourseCategoryController(CourseCategoryService categories) { this.categories = categories; }

    // Show the three supported business types and any retained legacy labels.
    @GetMapping
    public String list(Model model) { model.addAttribute("categories", categories.getAllCategories()); return "course-category-list"; }

    // Old category edit links lead to the inline label form.
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id) { return "redirect:/admin/categories"; }

    // Labels are editable; adding/deleting business types would require new application rules.
    @PostMapping("/{id}/rename")
    public String rename(@PathVariable Integer id, @RequestParam String label, RedirectAttributes redirect) {
        try { categories.rename(id, label); redirect.addFlashAttribute("success", "Category name saved."); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/categories";
    }
}
