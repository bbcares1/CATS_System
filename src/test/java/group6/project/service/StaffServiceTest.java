package group6.project.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import group6.project.model.Staff;
import group6.project.repo.StaffRepo;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

class StaffServiceTest {
    private final StaffRepo repo = mock(StaffRepo.class);
    private final StaffService service = new StaffService(repo);

    @Test
    void principalTakesPrecedenceOverSession() {
        Staff staff = new Staff();
        when(repo.findByUserName("employee")).thenReturn(Optional.of(staff));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", "other");
        assertSame(staff, service.requireStaff(() -> "employee", session));
        verify(repo, never()).findByUserName("other");
    }

    @Test
    void sessionAccountIsReloadedRatherThanTrustingStaleStaff() {
        Staff stale = new Staff();
        stale.setUserId(7);
        Staff current = new Staff();
        when(repo.findById(7)).thenReturn(Optional.of(current));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", stale);
        assertSame(current, service.requireStaff(null, session));
    }

    @Test
    void missingIdentityIsUnauthorized() {
        var error = assertThrows(ResponseStatusException.class, () -> service.requireStaff(null, null));
        assertEquals(401, error.getStatusCode().value());
    }
}
