package net.engineeringdigest.journalApp.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Body for sign-up and for creating an admin. Only these two fields are ever read from the client.
@Data
public class UserRequest {
    @NotBlank(message = "userName is required")
    @Size(max = 50, message = "userName must be at most 50 characters")
    private String userName;

    @NotBlank(message = "password is required")
    @Size(min = 8, max = 72, message = "password must be 8 to 72 characters") // BCrypt ignores anything past 72 bytes
    private String password;
}
