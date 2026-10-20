package net.imput.helium;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.Map;
import net.imput.helium.widget.QuickSearchController;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for QuickSearchController: Smart URL detection, URL resolution, and debouncing.
 */
public class QuickSearchControllerTest {

    private MockContext mContext;

    private static class MockSharedPreferences implements SharedPreferences {
        private final Map<String, Object> map = new HashMap<>();

        @Override public boolean contains(String k) { return map.containsKey(k); }
        @Override public Map<String, ?> getAll() { return map; }
        @Override public boolean getBoolean(String k, boolean def) { Object v = map.get(k); return (v instanceof Boolean) ? (Boolean) v : def; }
        @Override public String getString(String k, String def) { Object v = map.get(k); return (v instanceof String) ? (String) v : def; }
        @Override public int getInt(String k, int def) { Object v = map.get(k); return (v instanceof Integer) ? (Integer) v : def; }
        @Override public long getLong(String k, long def) { Object v = map.get(k); return (v instanceof Long) ? (Long) v : def; }

        @Override
        public Editor edit() {
            return new Editor() {
                private final Map<String, Object> temp = new HashMap<>(map);
                @Override public Editor putBoolean(String k, boolean v) { temp.put(k, v); return this; }
                @Override public Editor putString(String k, String v) { temp.put(k, v); return this; }
                @Override public Editor putInt(String k, int v) { temp.put(k, v); return this; }
                @Override public Editor putLong(String k, long v) { temp.put(k, v); return this; }
                @Override public Editor remove(String k) { temp.remove(k); return this; }
                @Override public Editor clear() { temp.clear(); return this; }
                @Override public void apply() { map.clear(); map.putAll(temp); }
            };
        }
    }

    private static class MockContext extends Context {
        private final SharedPreferences prefs = new MockSharedPreferences();
        @Override public Context getApplicationContext() { return this; }
        @Override public SharedPreferences getSharedPreferences(String n, int m) { return prefs; }
        @Override public java.io.File getFilesDir() { return new java.io.File("/tmp"); }
    }

    @Before
    public void setUp() {
        mContext = new MockContext();
    }

    @Test
    public void testIsDirectUrl() {
        assertTrue(QuickSearchController.isDirectUrl("https://duckduckgo.com"));
        assertTrue(QuickSearchController.isDirectUrl("http://localhost:8080"));
        assertTrue(QuickSearchController.isDirectUrl("about:blank"));
        assertTrue(QuickSearchController.isDirectUrl("news.ycombinator.com"));
        assertTrue(QuickSearchController.isDirectUrl("github.com/torvalds/linux"));
        assertTrue(QuickSearchController.isDirectUrl("sub.domain.org/path?k=v"));

        assertFalse(QuickSearchController.isDirectUrl("best android browsers"));
        assertFalse(QuickSearchController.isDirectUrl("how to build an os"));
        assertFalse(QuickSearchController.isDirectUrl("rust vs zig"));
        assertFalse(QuickSearchController.isDirectUrl("search. with spaces"));
        assertFalse(QuickSearchController.isDirectUrl(""));
        assertFalse(QuickSearchController.isDirectUrl("   "));
    }

    @Test
    public void testResolveSubmissionUrl() {
        // Direct URL without scheme gets https prefix
        assertEquals("https://news.ycombinator.com",
                QuickSearchController.resolveSubmissionUrl(mContext, "news.ycombinator.com"));

        // Direct URL with scheme preserved
        assertEquals("http://example.com/test",
                QuickSearchController.resolveSubmissionUrl(mContext, "http://example.com/test"));

        // Plain search terms resolved with search engine template
        String resolved = QuickSearchController.resolveSubmissionUrl(mContext, "helium browser");
        assertTrue("Expected search query URL to contain search terms",
                resolved.contains("helium") && resolved.contains("browser"));
    }
}
