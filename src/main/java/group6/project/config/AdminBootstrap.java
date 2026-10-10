package group6.project.config;

import group6.project.model.*;
import group6.project.repo.UserRepo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("prod")
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepo users;
    private final String userName, password, name, staffId, email;

    // First-install credentials come from the environment, never from development seeds.
    public AdminBootstrap(
            UserRepo users,
            @Value("${CATS_ADMIN_USER:}") String userName,
            @Value("${CATS_ADMIN_PASSWORD:}") String password,
            @Value("${CATS_ADMIN_NAME:CATS Administrator}") String name,
            @Value("${CATS_ADMIN_STAFF_ID:ADMIN}") String staffId,
            @Value("${CATS_ADMIN_EMAIL:}") String email) {
        this.users = users;
        this.userName = userName;
        this.password = password;
        this.name = name;
        this.staffId = staffId;
        this.email = email;
    }

    // Once an active Admin exists, a restart must not reset their password or identity.
    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (users.existsByRoleAndActiveTrue(Roles.ADMIN)) return;
        if (userName.isBlank()
                || password.isBlank()
                || name.isBlank()
                || staffId.isBlank()
                || email.isBlank()
                || userName.length() > 255
                || password.length() > 255
                || name.length() > 255
                || staffId.length() > 255) {
            throw new IllegalStateException(
                    "First production start needs CATS_ADMIN_USER, CATS_ADMIN_PASSWORD,"
                        + " CATS_ADMIN_NAME, CATS_ADMIN_STAFF_ID and CATS_ADMIN_EMAIL.");
        }
        if (users.findByUserName(userName.trim().toLowerCase(java.util.Locale.ROOT)).isPresent())
            throw new IllegalStateException(
                    "The bootstrap username already belongs to another account. Choose a new"
                        + " CATS_ADMIN_USER.");
        var admin = new Admin();
        admin.setUserName(userName.trim().toLowerCase(java.util.Locale.ROOT));
        admin.setPassword(password);
        admin.setName(name.trim());
        admin.setStaffId(staffId.trim());
        admin.setEmail(email.trim());
        users.saveAndFlush(admin);
    }
}
