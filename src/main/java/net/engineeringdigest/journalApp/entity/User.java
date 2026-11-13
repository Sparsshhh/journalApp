package net.engineeringdigest.journalApp.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private String userName;

    /** BCrypt hash, never the raw password. */
    private String password;

    private List<String> roles = new ArrayList<>();

    /** Bumped on logout and password change; access tokens carrying an older value are rejected. */
    private int tokenVersion;

    public User(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }
}
