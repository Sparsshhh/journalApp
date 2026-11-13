package net.engineeringdigest.journalApp.dto;

import lombok.Value;

// Returned by login and refresh. Send "token" as "Authorization: Bearer <token>";
// exchange "refreshToken" at /public/refresh for a new pair when the token expires.
@Value
public class TokenResponse {
    String token;
    String refreshToken;
}
