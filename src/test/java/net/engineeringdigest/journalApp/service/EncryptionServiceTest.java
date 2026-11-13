package net.engineeringdigest.journalApp.service;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class EncryptionServiceTest {

    private static final String KEY = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
    private static final String OTHER_KEY = Base64.getEncoder().encodeToString("ABCDEFGHIJKLMNOPQRSTUVWXYZ012345".getBytes());

    private final EncryptionService service = new EncryptionService(KEY);
    private final ObjectId alice = new ObjectId();
    private final ObjectId bob = new ObjectId();

    @Test
    void roundTripsTextIncludingUnicodeAndEmptyStrings() {
        for (String text : new String[]{"Dear diary", "", "नमस्ते 🌙 — ünïcødé", "x".repeat(50000)}) {
            String stored = service.encrypt(alice, text);
            assertTrue(stored.startsWith("enc:v1:"));
            assertFalse(!text.isEmpty() && stored.contains(text));
            assertEquals(text, service.decrypt(alice, stored));
        }
    }

    @Test
    void sameTextEncryptsDifferentlyEveryTime() {
        assertNotEquals(service.encrypt(alice, "same"), service.encrypt(alice, "same"));
    }

    @Test
    void oneUsersTextCannotBeDecryptedAsAnotherUser() {
        String stored = service.encrypt(alice, "private");
        assertThrows(IllegalStateException.class, () -> service.decrypt(bob, stored));
    }

    @Test
    void wrongKeyOrTamperedTextFailsInsteadOfReturningGarbage() {
        String stored = service.encrypt(alice, "private");
        assertThrows(IllegalStateException.class, () -> new EncryptionService(OTHER_KEY).decrypt(alice, stored));
        // Change one character in the middle of the encoded data (not the padded tail, whose last bits are unused).
        int index = "enc:v1:".length() + 20;
        String tampered = stored.substring(0, index) + (stored.charAt(index) == 'A' ? 'B' : 'A') + stored.substring(index + 1);
        assertThrows(IllegalStateException.class, () -> service.decrypt(alice, tampered));
    }

    @Test
    void nullAndLegacyPlainTextPassThrough() {
        assertNull(service.encrypt(alice, null));
        assertNull(service.decrypt(alice, null));
        assertEquals("written before encryption", service.decrypt(alice, "written before encryption"));
    }

    @Test
    void withoutAKeyTextIsStoredAsIsButEncryptedTextIsNotReadable() {
        EncryptionService off = new EncryptionService("");
        assertEquals("plain", off.encrypt(alice, "plain"));
        assertEquals("plain", off.decrypt(alice, "plain"));
        assertThrows(IllegalStateException.class, () -> off.decrypt(alice, service.encrypt(alice, "secret")));
    }

    @Test
    void badKeysAreRefusedAtStartup() {
        assertThrows(IllegalStateException.class, () -> new EncryptionService("not base64 !!"));
        assertThrows(IllegalStateException.class,
                () -> new EncryptionService(Base64.getEncoder().encodeToString("only-16-bytes-ok".getBytes())));
    }
}
