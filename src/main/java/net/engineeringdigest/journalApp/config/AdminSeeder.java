package net.engineeringdigest.journalApp.config;

import lombok.extern.slf4j.Slf4j;
import net.engineeringdigest.journalApp.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// Creates the first admin at startup, since /admin/create-admin-users itself requires an admin.
// Set admin.username and admin.password (or the ADMIN_USERNAME and ADMIN_PASSWORD environment variables).
@Component
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    private final UserService userService;
    private final String adminUserName;
    private final String adminPassword;

    public AdminSeeder(UserService userService,
                       @Value("${admin.username:}") String adminUserName,
                       @Value("${admin.password:}") String adminPassword) {
        this.userService = userService;
        this.adminUserName = adminUserName;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminUserName.trim().isEmpty() || adminPassword.isEmpty()) {
            return; // not configured: nothing to seed
        }
        if (userService.findByUserName(adminUserName) != null) {
            // Never touch an existing account: no password reset and no silent promotion to admin.
            log.info("Admin seed skipped: user '{}' already exists", adminUserName);
            return;
        }
        userService.register(adminUserName, adminPassword, UserService.ADMIN_ROLES);
        log.info("Seeded admin user '{}'", adminUserName);
    }
}
