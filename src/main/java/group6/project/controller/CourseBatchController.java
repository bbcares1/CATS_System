package group6.project.controller;

import org.springframework.web.bind.annotation.RestController;

import group6.project.service.CourseBatchService;

@RestController
public class CourseBatchController {

    private final CourseBatchService courseBatchService;

    public CourseBatchController(CourseBatchService courseBatchService) {
        this.courseBatchService = courseBatchService;
    }
}
