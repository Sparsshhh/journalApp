package net.engineeringdigest.journalApp.controller;

import jakarta.validation.Valid;
import net.engineeringdigest.journalApp.dto.UserRequest;
import net.engineeringdigest.journalApp.dto.UserResponse;
import net.engineeringdigest.journalApp.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/allUsers")
    public List<UserResponse> getAllUsers(){
        return userService.getAll().stream().map(UserResponse::from).toList();
    }

    @PostMapping("/create-admin-users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserRequest request){
        return UserResponse.from(userService.register(request.getUserName(), request.getPassword(), UserService.ADMIN_ROLES));
    }
}
