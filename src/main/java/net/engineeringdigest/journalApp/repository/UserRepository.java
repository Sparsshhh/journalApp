package net.engineeringdigest.journalApp.repository;

import net.engineeringdigest.journalApp.entity.user;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<user, ObjectId>{
    user findByUserName(String userName);

    void deleteByUserName(String userName);
}
