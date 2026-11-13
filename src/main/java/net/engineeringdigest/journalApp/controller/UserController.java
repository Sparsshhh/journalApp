package net.engineeringdigest.journalApp.controller;

import jakarta.validation.Valid;
import net.engineeringdigest.journalApp.dto.UserResponse;
import net.engineeringdigest.journalApp.dto.UserUpdateRequest;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Everything here acts on the logged-in user only.
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse getCurrentUser(@AuthenticationPrincipal User user) {
        return UserResponse.from(user);
    }

    // "/user/{userName}" is the original route and still works; the name in the path is ignored.
    @PutMapping({"", "/{userName}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUser(@AuthenticationPrincipal User user, @Valid @RequestBody UserUpdateRequest request){
        userService.updateCredentials(user, request.getUserName(), request.getPassword());
    }

    // Signs the user out everywhere: all access and refresh tokens stop working.
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal User user){
        userService.logout(user);
    }

    // Deletes the account together with its journal entries.
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@AuthenticationPrincipal User user){
        userService.deleteAccount(user);
    }
}
