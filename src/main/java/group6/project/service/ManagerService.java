package group6.project.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import group6.project.model.Manager;
import group6.project.repo.ManagerRepo;

@Service
public class ManagerService {

    private final ManagerRepo managerRepo;

    public ManagerService(ManagerRepo managerRepo) {
        this.managerRepo = managerRepo;
    }

    public List<Manager> getAllManagers() {
        return managerRepo.findAll();
    }

    public Manager getManager(Integer id) {
        return managerRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Manager not found with id: " + id));
    }

    public Manager getManagerByStaffId(String staffId) {
        return managerRepo.findByStaffId(staffId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Manager not found with staffId: " + staffId));
    }

    // The methods below come from the class diagram.
    // They will be filled in once CourseApplication and CourseFeeApplication are ready.

    public String approveCourseApplication() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String rejectCourseApplication() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void employeeCourseHistory() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void approveFeeClaim() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
