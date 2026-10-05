package group6.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        manager.setStaffNo("M001");
        when(managerRepo.findById(1)).thenReturn(Optional.of(manager));

        assertEquals("M001", managerService.getManager(1).getStaffNo());
    }

    @Test
    void getManagerThatDoesNotExistGivesNotFound() {
        when(managerRepo.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> managerService.getManager(99));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void getManagerByUnknownStaffNoGivesNotFound() {
        when(managerRepo.findByStaffNo("X999")).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> managerService.getManagerByStaffNo("X999"));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }
}
