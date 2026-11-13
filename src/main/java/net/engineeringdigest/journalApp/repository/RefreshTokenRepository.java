package net.engineeringdigest.journalApp.repository;

import net.engineeringdigest.journalApp.entity.RefreshToken;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, ObjectId> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    long deleteByTokenHash(String tokenHash);

    void deleteByUserId(ObjectId userId);
}
