package net.imput.helium;

import net.imput.helium.privacy.https.HttpsUpgrader;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for HttpsUpgrader automatic scheme upgrading.
 */
public class HttpsUpgraderTest {

    @Test
    public void testUpgradeInsecureHttp() {
        HttpsUpgrader upgrader = new HttpsUpgrader(true);

        assertTrue(upgrader.shouldUpgrade("http://example.com/test"));
        assertEquals("https://example.com/test", upgrader.upgrade("http://example.com/test"));

        // Already secure
        assertFalse(upgrader.shouldUpgrade("https://example.com"));
        assertEquals("https://example.com", upgrader.upgrade("https://example.com"));
    }

    @Test
    public void testLocalNetworkExclusion() {
        HttpsUpgrader upgrader = new HttpsUpgrader(true);

        assertFalse(upgrader.shouldUpgrade("http://localhost:8080"));
        assertFalse(upgrader.shouldUpgrade("http://127.0.0.1:3000"));
        assertFalse(upgrader.shouldUpgrade("http://192.168.1.100/admin"));
        assertFalse(upgrader.shouldUpgrade("http://10.0.0.1"));
    }

    @Test
    public void testDisabledMode() {
        HttpsUpgrader upgrader = new HttpsUpgrader(false);

        assertFalse(upgrader.shouldUpgrade("http://example.com"));
        assertEquals("http://example.com", upgrader.upgrade("http://example.com"));
    }
}
