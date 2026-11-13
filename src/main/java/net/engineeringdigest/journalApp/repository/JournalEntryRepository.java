package net.engineeringdigest.journalApp.repository;

import net.engineeringdigest.journalApp.entity.JournalEntry;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface JournalEntryRepository extends MongoRepository<JournalEntry, ObjectId>{

    Optional<JournalEntry> findByIdAndUserId(ObjectId id, ObjectId userId);

    long deleteByIdAndUserId(ObjectId id, ObjectId userId);

    void deleteByUserId(ObjectId userId);
}
