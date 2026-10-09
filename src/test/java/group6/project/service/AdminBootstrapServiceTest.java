package group6.project.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import group6.project.config.AdminBootstrap;
import group6.project.model.*;
import group6.project.repo.UserRepo;

class AdminBootstrapServiceTest {
    // First install creates only an explicit Admin, using the chosen classroom password.
    @Test void firstInstallCreatesAnAdmin() {
        var users=mock(UserRepo.class);when(users.findByUserName("operator")).thenReturn(Optional.empty());
        new AdminBootstrap(users,"operator","chosen-password","Operator","A-INIT").run(null);
        var saved=ArgumentCaptor.forClass(User.class);verify(users).saveAndFlush(saved.capture());
        assertInstanceOf(Admin.class,saved.getValue());assertEquals("chosen-password",saved.getValue().getPassword());
    }
    // Existing production accounts are never replaced or reset on a restart.
    @Test void existingAdminSkipsProvisioning() {
        var users=mock(UserRepo.class);when(users.existsByRoleAndActiveTrue(Roles.ADMIN)).thenReturn(true);
        new AdminBootstrap(users,"","","","").run(null);verify(users,never()).saveAndFlush(any());
    }
    // A fresh deployment fails clearly instead of loading a known development login.
    @Test void missingFirstInstallCredentialsFail() {
        var users=mock(UserRepo.class);assertThrows(IllegalStateException.class,()->new AdminBootstrap(users,"","","Operator","A").run(null));
        verify(users,never()).saveAndFlush(any());
    }
    // A username already owned by Staff cannot become Admin through environment configuration.
    @Test void bootstrapDoesNotOverwriteAnExistingUsername() {
        var users=mock(UserRepo.class);when(users.findByUserName("operator")).thenReturn(Optional.of(new Staff()));
        assertThrows(IllegalStateException.class,()->new AdminBootstrap(users,"operator","password","Operator","A").run(null));
        verify(users,never()).saveAndFlush(any());
    }
}
