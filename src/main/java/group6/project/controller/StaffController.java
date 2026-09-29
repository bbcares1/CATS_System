package group6.project.controller;

import org.springframework.web.bind.annotation.RestController;

import group6.project.service.StaffService;

@RestController
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }
}
