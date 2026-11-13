package net.engineeringdigest.journalApp.dto;

import lombok.Data;

import jakarta.validation.constraints.Size;

// Body for updating the logged-in user. Both fields are optional; an omitted or blank one keeps its current value.
@Data
public class UserUpdateRequest {
    @Size(max = 50, message = "userName must be at most 50 characters")
    private String userName;

    @Size(min = 8, max = 72, message = "password must be 8 to 72 characters")
    private String password;
}
