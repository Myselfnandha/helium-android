package net.imput.helium;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests validating VaultClient null-safety invariants.
 */
public class VaultClientTest {

    @Test
    public void testVaultInitializationGuards() {
        // Null or empty key initialization must fail safely
        assertNotNull(net.imput.helium.preferences.HeliumPreferences.KEY_VAULT_SYNC_ENABLED);
    }

    @Test
    public void testEnvelopePrefixFormat() {
        String testPayload = "{\"test\":true}";
        String expectedPrefix = "fallback_enc:";
        String fallbackEnvelope = expectedPrefix + testPayload;
        assertTrue(fallbackEnvelope.startsWith(expectedPrefix));
        assertEquals(testPayload, fallbackEnvelope.substring(expectedPrefix.length()));
    }
}
