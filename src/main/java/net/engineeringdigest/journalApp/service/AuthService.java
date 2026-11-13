package net.engineeringdigest.journalApp.service;

import net.engineeringdigest.journalApp.dto.TokenResponse;
import net.engineeringdigest.journalApp.entity.RefreshToken;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.RefreshTokenRepository;
import net.engineeringdigest.journalApp.repository.UserRepository;
import net.engineeringdigest.journalApp.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final Duration refreshTtl;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       @Value("${jwt.refresh-expiration-days:7}") long refreshDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTtl = Duration.ofDays(refreshDays);
    }

    public TokenResponse login(String userName, String password) {
        User user = userName == null ? null : userRepository.findByUserName(userName);
        // Same answer for an unknown user and a wrong password, so the response does not reveal which usernames exist.
        if (user == null || password == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect username or password");
        }
        return issueTokens(user);
    }

    // Each refresh token works once: using it returns a new pair and retires the old token.
    public TokenResponse refresh(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash).orElse(null);
        if (stored == null || stored.getExpiresAt().isBefore(Instant.now())
                || refreshTokenRepository.deleteByTokenHash(hash) == 0) { // 0: another request used it first
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"));
        return issueTokens(user);
    }

    private TokenResponse issueTokens(User user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokenRepository.save(new RefreshToken(sha256(refreshToken), user.getId(), Instant.now().plus(refreshTtl)));
        return new TokenResponse(jwtUtil.generateToken(user.getUserName(), user.getTokenVersion()), refreshToken);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
