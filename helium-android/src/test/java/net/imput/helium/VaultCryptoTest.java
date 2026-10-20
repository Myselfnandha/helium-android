package net.imput.helium;

import java.security.GeneralSecurityException;
import net.imput.helium.sync.VaultCrypto;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests validating AES-256-GCM authenticated encryption and key derivation.
 */
public class VaultCryptoTest {

    private static final String TEST_PASSWORD = "StrongHeliumVaultPassword!2026";

    @Test
    public void testEncryptDecryptRoundTrip() throws Exception {
        String original = "Helium Vault Payload with Unicode: 🚀🔒 & sensitive bookmarks";
        String envelope = VaultCrypto.encrypt(original, TEST_PASSWORD);

        assertNotNull(envelope);
        assertTrue(envelope.contains("\"v\":1"));
        assertTrue(envelope.contains("\"salt\":"));
        assertTrue(envelope.contains("\"iv\":"));
        assertTrue(envelope.contains("\"data\":"));

        String decrypted = VaultCrypto.decrypt(envelope, TEST_PASSWORD);
        assertEquals(original, decrypted);
    }

    @Test(expected = GeneralSecurityException.class)
    public void testIncorrectPasswordRejection() throws Exception {
        String original = "Confidential Sync State";
        String envelope = VaultCrypto.encrypt(original, TEST_PASSWORD);

        // Decryption with incorrect password must fail
        VaultCrypto.decrypt(envelope, "WrongPassword123");
    }

    @Test(expected = GeneralSecurityException.class)
    public void testTamperedCiphertextRejection() throws Exception {
        String original = "Authentic State";
        String envelope = VaultCrypto.encrypt(original, TEST_PASSWORD);

        // Tamper with the ciphertext data inside the envelope
        String tampered = envelope.replaceFirst("\"data\":\"[A-Za-z0-9+/=]{4}", "\"data\":\"AAAA");
        VaultCrypto.decrypt(tampered, TEST_PASSWORD);
    }
}
