package net.engineeringdigest.journalApp.controller;

import jakarta.validation.Valid;
import net.engineeringdigest.journalApp.dto.JournalEntryRequest;
import net.engineeringdigest.journalApp.dto.JournalEntryResponse;
import net.engineeringdigest.journalApp.dto.JournalEntryUpdateRequest;
import net.engineeringdigest.journalApp.dto.PageResponse;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.service.JournalEntryService;
import org.bson.types.ObjectId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

// Every route works only on the logged-in user's own entries.
@RestController
@RequestMapping("/journal")
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    public JournalEntryController(JournalEntryService journalEntryService) {
        this.journalEntryService = journalEntryService;
    }

    // Newest first. q: keyword in title or content; tag: exact tag; from/to: dates like 2026-01-31, inclusive.
    @GetMapping
    public PageResponse<JournalEntryResponse> getJournalEntries(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size){
        return journalEntryService.search(user, q, tag, from, to, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JournalEntryResponse createEntry(@AuthenticationPrincipal User user, @Valid @RequestBody JournalEntryRequest request){
        return journalEntryService.create(user, request);
    }

    @GetMapping("id/{myId}")
    public JournalEntryResponse getJournalEntryById(@AuthenticationPrincipal User user, @PathVariable ObjectId myId){
        return journalEntryService.findById(user, myId).orElseThrow(JournalEntryController::notFound);
    }

    @PutMapping("id/{myId}")
    public JournalEntryResponse updateJournalById(@AuthenticationPrincipal User user, @PathVariable ObjectId myId,
                                                  @Valid @RequestBody JournalEntryUpdateRequest request){
        return journalEntryService.update(user, myId, request).orElseThrow(JournalEntryController::notFound);
    }

    @DeleteMapping("id/{myId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJournalEntryById(@AuthenticationPrincipal User user, @PathVariable ObjectId myId){
        if (!journalEntryService.delete(user, myId)) {
            throw notFound();
        }
    }

    // Also the answer for another user's entry, so ids cannot be probed.
    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Journal entry not found");
    }
}
