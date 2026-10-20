package net.imput.helium;

import net.imput.helium.diagnostics.HeliumDiagnostics;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for PRD M28: Diagnostics & Health Monitoring.
 */
public class HeliumDiagnosticsTest {

    private HeliumDiagnostics mDiagnostics;

    @Before
    public void setUp() {
        mDiagnostics = new HeliumDiagnostics();
    }

    @Test
    public void testUrlRedaction() {
        String sensitiveUrl = "https://bank.example.com/account?token=secret123&user=john#balance";
        String redacted = HeliumDiagnostics.redactUrl(sensitiveUrl);

        assertEquals("https://bank.example.com/[redacted]", redacted);
        assertFalse(redacted.contains("token"));
        assertFalse(redacted.contains("secret123"));
        assertFalse(redacted.contains("balance"));

        assertEquals("[empty]", HeliumDiagnostics.redactUrl(null));
        assertEquals("[empty]", HeliumDiagnostics.redactUrl("   "));
    }

    @Test
    public void testScrubSensitiveData() {
        String logMessage = "Request failed with Authorization: Bearer eyJhbGciOiJIUzI1NiJ9 and password=SuperSecretPassword123";
        String scrubbed = HeliumDiagnostics.scrubSensitiveData(logMessage);

        assertFalse(scrubbed.contains("eyJhbGciOiJIUzI1NiJ9"));
        assertFalse(scrubbed.contains("SuperSecretPassword123"));
        assertTrue(scrubbed.contains("Bearer [REDACTED]"));
        assertTrue(scrubbed.contains("password=[REDACTED]"));
    }

    @Test
    public void testRecordAndAggregateErrors() {
        mDiagnostics.recordError(HeliumDiagnostics.CAT_NAVIGATION, "ERR_CERT_INVALID", "https://untrusted.com");
        mDiagnostics.recordError(HeliumDiagnostics.CAT_NAVIGATION, "ERR_CERT_INVALID", "https://other.com");
        mDiagnostics.recordError(HeliumDiagnostics.CAT_DATABASE, "SQLITE_BUSY", "db locked");

        assertEquals(2, mDiagnostics.getErrorCount(HeliumDiagnostics.CAT_NAVIGATION, "ERR_CERT_INVALID"));
        assertEquals(1, mDiagnostics.getErrorCount(HeliumDiagnostics.CAT_DATABASE, "SQLITE_BUSY"));
        assertEquals(0, mDiagnostics.getErrorCount(HeliumDiagnostics.CAT_SYNC, "SYNC_CONFLICT"));

        mDiagnostics.clearDiagnostics();
        assertEquals(0, mDiagnostics.getErrorCount(HeliumDiagnostics.CAT_NAVIGATION, "ERR_CERT_INVALID"));
    }
}
