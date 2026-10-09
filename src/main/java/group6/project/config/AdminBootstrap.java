package group6.project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import group6.project.model.*;
import group6.project.repo.UserRepo;

@Component @Profile("prod")
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepo users;
    private final String userName, password, name, staffId;

    // First-install credentials come from the environment, never from development seeds.
    public AdminBootstrap(UserRepo users,@Value("${CATS_ADMIN_USER:}") String userName,
            @Value("${CATS_ADMIN_PASSWORD:}") String password,@Value("${CATS_ADMIN_NAME:CATS Administrator}") String name,
            @Value("${CATS_ADMIN_STAFF_ID:ADMIN}") String staffId) {
        this.users=users;this.userName=userName;this.password=password;this.name=name;this.staffId=staffId;
    }

    // Once an active Admin exists, a restart must not reset their password or identity.
    @Override @Transactional
    public void run(ApplicationArguments arguments) {
        if(users.existsByRoleAndActiveTrue(Roles.ADMIN)) return;
        if(userName.isBlank() || password.isBlank() || name.isBlank() || staffId.isBlank()
                || userName.length()>255 || password.length()>255 || name.length()>255 || staffId.length()>255) {
            throw new IllegalStateException("First production start needs CATS_ADMIN_USER, CATS_ADMIN_PASSWORD, CATS_ADMIN_NAME and CATS_ADMIN_STAFF_ID.");
        }
        if(users.findByUserName(userName.trim()).isPresent()) throw new IllegalStateException("The bootstrap username already belongs to another account. Choose a new CATS_ADMIN_USER.");
        var admin=new Admin();admin.setUserName(userName.trim());admin.setPassword(password);admin.setName(name.trim());admin.setStaffId(staffId.trim());
        users.saveAndFlush(admin);
    }
}
