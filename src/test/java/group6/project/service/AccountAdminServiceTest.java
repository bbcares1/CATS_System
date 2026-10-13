// We check invalid account input before it can change saved accounts.
package group6.project.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import group6.project.form.AccountForm;
import group6.project.model.*;
import group6.project.repo.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

class AccountAdminServiceTest {
    UserRepo users = mock(UserRepo.class);
    AccountAdminService accounts =
            new AccountAdminService(
                    users,
                    mock(CourseApplicationRepo.class),
                    mock(CourseFeeApplicationRepo.class),
                    mock(TrainingEntitlementRepo.class));
    Admin actor;

    // Every write starts with a saved active Admin, not a role selected in a browser.
    @BeforeEach
    void prepare() {
        actor = new Admin();
        actor.setUserId(1);
        actor.setRole(Roles.ADMIN);
        actor.setVersion(0L);
        when(users.lockAccounts()).thenReturn(List.of(actor));
    }

    // A new Manager must be an actual Manager entity and inherit Staff capabilities.
    @Test
    void creationSelectsTheMatchingSubtypeAndKeepsSimpleCredentials() {
        when(users.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));
        User saved = accounts.save(null, form(), 1);
        assertInstanceOf(Manager.class, saved);
        assertInstanceOf(Staff.class, saved);
        assertEquals("new_manager", saved.getUserName());
        assertEquals("demo123", saved.getPassword());
    }

    // Protect the current Admin before changing any identity or password fields.
    @Test
    void actingAdminCannotRemoveTheirOwnAccess() {
        AccountForm form = form();
        form.setVersion(0L);
        form.setRole(Roles.STAFF);
        assertThrows(ResponseStatusException.class, () -> accounts.save(1, form, 1));
        verify(users, never()).saveAndFlush(any());
        verify(users, never()).changeRole(anyInt(), anyString());
    }

    // Duplicate usernames are validation failures rather than partial account inserts.
    @Test
    void duplicateUsernamesDoNotSaveAnAccount() {
        when(users.existsByUserNameIgnoreCaseAndUserIdNot("new_manager", -1)).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> accounts.save(null, form(), 1));
        verify(users, never()).saveAndFlush(any());
    }

    // Versions stop an older form from overwriting a profile edited by someone else.
    @Test
    void staleProfileEditsDoNotWrite() {
        Staff employee = new Staff();
        employee.setUserId(2);
        employee.setVersion(3L);
        employee.setRole(Roles.STAFF);
        when(users.lockAccounts()).thenReturn(List.of(actor, employee));
        AccountForm form = form();
        form.setVersion(2L);
        ResponseStatusException error =
                assertThrows(ResponseStatusException.class, () -> accounts.save(2, form, 1));
        assertEquals(409, error.getStatusCode().value());
        verify(users, never()).saveAndFlush(any());
    }

    // The input contains profile fields only; annual training limits have a separate form.
    private AccountForm form() {
        AccountForm form = new AccountForm();
        form.setUserName("New_Manager");
        form.setName("New manager");
        form.setStaffId("M-NEW");
        form.setEmail("manager@example.test");
        form.setPassword("demo123");
        form.setRole(Roles.MANAGER);
        return form;
    }
}
