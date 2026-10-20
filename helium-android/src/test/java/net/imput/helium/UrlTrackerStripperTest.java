package net.imput.helium;

import net.imput.helium.privacy.tracker.UrlTrackerStripper;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for UrlTrackerStripper query parameter stripping.
 */
public class UrlTrackerStripperTest {

    @Test
    public void testStripMarketingTrackingParams() {
        String dirtyUrl = "https://example.com/product?utm_source=google&utm_medium=cpc&utm_campaign=spring&id=123";
        String cleanUrl = UrlTrackerStripper.strip(dirtyUrl);

        assertTrue(cleanUrl.contains("id=123"));
        assertFalse(cleanUrl.contains("utm_source"));
        assertFalse(cleanUrl.contains("utm_medium"));
        assertFalse(cleanUrl.contains("utm_campaign"));
    }

    @Test
    public void testStripSocialAndAdClickIds() {
        String dirtyUrl = "https://news.ycombinator.com/?fbclid=IwAR123&gclid=456&msclkid=789";
        String cleanUrl = UrlTrackerStripper.strip(dirtyUrl);

        assertFalse(cleanUrl.contains("fbclid"));
        assertFalse(cleanUrl.contains("gclid"));
        assertFalse(cleanUrl.contains("msclkid"));
    }

    @Test
    public void testPreserveCleanUrls() {
        String cleanUrl = "https://duckduckgo.com/?q=helium+browser&ia=web";
        assertEquals(cleanUrl, UrlTrackerStripper.strip(cleanUrl));
    }

    @Test
    public void testContainsTrackingParameters() {
        assertTrue(UrlTrackerStripper.containsTrackingParameters("https://example.com/?utm_source=newsletter"));
        assertTrue(UrlTrackerStripper.containsTrackingParameters("https://example.com/?fbclid=abc"));
        assertFalse(UrlTrackerStripper.containsTrackingParameters("https://example.com/?q=search"));
        assertFalse(UrlTrackerStripper.containsTrackingParameters("https://example.com/plain"));
    }
}
