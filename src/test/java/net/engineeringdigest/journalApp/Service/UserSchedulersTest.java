package net.engineeringdigest.journalApp.Service;

import net.engineeringdigest.journalApp.Scheduler.UsersScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserSchedulersTest {

    @Autowired
    private UsersScheduler usersScheduler;

    @Test
    public void testFetchUsersAndSendSaMail(){
        usersScheduler.fetchUsersAndSendSaMail();
    }
}
