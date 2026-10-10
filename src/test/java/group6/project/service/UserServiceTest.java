package group6.project.service;

import group6.project.model.Manager;
import group6.project.model.Staff;
import group6.project.repo.UserRepo;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    // A trimmed username is accepted, but the password must match exactly.
    @Test
    void authenticatesSavedCredentials() {
        UserRepo repo = mock(UserRepo.class);
        Staff staff = new Staff();
        staff.setPassword("demo123");
        when(repo.findByUserName("staff")).thenReturn(Optional.of(staff));
        UserService service = new UserService(repo);
        assertSame(staff, service.authenticate(" staff ", "demo123"));
        assertNull(service.authenticate("staff", "wrong"));
        assertNull(service.authenticate(null, "demo123"));
    }

    // Removing an account also removes its saved session identity.
    @Test
    void deletedAccountCannotKeepItsSession() {
        UserRepo repo = mock(UserRepo.class);
        Staff staff = new Staff();
        staff.setUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", staff);
        when(repo.findById(1)).thenReturn(Optional.empty());
        assertNull(new UserService(repo).currentUser(session));
        assertNull(session.getAttribute("user"));
    }

    // Role checks follow the agreed Java inheritance.
    @Test
    void managerInheritsStaffAccess() {
        UserService service = new UserService(mock(UserRepo.class));
        assertTrue(service.isStaffOrManager(new Manager()));
        assertTrue(service.isManager(new Manager()));
        assertFalse(service.isAdmin(new Manager()));
    }
}
