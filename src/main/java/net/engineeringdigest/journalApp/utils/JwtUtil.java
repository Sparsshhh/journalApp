package net.engineeringdigest.journalApp.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    private static final String VERSION_CLAIM = "ver";

    /** What a valid access token says about its holder. */
    public record TokenClaims(String userName, int tokenVersion) {}

    private final SecretKey key;
    private final long expirationMs;

    // jwt.secret (or the JWT_SECRET environment variable) must be at least 32 characters.
    public JwtUtil(@Value("${jwt.secret:}") String secret,
                   @Value("${jwt.expiration-ms:900000}") long expirationMs) {
        if (secret == null || secret.trim().isEmpty()) {
            // No secret configured: use a random key rather than a hard-coded one.
            // Tokens stop working when the app restarts, so set jwt.secret for real deployments.
            log.warn("jwt.secret is not set; using a random signing key for this run");
            this.key = Jwts.SIG.HS256.key().build();
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.expirationMs = expirationMs;
    }

    public String generateToken(String userName, int tokenVersion) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userName)
                .claim(VERSION_CLAIM, tokenVersion)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    // Throws JwtException if the token is expired, malformed or has a bad signature.
    public TokenClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Integer version = claims.get(VERSION_CLAIM, Integer.class);
        return new TokenClaims(claims.getSubject(), version == null ? 0 : version);
    }
}
