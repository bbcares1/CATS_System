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

    @GetMapping("/staff-id/{staffId}")
    public Manager getManagerByStaffId(@PathVariable String staffId) {
        return managerService.getManagerByStaffId(staffId);
    }
}
