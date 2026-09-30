package group6.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import group6.project.service.ExcludedDaysService;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import group6.project.model.ExcludedDays;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/excluded-days")
public class ExcludedDaysController{

    @Autowired
    private ExcludedDaysService excludedDaysService;
    @GetMapping
    public List<ExcludedDays> getAllExcludedDays() {
        return excludedDaysService.getAllExcludedDays();
    }
    @PostMapping
    public ExcludedDays addExcludedDay(@RequestBody ExcludedDays excludedDay) {
    return excludedDaysService.saveExcludedDay(excludedDay);
    }
    @PutMapping("/{id}")
    public ExcludedDays updateExcludedDay(
        @PathVariable Long id,
        @RequestBody ExcludedDays excludedDay) {

    excludedDay.setId(id);
    return excludedDaysService.saveExcludedDay(excludedDay);
}
    @DeleteMapping("/{id}")
    public void deleteExcludedDay(@PathVariable Long id) {
    excludedDaysService.deleteExcludedDay(id);
}
}