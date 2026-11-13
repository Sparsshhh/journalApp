package net.engineeringdigest.journalApp.service;

import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Encrypts journal text before it is stored (AES-256-GCM), with a separate key per user derived
 * from one master key. This is encryption at rest: the server holds the key, so it protects a
 * leaked or stolen database, not against someone who controls the running server.
 */
@Component
@Slf4j
public class EncryptionService {

    private static final String PREFIX = "enc:v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private final byte[] masterKey; // null when encryption is switched off

    // journal.encryption-key (or JOURNAL_ENCRYPTION_KEY): 32 random bytes, base64-encoded.
    public EncryptionService(@Value("${journal.encryption-key:}") String base64Key) {
        if (base64Key == null || base64Key.trim().isEmpty()) {
            log.warn("journal.encryption-key is not set; journal entries are stored unencrypted");
            this.masterKey = null;
            return;
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("journal.encryption-key must be base64", e);
        }
        if (decoded.length != 32) {
            throw new IllegalStateException("journal.encryption-key must decode to exactly 32 bytes");
        }
        this.masterKey = decoded;
    }

    public String encrypt(ObjectId userId, String plainText) {
        if (plainText == null || masterKey == null) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keyFor(userId), new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] out = Arrays.copyOf(iv, iv.length + cipherText.length);
            System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not encrypt journal text", e);
        }
    }

    // Values without the prefix were written before encryption was switched on and are returned as they are.
    public String decrypt(ObjectId userId, String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            return stored;
        }
        if (masterKey == null) {
            throw new IllegalStateException("Entry is encrypted but journal.encryption-key is not set");
        }
        try {
            byte[] in = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keyFor(userId), new GCMParameterSpec(TAG_BITS, in, 0, IV_BYTES));
            return new String(cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Could not decrypt journal text; was journal.encryption-key changed?", e);
        }
    }

    private SecretKey keyFor(ObjectId userId) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(masterKey, "HmacSHA256"));
        return new SecretKeySpec(mac.doFinal(userId.toByteArray()), "AES");
    }
}
