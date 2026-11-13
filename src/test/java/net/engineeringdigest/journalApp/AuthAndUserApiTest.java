package net.engineeringdigest.journalApp;

import net.engineeringdigest.journalApp.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthAndUserApiTest extends IntegrationTestBase {

    private static final String AUTH = HttpHeaders.AUTHORIZATION;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---- public endpoints and sign-up ----

    @Test
    void publicEndpointsNeedNoLogin() throws Exception {
        mockMvc.perform(get("/public/health-check")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/index.html")).andExpect(status().isOk()).andExpect(content().string(containsString("<title>Journal</title>")));
    }

    @Test
    void signUpStoresHashedPasswordAndNeverReturnsIt() throws Exception {
        mockMvc.perform(json(post("/public/createUser"), credentials("alice", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userName").value("alice"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.id").isString())
                .andExpect(content().string(not(containsString("password"))));

        User stored = userRepository.findByUserName("alice");
        assertNotEquals(PASSWORD, stored.getPassword());
        assertTrue(passwordEncoder.matches(PASSWORD, stored.getPassword()));
    }

    @Test
    void signUpIgnoresClientSuppliedIdAndRoles() throws Exception {
        signUp("alice");
        String aliceId = userRepository.findByUserName("alice").getId().toHexString();

        // Tries to overwrite alice's account and to become an admin.
        mockMvc.perform(json(post("/public/createUser"), "{\"id\":\"" + aliceId + "\",\"userName\":\"mallory\","
                        + "\"password\":\"" + PASSWORD + "\",\"roles\":[\"ADMIN\"],\"tokenVersion\":7}"))
                .andExpect(status().isCreated());

        assertEquals(2, userRepository.count());
        assertEquals(aliceId, userRepository.findByUserName("alice").getId().toHexString());
        User mallory = userRepository.findByUserName("mallory");
        assertEquals(java.util.List.of("USER"), mallory.getRoles());
        assertEquals(0, mallory.getTokenVersion());
    }

    @Test
    void duplicateUserNameIsAConflict() throws Exception {
        signUp("alice");
        mockMvc.perform(json(post("/public/createUser"), credentials("alice", PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("That username is already taken"));
        assertEquals(1, userRepository.count());
    }

    @Test
    void invalidSignUpIsRejectedWithFieldErrors() throws Exception {
        mockMvc.perform(json(post("/public/createUser"), credentials("bob", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.password").value("password must be 8 to 72 characters"));
        mockMvc.perform(json(post("/public/createUser"), "{\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.userName").value("userName is required"));
        mockMvc.perform(json(post("/public/createUser"), "{not json"))
                .andExpect(status().isBadRequest());
        assertEquals(0, userRepository.count());
    }

    // ---- login and tokens ----

    @Test
    void loginReturnsTokensThatAuthenticate() throws Exception {
        signUp("alice");
        MvcResult login = login("alice", PASSWORD);
        assertNotNull(read(login, "$.refreshToken"));

        mockMvc.perform(get("/user").header(AUTH, bearer(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("alice"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void loginRejectsWrongPasswordAndUnknownUserAlike() throws Exception {
        signUp("alice");
        mockMvc.perform(json(post("/public/login"), credentials("alice", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Incorrect username or password"));
        mockMvc.perform(json(post("/public/login"), credentials("nobody", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Incorrect username or password"));
        mockMvc.perform(json(post("/public/login"), "{}")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoutesRejectMissingInvalidAndBasicCredentials() throws Exception {
        signUp("alice");
        mockMvc.perform(get("/user")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/journal")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/user").header(AUTH, "Bearer not-a-real-token")).andExpect(status().isUnauthorized());
        String basic = "Basic " + java.util.Base64.getEncoder().encodeToString(("alice:" + PASSWORD).getBytes());
        mockMvc.perform(get("/user").header(AUTH, basic)).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenWorksOnceAndReturnsANewPair() throws Exception {
        signUp("alice");
        String refreshToken = read(login("alice", PASSWORD), "$.refreshToken");
        String body = "{\"refreshToken\":\"" + refreshToken + "\"}";

        MvcResult refreshed = mockMvc.perform(json(post("/public/refresh"), body))
                .andExpect(status().isOk())
                .andReturn();
        assertNotEquals(refreshToken, read(refreshed, "$.refreshToken"));
        mockMvc.perform(get("/user").header(AUTH, bearer(refreshed))).andExpect(status().isOk());

        mockMvc.perform(json(post("/public/refresh"), body)).andExpect(status().isUnauthorized());
        mockMvc.perform(json(post("/public/refresh"), "{\"refreshToken\":\"made-up\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(json(post("/public/refresh"), "{}")).andExpect(status().isBadRequest());
    }

    @Test
    void refreshTokensAreStoredHashed() throws Exception {
        signUp("alice");
        String refreshToken = read(login("alice", PASSWORD), "$.refreshToken");
        assertEquals(1, refreshTokenRepository.count());
        assertNotEquals(refreshToken, refreshTokenRepository.findAll().get(0).getTokenHash());
    }

    @Test
    void logoutInvalidatesAccessAndRefreshTokens() throws Exception {
        signUp("alice");
        MvcResult login = login("alice", PASSWORD);
        String refreshToken = read(login, "$.refreshToken");

        mockMvc.perform(post("/user/logout").header(AUTH, bearer(login))).andExpect(status().isNoContent());

        mockMvc.perform(get("/user").header(AUTH, bearer(login))).andExpect(status().isUnauthorized());
        mockMvc.perform(json(post("/public/refresh"), "{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
        // Logging in again works.
        mockMvc.perform(get("/user").header(AUTH, bearer(login("alice", PASSWORD)))).andExpect(status().isOk());
    }

    // ---- updating and deleting the account ----

    @Test
    void passwordChangeIsHashedAndSignsOutOldTokens() throws Exception {
        signUp("alice");
        MvcResult oldLogin = login("alice", PASSWORD);

        mockMvc.perform(json(put("/user"), "{\"password\":\"a-brand-new-password\"}").header(AUTH, bearer(oldLogin)))
                .andExpect(status().isNoContent());

        User stored = userRepository.findByUserName("alice");
        assertTrue(passwordEncoder.matches("a-brand-new-password", stored.getPassword()));
        assertEquals(java.util.List.of("USER"), stored.getRoles());
        mockMvc.perform(get("/user").header(AUTH, bearer(oldLogin))).andExpect(status().isUnauthorized());
        mockMvc.perform(json(post("/public/login"), credentials("alice", PASSWORD))).andExpect(status().isUnauthorized());
        login("alice", "a-brand-new-password");
        assertEquals(1, refreshTokenRepository.count()); // only the one from the login just above
    }

    @Test
    void updateRejectsShortPasswordAndKeepsOriginalRouteWorking() throws Exception {
        String token = signUpAndLogin("alice");
        mockMvc.perform(json(put("/user"), "{\"password\":\"short\"}").header(AUTH, token))
                .andExpect(status().isBadRequest());
        assertTrue(passwordEncoder.matches(PASSWORD, userRepository.findByUserName("alice").getPassword()));

        mockMvc.perform(json(put("/user/alice"), "{\"userName\":\"alicia\"}").header(AUTH, token))
                .andExpect(status().isNoContent());
        assertNotNull(userRepository.findByUserName("alicia"));
        assertNull(userRepository.findByUserName("alice"));
    }

    @Test
    void renamingToATakenUserNameIsAConflict() throws Exception {
        signUp("bob");
        String token = signUpAndLogin("alice");
        mockMvc.perform(json(put("/user"), "{\"userName\":\"bob\"}").header(AUTH, token))
                .andExpect(status().isConflict());
    }

    @Test
    void deletingTheAccountRemovesItsEntriesAndTokens() throws Exception {
        String alice = signUpAndLogin("alice");
        String bob = signUpAndLogin("bob");
        mockMvc.perform(json(post("/journal"), "{\"title\":\"alice's\"}").header(AUTH, alice)).andExpect(status().isCreated());
        mockMvc.perform(json(post("/journal"), "{\"title\":\"bob's\"}").header(AUTH, bob)).andExpect(status().isCreated());

        mockMvc.perform(delete("/user").header(AUTH, alice)).andExpect(status().isNoContent());

        assertNull(userRepository.findByUserName("alice"));
        assertEquals(1, journalEntryRepository.count());
        assertEquals(1, refreshTokenRepository.count());
        mockMvc.perform(get("/user").header(AUTH, alice)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/journal").header(AUTH, bob)).andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---- admin ----

    @Test
    void adminRoutesRejectAnonymousAndNormalUsers() throws Exception {
        String alice = signUpAndLogin("alice");
        mockMvc.perform(get("/admin/allUsers")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin/allUsers").header(AUTH, alice)).andExpect(status().isForbidden());
        mockMvc.perform(json(post("/admin/create-admin-users"), credentials("evil", PASSWORD)).header(AUTH, alice))
                .andExpect(status().isForbidden());
        assertNull(userRepository.findByUserName("evil"));
    }

    @Test
    void adminCanListUsersAndCreateAdmins() throws Exception {
        signUp("alice");
        userService.register("root", PASSWORD, net.engineeringdigest.journalApp.service.UserService.ADMIN_ROLES);
        String root = bearer(login("root", PASSWORD));

        mockMvc.perform(get("/admin/allUsers").header(AUTH, root))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("$2a$"))));

        mockMvc.perform(json(post("/admin/create-admin-users"), credentials("second-admin", PASSWORD)).header(AUTH, root))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[1]").value("ADMIN"));
        mockMvc.perform(get("/admin/allUsers").header(AUTH, bearer(login("second-admin", PASSWORD))))
                .andExpect(status().isOk());
    }

    @Test
    void unknownRouteIsNotFoundForALoggedInUser() throws Exception {
        String alice = signUpAndLogin("alice");
        mockMvc.perform(get("/no-such-route").header(AUTH, alice)).andExpect(status().isNotFound());
    }
}
