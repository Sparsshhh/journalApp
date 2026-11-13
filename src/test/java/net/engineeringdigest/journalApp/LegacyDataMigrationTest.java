package net.engineeringdigest.journalApp;

import com.mongodb.DBRef;
import net.engineeringdigest.journalApp.config.LegacyDataMigration;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Data in the shape the app wrote before this version: users hold a list of entry references,
// entries have no owner field and are not encrypted.
class LegacyDataMigrationTest extends IntegrationTestBase {

    @Autowired
    private LegacyDataMigration migration;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private ObjectId insertLegacyEntry(String title) {
        ObjectId id = new ObjectId();
        mongoTemplate.getCollection("journal_entries").insertOne(new Document("_id", id)
                .append("title", title).append("content", "old content").append("date", new Date())
                .append("_class", "net.engineeringdigest.journalApp.entity.JournalEntry"));
        return id;
    }

    private ObjectId insertLegacyUser(String userName, ObjectId... entryIds) {
        ObjectId id = new ObjectId();
        List<DBRef> refs = java.util.Arrays.stream(entryIds).map(e -> new DBRef("journal_entries", e)).toList();
        mongoTemplate.getCollection("users").insertOne(new Document("_id", id)
                .append("userName", userName).append("password", passwordEncoder.encode(PASSWORD))
                .append("roles", List.of("USER")).append("journalEntries", refs)
                .append("_class", "net.engineeringdigest.journalApp.entity.user"));
        return id;
    }

    @Test
    void oldUsersKeepTheirEntriesAndCanStillLogIn() throws Exception {
        ObjectId a1 = insertLegacyEntry("alice one");
        ObjectId a2 = insertLegacyEntry("alice two");
        ObjectId b1 = insertLegacyEntry("bob one");
        ObjectId orphan = insertLegacyEntry("belongs to nobody");
        ObjectId alice = insertLegacyUser("alice", a1, a2);
        ObjectId bob = insertLegacyUser("bob", b1);
        insertLegacyUser("carol");

        migration.run(null);
        migration.run(null); // a second run changes nothing

        var entries = mongoTemplate.getCollection("journal_entries");
        assertEquals(alice, entries.find(new Document("_id", a1)).first().getObjectId("userId"));
        assertEquals(alice, entries.find(new Document("_id", a2)).first().getObjectId("userId"));
        assertEquals(bob, entries.find(new Document("_id", b1)).first().getObjectId("userId"));
        assertNull(entries.find(new Document("_id", orphan)).first().get("userId"));
        assertEquals(0, mongoTemplate.getCollection("users").countDocuments(new Document("journalEntries", new Document("$exists", true))));
        assertEquals(3, userRepository.count());

        // The old, unencrypted entries are readable through the API by their owner only.
        mockMvc.perform(get("/journal").header(HttpHeaders.AUTHORIZATION, bearer(login("alice", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].content").value("old content"));
        mockMvc.perform(get("/journal/id/" + a1.toHexString()).header(HttpHeaders.AUTHORIZATION, bearer(login("bob", PASSWORD))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/journal").header(HttpHeaders.AUTHORIZATION, bearer(login("carol", PASSWORD))))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void anEntryAlreadyOwnedIsNeverReassigned() {
        ObjectId entry = insertLegacyEntry("shared reference");
        ObjectId first = insertLegacyUser("first", entry);
        migration.run(null);
        insertLegacyUser("second", entry); // a stale reference to an entry that now has an owner
        migration.run(null);

        assertEquals(first, mongoTemplate.getCollection("journal_entries")
                .find(new Document("_id", entry)).first().getObjectId("userId"));
    }
}
