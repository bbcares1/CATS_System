package group6.project.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import group6.project.model.Staff;
import group6.project.repo.UserRepo;

class UserServiceTest {
    // Reject an incorrect password without establishing a login session.
    @Test
    void authenticationRejectsWrongPasswords() {
        UserRepo repo = mock(UserRepo.class);
        Staff staff = new Staff();
        staff.setPassword("correct-password");
        when(repo.findByUserName("alex")).thenReturn(Optional.of(staff));
        UserService service = new UserService(repo);
        assertSame(staff, service.authenticate("alex", "correct-password"));
        assertNull(service.authenticate("alex", "wrong-password"));
    }

    // Missing credentials must fail without looking up another account.
    @Test
    void missingCredentialsAndUnknownAccountsAreRejected() {
        UserRepo repo = mock(UserRepo.class);
        UserService service = new UserService(repo);
        assertNull(service.authenticate(null, "password"));
        assertNull(service.authenticate("alex", null));
        verifyNoInteractions(repo);
        when(repo.findByUserName("missing")).thenReturn(Optional.empty());
        assertNull(service.authenticate("missing", "password"));
    }
}
