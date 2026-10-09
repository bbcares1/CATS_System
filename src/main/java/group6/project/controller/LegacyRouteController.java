package group6.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class LegacyRouteController {
    // All personal history links lead to the retained Staff workflow.
    @GetMapping({"/course-applications", "/staff/course-applications"})
    public String history() { return "redirect:/staff/personal"; }

    // Old form bookmarks open the current form rather than a second implementation.
    @GetMapping({"/course-applications/new", "/staff/course-applications/new"})
    public String apply() { return "redirect:/staff/apply"; }

    // Ownership is checked by the destination page before details are rendered.
    @GetMapping({"/course-applications/{id}", "/staff/course-applications/{id}"})
    public String application(@PathVariable Integer id) { return "redirect:/staff/applications/" + id; }

    // The destination enforces pending status and employee ownership.
    @GetMapping({"/course-applications/{id}/edit", "/staff/course-applications/{id}/edit"})
    public String edit(@PathVariable Integer id) { return "redirect:/staff/applications/" + id + "/edit"; }

    // Retire the unscoped claims list and form in favour of personal claims.
    @GetMapping({"/course-fee-applications", "/course-fee-applications/new"})
    public String claims() { return "redirect:/staff/fee"; }

    // A legacy claim bookmark cannot open another employee's record.
    @GetMapping("/course-fee-applications/{id}")
    public String claim(@PathVariable Integer id) { return "redirect:/staff/claims/" + id; }

    // Downloads are checked by the same owner rule as the personal claim page.
    @GetMapping("/course-fee-applications/{id}/{document:receipt|certificate}")
    public String document(@PathVariable Integer id, @PathVariable String document) {
        return "redirect:/staff/claims/" + id + "/" + document;
    }

    // Use the Admin-maintained calendar data instead of missing duplicate templates.
    @GetMapping({"/excluded-days", "/excluded-days/add"})
    public String holidays() { return "redirect:/admin/excludedDays"; }

    // The Admin edit page applies the same validation as the retained list.
    @GetMapping("/excluded-days/edit/{id}")
    public String editHoliday(@PathVariable Integer id) { return "redirect:/admin/excludedDays/edit/" + id; }
}
