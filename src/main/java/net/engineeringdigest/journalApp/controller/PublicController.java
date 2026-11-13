package net.engineeringdigest.journalApp.controller;

import jakarta.validation.Valid;
import net.engineeringdigest.journalApp.dto.LoginRequest;
import net.engineeringdigest.journalApp.dto.RefreshRequest;
import net.engineeringdigest.journalApp.dto.TokenResponse;
import net.engineeringdigest.journalApp.dto.UserRequest;
import net.engineeringdigest.journalApp.dto.UserResponse;
import net.engineeringdigest.journalApp.service.AuthService;
import net.engineeringdigest.journalApp.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public")
public class PublicController {

    private final UserService userService;
    private final AuthService authService;

    public PublicController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping("/health-check")
    public String healthCheck(){
        return "OK";
    }

    @PostMapping("/createUser")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserRequest request){
        return UserResponse.from(userService.register(request.getUserName(), request.getPassword(), UserService.USER_ROLES));
    }

    // Exchanges a username and password for an access token and a refresh token.
    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request){
        return authService.login(request.getUserName(), request.getPassword());
    }

    // Exchanges a refresh token for a new access token and a new refresh token.
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request){
        return authService.refresh(request.getRefreshToken());
    }
}
