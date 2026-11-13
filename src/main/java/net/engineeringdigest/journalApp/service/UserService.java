package net.engineeringdigest.journalApp.service;

import lombok.extern.slf4j.Slf4j;
import net.engineeringdigest.journalApp.entity.user;
import net.engineeringdigest.journalApp.repository.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    public boolean saveNewEntry(user user) {
        userRepository.save(newUserFrom(user, Arrays.asList("USER")));
        return true;
    }


    public void saveAdmin(user user) {
        userRepository.save(newUserFrom(user, Arrays.asList("USER", "ADMIN")));
    }

    // Copies only userName and password from the request body. A client-supplied id would
    // overwrite an existing account, and client-supplied journalEntries would expose other
    // users' entries, so neither is carried over.
    private user newUserFrom(user request, List<String> roles) {
        user created = new user(request.getUserName(), passwordEncoder.encode(request.getPassword()));
        created.setRoles(roles);
        return created;
    }

    // Changes the logged-in user's name and/or password; blank values keep the current one.
    public void updateCredentials(String currentUserName, String newUserName, String newPassword) {
        user userInDb = userRepository.findByUserName(currentUserName);
        if (newUserName != null && !newUserName.trim().isEmpty()) {
            userInDb.setUserName(newUserName);
        }
        if (newPassword != null && !newPassword.isEmpty()) {
            userInDb.setPassword(passwordEncoder.encode(newPassword));
        }
        userRepository.save(userInDb);
    }


    public void saveUser(user user) {
        userRepository.save(user);
    }


    public void saveNewUser(user user) {
        userRepository.save(user);
    }


    public List<user> getAll() {
        return userRepository.findAll();
    }


    public Optional<user> findById(ObjectId id) {
        return userRepository.findById(id);
    }


    public void deleteById(ObjectId id) {
        userRepository.deleteById(id);
    }


    public user findByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }
}