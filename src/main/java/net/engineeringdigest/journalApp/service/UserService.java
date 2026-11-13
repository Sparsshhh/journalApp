package net.engineeringdigest.journalApp.service;

import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.JournalEntryRepository;
import net.engineeringdigest.journalApp.repository.RefreshTokenRepository;
import net.engineeringdigest.journalApp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    public static final List<String> USER_ROLES = List.of("USER");
    public static final List<String> ADMIN_ROLES = List.of("USER", "ADMIN");

    private final UserRepository userRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, JournalEntryRepository journalEntryRepository,
                       RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Throws DuplicateKeyException (answered as 409) if the username is taken.
    public User register(String userName, String rawPassword, List<String> roles) {
        User user = new User(userName, passwordEncoder.encode(rawPassword));
        user.setRoles(roles);
        return userRepository.save(user);
    }

    public User findByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    // Changes the user's name and/or password; null or blank values keep the current one.
    public void updateCredentials(User user, String newUserName, String newPassword) {
        if (newUserName != null && !newUserName.trim().isEmpty()) {
            user.setUserName(newUserName);
        }
        boolean passwordChanged = newPassword != null && !newPassword.isEmpty();
        if (passwordChanged) {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setTokenVersion(user.getTokenVersion() + 1); // sign out everywhere
        }
        userRepository.save(user);
        if (passwordChanged) {
            refreshTokenRepository.deleteByUserId(user.getId());
        }
    }

    // Invalidates every access and refresh token the user holds, on all devices.
    public void logout(User user) {
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        refreshTokenRepository.deleteByUserId(user.getId());
    }

    public void deleteAccount(User user) {
        journalEntryRepository.deleteByUserId(user.getId());
        refreshTokenRepository.deleteByUserId(user.getId());
        userRepository.deleteById(user.getId());
    }
}
