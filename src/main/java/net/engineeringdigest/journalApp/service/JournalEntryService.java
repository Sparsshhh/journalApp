package net.engineeringdigest.journalApp.service;

import net.engineeringdigest.journalApp.dto.JournalEntryRequest;
import net.engineeringdigest.journalApp.dto.JournalEntryResponse;
import net.engineeringdigest.journalApp.dto.JournalEntryUpdateRequest;
import net.engineeringdigest.journalApp.dto.PageResponse;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.JournalEntryRepository;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// controller ---> service ----> repository
@Service
public class JournalEntryService {

    public static final int MAX_PAGE_SIZE = 100;

    private final JournalEntryRepository journalEntryRepository;
    private final MongoTemplate mongoTemplate;
    private final EncryptionService encryptionService;

    public JournalEntryService(JournalEntryRepository journalEntryRepository, MongoTemplate mongoTemplate,
                               EncryptionService encryptionService) {
        this.journalEntryRepository = journalEntryRepository;
        this.mongoTemplate = mongoTemplate;
        this.encryptionService = encryptionService;
    }

    public JournalEntryResponse create(User user, JournalEntryRequest request) {
        JournalEntry entry = new JournalEntry();
        entry.setUserId(user.getId());
        entry.setTitle(encryptionService.encrypt(user.getId(), request.getTitle()));
        entry.setContent(encryptionService.encrypt(user.getId(), request.getContent()));
        entry.setTags(normalizeTags(request.getTags()));
        entry.setDate(LocalDateTime.now());
        return toResponse(journalEntryRepository.save(entry));
    }

    /**
     * The user's entries, newest first. Tag and date filters run in the database. The keyword
     * filter runs here after decryption, because encrypted text cannot be searched in the
     * database; that means every entry matching the tag and date filters is loaded per request.
     */
    public PageResponse<JournalEntryResponse> search(User user, String keyword, String tag,
                                                     LocalDate from, LocalDate to, int page, int size) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Criteria criteria = Criteria.where("userId").is(user.getId());
        if (tag != null && !tag.trim().isEmpty()) {
            criteria = criteria.and("tags").is(normalizeTag(tag));
        }
        if (from != null || to != null) {
            Criteria date = criteria.and("date");
            if (from != null) {
                date.gte(from.atStartOfDay());
            }
            if (to != null) {
                date.lt(to.plusDays(1).atStartOfDay()); // "to" is inclusive
            }
        }
        Query query = Query.query(criteria).with(Sort.by(Sort.Direction.DESC, "date"));

        String needle = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<JournalEntryResponse> matches = new ArrayList<>();
        for (JournalEntry entry : mongoTemplate.find(query, JournalEntry.class)) {
            JournalEntryResponse response = toResponse(entry);
            if (needle.isEmpty() || contains(response.getTitle(), needle) || contains(response.getContent(), needle)) {
                matches.add(response);
            }
        }

        long start = (long) page * size;
        List<JournalEntryResponse> content = start >= matches.size()
                ? List.of()
                : matches.subList((int) start, (int) Math.min(start + size, matches.size()));
        int totalPages = (int) Math.ceil((double) matches.size() / size);
        return new PageResponse<>(content, page, size, matches.size(), totalPages);
    }

    public Optional<JournalEntryResponse> findById(User user, ObjectId id) {
        return journalEntryRepository.findByIdAndUserId(id, user.getId()).map(this::toResponse);
    }

    public Optional<JournalEntryResponse> update(User user, ObjectId id, JournalEntryUpdateRequest request) {
        return journalEntryRepository.findByIdAndUserId(id, user.getId()).map(entry -> {
            if (request.getTitle() != null && !request.getTitle().isEmpty()) {
                entry.setTitle(encryptionService.encrypt(user.getId(), request.getTitle()));
            }
            if (request.getContent() != null && !request.getContent().isEmpty()) {
                entry.setContent(encryptionService.encrypt(user.getId(), request.getContent()));
            }
            if (request.getTags() != null) {
                entry.setTags(normalizeTags(request.getTags()));
            }
            return toResponse(journalEntryRepository.save(entry));
        });
    }

    public boolean delete(User user, ObjectId id) {
        return journalEntryRepository.deleteByIdAndUserId(id, user.getId()) > 0;
    }

    private JournalEntryResponse toResponse(JournalEntry entry) {
        return new JournalEntryResponse(
                entry.getId().toHexString(),
                encryptionService.decrypt(entry.getUserId(), entry.getTitle()),
                encryptionService.decrypt(entry.getUserId(), entry.getContent()),
                entry.getTags() == null ? List.of() : entry.getTags(),
                entry.getDate());
    }

    private static boolean contains(String text, String lowerCaseNeedle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(lowerCaseNeedle);
    }

    // Tags are compared case-insensitively, so they are stored trimmed and lower-cased, without duplicates.
    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(tags.stream().map(JournalEntryService::normalizeTag).distinct().toList());
    }

    private static String normalizeTag(String tag) {
        return tag.trim().toLowerCase(Locale.ROOT);
    }
}
