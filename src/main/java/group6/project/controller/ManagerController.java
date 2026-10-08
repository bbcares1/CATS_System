package group6.project.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import group6.project.model.Manager;
import group6.project.service.ManagerService;

@RestController
@RequestMapping("/api/managers")
public class ManagerController {

    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @GetMapping
    public List<Manager> getAllManagers() {
        return managerService.getAllManagers();
    }

    @GetMapping("/{id}")
    public Manager getManager(@PathVariable Integer id) {
        return managerService.getManager(id);
    }

    @GetMapping("/staff-no/{staffNo}")
    public Manager getManagerByStaffNo(@PathVariable String staffNo) {
        return managerService.getManagerByStaffNo(staffNo);
    }
    @GetMapping("/me/course-applications")
    public List<group6.project.model.CourseApplication> pendingApplications(
            java.security.Principal principal, jakarta.servlet.http.HttpSession session) {
        return managerService.pendingApplications(managerService.requireManager(principal, session));
    }

    @org.springframework.web.bind.annotation.PostMapping("/me/course-applications/{id}/approve")
    public group6.project.model.CourseApplication approve(@PathVariable Integer id,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String reason,
            java.security.Principal principal, jakarta.servlet.http.HttpSession session) {
        return managerService.approveCourseApplication(id, managerService.requireManager(principal, session), reason);
    }

    @org.springframework.web.bind.annotation.PostMapping("/me/course-applications/{id}/reject")
    public group6.project.model.CourseApplication reject(@PathVariable Integer id,
            @org.springframework.web.bind.annotation.RequestParam String reason,
            java.security.Principal principal, jakarta.servlet.http.HttpSession session) {
        return managerService.rejectCourseApplication(id, managerService.requireManager(principal, session), reason);
    }

    @GetMapping("/me/staff/{staffId}/course-applications")
    public List<group6.project.model.CourseApplication> employeeCourseHistory(@PathVariable Integer staffId,
            java.security.Principal principal, jakarta.servlet.http.HttpSession session) {
        return managerService.employeeCourseHistory(staffId, managerService.requireManager(principal, session));
    }
}
