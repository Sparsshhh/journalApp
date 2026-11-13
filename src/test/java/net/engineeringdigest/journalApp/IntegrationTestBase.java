package net.engineeringdigest.journalApp;

import com.jayway.jsonpath.JsonPath;
import net.engineeringdigest.journalApp.repository.JournalEntryRepository;
import net.engineeringdigest.journalApp.repository.RefreshTokenRepository;
import net.engineeringdigest.journalApp.repository.UserRepository;
import net.engineeringdigest.journalApp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base for tests that drive the whole app over HTTP against a real (embedded) MongoDB.
 * The database is emptied before every test.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationTestBase {

    protected static final String PASSWORD = "correct-horse-battery";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected MongoTemplate mongoTemplate;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected JournalEntryRepository journalEntryRepository;
    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;
    @Autowired
    protected UserService userService;

    @BeforeEach
    void emptyDatabase() {
        journalEntryRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    protected static String credentials(String userName, String password) {
        return "{\"userName\":\"" + userName + "\",\"password\":\"" + password + "\"}";
    }

    protected void signUp(String userName) throws Exception {
        mockMvc.perform(json(post("/public/createUser"), credentials(userName, PASSWORD)))
                .andExpect(status().isCreated());
    }

    protected MvcResult login(String userName, String password) throws Exception {
        return mockMvc.perform(json(post("/public/login"), credentials(userName, password)))
                .andExpect(status().isOk())
                .andReturn();
    }

    /** Signs the user up and returns a ready-to-use Authorization header value. */
    protected String signUpAndLogin(String userName) throws Exception {
        signUp(userName);
        return bearer(login(userName, PASSWORD));
    }

    protected static String bearer(MvcResult loginResult) throws Exception {
        return "Bearer " + read(loginResult, "$.token");
    }

    protected static <T> T read(MvcResult result, String jsonPath) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), jsonPath);
    }
}
