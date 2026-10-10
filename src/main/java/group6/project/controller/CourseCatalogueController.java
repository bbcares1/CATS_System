package group6.project.controller;

import group6.project.model.CourseCategoryType;
import group6.project.service.CourseCatalogueService;
import group6.project.service.CourseProviderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/staff/courses")
public class CourseCatalogueController {
    private final CourseCatalogueService catalogue;
    private final CourseProviderService providers;

    // Staff and Manager use the same catalogue in their personal workspace.
    public CourseCatalogueController(CourseCatalogueService catalogue, CourseProviderService providers) {
        this.catalogue = catalogue;
        this.providers = providers;
    }

    // Keep search and filters in the URL so a result can be bookmarked.
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) CourseCategoryType category,
            @RequestParam(required = false) Integer providerId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Model model) {
        var result = PageSupport.page(catalogue.search(query, category, providerId), page, size);
        model.addAttribute("courses", result.getContent());
        model.addAttribute("pageData", result);
        model.addAttribute("query", query);
        model.addAttribute("category", category);
        model.addAttribute("providerId", providerId);
        model.addAttribute("categories", CourseCategoryType.values());
        model.addAttribute("providers", providers.all().stream().filter(p -> p.isActive()).toList());
        return "course-catalogue";
    }

    // Details show the fixed course information before the employee begins an application.
    @GetMapping("/{id}")
    public String details(@PathVariable Integer id, Model model) {
        model.addAttribute("course", catalogue.offer(id));
        model.addAttribute("schedules", catalogue.schedules(id));
        return "course-catalogue-detail";
    }
}
