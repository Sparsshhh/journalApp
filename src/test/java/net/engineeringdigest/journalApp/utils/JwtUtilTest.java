package net.engineeringdigest.journalApp.utils;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JwtUtilTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-characters-long";

    @Test
    void tokenRoundTripsUserNameAndVersion() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60000);
        JwtUtil.TokenClaims claims = jwtUtil.parse(jwtUtil.generateToken("sparsh", 3));
        assertEquals("sparsh", claims.userName());
        assertEquals(3, claims.tokenVersion());
    }

    @Test
    void expiredTokenIsRejected() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, -60000);
        String token = jwtUtil.generateToken("sparsh", 0);
        assertThrows(JwtException.class, () -> jwtUtil.parse(token));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = new JwtUtil("", 60000).generateToken("sparsh", 0);
        assertThrows(JwtException.class, () -> new JwtUtil(SECRET, 60000).parse(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60000);
        String token = jwtUtil.generateToken("sparsh", 0);
        String[] parts = token.split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"admin\",\"ver\":0,\"exp\":9999999999}".getBytes());
        assertThrows(JwtException.class, () -> jwtUtil.parse(parts[0] + "." + forgedPayload + "." + parts[2]));
    }

    @Test
    void shortSecretIsRefusedAtStartup() {
        assertThrows(RuntimeException.class, () -> new JwtUtil("too-short", 60000));
    }
}
