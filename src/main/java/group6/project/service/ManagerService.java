package group6.project.service;

import group6.project.model.ApplicationStatus;
import group6.project.model.CourseApplication;
import group6.project.model.Manager;
import group6.project.model.Staff;
import group6.project.model.User;
import group6.project.repo.CourseApplicationRepo;
import group6.project.repo.ManagerRepo;
import group6.project.repo.StaffRepo;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ManagerService {

    private final ManagerRepo managerRepo;
    private final CourseApplicationRepo courseApplicationRepo;
    private final StaffRepo employees;

    public ManagerService(
            ManagerRepo managerRepo,
            CourseApplicationRepo courseApplicationRepo,
            StaffRepo employees) {
        this.employees = employees;
        this.managerRepo = managerRepo;
        this.courseApplicationRepo = courseApplicationRepo;
    }

    public List<Manager> getAllManagers() {
        return managerRepo.findAll();
    }

    public Manager getManager(Integer id) {
        return managerRepo
                .findById(id)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Manager not found with id: " + id));
    }

    public Manager getManagerByStaffId(String staffId) {
        return managerRepo
                .findByStaffId(staffId)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Manager not found with staffId: " + staffId));
    }

    // Group by database ID, so employees with the same name are not combined.
    @Transactional(readOnly = true)
    public List<ApplicationGroup> getPendingApplicationGroups(Integer managerId) {
        getManager(managerId);
        Map<Integer, List<CourseApplication>> groups = new LinkedHashMap<>();
        for (CourseApplication application :
                courseApplicationRepo.findPendingForManager(
                        managerId, List.of(ApplicationStatus.APPLIED, ApplicationStatus.UPDATED))) {
            groups.computeIfAbsent(application.getApplicant().getUserId(), key -> new ArrayList<>())
                    .add(application);
        }
        List<ApplicationGroup> result = new ArrayList<>();
        for (List<CourseApplication> rows : groups.values()) {
            User employee = rows.getFirst().getApplicant();
            result.add(
                    new ApplicationGroup(
                            employee.getUserId(), employee.getName(), employee.getStaffId(), rows));
        }
        return result;
    }

    // Assigned reviewers and the current reporting Manager may read the saved application.
    @Transactional(readOnly = true)
    public CourseApplication getApplicationForManager(Integer managerId, Integer applicationId) {
        getManager(managerId);
        return courseApplicationRepo
                .findForManager(applicationId, managerId)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Course application not found."));
    }

    // Include direct reports only; a Manager's own history belongs in the Staff workspace.
    @Transactional(readOnly = true)
    public List<Staff> team(Integer managerId) {
        getManager(managerId);
        return employees.findByManager_UserId(managerId).stream()
                .filter(employee -> !employee.getUserId().equals(managerId))
                .toList();
    }

    // The mandatory team history shows all states in the current year.
    @Transactional(readOnly = true)
    public List<CourseApplication> history(Integer managerId, Integer employeeId) {
        if (team(managerId).stream()
                .noneMatch(employee -> employee.getUserId().equals(employeeId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team member not found.");
        }
        int year = LocalDate.now().getYear();
        return courseApplicationRepo
                .findByApplicant_UserIdAndCourseStartDateBetweenOrderByCourseStartDateAsc(
                        employeeId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    // Show approved absences of other team members during the requested period.
    @Transactional(readOnly = true)
    public List<CourseApplication> overlaps(Integer managerId, CourseApplication selected) {
        List<CourseApplication> result = new ArrayList<>();
        for (Staff employee : team(managerId)) {
            if (employee.getUserId().equals(selected.getApplicant().getUserId())) continue;
            for (CourseApplication other :
                    courseApplicationRepo.findByApplicant_UserIdAndStatusIn(
                            employee.getUserId(), List.of(ApplicationStatus.APPROVED))) {
                if (CourseApplicationService.overlaps(selected, other)) result.add(other);
            }
        }
        return result;
    }

    public record ApplicationGroup(
            Integer employeeId,
            String employeeName,
            String staffId,
            List<CourseApplication> applications) {}
}
