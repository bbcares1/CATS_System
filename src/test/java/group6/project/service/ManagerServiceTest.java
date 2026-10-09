package group6.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import group6.project.model.ApplicationStatus;
import group6.project.model.CourseApplication;
import group6.project.model.CourseCategoryType;
import group6.project.model.Manager;
import group6.project.model.Staff;
import group6.project.repo.CourseApplicationRepo;
import group6.project.repo.ManagerRepo;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock
    private ManagerRepo managerRepo;

    @Mock
    private CourseApplicationRepo courseApplicationRepo;

    @InjectMocks
    private ManagerService managerService;

    @Test
    void getManagerReturnsTheManager() {
        Manager manager = new Manager();
        manager.setUserId(1);
        manager.setStaffId("M001");
        when(managerRepo.findById(1)).thenReturn(Optional.of(manager));

        Staff staff = assertInstanceOf(Staff.class, managerService.getManager(1));
        assertEquals("M001", staff.getStaffId());
    }

    @Test
    void getManagerThatDoesNotExistGivesNotFound() {
        when(managerRepo.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> managerService.getManager(99));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void getManagerByStaffIdReturnsTheManager() {
        Manager manager = new Manager();
        manager.setStaffId("M001");
        when(managerRepo.findByStaffId("M001")).thenReturn(Optional.of(manager));

        assertEquals(manager, managerService.getManagerByStaffId("M001"));
    }

    @Test
    void getManagerByUnknownStaffIdGivesNotFound() {
        when(managerRepo.findByStaffId("X999")).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> managerService.getManagerByStaffId("X999"));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void pendingApplicationsGroupByEmployeeIdEvenWhenNamesMatch() {
        Manager manager = manager();
        CourseApplication first = application(10, 2, "S002", ApplicationStatus.APPLIED);
        CourseApplication updated = application(11, 2, "S002", ApplicationStatus.UPDATED);
        CourseApplication namesake = application(12, 3, "S003", ApplicationStatus.APPLIED);
        when(managerRepo.findById(1)).thenReturn(Optional.of(manager));
        when(courseApplicationRepo.findPendingForManager(1,
                List.of(ApplicationStatus.APPLIED, ApplicationStatus.UPDATED)))
                .thenReturn(List.of(first, updated, namesake));

        var groups = managerService.getPendingApplicationGroups(1);

        assertEquals(2, groups.size());
        assertEquals(2, groups.getFirst().employeeId());
        assertEquals("S002", groups.getFirst().staffId());
        assertEquals(List.of(10, 11), groups.getFirst().applications().stream()
                .map(ManagerService.ApplicationView::applicationId).toList());
        assertEquals(3, groups.get(1).employeeId());
        assertEquals("S003", groups.get(1).staffId());
        assertEquals("Alex", groups.getFirst().employeeName());
        assertEquals("Alex", groups.get(1).employeeName());
        verify(courseApplicationRepo, never()).save(any());
    }

    @Test
    void managerWithNoPendingApplicationsGetsAnEmptyList() {
        when(managerRepo.findById(1)).thenReturn(Optional.of(manager()));
        when(courseApplicationRepo.findPendingForManager(1,
                List.of(ApplicationStatus.APPLIED, ApplicationStatus.UPDATED)))
                .thenReturn(List.of());

        assertEquals(List.of(), managerService.getPendingApplicationGroups(1));
    }

    @Test
    void unknownManagerCannotQueryApplications() {
        when(managerRepo.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> managerService.getPendingApplicationGroups(99));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(courseApplicationRepo);
    }

    @Test
    void detailsKeepTheApplicationAndDecisionFieldsWithoutWriting() {
        CourseApplication application = application(10, 2, "S002", ApplicationStatus.REJECTED);
        application.setReviewedAt(LocalDateTime.of(2026, 10, 9, 11, 0));
        application.setDecisionReason("Conflicts with the project deadline.");
        application.setExperienceComments("Previous learning was useful.");
        when(managerRepo.findById(1)).thenReturn(Optional.of(manager()));
        when(courseApplicationRepo.findForManager(10, 1)).thenReturn(Optional.of(application));

        var detail = managerService.getApplicationForManager(1, 10);

        assertEquals("Alex", detail.applicantName());
        assertEquals("S002", detail.staffId());
        assertEquals("Java architecture", detail.title());
        assertEquals(CourseCategoryType.EXTERNAL_COURSE, detail.category());
        assertEquals("NUS-ISS", detail.provider());
        assertEquals(LocalDate.of(2026, 11, 12), detail.startDate());
        assertEquals(LocalDate.of(2026, 11, 13), detail.endDate());
        assertEquals(2.0, detail.trainingDays());
        assertEquals(0, new java.math.BigDecimal("1800").compareTo(detail.fee()));
        assertEquals("Improve our system design.", detail.justification());
        assertEquals("Share the learning with the team.", detail.workDissemination());
        assertEquals(ApplicationStatus.REJECTED, detail.status());
        assertEquals(application.getReviewedAt(), detail.reviewedAt());
        assertEquals(application.getDecisionReason(), detail.decisionReason());
        assertEquals(application.getExperienceComments(), detail.experienceComments());
        verify(courseApplicationRepo, never()).save(any());
    }

    @Test
    void missingOrOtherTeamApplicationGivesNotFound() {
        when(managerRepo.findById(1)).thenReturn(Optional.of(manager()));
        when(courseApplicationRepo.findForManager(99, 1)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> managerService.getApplicationForManager(1, 99));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    private Manager manager() {
        Manager manager = new Manager();
        manager.setUserId(1);
        return manager;
    }

    private CourseApplication application(Integer id, Integer employeeId, String staffId,
            ApplicationStatus status) {
        Staff employee = new Staff();
        employee.setUserId(employeeId);
        employee.setName("Alex");
        employee.setStaffId(staffId);
        employee.setPassword("not-for-the-view");
        CourseApplication application = new CourseApplication();
        application.setCourseId(id);
        application.setApplicant(employee);
        application.setCourseTitle("Java architecture");
        application.setCourseCategory(CourseCategoryType.EXTERNAL_COURSE);
        application.setTrainingProvider("NUS-ISS");
        application.setCourseStartDate(LocalDate.of(2026, 11, 12));
        application.setCourseEndDate(LocalDate.of(2026, 11, 13));
        application.setTrainingDays(2.0);
        application.setCourseFee(new java.math.BigDecimal("1800.0"));
        application.setJustification("Improve our system design.");
        application.setWorkDissemination("Share the learning with the team.");
        application.setStatus(status);
        application.setSubmittedAt(LocalDateTime.of(2026, 10, 9, 10, 0));
        return application;
    }
}
