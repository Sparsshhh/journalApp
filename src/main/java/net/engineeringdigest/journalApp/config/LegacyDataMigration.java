package net.engineeringdigest.journalApp.config;

import com.mongodb.DBRef;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * One-time upgrade of data written by earlier versions, where each user document held a
 * "journalEntries" list of references. Entries now carry the owner's id in "userId" instead.
 * For every user that still has the old list, this stamps "userId" on the referenced entries
 * and then removes the list. It also updates the class name recorded on old user documents.
 * Running it again finds nothing to do.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class LegacyDataMigration implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    public LegacyDataMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        MongoCollection<Document> users = mongoTemplate.getCollection("users");
        MongoCollection<Document> entries = mongoTemplate.getCollection("journal_entries");

        // The entity class was renamed from "user" to "User". Old documents record the old class name,
        // which fails to load on case-insensitive file systems (macOS, Windows), so point them at the new one.
        users.updateMany(Filters.eq("_class", "net.engineeringdigest.journalApp.entity.user"),
                Updates.set("_class", "net.engineeringdigest.journalApp.entity.User"));

        int migratedUsers = 0;
        long migratedEntries = 0;
        for (Document user : users.find(Filters.exists("journalEntries"))) {
            List<Object> entryIds = new ArrayList<>();
            Object refs = user.get("journalEntries");
            if (refs instanceof List<?> list) {
                for (Object ref : list) {
                    if (ref instanceof DBRef dbRef) {
                        entryIds.add(dbRef.getId());
                    } else if (ref instanceof Document doc && doc.get("$id") != null) {
                        entryIds.add(doc.get("$id"));
                    }
                }
            }
            if (!entryIds.isEmpty()) {
                // Only entries without an owner yet, so a re-run can never move an entry to another user.
                migratedEntries += entries.updateMany(
                        Filters.and(Filters.in("_id", entryIds), Filters.exists("userId", false)),
                        Updates.set("userId", user.get("_id"))).getModifiedCount();
            }
            // The list is removed only after its entries are stamped; a failure above leaves it in place for the next start.
            users.updateOne(Filters.eq("_id", user.get("_id")), Updates.unset("journalEntries"));
            migratedUsers++;
        }
        if (migratedUsers > 0) {
            log.info("Migrated {} journal entries of {} users to the userId data model", migratedEntries, migratedUsers);
        }
    }
}
