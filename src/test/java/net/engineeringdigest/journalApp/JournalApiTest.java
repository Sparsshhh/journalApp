package net.engineeringdigest.journalApp;

import net.engineeringdigest.journalApp.entity.JournalEntry;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JournalApiTest extends IntegrationTestBase {

    private static final String AUTH = HttpHeaders.AUTHORIZATION;

    private String createEntry(String token, String body) throws Exception {
        MvcResult result = mockMvc.perform(json(post("/journal"), body).header(AUTH, token))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result, "$.id");
    }

    private void setDate(String entryId, LocalDateTime date) {
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(new ObjectId(entryId))),
                Update.update("date", date), JournalEntry.class);
    }

    @Test
    void createReadUpdateDelete() throws Exception {
        String alice = signUpAndLogin("alice");

        MvcResult created = mockMvc.perform(json(post("/journal"),
                        "{\"title\":\"First day\",\"content\":\"Hello journal\",\"tags\":[\" Work \",\"work\",\"Ideas\"]}")
                        .header(AUTH, alice))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.title").value("First day"))
                .andExpect(jsonPath("$.content").value("Hello journal"))
                .andExpect(jsonPath("$.tags", contains("work", "ideas"))) // trimmed, lower-cased, de-duplicated
                .andExpect(jsonPath("$.date").isNotEmpty())
                .andReturn();
        String id = read(created, "$.id");

        mockMvc.perform(get("/journal/id/" + id).header(AUTH, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("First day"));

        // Only the content is sent: the title and tags stay as they were.
        mockMvc.perform(json(put("/journal/id/" + id), "{\"content\":\"Edited\"}").header(AUTH, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("First day"))
                .andExpect(jsonPath("$.content").value("Edited"))
                .andExpect(jsonPath("$.tags", hasSize(2)));
        mockMvc.perform(json(put("/journal/id/" + id), "{\"tags\":[]}").header(AUTH, alice))
                .andExpect(jsonPath("$.tags", hasSize(0)));

        mockMvc.perform(delete("/journal/id/" + id).header(AUTH, alice)).andExpect(status().isNoContent());
        mockMvc.perform(get("/journal/id/" + id).header(AUTH, alice)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/journal/id/" + id).header(AUTH, alice)).andExpect(status().isNotFound());
    }

    @Test
    void createIgnoresClientSuppliedIdAndOwner() throws Exception {
        String alice = signUpAndLogin("alice");
        String bob = signUpAndLogin("bob");
        String aliceEntry = createEntry(alice, "{\"title\":\"alice's secret\"}");
        String aliceId = userRepository.findByUserName("alice").getId().toHexString();

        // Bob tries to overwrite alice's entry, and to file an entry under alice's account.
        String bobEntry = createEntry(bob, "{\"id\":\"" + aliceEntry + "\",\"userId\":\"" + aliceId + "\",\"title\":\"overwritten\"}");

        assertNotEquals(aliceEntry, bobEntry);
        mockMvc.perform(get("/journal/id/" + aliceEntry).header(AUTH, alice))
                .andExpect(jsonPath("$.title").value("alice's secret"));
        mockMvc.perform(get("/journal").header(AUTH, alice)).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void usersCannotSeeOrChangeEachOthersEntries() throws Exception {
        String alice = signUpAndLogin("alice");
        String bob = signUpAndLogin("bob");
        String id = createEntry(alice, "{\"title\":\"private\",\"content\":\"for alice only\"}");

        mockMvc.perform(get("/journal/id/" + id).header(AUTH, bob)).andExpect(status().isNotFound());
        mockMvc.perform(json(put("/journal/id/" + id), "{\"title\":\"hacked\"}").header(AUTH, bob)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/journal/id/" + id).header(AUTH, bob)).andExpect(status().isNotFound());
        mockMvc.perform(get("/journal").header(AUTH, bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/journal/id/" + id).header(AUTH, alice)).andExpect(jsonPath("$.title").value("private"));
    }

    @Test
    void titleAndContentAreEncryptedInTheDatabase() throws Exception {
        String alice = signUpAndLogin("alice");
        String bob = signUpAndLogin("bob");
        createEntry(alice, "{\"title\":\"Top secret title\",\"content\":\"Top secret content\",\"tags\":[\"diary\"]}");
        createEntry(bob, "{\"title\":\"Top secret title\",\"content\":\"Top secret content\"}");

        var stored = mongoTemplate.getCollection("journal_entries").find().into(new java.util.ArrayList<Document>());
        assertEquals(2, stored.size());
        for (Document doc : stored) {
            assertTrue(doc.getString("title").startsWith("enc:v1:"));
            assertTrue(doc.getString("content").startsWith("enc:v1:"));
            assertFalse(doc.toJson().contains("Top secret"));
        }
        // Same text, different users (and a fresh nonce each time): the stored values differ.
        assertNotEquals(stored.get(0).getString("title"), stored.get(1).getString("title"));
    }

    @Test
    void invalidEntriesAndIdsAreRejected() throws Exception {
        String alice = signUpAndLogin("alice");
        mockMvc.perform(json(post("/journal"), "{\"title\":\"\",\"content\":\"text\"}").header(AUTH, alice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("title is required"));
        mockMvc.perform(json(post("/journal"), "{\"title\":\"ok\",\"tags\":[\"  \"]}").header(AUTH, alice))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/journal/id/not-an-id").header(AUTH, alice)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/journal?from=yesterday").header(AUTH, alice)).andExpect(status().isBadRequest());
        assertEquals(0, journalEntryRepository.count());
    }

    @Test
    void listIsNewestFirstAndPaginated() throws Exception {
        String alice = signUpAndLogin("alice");
        for (int i = 1; i <= 25; i++) {
            setDate(createEntry(alice, "{\"title\":\"entry " + i + "\"}"), LocalDateTime.of(2026, 1, i, 9, 0));
        }

        mockMvc.perform(get("/journal").header(AUTH, alice))
                .andExpect(jsonPath("$.content", hasSize(20))) // default page size
                .andExpect(jsonPath("$.content[0].title").value("entry 25"))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/journal?page=2&size=10").header(AUTH, alice))
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.content[0].title").value("entry 5"))
                .andExpect(jsonPath("$.content[4].title").value("entry 1"))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalPages").value(3));
        mockMvc.perform(get("/journal?page=9&size=10").header(AUTH, alice))
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(25));
        // Out-of-range values are clamped instead of failing.
        mockMvc.perform(get("/journal?page=-3&size=100000").header(AUTH, alice))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.content", hasSize(25)));
    }

    @Test
    void searchByKeywordTagAndDate() throws Exception {
        String alice = signUpAndLogin("alice");
        String bob = signUpAndLogin("bob");
        setDate(createEntry(alice, "{\"title\":\"Trip to Goa\",\"content\":\"Beach and sunsets\",\"tags\":[\"travel\"]}"),
                LocalDateTime.of(2026, 3, 10, 23, 30));
        setDate(createEntry(alice, "{\"title\":\"Sprint review\",\"content\":\"Planned a team trip\",\"tags\":[\"work\",\"travel\"]}"),
                LocalDateTime.of(2026, 3, 20, 8, 0));
        setDate(createEntry(alice, "{\"title\":\"Groceries\",\"content\":\"Milk, eggs\",\"tags\":[\"home\"]}"),
                LocalDateTime.of(2026, 4, 1, 12, 0));
        createEntry(bob, "{\"title\":\"Bob's trip\",\"tags\":[\"travel\"]}");

        // Keyword: case-insensitive, in title or content, and only in alice's entries.
        mockMvc.perform(get("/journal?q=TRIP").header(AUTH, alice))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Sprint review"))
                .andExpect(jsonPath("$.content[1].title").value("Trip to Goa"));
        mockMvc.perform(get("/journal?q=sunsets").header(AUTH, alice)).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/journal?q=nothing-matches").header(AUTH, alice)).andExpect(jsonPath("$.totalElements").value(0));
        // A keyword is plain text, not a pattern.
        mockMvc.perform(get("/journal?q=.*").header(AUTH, alice)).andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/journal?tag=Travel").header(AUTH, alice)).andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/journal?tag=work").header(AUTH, alice))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Sprint review"));

        // Dates are inclusive on both ends.
        mockMvc.perform(get("/journal?from=2026-03-10&to=2026-03-10").header(AUTH, alice))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Trip to Goa"));
        mockMvc.perform(get("/journal?from=2026-03-11").header(AUTH, alice)).andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/journal?to=2026-03-20").header(AUTH, alice)).andExpect(jsonPath("$.totalElements").value(2));

        // Filters combine.
        mockMvc.perform(get("/journal?q=trip&tag=travel&from=2026-03-15").header(AUTH, alice))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Sprint review"));
    }
}
