package net.engineeringdigest.journalApp.Service;

import net.engineeringdigest.journalApp.entity.user;
import net.engineeringdigest.journalApp.repository.UserRepository;
import net.engineeringdigest.journalApp.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.*;

@ActiveProfiles("dev")
public class UserDetailsServiceImplTest {

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private UserRepository userRepository;
    @BeforeEach
    void setup(){
         MockitoAnnotations.initMocks(this);
    }
    @Test
    void loadUsernameTest(){
        when(userRepository.findByUserName(ArgumentMatchers.anyString())).thenReturn((user) User.builder().username("Sparsh").password("Sparsh").build());
        UserDetails user = userDetailsService.loadUserByUsername("Sparsh");
        Assertions.assertEquals("Sparsh",user.getUsername());
    }
}

