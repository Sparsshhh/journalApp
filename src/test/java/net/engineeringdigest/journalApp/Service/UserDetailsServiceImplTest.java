package net.engineeringdigest.journalApp.Service;

import net.engineeringdigest.journalApp.entity.user;
import net.engineeringdigest.journalApp.repository.UserRepository;
import net.engineeringdigest.journalApp.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;

import static org.mockito.Mockito.when;
@Disabled
public class UserDetailsServiceImplTest {

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
    }
@Disabled
    @Test
    void loadUsernameTest() {

        // Create your own user entity
        user myUser = new user("Sparsh", "Sparsh");
        myUser.setRoles(Arrays.asList("USER"));

        // Mock repository response
        when(userRepository.findByUserName(ArgumentMatchers.anyString()))
                .thenReturn(myUser);

        // Call service method
        UserDetails userDetails = userDetailsService.loadUserByUsername("Sparsh");

        // Verify result
        Assertions.assertEquals("Sparsh", userDetails.getUsername());
    }
}