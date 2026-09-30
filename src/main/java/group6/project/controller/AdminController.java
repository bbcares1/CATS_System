package group6.project.controller;

import group6.project.service.AdminService;

public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService){
         this.adminService = adminService;
    }
}
