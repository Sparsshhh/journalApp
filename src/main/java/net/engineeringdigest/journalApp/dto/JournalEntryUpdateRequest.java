package net.engineeringdigest.journalApp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

// Body for updating a journal entry. Every field is optional; an omitted one keeps its current value.
// An empty title or content also keeps the current value; an empty tags list removes all tags.
@Data
public class JournalEntryUpdateRequest {
    @Size(max = 200, message = "title must be at most 200 characters")
    private String title;

    @Size(max = 50000, message = "content must be at most 50000 characters")
    private String content;

    @Size(max = 20, message = "at most 20 tags")
    private List<@NotBlank(message = "tags must not be blank") @Size(max = 30, message = "a tag must be at most 30 characters") String> tags;
}
