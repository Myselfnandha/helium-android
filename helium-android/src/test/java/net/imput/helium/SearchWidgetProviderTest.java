package net.imput.helium;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.Map;
import net.imput.helium.widget.SearchWidgetProvider;
import net.imput.helium.widget.WidgetPreferencesStore;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for SearchWidgetProvider: Intent actions and widget shortcut configuration.
 */
public class SearchWidgetProviderTest {

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
    public void testActionConstants() {
        assertEquals("net.imput.helium.widget.ACTION_QUICK_SEARCH", SearchWidgetProvider.ACTION_QUICK_SEARCH);
        assertEquals("net.imput.helium.widget.EXTRA_START_VOICE", SearchWidgetProvider.EXTRA_START_VOICE);
        assertEquals("net.imput.helium.widget.EXTRA_OPEN_BOOKMARKS", SearchWidgetProvider.EXTRA_OPEN_BOOKMARKS);
    }

    @Test
    public void testWidgetShortcutToggles() {
        int widgetId = 201;
        assertTrue(WidgetPreferencesStore.isVoiceEnabled(mContext, widgetId));
        assertTrue(WidgetPreferencesStore.isBookmarksEnabled(mContext, widgetId));
        assertFalse(WidgetPreferencesStore.isQrEnabled(mContext, widgetId));

        WidgetPreferencesStore.setVoiceEnabled(mContext, widgetId, false);
        WidgetPreferencesStore.setQrEnabled(mContext, widgetId, true);

        assertFalse(WidgetPreferencesStore.isVoiceEnabled(mContext, widgetId));
        assertTrue(WidgetPreferencesStore.isQrEnabled(mContext, widgetId));
    }
}
