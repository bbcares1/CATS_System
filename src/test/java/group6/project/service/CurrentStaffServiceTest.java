package group6.project.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;
import group6.project.model.Admin;
import group6.project.model.Manager;
import group6.project.repo.StaffRepo;

class CurrentStaffServiceTest {
    // Managers inherit Staff capabilities and use the agreed session key.
    @Test
    void managerIsReloadedFromTheUserSession() {
        StaffRepo repo = mock(StaffRepo.class);
        Manager manager = new Manager();
        manager.setUserId(7);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", manager);
        when(repo.findById(7)).thenReturn(Optional.of(manager));
        assertSame(manager, new CurrentStaffService(repo).requireStaff(null, session));
    }

    // An Admin or a deleted employee cannot reuse the Staff workspace.
    @Test
    void missingAdminAndDeletedSessionsAreRejected() {
        StaffRepo repo = mock(StaffRepo.class);
        CurrentStaffService service = new CurrentStaffService(repo);
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> service.requireStaff(null, null)).getStatusCode().value());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", new Admin());
        assertThrows(ResponseStatusException.class, () -> service.requireStaff(null, session));
        Manager deleted = new Manager();
        deleted.setUserId(7);
        session.setAttribute("user", deleted);
        when(repo.findById(7)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.requireStaff(null, session));
    }
}
