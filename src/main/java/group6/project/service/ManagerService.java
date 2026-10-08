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

    private final CourseApplicationService courseApplicationService;
    private final StaffService staffService;

    public ManagerService(ManagerRepo managerRepo, CourseApplicationService courseApplicationService,
            StaffService staffService) {
        this.courseApplicationService = courseApplicationService;
        this.staffService = staffService;
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

    public Manager getManagerByStaffNo(String staffNo) {
        return managerRepo.findByStaffNo(staffNo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Manager not found with staffNo: " + staffNo));
    }

    public Manager requireManager(java.security.Principal principal, jakarta.servlet.http.HttpSession session) {
        String username = principal == null ? null : principal.getName();
        if (principal == null && session != null) {
            Object user = session.getAttribute("currentUser");
            if (user == null) user = session.getAttribute("loggedInUser");
            if (user instanceof group6.project.model.User account && account.getUserId() != null) {
                return managerRepo.findById(account.getUserId()).orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in as a manager."));
            }
            if (user instanceof String name) username = name;
        }
        if (username == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in as a manager.");
        return managerRepo.findByUserName(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in as a manager."));
    }

    public List<group6.project.model.CourseApplication> pendingApplications(Manager manager) {
        return courseApplicationService.pendingForManager(manager);
    }

    public group6.project.model.CourseApplication approveCourseApplication(Integer id, Manager manager, String reason) {
        return courseApplicationService.review(id, manager, true, reason);
    }

    public group6.project.model.CourseApplication rejectCourseApplication(Integer id, Manager manager, String reason) {
        return courseApplicationService.review(id, manager, false, reason);
    }

    public List<group6.project.model.CourseApplication> employeeCourseHistory(Integer staffId, Manager manager) {
        return courseApplicationService.employeeCourseHistory(staffService.getStaff(staffId), manager);
    }

    public void approveFeeClaim() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
