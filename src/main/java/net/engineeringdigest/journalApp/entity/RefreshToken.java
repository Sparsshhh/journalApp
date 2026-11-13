package net.engineeringdigest.journalApp.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "refresh_tokens")
@Data
@NoArgsConstructor
public class RefreshToken {

    @Id
    private ObjectId id;

    /** SHA-256 of the token. The token itself is only ever held by the client. */
    @Indexed(unique = true)
    private String tokenHash;

    @Indexed
    private ObjectId userId;

    /** MongoDB deletes the document automatically once this time has passed. */
    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    public RefreshToken(String tokenHash, ObjectId userId, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }
}
