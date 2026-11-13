package net.engineeringdigest.journalApp.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "journal_entries")
@Data
@NoArgsConstructor
public class JournalEntry {

    @Id
    private ObjectId id;

    /** Owner of the entry. Every query filters on this, so one user can never reach another's entries. */
    @Indexed
    private ObjectId userId;

    /** Stored encrypted when an encryption key is configured (see EncryptionService). */
    private String title;

    /** Stored encrypted when an encryption key is configured. */
    private String content;

    /** Stored in plain text so entries can be filtered by tag in the database. */
    private List<String> tags = new ArrayList<>();

    private LocalDateTime date;
}
