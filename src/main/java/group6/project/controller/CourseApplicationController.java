package group6.project.controller;

import org.springframework.web.bind.annotation.RestController;

import group6.project.service.CourseApplicationService;

@RestController
public class CourseApplicationController {

    private final CourseApplicationService courseApplicationService;

    public CourseApplicationController(CourseApplicationService courseApplicationService) {
        this.courseApplicationService = courseApplicationService;
    }
}
