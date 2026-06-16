package net.engineeringdigest.journalApp.controller;

import net.engineeringdigest.journalApp.entity.user;
import net.engineeringdigest.journalApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public")
public class PublicController {
    @Autowired
    UserService userService;
    @GetMapping("/health-check")
    public String healthCheck(){
        return "OK";
    }
    @PostMapping("/createUser")
    public void createUser(@RequestBody user user){
        userService.saveNewEntry(user);
    }
}
