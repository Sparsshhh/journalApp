package net.engineeringdigest.journalApp.config;

import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.service.UserService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class AdminSeederTest {

    private final UserService userService = mock(UserService.class);

    @Test
    void createsAdminWhenConfiguredAndMissing() {
        new AdminSeeder(userService, "root", "root-password").run(null);

        verify(userService).register("root", "root-password", UserService.ADMIN_ROLES);
    }

    @Test
    void leavesExistingUserUntouched() {
        when(userService.findByUserName("root")).thenReturn(new User("root", "existing-hash"));

        new AdminSeeder(userService, "root", "root-password").run(null);

        verify(userService, never()).register(any(), any(), any());
    }

    @Test
    void doesNothingWhenNotConfigured() {
        new AdminSeeder(userService, "", "").run(null);
        new AdminSeeder(userService, "root", "").run(null);

        verifyNoInteractions(userService);
    }
}
