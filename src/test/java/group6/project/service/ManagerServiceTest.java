package group6.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import group6.project.model.Manager;
import group6.project.model.Staff;
import group6.project.repo.ManagerRepo;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock
    private ManagerRepo managerRepo;

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
}
