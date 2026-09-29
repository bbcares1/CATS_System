package group6.project.controller;

import org.springframework.web.bind.annotation.RestController;

import group6.project.service.CourseFeeApplicationService;

@RestController
public class CourseFeeApplicationController {

    private final CourseFeeApplicationService courseFeeApplicationService;

    public CourseFeeApplicationController(CourseFeeApplicationService courseFeeApplicationService) {
        this.courseFeeApplicationService = courseFeeApplicationService;
    }
}
