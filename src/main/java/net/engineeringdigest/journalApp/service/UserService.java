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
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setRoles(Arrays.asList("USER"));
            userRepository.save(user);
            return true;

        } catch (Exception e) {
            throw e;
        }
    }


    public void saveAdmin(user user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(Arrays.asList("USER", "ADMIN"));
        userRepository.save(user);
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