package net.engineeringdigest.journalApp.dto;

import lombok.Value;
import net.engineeringdigest.journalApp.entity.User;

import java.util.List;

// What the API returns for a user: never the password hash, and the id as a plain string.
@Value
public class UserResponse {
    String id;
    String userName;
    List<String> roles;

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId() == null ? null : user.getId().toHexString(),
                user.getUserName(),
                user.getRoles());
    }
}
