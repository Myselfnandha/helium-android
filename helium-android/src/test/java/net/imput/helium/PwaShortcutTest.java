package net.imput.helium;

import android.content.Context;
import android.content.Intent;
import net.imput.helium.browser.shortcut.PwaShortcutManager;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for PRD M25: Home-Screen Web Applications (PwaShortcutManager).
 */
public class PwaShortcutTest {

    private static class MockContext extends android.content.Context {
        @Override public android.content.Context getApplicationContext() { return this; }
        @Override public android.content.SharedPreferences getSharedPreferences(String n, int m) { return null; }
        @Override public java.io.File getFilesDir() { return new java.io.File("/tmp"); }
        public String getPackageName() { return "net.imput.helium"; }
    }

    @Test
    public void testUrlEligibility() {
        assertTrue(PwaShortcutManager.isEligibleUrl("https://duckduckgo.com"));
        assertTrue(PwaShortcutManager.isEligibleUrl("http://localhost:3000"));
        assertTrue(PwaShortcutManager.isEligibleUrl("HTTPS://EXAMPLE.COM/APP"));

        assertFalse("about:blank must not be eligible for home screen shortcut",
                PwaShortcutManager.isEligibleUrl("about:blank"));
        assertFalse("file:// URLs must not be eligible for security",
                PwaShortcutManager.isEligibleUrl("file:///android_asset/startpage/index.html"));
        assertFalse("javascript: scheme must be strictly rejected",
                PwaShortcutManager.isEligibleUrl("javascript:alert(1)"));
        assertFalse(PwaShortcutManager.isEligibleUrl(null));
        assertFalse(PwaShortcutManager.isEligibleUrl(""));
        assertFalse(PwaShortcutManager.isEligibleUrl("   "));
    }

    @Test
    public void testBuildShortcutIntent() {
        MockContext context = new MockContext();
        String url = "https://news.ycombinator.com";
        String title = "Hacker News";

        Intent intent = PwaShortcutManager.buildShortcutIntent(context, url, title);
        assertNotNull(intent);
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals("net.imput.helium", intent.getPackage());
        assertEquals("https://news.ycombinator.com", intent.getDataString());
        assertEquals(title, intent.getStringExtra("shortcut_title"));
        assertTrue(intent.getBooleanExtra("from_pwa_shortcut", false));
        assertEquals(url, intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_URL));
        assertEquals(title, intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_TITLE));
        assertEquals(PwaShortcutManager.PWA_ACTIVITY_CLASS, intent.getClassName());
    }

    @Test
    public void testBuildShortcutIntentWithThemeColor() {
        MockContext context = new MockContext();
        String url = "https://app.slack.com";
        String title = "Slack";
        String themeColor = "#4A154B";

        Intent intent = PwaShortcutManager.buildShortcutIntent(context, url, title, themeColor);
        assertNotNull(intent);
        assertEquals(themeColor, intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_THEME_COLOR));
        assertEquals(url, intent.getStringExtra(PwaShortcutManager.EXTRA_PWA_URL));
    }

    @Test
    public void testRequestPinShortcutFromManifest() {
        MockContext context = new MockContext();
        net.imput.helium.browser.shortcut.WebAppManifest manifest =
                new net.imput.helium.browser.shortcut.WebAppManifest(
                        "Twitter Lite",
                        "Twitter",
                        "https://mobile.twitter.com",
                        net.imput.helium.browser.shortcut.WebAppManifest.DisplayMode.STANDALONE,
                        "#1DA1F2",
                        "#000000",
                        java.util.Collections.emptyList()
                );

        boolean pinned = PwaShortcutManager.requestPinShortcut(context, manifest, null);
        assertTrue("Eligible PWA manifest should be accepted for pinning", pinned);
    }
}
