package net.engineeringdigest.journalApp.service;

import lombok.extern.slf4j.Slf4j;
import net.engineeringdigest.journalApp.repository.UserRepository;
import net.engineeringdigest.journalApp.entity.user;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class UserService {

    @Autowired
    private UserRepository UserRepository;

    private static final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean saveNewEntry(user user){
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setRoles(Arrays.asList("USER"));
            UserRepository.save(user);
            return true;
        }catch (Exception e){
            logger.info("Error occurred for {} :",user.getUserName(), e);
            log.info("using slf4j instance");
            return false;
        }
    }

    public void saveAdmin(user user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(Arrays.asList("USER", "ADMIN"));
        UserRepository.save(user);
    }

    public void saveUser(user user){
        UserRepository.save(user);
    }

    public void saveNewUser(user user){
       UserRepository.save(user);
    }

    public List<user> getAll(){
        return UserRepository.findAll();
    }

    public Optional<user> findById(ObjectId id){
        return UserRepository.findById(id);
    }

    public void deleteById(ObjectId id){
        UserRepository.deleteById(id);
    }

    public user findByUserName(String userName){
        return UserRepository.findByUserName(userName);
    }
}

// controller ---> service ----> repository