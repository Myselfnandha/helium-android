package net.imput.helium.widget;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * SearchHistoryStore: Local on-device storage for search widget queries,
 * unfinished drafts, and recoverable tab metadata with 90-day retention policies.
 */
public final class SearchHistoryStore {

    private static final String PREF_NAME = "helium_search_widget_history";
    private static final String KEY_QUERIES = "recent_queries_json";
    private static final String KEY_DRAFT = "unfinished_search_draft";
    private static final String KEY_RECOVERY = "tab_recovery_json";

    public static final int DEFAULT_MAX_CAPACITY = 50;
    public static final long RETENTION_PERIOD_MS = 90L * 24 * 60 * 60 * 1000L; // 90 days

    public static final class QueryRecord {
        private final String mQuery;
        private final long mLastUsed;

        public QueryRecord(@NonNull String query, long lastUsed) {
            mQuery = query;
            mLastUsed = lastUsed;
        }

        @NonNull
        public String getQuery() {
            return mQuery;
        }

        public long getLastUsed() {
            return mLastUsed;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("query", mQuery);
            obj.put("last_used", mLastUsed);
            return obj;
        }

        @Nullable
        public static QueryRecord fromJson(@NonNull JSONObject obj) {
            String q = obj.optString("query", "");
            long t = obj.optLong("last_used", 0);
            if (q.isEmpty()) return null;
            return new QueryRecord(q, t);
        }
    }

    public static final class TabRecoveryState {
        private final String mUrl;
        private final String mQuery;
        private final String mTabId;
        private final long mTimestamp;

        public TabRecoveryState(@NonNull String url, @NonNull String query, @Nullable String tabId, long timestamp) {
            mUrl = url;
            mQuery = query;
            mTabId = tabId;
            mTimestamp = timestamp;
        }

        @NonNull
        public String getUrl() { return mUrl; }
        @NonNull
        public String getQuery() { return mQuery; }
        @Nullable
        public String getTabId() { return mTabId; }
        public long getTimestamp() { return mTimestamp; }
    }

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Adds or updates a search query in history, enforcing 50-item capacity and 90-day retention.
     */
    public static synchronized void addQuery(@NonNull Context context, @NonNull String query) {
        String trimmed = query.trim();
        if (trimmed.isEmpty()) return;

        List<QueryRecord> records = loadAndPrune(context);
        long now = System.currentTimeMillis();

        // Deduplicate case-insensitively
        Iterator<QueryRecord> it = records.iterator();
        while (it.hasNext()) {
            if (it.next().getQuery().equalsIgnoreCase(trimmed)) {
                it.remove();
            }
        }

        records.add(0, new QueryRecord(trimmed, now));

        // Enforce max capacity
        if (records.size() > DEFAULT_MAX_CAPACITY) {
            records = records.subList(0, DEFAULT_MAX_CAPACITY);
        }

        saveRecords(context, records);
    }

    /**
     * Returns recent queries, pruned of expired entries (>90 days).
     */
    @NonNull
    public static synchronized List<String> getRecentQueries(@NonNull Context context, int limit) {
        List<QueryRecord> records = loadAndPrune(context);
        int max = Math.min(limit, records.size());
        List<String> result = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            result.add(records.get(i).getQuery());
        }
        return result;
    }

    /**
     * Removes a single query from history.
     */
    public static synchronized void removeQuery(@NonNull Context context, @NonNull String query) {
        List<QueryRecord> records = loadAndPrune(context);
        boolean modified = false;
        Iterator<QueryRecord> it = records.iterator();
        while (it.hasNext()) {
            if (it.next().getQuery().equalsIgnoreCase(query.trim())) {
                it.remove();
                modified = true;
            }
        }
        if (modified) {
            saveRecords(context, records);
        }
    }

    /**
     * Clears all search history and saved drafts.
     */
    public static synchronized void clearAll(@NonNull Context context) {
        getPrefs(context).edit().clear().apply();
    }

    // --- Draft Management (Separate from History) ---

    public static synchronized void saveDraft(@NonNull Context context, @Nullable String draft) {
        if (draft == null || draft.trim().isEmpty()) {
            clearDraft(context);
        } else {
            getPrefs(context).edit().putString(KEY_DRAFT, draft.trim()).apply();
        }
    }

    @Nullable
    public static synchronized String getDraft(@NonNull Context context) {
        String draft = getPrefs(context).getString(KEY_DRAFT, null);
        return (draft != null && !draft.isEmpty()) ? draft : null;
    }

    public static synchronized void clearDraft(@NonNull Context context) {
        getPrefs(context).edit().remove(KEY_DRAFT).apply();
    }

    // --- Tab Recovery State ---

    public static synchronized void saveTabRecovery(
            @NonNull Context context,
            @NonNull String url,
            @NonNull String query,
            @Nullable String tabId) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("url", url);
            obj.put("query", query);
            obj.put("tab_id", (tabId != null) ? tabId : "");
            obj.put("timestamp", System.currentTimeMillis());
            getPrefs(context).edit().putString(KEY_RECOVERY, obj.toString()).apply();
        } catch (JSONException ignored) {}
    }

    @Nullable
    public static synchronized TabRecoveryState getTabRecovery(@NonNull Context context) {
        String raw = getPrefs(context).getString(KEY_RECOVERY, null);
        if (raw == null || raw.isEmpty()) return null;
        try {
            JSONObject obj = new JSONObject(raw);
            String url = obj.optString("url", "");
            String query = obj.optString("query", "");
            String tabId = obj.optString("tab_id", null);
            long ts = obj.optLong("timestamp", 0);
            return new TabRecoveryState(url, query, tabId, ts);
        } catch (JSONException e) {
            return null;
        }
    }

    public static synchronized void clearTabRecovery(@NonNull Context context) {
        getPrefs(context).edit().remove(KEY_RECOVERY).apply();
    }

    // --- Internal Helpers ---

    private static List<QueryRecord> loadAndPrune(Context context) {
        String raw = getPrefs(context).getString(KEY_QUERIES, null);
        List<QueryRecord> list = new ArrayList<>();
        long now = System.currentTimeMillis();

        if (raw != null && !raw.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(raw);
                for (int i = 0; i < arr.length(); i++) {
                    QueryRecord rec = QueryRecord.fromJson(arr.getJSONObject(i));
                    // 90-day retention filter
                    if (rec != null && (now - rec.getLastUsed() <= RETENTION_PERIOD_MS)) {
                        list.add(rec);
                    }
                }
            } catch (JSONException ignored) {}
        }

        // Sort descending by last used
        Collections.sort(list, (a, b) -> Long.compare(b.getLastUsed(), a.getLastUsed()));
        return list;
    }

    private static void saveRecords(Context context, List<QueryRecord> records) {
        JSONArray arr = new JSONArray();
        for (QueryRecord r : records) {
            try {
                arr.put(r.toJson());
            } catch (JSONException ignored) {}
        }
        getPrefs(context).edit().putString(KEY_QUERIES, arr.toString()).apply();
    }
}
