package net.engineeringdigest.journalApp.dto;

import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

// What the API returns for a journal entry: decrypted, with the id as a plain string.
@Value
public class JournalEntryResponse {
    String id;
    String title;
    String content;
    List<String> tags;
    LocalDateTime date;
}
