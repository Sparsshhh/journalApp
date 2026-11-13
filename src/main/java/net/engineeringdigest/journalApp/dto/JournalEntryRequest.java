package net.engineeringdigest.journalApp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

// Body for creating a journal entry.
@Data
public class JournalEntryRequest {
    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must be at most 200 characters")
    private String title;

    @Size(max = 50000, message = "content must be at most 50000 characters")
    private String content;

    @Size(max = 20, message = "at most 20 tags")
    private List<@NotBlank(message = "tags must not be blank") @Size(max = 30, message = "a tag must be at most 30 characters") String> tags;
}
