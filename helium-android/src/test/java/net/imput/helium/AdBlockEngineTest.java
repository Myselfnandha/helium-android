package net.imput.helium;

import net.imput.helium.adblock.AdBlockEngine;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests verifying AdBlockEngine filtering logic and metrics.
 */
public class AdBlockEngineTest {

    private AdBlockEngine mEngine;

    @Before
    public void setUp() {
        mEngine = AdBlockEngine.getInstance();
        mEngine.resetBlockedCount();
    }

    @Test
    public void testHostExtraction() {
        assertEquals("example.com", AdBlockEngine.extractHost("https://example.com/test?q=1"));
        assertEquals("sub.domain.org", AdBlockEngine.extractHost("http://sub.domain.org:8080/path"));
        assertNull(AdBlockEngine.extractHost("invalid-url-without-scheme"));
    }

    @Test
    public void testAdDomainBlocking() {
        assertTrue(mEngine.isHostBlocked("doubleclick.net"));
        assertTrue(mEngine.isHostBlocked("adservice.google.com"));
        assertTrue(mEngine.isHostBlocked("pagead2.googlesyndication.com"));
        assertTrue(mEngine.isHostBlocked("pixel.facebook.com"));
        assertTrue(mEngine.isHostBlocked("scorecardresearch.com"));
        assertTrue(mEngine.isHostBlocked("amazon-adsystem.com"));

        assertFalse(mEngine.isHostBlocked("wikipedia.org"));
        assertFalse(mEngine.isHostBlocked("duckduckgo.com"));
        assertFalse(mEngine.isHostBlocked("github.com"));
    }

    @Test
    public void testUrlFilterAndMetrics() {
        long initialCount = mEngine.getBlockedCount();

        boolean blocked = mEngine.shouldBlock("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js");
        assertTrue(blocked);
        assertEquals(initialCount + 1, mEngine.getBlockedCount());

        boolean allowed = mEngine.shouldBlock("https://en.wikipedia.org/wiki/Main_Page");
        assertFalse(allowed);
        assertEquals(initialCount + 1, mEngine.getBlockedCount());
    }

    @Test
    public void testInternalSchemeBypass() {
        assertFalse(mEngine.shouldBlock("file:///android_asset/startpage/index.html"));
        assertFalse(mEngine.shouldBlock("about:blank"));
    }

    @Test
    public void testCustomBlockedRule() {
        String customAd = "custom-tracker-test.xyz";
        assertFalse(mEngine.isHostBlocked(customAd));

        mEngine.addCustomBlockedHost(customAd);
        assertTrue(mEngine.isHostBlocked(customAd));
        assertTrue(mEngine.isHostBlocked("tracker." + customAd));

        mEngine.removeCustomBlockedHost(customAd);
        assertFalse(mEngine.isHostBlocked(customAd));
    }
}
