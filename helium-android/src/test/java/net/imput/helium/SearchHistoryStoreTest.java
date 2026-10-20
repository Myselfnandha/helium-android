package net.imput.helium;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.imput.helium.widget.SearchHistoryStore;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for SearchHistoryStore: Recent queries, retention policy, drafts, and tab recovery.
 */
public class SearchHistoryStoreTest {

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
        SearchHistoryStore.clearAll(mContext);
    }

    @Test
    public void testAddAndRetrieveQueries() {
        SearchHistoryStore.addQuery(mContext, "duckduckgo");
        SearchHistoryStore.addQuery(mContext, "helium browser");
        SearchHistoryStore.addQuery(mContext, "android 14 pwa");

        List<String> recents = SearchHistoryStore.getRecentQueries(mContext, 10);
        assertEquals(3, recents.size());
        assertEquals("android 14 pwa", recents.get(0)); // Most recent first
        assertEquals("helium browser", recents.get(1));
        assertEquals("duckduckgo", recents.get(2));
    }

    @Test
    public void testDeduplicationAndPromotion() {
        SearchHistoryStore.addQuery(mContext, "github");
        SearchHistoryStore.addQuery(mContext, "reddit");
        SearchHistoryStore.addQuery(mContext, "github"); // Promoted to top

        List<String> recents = SearchHistoryStore.getRecentQueries(mContext, 10);
        assertEquals(2, recents.size());
        assertEquals("github", recents.get(0));
        assertEquals("reddit", recents.get(1));
    }

    @Test
    public void testCapacityLimit50() {
        for (int i = 0; i < 60; i++) {
            SearchHistoryStore.addQuery(mContext, "query_" + i);
        }

        List<String> recents = SearchHistoryStore.getRecentQueries(mContext, 100);
        assertEquals(SearchHistoryStore.DEFAULT_MAX_CAPACITY, recents.size());
        assertEquals("query_59", recents.get(0));
    }

    @Test
    public void testDraftIsolationFromSubmittedHistory() {
        SearchHistoryStore.saveDraft(mContext, "unfinished typi");

        // Draft should not appear in submitted history
        List<String> recents = SearchHistoryStore.getRecentQueries(mContext, 10);
        assertTrue(recents.isEmpty());

        assertEquals("unfinished typi", SearchHistoryStore.getDraft(mContext));

        SearchHistoryStore.clearDraft(mContext);
        assertNull(SearchHistoryStore.getDraft(mContext));
    }

    @Test
    public void testTabRecoveryMetadata() {
        SearchHistoryStore.saveTabRecovery(mContext, "https://wikipedia.org", "!w quantum", "tab-9988");

        SearchHistoryStore.TabRecoveryState recovery = SearchHistoryStore.getTabRecovery(mContext);
        assertNotNull(recovery);
        assertEquals("https://wikipedia.org", recovery.getUrl());
        assertEquals("!w quantum", recovery.getQuery());
        assertEquals("tab-9988", recovery.getTabId());
        assertTrue(recovery.getTimestamp() > 0);

        SearchHistoryStore.clearTabRecovery(mContext);
        assertNull(SearchHistoryStore.getTabRecovery(mContext));
    }

    @Test
    public void testRemoveIndividualQueryAndClearAll() {
        SearchHistoryStore.addQuery(mContext, "alpha");
        SearchHistoryStore.addQuery(mContext, "beta");
        SearchHistoryStore.addQuery(mContext, "gamma");

        SearchHistoryStore.removeQuery(mContext, "beta");
        List<String> recents = SearchHistoryStore.getRecentQueries(mContext, 10);
        assertEquals(2, recents.size());
        assertFalse(recents.contains("beta"));

        SearchHistoryStore.clearAll(mContext);
        assertTrue(SearchHistoryStore.getRecentQueries(mContext, 10).isEmpty());
    }
}
