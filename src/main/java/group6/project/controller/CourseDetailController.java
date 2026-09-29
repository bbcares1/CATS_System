package group6.project.controller;

import org.springframework.web.bind.annotation.RestController;

import group6.project.service.CourseDetailService;

@RestController
public class CourseDetailController {

    private final CourseDetailService courseDetailService;

    public CourseDetailController(CourseDetailService courseDetailService) {
        this.courseDetailService = courseDetailService;
    }
}
