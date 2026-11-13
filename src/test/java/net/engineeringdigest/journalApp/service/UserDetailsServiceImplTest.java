package net.engineeringdigest.journalApp.service;

import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserDetailsServiceImplTest {

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private UserRepository userRepository;

    @Test
    void loadsUserWithRoles() {
        User myUser = new User("Sparsh", "hash");
        myUser.setRoles(List.of("USER", "ADMIN"));
        when(userRepository.findByUserName("Sparsh")).thenReturn(myUser);

        UserDetails userDetails = userDetailsService.loadUserByUsername("Sparsh");

        assertEquals("Sparsh", userDetails.getUsername());
        assertEquals(2, userDetails.getAuthorities().size());
    }

    @Test
    void userWithoutRolesLoadsWithNoAuthorities() {
        User myUser = new User("Sparsh", "hash");
        myUser.setRoles(null);
        when(userRepository.findByUserName("Sparsh")).thenReturn(myUser);

        assertTrue(userDetailsService.loadUserByUsername("Sparsh").getAuthorities().isEmpty());
    }

    @Test
    void unknownUserIsRejected() {
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("nobody"));
    }
}
