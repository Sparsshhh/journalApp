package net.engineeringdigest.journalApp.Service;

import net.engineeringdigest.journalApp.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EmailServiceTests {

    @Autowired
    EmailService emailService;

    @Test
    void testSendEmail() {
        emailService.sendEmail("sparsh3863@gmail.com","Testing Java Mail Sender", "Hi, How are you?");
    }
}
