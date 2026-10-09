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
    // Deleted accounts cannot keep using a previously valid session.
    @Test
    void sessionIdentityIsReloadedAndMissingAccountsAreRemoved() {
        UserRepo repo=mock(UserRepo.class);
        UserService service=new UserService(repo);
        var session=new org.springframework.mock.web.MockHttpSession();
        Staff stored=new Staff(); stored.setUserId(7); session.setAttribute("user",stored);
        var manager=new group6.project.model.Manager(); manager.setUserId(7);
        when(repo.findById(7)).thenReturn(Optional.of(manager),Optional.empty());
        assertSame(manager,service.currentUser(session));
        assertSame(manager,session.getAttribute("user"));
        assertNull(service.currentUser(session));
        assertNull(session.getAttribute("user"));
    }

    // Unexpected session values fail quietly rather than being cast to a User.
    @Test
    void invalidSessionValuesDoNotQueryAccounts() {
        UserRepo repo=mock(UserRepo.class);
        UserService service=new UserService(repo);
        var session=new org.springframework.mock.web.MockHttpSession(); session.setAttribute("user","alex");
        assertNull(service.currentUser(session)); assertNull(service.currentUser(null));
        verifyNoInteractions(repo);
    }

    // Even an accidental JSON response must not include the password property or value.
    @Test
    void userSerializationDoesNotExposePasswords() throws Exception {
        Staff staff=new Staff(); staff.setName("Alex"); staff.setPassword("private-password");
        String json=tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(staff);
        assertFalse(json.contains("password"));
        assertTrue(json.contains("Alex"));
    }
}
