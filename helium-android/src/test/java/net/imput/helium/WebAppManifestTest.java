package net.imput.helium;

import net.imput.helium.browser.shortcut.WebAppManifest;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for PRD M25: W3C Web App Manifest parsing & URL resolution.
 */
public class WebAppManifestTest {

    @Test
    public void testParseCompleteManifest() {
        String json = "{\n" +
                "  \"name\": \"Helium Notes\",\n" +
                "  \"short_name\": \"Notes\",\n" +
                "  \"start_url\": \"/notes/app\",\n" +
                "  \"display\": \"standalone\",\n" +
                "  \"theme_color\": \"#0F172A\",\n" +
                "  \"background_color\": \"#020617\",\n" +
                "  \"icons\": [\n" +
                "    { \"src\": \"/icons/icon-48.png\", \"sizes\": \"48x48\", \"type\": \"image/png\" },\n" +
                "    { \"src\": \"/icons/icon-192.png\", \"sizes\": \"192x192\", \"type\": \"image/png\" },\n" +
                "    { \"src\": \"/icons/icon-512.png\", \"sizes\": \"512x512\", \"type\": \"image/png\" }\n" +
                "  ]\n" +
                "}";

        WebAppManifest manifest = WebAppManifest.parse(json, "https://notes.imput.net/index.html");
        assertNotNull(manifest);

        assertEquals("Helium Notes", manifest.getName());
        assertEquals("Notes", manifest.getShortName());
        assertEquals("https://notes.imput.net/notes/app", manifest.getStartUrl());
        assertEquals(WebAppManifest.DisplayMode.STANDALONE, manifest.getDisplay());
        assertTrue(manifest.isStandalone());
        assertEquals("#0F172A", manifest.getThemeColor());
        assertEquals("#020617", manifest.getBackgroundColor());

        assertEquals(3, manifest.getIcons().size());
        assertEquals("https://notes.imput.net/icons/icon-48.png", manifest.getIcons().get(0).getSrc());

        WebAppManifest.ManifestIcon best = manifest.getBestIcon();
        assertNotNull(best);
        assertEquals("https://notes.imput.net/icons/icon-512.png", best.getSrc());
        assertEquals(512, best.getSizePixels());
    }

    @Test
    public void testRelativeUrlResolution() {
        assertEquals("https://app.com/dashboard",
                WebAppManifest.resolveUrl("https://app.com/login", "/dashboard"));
        assertEquals("https://app.com/sub/feed",
                WebAppManifest.resolveUrl("https://app.com/sub/", "feed"));
        assertEquals("https://cdn.other.com/icon.png",
                WebAppManifest.resolveUrl("https://app.com/", "https://cdn.other.com/icon.png"));
    }

    @Test
    public void testDisplayModeFallbacks() {
        assertEquals(WebAppManifest.DisplayMode.STANDALONE, WebAppManifest.DisplayMode.fromString("standalone"));
        assertEquals(WebAppManifest.DisplayMode.FULLSCREEN, WebAppManifest.DisplayMode.fromString("fullscreen"));
        assertEquals(WebAppManifest.DisplayMode.MINIMAL_UI, WebAppManifest.DisplayMode.fromString("minimal-ui"));
        assertEquals(WebAppManifest.DisplayMode.BROWSER, WebAppManifest.DisplayMode.fromString("browser"));
        assertEquals(WebAppManifest.DisplayMode.BROWSER, WebAppManifest.DisplayMode.fromString("unknown-mode"));
        assertEquals(WebAppManifest.DisplayMode.BROWSER, WebAppManifest.DisplayMode.fromString(null));
    }

    @Test
    public void testMalformedOrNullManifest() {
        assertNull(WebAppManifest.parse(null, "https://example.com"));
        assertNull(WebAppManifest.parse("   ", "https://example.com"));
        assertNull(WebAppManifest.parse("not-json", "https://example.com"));
    }
}
